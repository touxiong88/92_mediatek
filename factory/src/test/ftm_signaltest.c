/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

#include <ctype.h>
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <dirent.h>
#include <fcntl.h>
#include <pthread.h>
#include <sys/mount.h>
#include <sys/statfs.h>

#include <termios.h> /* POSIX terminal control definitions */


#include "common.h"
#include "miniui.h"
#include "ftm.h"
#include "utils.h"

//#ifdef FEATURE_FTM_SIGNALTEST

#define TAG "[Signal Test] "

#define SLEEPMODE "AT+ESLP=0"
#define EMER_CALL "ATD112;"
#define GET_SN "AT+EGMR=0,5"
// Set SN should be "AT+EGMR=1,5,'SN'"
#define SET_SN "AT+EGMR=1,5"
#define AT "AT"
#define ATE0 "ATE0"
#define ATH "ATH"
#define BUF_SIZE 128

int g_mdFlag=1;

enum {
    ITEM_PASS,
    ITEM_FAIL,
};


static item_t sigtest_items[] = {
    item(-1, NULL),
};

struct sigtest {  
    char         info[1024];
    bool         exit_thd;
    int 	 fd_atmd;

	int fd_atmd2;

    int fd_atmd_dt;

    text_t title;
    text_t text;
    
    pthread_t update_thd;
    struct ftm_module *mod;
    struct itemview *iv;
};

#define mod_to_sigtest(p)  (struct sigtest*)((char*)(p) + sizeof(struct ftm_module))
#define FREEIF(p)   do { if(p) free(p); (p) = NULL; } while(0)
#define HALT_INTERVAL 50000


pthread_t g_AP_UART_USB_RX;
pthread_t g_AP_CCCI_RX;



int g_fd_atcmd = -1, g_fd_uart = -1;
int g_fd_atcmdmd2=-1;
int g_fd_atcmdmd_dt = -1;

int g_hUsbComPort = -1;


int AT_Pre_Process (char *buf_cmd)
{
	#ifdef MTK_DT_SUPPORT
		if(!strcmp(buf_cmd, "AT+SWITCH"))
		{
            LOGD(TAG "AT+SWITCH");	
			if(g_mdFlag==1)
				g_mdFlag=2;
			else if(g_mdFlag==2)
				g_mdFlag=1;
			else
				LOGD(TAG "Unsupported MD Flag\n");
        }
	#endif
	return 0;
}

int ACK_Pre_Process (char *buf_cmd)
{
	if (strstr(buf_cmd, "RING"))
	{
		LOGD(TAG "MT call: RING\n");
		send_at(g_fd_atcmd, "ATA\r");
		wait4_ack (g_fd_atcmd, NULL, 1000);
	}
	return 0;
}


void * AP_UART_USB_RX (void* lpParameter)
{
	char buf_cmd [BUF_SIZE] = {0};
	int len;
    int wr_len;
    char result[64] = {0};
	
	LOGD(TAG "Enter AP_UART_USB_RX()\n");
    cmd_handler_init ();

	for (;;)
	{
		len = read_a_line(g_fd_uart, buf_cmd, BUF_SIZE);            
		if (len>0)
		{
			buf_cmd[len] = '\0';
        	LOGD(TAG "AP_UART_USB_RX Command: %s, Len: %d\n" ,buf_cmd, len);
			AT_Pre_Process (buf_cmd);
            LOGD("buf_cmd:%s\n", buf_cmd);
#if 0
			#ifdef MTK_DT_SUPPORT
				
				if(g_mdFlag==1)
			send_at (g_fd_atcmd, buf_cmd);
				if(g_mdFlag==2)
					send_at (g_fd_atcmdmd2, buf_cmd);
			#else
				send_at (g_fd_atcmd, buf_cmd);
			#endif
#endif

            CMD_OWENR_ENUM owner = rmmi_cmd_processor(buf_cmd, result);
            LOGD(TAG "result:%s\n", result);
            if(owner == CMD_OWENR_AP)
            {
                wr_len = write_chars (g_fd_uart, result, strlen(result));
			    if (wr_len != strlen(result))
				LOGE(TAG "AP_CCCI_RX: wr_len != rd_len\n");
            }
            else
            {

			if(g_mdFlag == 1)
			{
				#if defined(MTK_EXTERNAL_MODEM_SLOT)
				if(!strcmp(MTK_EXTERNAL_MODEM_SLOT, "1"))
				{
					#ifndef EVDO_DT_SUPPORT
						send_at(g_fd_atcmdmd_dt, buf_cmd);
					#endif
				}
				else{

                    if(is_support_modem(1)){
						send_at(g_fd_atcmd, buf_cmd);

					}else if(is_support_modem(2)){
					    send_at(g_fd_atcmdmd2, buf_cmd);

                    }
				}
				#else
				

					if(is_support_modem(1)){
						send_at(g_fd_atcmd, buf_cmd);

                    }else if(is_support_modem(2)){
					    send_at(g_fd_atcmdmd2, buf_cmd);

                    }
				#endif
			}
		    else if(g_mdFlag == 2)
		    {
				#if defined(MTK_EXTERNAL_MODEM_SLOT)
				if(!strcmp(MTK_EXTERNAL_MODEM_SLOT, "2"))
				{
					#ifndef EVDO_DT_SUPPORT
						send_at(g_fd_atcmdmd_dt, buf_cmd);
					#endif
				}else{

					if(is_support_modem(1)){
						send_at(g_fd_atcmd, buf_cmd);

                    }else if(is_support_modem(2)){
					    send_at(g_fd_atcmdmd2, buf_cmd);

                    }
				}
				#else
				

					if(is_support_modem(1)){
						send_at(g_fd_atcmd, buf_cmd);

                    }else if(is_support_modem(2)){
					    send_at(g_fd_atcmdmd2, buf_cmd);

                    }
				#endif
			}
		}
 	}
 	}

	//pthread_exit (NULL);
}
void * AP_CCCI_RX (void* lpParameter)
{
	char buf_ack [BUF_SIZE] = {0};
	char buf_log [BUF_SIZE] = {0};
	int rd_len=0, wr_len=0;
	
	LOGD(TAG "Enter AP_CCCI_RX()\n");
	
	for (;;)
	{

if(g_mdFlag == 1)
					{
			#if defined(MTK_EXTERNAL_MODEM_SLOT)
			if(!strcmp(MTK_EXTERNAL_MODEM_SLOT, "1"))
			{
				#ifndef EVDO_DT_SUPPORT
					rd_len = read_ack(g_fd_atcmdmd_dt, buf_ack, BUF_SIZE);
				#endif
			}
			else
			{

				if(is_support_modem(1)){
					rd_len = read_ack(g_fd_atcmd, buf_ack, BUF_SIZE);

                }else if(is_support_modem(2)){
					rd_len = read_ack(g_fd_atcmdmd2, buf_ack, BUF_SIZE);

                }
			}
			#else

				if(is_support_modem(1)){
					rd_len = read_ack(g_fd_atcmd, buf_ack, BUF_SIZE);

                }else if(is_support_modem(2)){
					rd_len = read_ack(g_fd_atcmdmd2, buf_ack, BUF_SIZE);

                }
			#endif
		}
		else if(g_mdFlag == 2)
		{
			#if defined(MTK_EXTERNAL_MODEM_SLOT)
			if(!strcmp(MTK_EXTERNAL_MODEM_SLOT, "2"))
			{
				#ifndef EVDO_DT_SUPPORT
					rd_len = read_ack(g_fd_atcmdmd_dt, buf_ack, BUF_SIZE);
				#endif
			}
			else
			{

				if(is_support_modem(1)){
					rd_len = read_ack(g_fd_atcmd, buf_ack, BUF_SIZE);

                }else if(is_support_modem(2)){
					rd_len = read_ack(g_fd_atcmdmd2, buf_ack, BUF_SIZE);

                }
			}
			#else
			

				if(is_support_modem(1)){
					rd_len = read_ack(g_fd_atcmd, buf_ack, BUF_SIZE);

                }else if(is_support_modem(2)){
					rd_len = read_ack(g_fd_atcmdmd2, buf_ack, BUF_SIZE);

                }
			#endif
		}
		
		if (rd_len>0)
		{
			memcpy (buf_log, buf_ack, rd_len);
			buf_log[rd_len] = '\0';
			LOGD(TAG "AP_CCCI_RX: %s, rd_len = %d\n", buf_log, rd_len);

			ACK_Pre_Process(buf_log);
			
			wr_len = write_chars (g_fd_uart, buf_ack, rd_len);
			if (wr_len != rd_len)
				LOGE(TAG "AP_CCCI_RX: wr_len != rd_len\n");
		}
	}
	//pthread_exit(NULL);
}



void New_Thread ()
{
	LOGD(TAG "Enter New_Thread() \n");
	if(pthread_create (&g_AP_UART_USB_RX, NULL, AP_UART_USB_RX, NULL) != 0)
	{
		LOGD (TAG "main:Create AP_UART_USB_RX thread failed");
		return;
	}

	if(pthread_create (&g_AP_CCCI_RX, NULL, AP_CCCI_RX, NULL) != 0)
	{
		LOGD (TAG "main:Create AP_CCCI_RX thread failed");
		return;
	}
	LOGD(TAG "Exit New_Thread() \n");
}

int Wait_Thread (pthread_t arg)
{   /* exit thread by pthread_kill -> pthread_join*/
    int err;
    if ((err = pthread_kill(arg, SIGUSR1)))
        return err;

    if ((err = pthread_join(arg, NULL)))
        return err;
    return 0;
}

int Free_Thread ()
{   /* exit thread by pthread_kill -> pthread_join*/
    int err;
    if ( 0 != (err = Wait_Thread(g_AP_UART_USB_RX)) )
        return err;

    if ( 0 != (err = Wait_Thread(g_AP_CCCI_RX)) )
        return err;

    return 0;
}


int sigtest_entry(struct ftm_param *param, void *priv)
{
    char *ptr;
    int chosen, i;
    bool exit = false;
    struct sigtest *st = (struct sigtest *)priv;
    struct itemview *iv;
    int ret;
    const char *pret;
	int test_result_temp = FTM_TEST_PASS;

    LOGD(TAG "%s\n", __FUNCTION__);


if(is_support_modem(1)){
	st->fd_atmd = openDeviceWithDeviceName(CCCI_MODEM_MT6575);
    if (-1 == st->fd_atmd) {
				LOGD(TAG "Fail to open CCCI interface\n");
				return 0;
    }
	for (i = 0; i<30; i++) usleep(50000); //sleep 1s wait for modem bootup

}

if(is_support_modem(2)){
	st->fd_atmd2 = openDeviceWithDeviceName(CCCI_MODEM2);
    if (-1 == st->fd_atmd2) {
				LOGD(TAG "Fail to open CCCI interface\n");
				return 0;
    }
	for (i = 0; i<30; i++) usleep(50000); //sleep 1s wait for modem bootup

}
#if defined(MTK_EXTERNAL_MODEM_SLOT) && !defined(EVDO_DT_SUPPORT)
        #if defined(PURE_AP_USE_EXTERNAL_MODEM)
            st->fd_atmd_dt = openDeviceWithDeviceName("/dev/ttyUSB1");
        #else
	        st->fd_atmd_dt = openDeviceWithDeviceName("/dev/ttyMT0");
        #endif
		//st->fd_atmd_dt= openDeviceWithDeviceName(CCCI_MODEM_MT6252);
		if (-1 == st->fd_atmd_dt) {
					LOGD(TAG "Fail to open CCCI interface\n");
					return 0;
		}
	for (i = 0; i<30; i++) usleep(50000); //sleep 1s wait for modem bootup
#endif


for (i = 0; i<50; i++) usleep(50000); //sleep 1s wait for modem bootup

if(is_support_modem(1)){
	ExitFlightMode (st->fd_atmd, TRUE);
}


if(is_support_modem(2)){
	ExitFlightMode_DualTalk (st->fd_atmd2, TRUE);
}

#if defined(MTK_EXTERNAL_MODEM_SLOT) && !defined(EVDO_DT_SUPPORT)
	ExitFlightMode_DualTalk(st->fd_atmd_dt, TRUE);
#endif

    for (i = 0; i<30; i++) usleep(50000); //sleep 1s wait for modem bootup


    init_text(&st->title, param->name, COLOR_YELLOW);
    init_text(&st->text, &st->info[0], COLOR_YELLOW);   

    //sigtest_update_info(st, st->info);
//    sprintf(st->info, "%s", uistr_info_emergency_call_not_start);
    memset(&st->info[0], 0, sizeof(st->info));
    sprintf(st->info, "%s\n", uistr_info_emergency_call_testing);
    st->exit_thd = false;

    if (!st->iv) {
        iv = ui_new_itemview();
        if (!iv) {
            LOGD(TAG "No memory");
            return -1;
        }
        st->iv = iv;
    }
    
    iv = st->iv;
    iv->set_title(iv, &st->title);
    iv->set_items(iv, sigtest_items, 0);
    iv->set_text(iv, &st->text); 
	iv->start_menu(iv,0);

	iv->redraw(iv);


if(is_support_modem(1)){
	LOGD(TAG "Come to EMG Call IN MTK_ENABLE_MD1\n");
	pret = dial112(st->fd_atmd);
	if(!strcmp(pret, "OK"))
	{
		LOGD(TAG "Dial 112 Success\n");
		sprintf(st->info, "%s\n", uistr_info_emergency_call_success_in_modem1);
		st->mod->test_result = FTM_TEST_PASS;
		test_result_temp = FTM_TEST_PASS;
	}
	else 
	{
		LOGD(TAG "Dial 112 Fail IN MTK_ENABLE_MD1\n");
		sprintf(st->info, "%s\n", uistr_info_emergency_call_fail_in_modem1);
		st->mod->test_result = FTM_TEST_FAIL;
		test_result_temp = FTM_TEST_FAIL;
	}
	ExitFlightMode (st->fd_atmd, FALSE);
    closeDevice(st->fd_atmd);
    iv->redraw(iv);
}


if(is_support_modem(2)){
	LOGD(TAG "Come to EMG Call\n");
		pret = dial112(st->fd_atmd2);
		if(!strcmp(pret, "OK"))
		{
			LOGD(TAG "Dial 112 Success\n");
			sprintf(st->info, "%s\n", uistr_info_emergency_call_success_in_modem2);
			if(test_result_temp == FTM_TEST_PASS)
			{
				st->mod->test_result = FTM_TEST_PASS;
			}
			else
			{
				st->mod->test_result = FTM_TEST_FAIL;
			}
		}
		else 
		{
			LOGD(TAG "Dial 112 Fail in MTK_ENABLE_MD2\n");
			sprintf(st->info, "%s\n", uistr_info_emergency_call_fail_in_modem2);
			st->mod->test_result = FTM_TEST_FAIL;
		}
		ExitFlightMode (st->fd_atmd2, FALSE);
		
	closeDevice (st->fd_atmd2);
    iv->redraw(iv);
}


#if defined(MTK_EXTERNAL_MODEM_SLOT) && !defined(EVDO_DT_SUPPORT)
		LOGD(TAG "Come to EMG Call\n");
			pret = dial112(st->fd_atmd_dt);
			if(!strcmp(pret, "OK"))
			{
				LOGD(TAG "Dial 112 Success\n");
				sprintf(st->info, "%s\n", uistr_info_emergency_call_success_in_modem2);
				if(test_result_temp == FTM_TEST_PASS)
			    {
	    			st->mod->test_result = FTM_TEST_PASS;
		    	}
		    	else
		    	{
				    st->mod->test_result = FTM_TEST_FAIL;
		    	}
			}
			else 
			{
				LOGD(TAG "Dial 112 Fail in MTK_ENABLE_MD2\n");
				sprintf(st->info, "%s\n", uistr_info_emergency_call_fail_in_modem2);
				st->mod->test_result = FTM_TEST_FAIL;
			}
			ExitFlightMode (st->fd_atmd_dt, FALSE);
			
		closeDevice (st->fd_atmd_dt);
        iv->redraw(iv);
#endif

	

    return 0;
}

struct ftm_module* sigtest_init(void)
{
    int ret;
    struct ftm_module *mod;
    struct sigtest *st;

    LOGD(TAG "%s\n", __FUNCTION__);
    
    mod = ftm_alloc(ITEM_SIGNALTEST, sizeof(struct sigtest));
    st  = mod_to_sigtest(mod);

    st->mod = mod;
    
    if (!mod)
        return NULL;

    ret = ftm_register(mod, sigtest_entry, (void*)st);

    if(ret == 0) {
        LOGD(TAG "ftm_register success!\n");
        return mod;
    }
    else {
        LOGD(TAG "ftm_register fail!\n");
        return NULL;
    }
            
}

/// The below is for ATE signal test.
void ate_signal(void)
{
    struct itemview ate;
    text_t ate_title, info;
    char buf[100];
    char buf_cmd[100];
    char buf_ret[100];
    int len, i;
    const char *pret;


	int flag=0;
    
    ui_init_itemview(&ate);
    init_text(&info, buf, COLOR_YELLOW);
    ate.set_text(&ate, &info);
    len = sprintf(buf, "%s", "ATE Signaling Test\nEmergency call is not started\n");
    //sprintf(buf+len, "%s", "Emergency call is not started\n");
    ate.redraw(&ate);


if(is_support_modem(1)){
	if(-1 == COM_Init (&g_fd_atcmd, &g_fd_uart, &g_hUsbComPort))
	{
		LOGE(TAG "COM_Init init fail!\n");
		return;
	}
	g_fd_uart = g_hUsbComPort;
}
	
	
if(is_support_modem(2)){
	if(g_fd_uart != -1 && g_hUsbComPort != -1)
	{
	    g_fd_atcmdmd2 = openDeviceWithDeviceName("/dev/ccci2_tty0");
	if(g_fd_atcmdmd2 == -1)
	{
		LOGE(TAG "Open md2 fail\r\n");
		return;
	}
	}
	else
	{
	    if(-1 == COM_Init (&g_fd_atcmdmd2, &g_fd_uart, &g_hUsbComPort))
	    {
	    	LOGE(TAG "COM_Init init fail!\n");
	    	return;
	    }
	g_fd_uart = g_hUsbComPort;

    }
}

	
#if defined(MTK_DT_SUPPORT) && !defined(EVDO_DT_SUPPORT)
		if(g_fd_uart != -1 && g_hUsbComPort != -1)
		{
			g_fd_atcmdmd_dt= openDeviceWithDeviceName("/dev/ttyMT0");
			if(g_fd_atcmdmd_dt== -1)
			{
				LOGE(TAG "Open md2 fail\r\n");
				return;
			}
		}
		else
		{
			if(-1 == COM_Init (&g_fd_atcmdmd2, &g_fd_uart, &g_hUsbComPort))
			{
				LOGE(TAG "COM_Init init fail!\n");
				return;
			}
			g_fd_uart = g_hUsbComPort;
		}
#endif

	
	for (i = 0; i<30; i++) usleep(50000); //sleep 1s wait for modem bootup
	LOGD(TAG "fd_atcmd = %d,  fd_uart = %d, hUsbComPort = %d\r\n", g_fd_atcmd, g_fd_uart, g_hUsbComPort);

	if(is_support_modem(1)){
	ExitFlightMode (g_fd_atcmd, TRUE);
    }


	if(is_support_modem(2)){
	ExitFlightMode_DualTalk (g_fd_atcmdmd2, TRUE);
    }

	#if defined(MTK_DT_SUPPORT) && !defined(EVDO_DT_SUPPORT)
	ExitFlightMode_DualTalk(g_fd_atcmdmd_dt, TRUE);
	#endif
	New_Thread ();

    while(1) {

         usleep(HALT_INTERVAL*20);
    }
	
	Free_Thread ();

	if(is_support_modem(1)){
	COM_DeInit (&g_fd_atcmd, &g_fd_uart, &g_hUsbComPort);

	    if(is_support_modem(2)){
		    if(g_fd_atcmdmd2 != -1)
		    {
		        close(g_fd_atcmdmd2);
		    	g_fd_atcmdmd2 = -1;
		    }
       }


		#if defined(MTK_DT_SUPPORT) && !defined(EVDO_DT_SUPPORT)
			if(g_fd_atcmdmd_dt != -1)
			{
				close(g_fd_atcmdmd_dt);
				g_fd_atcmdmd_dt = -1;
			}
		#endif

     }else if(is_support_modem(2)){
		COM_DeInit(&g_fd_atcmdmd2, &g_fd_uart, &g_hUsbComPort);

     }
	return;

}
//#endif
