/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
#include <ctype.h>
#include <errno.h>
#include <fcntl.h>
#include <getopt.h>
#include <limits.h>
#include <linux/input.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/reboot.h>
#include <sys/types.h>
#include <time.h>
#include <unistd.h>
#include <cutils/properties.h>
#include <unistd.h>
#include <pthread.h>


#include "common.h"
#include "ftm.h"
#include "miniui.h"
#include "utils.h"

//#include "libnvram.h"
//#include "CFG_file_info_custom.h"

#include "item.h"


#define TAG        "[FACTORY] "

#define FTM_CUST_FILE1  "/sdcard/factory.ini"
#define FTM_CUST_FILE2  "/etc/factory.ini"

#define START "AT+START"
#define STOP "AT+STOP"
#define REQUEST_DATA "AT+REQUESTDATA"
#define VERSION "AT+VERSION"
#define READ_BARCODE "AT+READBARCODE"
#define WRITE_BARCODE "AT+WRITEBARCODE"
#define VIBRATOR_ENABLE "/sys/class/timed_output/vibrator/enable"


// add for idle current auto test
int usb_com_port = -1;
int usb_status = 0;
int usb_plug_in = 1;

int display_version(int autoreturn);

pthread_mutex_t at_command_mutex = PTHREAD_MUTEX_INITIALIZER;


static item_t ftm_menu_items[] = {
    //item(ITEM_MUI_TEST,"Mini-UI Test"),
    item(ITEM_AUTO_TEST, uistr_auto_test),
    item(ITEM_FULL_TEST, uistr_full_test),
    item(ITEM_ITEM_TEST, uistr_item_test),
    item(ITEM_REPORT,    uistr_test_report),
    item(ITEM_DEBUG_TEST,uistr_debug_test),
#ifdef FEATURE_FTM_CLEARFLASH
    item(ITEM_CLRFLASH,  uistr_clear_flash),
#endif
#ifdef FEATURE_FTM_CLEAREMMC
    item(ITEM_CLREMMC,  uistr_clear_emmc),
#endif
    item(ITEM_VERSION,   uistr_version),
    item(ITEM_REBOOT,    uistr_reboot),
    item(ITEM_MAX_IDS,   NULL),
};

extern item_t ftm_auto_test_items[];
extern item_t pc_control_items[]; 
extern item_t ftm_debug_test_items[];
extern item_t ftm_test_items[];
extern item_t ftm_cust_items[ITEM_MAX_IDS];
extern item_t ftm_cust_auto_items[ITEM_MAX_IDS];


char at_command[128] = {0};
#ifdef FEATURE_FTM_VIBRATOR
extern bool vibrator_test_exit;
#endif
#ifdef FEATURE_FTM_LED
extern bool keypadled_test_exit;
extern bool led_test_exit;
#endif
#ifdef FEATURE_FTM_AUDIO
extern bool bMicbias_exit;
#endif

//add for saving test report
enum {
    TEST_REPORT_UNTEST,
	TEST_REPORT_PASS,
	TEST_REPORT_FAIL,
};

int g_nr_lines = 0;
char test_data[128];
int status = 0;


sp_ata_data return_data;
// add for idle current auto test


static int get_AT_command(char *origin_at_command)
{
    char *ptr = NULL;
	char *p;
	char *temp_at_command = origin_at_command;
	int result = 0;
	int i = 0;
	int len = strlen(origin_at_command);
	p = origin_at_command;
	ptr = strchr(temp_at_command, '=');
	if(ptr == NULL)
	{
	    LOGD(TAG "ptr is null\n");
        pthread_mutex_lock (&at_command_mutex);
		strcpy(at_command, origin_at_command);
        pthread_mutex_unlock (&at_command_mutex);
		result = 0;
	}
	else
	{
		if(!strncmp(++ptr, "CLOSE", strlen("CLOSE")))
		{
			*(--ptr) = '\0';

			result = 1;
		}
		else
		{
	    	for (i = 0; i < len; i++, p++)
			{
           		if ((*p == '+') && ((i + 1) < len))
				{
                *p = '\0';
                break;
            }
        }
	    LOGD(TAG "ptr is not null\n");
//		strcpy(at_command, ++p);
        *(--ptr) = '\0';
		ftm_set_prop(++p, ++ptr);
			result = 2;
        }

	}
//    LOGD(TAG "%s\n");
	return result;
}


//add for saving test report
int write_test_report(item_t *items, FILE *fp)
{

    int i = 0, test_report_len = 0, write_result = -1;
	char test_report[1024] = {0};
	char *get_test_report = test_report;
	char result[] = { ' ', 'O', 'X' };
	int state = 0;

    while (i < ITEM_MAX_IDS && items->name) {

//		LOGD(TAG "items.name=%s item.background=%d", items->name, items->background);
		if(items->background == 0)
		{
            state = TEST_REPORT_UNTEST;
		}
		else if (items->background == COLOR_GREEN)
		{
            state = TEST_REPORT_PASS;
		}
		else if (items->background == COLOR_RED)
		{
            state = TEST_REPORT_FAIL;
		}
//		LOGD(TAG "state = %d", state);
        if(strncmp(items->name, uistr_info_test_report_back, strlen(uistr_info_test_report_back)))
        {
		    get_test_report = test_report + test_report_len;
            test_report_len += snprintf(get_test_report, 40, "%s=%c\n", items->name+4,
				result[state]);
        }
//		LOGD(TAG "%s", get_test_report);
//	    LOGD(TAG "%s", test_report);

        i++;
        items++;
    }

    LOGD(TAG "before write");
	LOGD(TAG "%s", test_report);
    write_result = fputs(test_report, fp);
	LOGD(TAG "The result of fputs is %d", write_result);

	return 0;
}



int get_is_ata(){
    LOGD(TAG "status........................... = %d\n", status);
    return status;
}

static int item_test_report(item_t *items, char *buf, int bufsz)
{
    int    num;
    int    chosen_item;
    bool   quit;
    struct itemview triv; /* test report item view */
    item_t rpt_items[ITEM_MAX_IDS + 1];
    text_t tr_title;
    struct ftm_param param;

    init_text(&tr_title, uistr_test_report, COLOR_YELLOW);

    ui_init_itemview(&triv);

    quit = false;
    memset(rpt_items, 0, sizeof(item_t) * (ITEM_MAX_IDS + 1));
    num = create_report(items, rpt_items, ITEM_MAX_IDS, buf, bufsz);
    triv.set_title(&triv, &tr_title);
    triv.set_items(&triv, rpt_items, 0);
    while (!quit) {
        chosen_item = triv.run(&triv, &quit);
        if (chosen_item == ITEM_MAX_IDS)
            break;
        param.name = get_item_name(items, chosen_item);
        ftm_entry(chosen_item, &param);
        create_report(items, rpt_items, ITEM_MAX_IDS, buf, bufsz);
    }
    return 0;
}

static int full_test_mode(char *buf, int bufsz)
{
    int i = 0;
    item_t *items;
    struct ftm_module *mod;
    struct ftm_param param;
    //handle of testreport.log
    FILE *fp = NULL;
    //add for saving test report
	item_t rpt_items[ITEM_MAX_IDS + 1];
    int stopmode = 0;
    char *stopprop = ftm_get_prop("FTM.FailStop");

    if (stopprop && !strncasecmp(stopprop, "yes", strlen("yes")))
        stopmode = 1;

    LOGD(TAG "full_test_mode: %d", stopmode);

//    items = get_manual_item_list();

    items = ftm_cust_items;

	LOGD(TAG "get_manual_item_list end");

    while (items[i].name)
	{
		LOGD(TAG "name = %s,id = %d,mode=%d",items[i].name,items[i].id,items[i].mode);
		if(items[i].mode != FTM_AUTO_ITEM)
		{
			LOGD(TAG "%s:%d", items[i].name, items[i].id);

			switch (items[i].id)
			{
			case ITEM_IDLE: /* skip items */
				break;
			case ITEM_REPORT:
				item_test_report(items, buf, bufsz);
				break;
			default:
				mod = ftm_get_module(items[i].id);
				if (mod && mod->visible)
				{
					param.name = items[i].name;
					ftm_entry(items[i].id, &param);
					if (stopmode && mod->test_result != FTM_TEST_PASS)
						continue;
				}
				break;
			}
		}
		i++;
	}

    //add for saving test report
    fp = open_file(TEST_REPORT_SAVE_FILE);
	if(fp != NULL)
	{
		create_report(items, rpt_items, ITEM_MAX_IDS , buf, bufsz);
        write_test_report(rpt_items, fp);
		fclose(fp);
	}
    //add for saving test report

    return 0;
}

static int auto_test_mode(char *buf, int bufsz)
{
    int i = 0;
    item_t *items, *cust_items;
    struct ftm_module *mod;
    struct ftm_param param;
    //handle of testreport.log
    FILE *fp = NULL;
    //add for saving test report
    item_t rpt_items[ITEM_MAX_IDS + 1];
    int stopmode = 0;
    char *stopprop = ftm_get_prop("FTM.FailStop");

    if (stopprop && !strncasecmp(stopprop, "yes", strlen("yes")))
        stopmode = 1;

    LOGD(TAG "auto_test_mode: %d", stopmode);

    items = get_auto_item_list();
    //add for saving test report
    cust_items = get_item_list();
    memset(rpt_items, 0, sizeof(item_t) * (ITEM_MAX_IDS + 1));

    while (items[i].name) {
        LOGD(TAG "%s:%d", items[i].name, items[i].id);
        switch (items[i].id) {
        case ITEM_IDLE: /* skip items */
            break;
        case ITEM_REPORT:
            item_test_report(items, buf, bufsz);
            break;
        default:
            mod = ftm_get_module(items[i].id);
            if (mod && mod->visible) {
                param.name = items[i].name;
                ftm_entry(items[i].id, &param);
                if (stopmode && mod->test_result != FTM_TEST_PASS)
                    continue;
            }
            break;
        }
        i++;
    }

    //add for saving testreport.log
    fp = open_file(TEST_REPORT_SAVE_FILE);
	if(fp != NULL)
	{
		create_report(cust_items, rpt_items, ITEM_MAX_IDS , buf, bufsz);
//        write_test_report(rpt_items, fp);
		fclose(fp);
	}
    //add for saving testreport.log

    return 0;
}

static int item_test_mode(char *buf, int bufsz)
{
    int chosen_item = 0;
    bool exit = false;
    struct itemview itv;  /* item test menu */
    struct ftm_param param;
    text_t  title;
    item_t *items;
    //handle of testreport.log
	FILE *fp = NULL;
    //add for saving test report
	item_t rpt_items[ITEM_MAX_IDS + 1];

    LOGD(TAG "item_test_mode");

    items = get_item_list();

    ui_init_itemview(&itv);
    init_text(&title, uistr_item_test, COLOR_YELLOW);

    itv.set_title(&itv, &title);
    itv.set_items(&itv, items, 0);

    while (1) {
        chosen_item = itv.run(&itv, &exit);
        if (exit == true)
            break;
        switch (chosen_item) {
        case ITEM_REPORT:
            item_test_report(items, buf, bufsz);
            break;
        default:
            param.name = get_item_name(items, chosen_item);
            ftm_entry(chosen_item, &param);
            LOGD(TAG "ITEM TEST ftm_entry before");
            //add for saving test report
			fp = open_file(TEST_REPORT_SAVE_FILE);
			if(fp != NULL)
			{
			    create_report(items, rpt_items, ITEM_MAX_IDS , buf, bufsz);
				fclose(fp);
			}
            //add for saving test report
            break;
        }
    }
    return 0;
}

static int debug_test_mode(char *buf, int bufsz)
{
    int chosen_item = 0;
    bool exit = false;
    struct itemview itv;  /* item test menu */
    struct ftm_param param;
    text_t  title;
    item_t *items;

    LOGD(TAG "debug_test_mode");

    items = get_debug_item_list();

    ui_init_itemview(&itv);
    init_text(&title, uistr_item_test, COLOR_YELLOW);

    itv.set_title(&itv, &title);
    itv.set_items(&itv, items, 0);

    while (1) {
        chosen_item = itv.run(&itv, &exit);
        if (exit == true)
            break;
        switch (chosen_item) {
        default:
			LOGD(TAG "chosen_item=%d",chosen_item);
            param.name = get_item_name(items, chosen_item);
            ftm_debug_entry(chosen_item, &param);
            break;
        }
    }
    return 0;
}


static int test_module()
{

		int arg = 0;
		int id = -1;
		int write_len = 0;
		struct ftm_param param;
		char test_result[128] = {0};
		item_t *items;
		struct ftm_module *mod;
		char *prop_name = NULL;
		char *prop_val = NULL;
		char result[3][16] = {"not test\r\n", "pass\r\n", "fail\r\n" };
		char temp_at_command[128] = {0};
		int i = 0;
		char p[16] = {0};
		items = get_item_list();

		char buf[8] = {0};
        int at_command_len = 0;
		strcpy(buf, "quit");

		while(1)
		{
		    pthread_mutex_lock (&at_command_mutex);
            at_command_len = strlen(at_command);
            strcpy(p, at_command);
            memset(at_command, 0, sizeof(at_command));
			if(at_command_len <= 3){
		    pthread_mutex_unlock (&at_command_mutex);
				continue;
            }

            if(!memcmp(buf, p, 4))
            {
		    pthread_mutex_unlock (&at_command_mutex);
//                memset(at_command, 0, sizeof(at_command));
                break;
            }
			LOGD(TAG "at command:%d, 0x%02x, 0x%02x, 0x%02x, 0x%02x, 0x%02x, 0x%02x, 0x%02x, 0x%02x, %s,%d \n",
				strlen(at_command), at_command[0], at_command[1], at_command[2], at_command[3],
				buf[0], buf[1], buf[2], buf[3], at_command, strcmp(buf, at_command));
		    pthread_mutex_unlock (&at_command_mutex);

			id = get_item_id(pc_control_items, p);
			LOGD(TAG "test item id is %d\n", id);
			if(id >= 0)
			{
				LOGD(TAG "before get_item_name");
				param.name = get_item_name(items, id);
				if(param.name == NULL)
				{
					param.name = get_item_name(ftm_menu_items, id);
				}
				LOGD(TAG "after get_item_name is %s\n", param.name);
				if(param.name != NULL)
				{
					mod = ftm_get_module(id);
					if (mod && mod->visible)
					{
						ftm_entry(id, &param);
                        LOGD(TAG "test_result:%d\n", mod->test_result);
                        if(mod->test_result >= FTM_TEST_MAX)
                        {
                            strcpy(test_result, result[0]);
                        }
                        else
                        {
						strcpy(test_result, result[mod->test_result]);
					}
				}
			}
			}
			else
			{
				strcpy(test_result, "Cannot find the module!\r\n");
			}
            LOGD(TAG "before write data to pc\n");
            while(usb_status != 1)
            {
                sleep(1);
            }
			write_len = write(usb_com_port, test_result, strlen(test_result));
            LOGD(TAG "after write data to pc\n");
		    if(write_len != (int)strlen(test_result))
			{
				LOGD(TAG "write data to pc fail\n");
			}
//			memset(at_command, 0, sizeof(at_command));

		}
		LOGD(TAG "test_result is %s, the %s\n", test_result, test_data);
		return 0;
}

static int is_pc_control(int fd)
{
    struct timeval startime, endtime;
	double time_use = 0;
	int read_from_usb = 0;
	char USB_read_buffer[BUFSZ] = {0};
	bool pc_control = false;
	double max_time = 1500000;
	gettimeofday(&startime, 0);
	LOGD(TAG "time_use = %lf\n", time_use);

	if(usb_com_port == -1)
	{
		return 0;
	}

	while(time_use < max_time)
	{
//		LOGD(TAG "time_use = %lf\n", time_use);
        if(usb_com_port != -1)
        {
            read_from_usb = read(usb_com_port, USB_read_buffer, sizeof(USB_read_buffer));
//			LOGD(TAG "read_from_usb = %d, USB_read_buffer = %s\n", fd, USB_read_buffer);
		}
		if(read_from_usb == -1)
		{
			gettimeofday(&endtime, 0);
			time_use = 1000000 * (endtime.tv_sec - startime.tv_sec) +
				endtime.tv_usec - startime.tv_usec;
            continue;
		}
		else if (read_from_usb > 0)
		{
			if(strncmp(USB_read_buffer, START, strlen(START)) == 0)
			{
                LOGD(TAG "start\n");
                int len = write(usb_com_port, "pass\r\n", strlen("pass\r\n"));
				if(len != (int)strlen("pass\r\n"))
				{
					LOGD(TAG "write pass fail in is_pc_control");
				}
				else
				{
					LOGD(TAG "write pass in is_pc_control");
					pc_control = true;
                    usb_status = 1;
					break;
				}
			}
		}
	}
//	write(fd,"ok",strlen("ok"));
	return pc_control;
}


//MTKBEGIN  [mtk0625][DualTalk]
#if defined(MTK_EXTERNAL_MODEM_SLOT) && !defined(EVDO_DT_SUPPORT)
#define EXT_MD_IOC_MAGIC			'E'
#define EXT_MD_IOCTL_LET_MD_GO		_IO(EXT_MD_IOC_MAGIC, 1)
#define EXT_MD_IOCTL_REQUEST_RESET	_IO(EXT_MD_IOC_MAGIC, 2)
#define EXT_MD_IOCTL_POWER_ON_HOLD	_IO(EXT_MD_IOC_MAGIC, 3)

int boot_modem(int is_reset)
{
    LOGD(TAG "%s\n", __FUNCTION__);

	int ret;
	int ext_md_ctl_n0, ext_md_ctl_n1;

	ext_md_ctl_n0 = open("/dev/ext_md_ctl0", O_RDWR);
	if(ext_md_ctl_n0 <0) {
        LOGD(TAG "open ext_md_ctl0 fail");
		return	ext_md_ctl_n0;
	}
	ret = ioctl(ext_md_ctl_n0, EXT_MD_IOCTL_POWER_ON_HOLD, NULL);
	if (ret < 0) {
        LOGD(TAG "power on modem fail");
		return	ret;
	}

	ext_md_ctl_n1 = open("/dev/ext_md_ctl1", O_RDWR);
	if(ext_md_ctl_n1 <0) {
        LOGD(TAG "open ext_md_ctl_n1 fail");
		return	ext_md_ctl_n1;
	}
	ret = ioctl(ext_md_ctl_n1, EXT_MD_IOCTL_LET_MD_GO, NULL);
	if (ret < 0) {
        LOGD(TAG "EXT_MD_IOCTL_LET_MD_GO fail");
		return	ret;
	}

	return	ret;
}
#endif  /* MTK_DT_SUPPORT */
//MTKEND    [mtk80625][DualTalk]

void *read_data_thread_callback(void* data)
{
	int read_from_usb = 0;
	char USB_read_buffer[BUFSZ] = {0};
	bool exit = false;

	while(1)
    {
        if(usb_plug_in == 0)
        {
            LOGD("FACTORY.C usb_plug_in == 0\n");
            continue;
        }
        else if(is_USB_State_PlugIn())
        {
            open_usb();
        }
		if(usb_com_port != -1)
    	{
            read_from_usb = read_a_line_test(usb_com_port, USB_read_buffer, sizeof(USB_read_buffer));
		}
        else
    	{
            continue;
		}
		if(read_from_usb == -1)
		{
            continue;
		}
		else if(read_from_usb > 0)
		{
            LOGD(TAG "read from usb is %s\n",USB_read_buffer);
			if(read_from_usb > 3)
			{
			USB_read_buffer[read_from_usb-1] = 0;
            pthread_mutex_lock (&at_command_mutex);
			LOGD(TAG "-----------> AT COMMAND = %s\n", at_command);
            pthread_mutex_unlock (&at_command_mutex);

		    if(strncmp(USB_read_buffer, STOP, strlen(STOP)) == 0){
                LOGD(TAG "stop\n");
                pthread_mutex_lock (&at_command_mutex);
				strcpy(at_command, "quit");
				LOGD(TAG "compare at_command and quit:%d\n", strncmp(at_command, "quit", strlen("quit")));
                pthread_mutex_unlock (&at_command_mutex);
                int n = write(usb_com_port, "pass\r\n", strlen("pass\r\n"));
				if(n != (int)strlen("pass\r\n"))
			    {
				    LOGD(TAG "Write stop pass fail\n");
			    }
				else
				{
					LOGD(TAG "Write stop pass successfully\n");
				}
				close(usb_com_port);
				break;
		    }
			else if(strncmp(USB_read_buffer, START, strlen(START)) == 0)
			{
                LOGD(TAG "start\n");
                int n = write(usb_com_port, "pass\r\n", strlen("pass\r\n"));
                usb_status = 1;
				if(n != (int)strlen("pass\r\n"))
			    {
				    LOGD(TAG "Write start pass fail\n");
			    }
				else
				{
					 LOGD(TAG "Write start pass successfully\n");
				}
			}
			else if(strncmp(USB_read_buffer, REQUEST_DATA, strlen(REQUEST_DATA)) == 0)
			{

				LOGD(TAG "name:%s, mac:%s, rssi:%d, channel:%d ,rate%d\n", return_data.wifi.wifi_name,
					return_data.wifi.wifi_mac, return_data.wifi.wifi_rssi,
					return_data.wifi.channel, return_data.wifi.rate);
				int i = 0;
				for(i = 0 ; i < return_data.bt.num; i++)
				{
				LOGD(TAG "bt_mac:%s, rssi:%d\n", return_data.bt.bt[i].bt_mac, return_data.bt.bt[i].bt_rssi);
				}
				char temp_buf[2048] = {0};
				memcpy(temp_buf, &return_data, sizeof(return_data));
				strcpy(temp_buf+sizeof(return_data), "\r\n");
			    int n = write(usb_com_port, temp_buf, sizeof(temp_buf));
			   // int n = write(fd, return_data.wifi.wifi_name, sizeof(return_data.wifi.wifi_name));
			    if(n != sizeof(temp_buf))
			    {
				    LOGD(TAG "Write test_data fail,%d\n",  sizeof(temp_buf));
			    }
				else
				{
					 LOGD(TAG "Write test_data successfully,%d\n", sizeof(temp_buf));
				}
			}
			else if(strncmp(USB_read_buffer, VERSION, strlen(VERSION))==0)
			{
				display_version(1);
				int n = write(usb_com_port, "pass\r\n", sizeof("pass\r\n"));
				if(n != sizeof("pass\r\n"))
				{
					LOGD(TAG "Write test_data in version fail\n");
				}
				else
				{
					LOGD(TAG "Write test_data in version successfully\n");
				}
			}
			else if(strncmp(USB_read_buffer, READ_BARCODE, strlen(READ_BARCODE))==0)
            {
                int ccci_handle = -1;
                int i = 0;
                char result[BUFSZ] = {0};
                if(is_support_modem(1))
                {
	                LOGD(TAG "MTK_ENABLE_MD1\n");
	                ccci_handle = openDevice();
                }
                else if(is_support_modem(2))
                {
                    ccci_handle = openDeviceWithDeviceName("/dev/ccci2_tty0");
                }
                if(-1 == ccci_handle) 
                {
            	    LOGD(TAG "Fail to open ttyMT0 interface\n");
		                return 0;
                }
	            for (i = 0; i<30; i++) usleep(50000); //sleep 1s wait for modem bootup
                send_at (ccci_handle, "AT\r\n");
            	wait4_ack (ccci_handle, NULL, 3000);
                getBarcode(ccci_handle,result);
                char *ptr = strchr(result, '\"');
                if ((ptr != NULL) && (strlen(result) > 0)) 
                {
                    *ptr = 0;
                }
                else
                {
                    strcpy(result, "fail\r\n");
                }
                int n = write(usb_com_port, result, sizeof(result));
				if(n != sizeof(result))
				{
					LOGD(TAG "Write test_data in version fail\n");
				}
				else
				{
					LOGD(TAG "Write test_data in version successfully\n");
				}
                if(-1 != ccci_handle)
                {
                    closeDevice(ccci_handle);
                }
            }
            else if(strncmp(USB_read_buffer, WRITE_BARCODE, strlen(WRITE_BARCODE))==0)
            {
                LOGD(TAG "Entry write barcode!\n");
                int ccci_handle = -1;
                int i = 0;
                int result = -1;
                char *barcode = strchr(USB_read_buffer, '=');
                char return_result[16] = {0};
                if(barcode == NULL)
                {
                    LOGD(TAG "barcode is null!\n");               
                }
                else
                {
                    barcode++;
                    LOGD(TAG "%s\n", barcode);
                    if(is_support_modem(1))
                    {
	                    LOGD(TAG "MTK_ENABLE_MD1\n");

	                    ccci_handle= openDevice();
                    }
                    else if(is_support_modem(2))
                    {
                        ccci_handle = openDeviceWithDeviceName("/dev/ccci2_tty0");
                    }
	                if(-1 == ccci_handle) 
                    {
 		                    LOGD(TAG "Fail to open CCCI interface\n");
		                    return 0;
                    }
	                for (i = 0; i<30; i++) usleep(50000); //sleep 1s wait for modem bootup
	                send_at (ccci_handle, "AT\r\n");
	                wait4_ack (ccci_handle, NULL, 3000);

                    result = write_barcode(ccci_handle, barcode);
                }
                if(result == 0)
                {
                    strncpy(return_result, "pass\r\n", strlen("pass\r\n"));
				}
				else
				{
                    strncpy(return_result, "fail\r\n", strlen("fail\r\n"));
				}
                int n = write(usb_com_port, return_result, strlen(return_result));
                
                if(n != strlen(return_result))
				{
					LOGD(TAG "Write test_data in version fail\n");
				}
				else
				{
					LOGD(TAG "Write test_data in version successfully\n");
				}
               
                if(-1 != ccci_handle)
                {
                    closeDevice(ccci_handle);
                }
            }
			else
			{
               LOGD(TAG "module\n");
               //test_module(fd, USB_read_buffer);
               int ret = 0;
			   int id = -1;
			   ret = get_AT_command(USB_read_buffer);
			   if(ret == 1)
			   {
					id = get_item_id(pc_control_items, USB_read_buffer);
					switch(id)
					{
						case ITEM_VIBRATOR:
							#ifdef FEATURE_FTM_VIBRATOR
							vibrator_test_exit = true;
							#endif
							break;
						case ITEM_LED:
							#ifdef FEATURE_FTM_LED
							keypadled_test_exit = true;
							led_test_exit = true;
							#endif
							break;
						case ITEM_MICBIAS:
							#ifdef FEATURE_FTM_AUDIO
							bMicbias_exit = true;
							#endif
							break;
						default:
							break;
					}
			   }
			   else if(ret ==2)
			   {

					int n = write(usb_com_port, "pass\r\n", sizeof("pass\r\n"));
					if(n != sizeof("pass\r\n"))
					{
						LOGD(TAG "Write test_data in version fail\n");
					}
					else
					{
						LOGD(TAG "Write test_data in version successfully\n");
					}

			   }
		    }
				}

			LOGD(TAG "BOOL IS %d\n", exit);
		}
	}//while

	pthread_exit(NULL);
	return NULL;
}



static int pc_control_mode(int fd)
{
	LOGD(TAG "CALL pc_control_mode1");
	test_module();
	return 0;
}

int main(int argc, char **argv)
{
	int exit = 0;
	int    sel=0;
	int nr_line=0;
	int avail_lines = 0;
	bool   quit=false;
	char  *buf = NULL;

	//int n;
    struct ftm_param param;
    struct itemview fiv;  /* factory item menu */
    struct itemview miv;  /* mini-ui item menu */
	//struct textview vi;   /* version info */
    //struct itemview ate;  /* ATE factory mode*/
    item_t *items;
    text_t ftm_title;
    int bootMode;
	int g_fd_atcmd = -1, g_fd_uart = -1;
    int g_hUsbComPort = -1;
    //add for saving test report
	item_t rpt_items[ITEM_MAX_IDS + 1];

	//text_t vi_title;
	//text_t ate_title;
    text_t rbtn;
    text_t info;
	pthread_t read_thread;

    ui_init();

    /* CHECKME! should add this fuctnion to avoid UI not displayed */
 	//ui_print("factory mode\n");
    show_slash_screen(uistr_factory_mode, 1000);

    bootMode = getBootMode();

    if(ATE_FACTORY_BOOT == bootMode)
    {
        ui_print("Enter ATE factory mode...\n");

        ate_signal();

        while(1){}
    }
    else if(FACTORY_BOOT == bootMode)
    {
		buf = malloc(BUFSZ);
		if (NULL == buf)
		{
		    ui_print("Fail to get memory!\n");
		}

		ftm_init();
		avail_lines = get_avail_textline();
		if (!read_config(FTM_CUST_FILE1))
			read_config(FTM_CUST_FILE2);


		usb_com_port = open("dev/ttyGS0", O_RDWR | O_NOCTTY | O_NDELAY);

        LOGD(TAG "Open USB dev/ttyGS0 %s.\n", (-1==usb_com_port)? "failed":"success");

		if(is_pc_control(usb_com_port))
		{
			pthread_create(&read_thread, NULL, read_data_thread_callback, (void *)&usb_com_port);
			LOGD(TAG "after create pthread");
			status = 1;
			pc_control_mode(usb_com_port);
			ALOGD(TAG "pc control stops in if()!\n");
			status = 0;
		}

		LOGD(TAG "pc control stops!\n");

		ui_init_itemview(&fiv);
		ui_init_itemview(&miv);
		//ui_init_textview(&vi, textview_key_handler, &vi);

		init_text(&ftm_title, uistr_factory_mode, COLOR_YELLOW);
		//init_text(&vi_title, uistr_version, COLOR_YELLOW);
		//init_text(&rbtn, uistr_key_back, COLOR_YELLOW);
		//init_text(&info, buf, COLOR_YELLOW);

		items = ftm_menu_items;
		fiv.set_title(&fiv, &ftm_title);
		fiv.set_items(&fiv, items, 0);
		//vi.set_btn(&vi, NULL, NULL, &rbtn);
 
#if defined(MTK_EXTERNAL_MODEM_SLOT) && !defined(EVDO_DT_SUPPORT)
		boot_modem(0);
#endif  /* MTK_DT_SUPPORT */ 
		
		while (!exit) 
		{
			int chosen_item = fiv.run(&fiv, NULL);
			switch (chosen_item) 
			{
			case ITEM_FULL_TEST:
				full_test_mode(buf, BUFSZ);
				break;
			case ITEM_AUTO_TEST:
				auto_test_mode(buf, BUFSZ);
				item_test_report(get_auto_item_list(), buf, BUFSZ);
				//add for saving test report 
		        memset(rpt_items, 0, sizeof(item_t) * (ITEM_MAX_IDS + 1));
				create_report(get_item_list(), rpt_items, ITEM_MAX_IDS , buf, BUFSZ);
				//add for saving test report
				break;
			case ITEM_ITEM_TEST:
				item_test_mode(buf, BUFSZ);
				break;
			case ITEM_DEBUG_TEST:
				debug_test_mode(buf, BUFSZ);
				break;
			case ITEM_REPORT:
				item_test_report(get_item_list(), buf, BUFSZ);
				break;
			case ITEM_VERSION:
                display_version(0);				
				break;
			case ITEM_REBOOT:
				exit = 1;
				fiv.exit(&fiv);
				break;
			default:
				param.name = get_item_name(items, chosen_item);
				ftm_entry(chosen_item, &param);
				break;
			}
		}//end while

		if (buf)
			free(buf);

		ui_printf("Entering factory reset mode...\n");
		ui_printf("Rebooting...\n");
		sync();
		reboot(RB_AUTOBOOT);

		return EXIT_SUCCESS;
	}
	else
	{
		LOGE(TAG "Unsupported Factory mode\n");
	}
	
	return EXIT_SUCCESS;
}
