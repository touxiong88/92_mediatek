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
 

#include "common.h"
#include "ftm.h"
#include "miniui.h"
#include "utils.h"
 
#include "libnvram.h"
#include "CFG_file_info_custom.h"

//#include "version.h"

#define MAX_RETRY_COUNT 20
 
#define TAG        "[VERSION] "
  
extern int g_nr_lines;
extern sp_ata_data return_data;
extern char test_data[128];

extern int textview_key_handler(int key, void *priv);
extern int write_test_report(item_t *items, FILE *fp);

void getIMEI(int sim, int fd,char *result)
{
	unsigned int i=0,j=0;
	char buf[64]={0};
	strcpy(result, "unknown");
	int count = 0;
	if(sim==1) strcpy(buf, "AT+EGMR=0,7\r\n");
	else if(sim==2) strcpy(buf, "AT+EGMR=0,10\r\n");
	else if(sim==3) strcpy(buf, "AT+EGMR=0,11\r\n");
	else strcpy(buf, "AT+EGMR=0,12\r\n");

retry:
	send_at(fd, buf);
	memset(buf,'\0',64);
	read_ack(fd,buf,64);
	LOGD("buf %s",buf);
	buf[63]=0;
	i=0, j=0;
	if(strlen(buf)>14)
	{
		for(i=0;i<strlen(buf)-14;i++)
		{
			for(j=i;j<i+15;j++)
			{
				if(buf[j]<'0' || buf[j]>'9') break;
			}
			if(j==i+15) break; else i=j;
		}
		buf[j]=0;
		if(j==i+15)
		{
			strcpy(result, &(buf[i]));
			count = 3;
		}
		else
		{
			count++;
		}
	}
	else
	{
		count++;
	}

	LOGD(TAG "COUNT = %d\n", count);

	if(count < 3)
	{
		LOGD(TAG "go to retry");
		strcpy(buf, "AT\r\n");
		goto retry;
	}

	LOGE("getIMEI %s",result);
}


int getModemVersion(int fd,char *result)
{
	const int BUF_SIZE = 128;
	char buf[BUF_SIZE];
	memset(buf,'\0',BUF_SIZE);
	const int HALT_TIME = 100 * 1000;
	int count = 0;
	char *p = NULL;

	strcpy(buf, "AT+CGMR\r\n");
	strcpy(result, "unknow\n");

retry:
	send_at(fd, buf);
	memset(buf,'\0',BUF_SIZE);
	read_ack(fd,buf,BUF_SIZE);
	LOGD("buf %s",buf);

	//const char *tok = "+CGMR: ";
	//p = strstr(buf, tok);
	p = strchr(buf, ' '); // find the first space char.
	if(p) {
		strcpy(result, ++p);
		count = 3;
	} else {
		strcpy(buf, "AT\r\n");
		count++;
	}

	if(count < 3)
	{
		LOGD(TAG "COUNT in modem IS %d\n", count);
		goto retry;
	}

	LOGE(TAG "getModemVersion result = %s\n", result);
	return 0;
}

/*
* Caution: here we execute UART command to retrieve the barcode number from modem. And notice that we rely on the fact that there are double quotations(i.e.") in
* the bar-code returned by UART command.
* If the format of the bar-code number changes, the following code will probably NOT do what it is supposed to do:(
*/
int getBarcode(int fd,char *result)
{
	const int BUF_SIZE = 128;
	char buf[BUF_SIZE];
	const int HALT_TIME = 100 * 1000;
	int count = 0;
	char *p = NULL;

	strcpy(buf, "AT+EGMR=0,5\r\n");
	strcpy(result, "unknown");

retry:
	send_at(fd, buf);
	memset(buf,'\0',BUF_SIZE);
	read_ack(fd,buf,BUF_SIZE);
	LOGD("buf %s",buf);

	p = strchr(buf, '\"'); // find the first double quotation mark.
	if(p) {
		strcpy(result, ++p);
		count = 3;
	} else {
		strcpy(buf, "AT\r\n");
		count++;
	}

	if(count < 3)
	{
		LOGD(TAG "COUNT IN BARCODE IS %d\n", count);
		goto retry;
	}
	LOGE("getBarcode result = %s\n", result);
	return 0;
}

int write_barcode(int fd, char* barcode)
{
    char buf[128] = {0};
    int result = 0;
    if((fd == -1) || (barcode == NULL))
    {
        return -1;
    }
    if(strlen(barcode) > 113)
    {
        // barcode is too long, buf will leak
        return -1;
    }
    sprintf(buf, "AT+EGMR=1,5,\"%s\"\r\n", barcode);
    send_at(fd, buf);

    memset(buf, 0, 128);

    result = wait4_ack (fd, NULL, 3000);

    return result;
}


void print_verinfo(char *info, int *len, char *tag, char *msg)
{
	char buf[256];
	int _len = *len;
	int tag_len = strlen(tag);

	int max_len = gr_fb_width() / CHAR_WIDTH *2;
	int msg_len = strlen(msg);

	int buf_len = gr_fb_width() / CHAR_WIDTH;

	_len += sprintf(info + _len, "%s", tag);
	_len += sprintf(info + _len, ": ");

	if(msg_len>max_len-tag_len-2) {
		_len += sprintf(info+_len,"\n    ");
		g_nr_lines++;
	}

	while(msg_len>0) {
		buf_len = max_len - 4;
		buf_len = (msg_len > buf_len ? buf_len : msg_len);
		strncpy(buf, msg, buf_len);
		buf[buf_len] = 0;

		_len += sprintf(info + _len, "%s", buf);
		_len += sprintf(info + _len, "\n");
		g_nr_lines++;
		msg_len-=buf_len;
		msg = &(msg[buf_len]);
		while(msg_len>0 && msg[0]==' ') {
			msg_len--;
			msg = &(msg[1]);
		}
#if 1
		if(msg_len>0) {
			for(buf_len=0; buf_len < 4; buf_len++) buf[buf_len]=' ';
			buf[buf_len]=0;
			//_len += sprintf(info+_len, buf);
			// Fix Anroid 2.3 build error
			_len += sprintf(info + _len, "%s", buf);
		}
#endif
	}
	*len = _len;
	//LOGE("In factory mode: g_nr_lines = %d\n", g_nr_lines);
}

#if defined(FEATURE_FTM_3GDATA_SMS) || defined(FEATURE_FTM_3GDATA_ONLY) || defined(FEATURE_FTM_WIFI_ONLY)

int get_barcode_from_nvram(char *barcode_result)
{

	int read_nvram_ready_retry = 0;
	F_ID fid;
	int rec_size = 0;
	int rec_num = 0;
	int barcode_lid = AP_CFG_REEB_PRODUCT_INFO_LID;
	PRODUCT_INFO *barcode_struct;
	bool isread = true;
	char nvram_init_val[128] = {0};
	LOGD(TAG "Entry get_barcode_from_nvram");
	while(read_nvram_ready_retry < MAX_RETRY_COUNT)
	{
		read_nvram_ready_retry++;
		property_get("nvram_init", nvram_init_val, NULL);
		if(strcmp(nvram_init_val, "Ready") == 0)
		{
			break;
		}
		else
		{
			usleep(500*1000);
		}
	}

	if(read_nvram_ready_retry >= MAX_RETRY_COUNT)
	{
		LOGD(TAG "Get nvram restore ready failed!");
		return 0;
	}

	barcode_struct= (PRODUCT_INFO *)malloc(sizeof(PRODUCT_INFO));
	if(barcode_struct == NULL)
	{
		return 0;
	}

	fid = NVM_GetFileDesc(barcode_lid, &rec_size, &rec_num, isread);

	if(fid.iFileDesc < 0)
	{
		LOGD(TAG "fid.iFileDesc < 0");
		return 0;
	}

	if(rec_size != read(fid.iFileDesc, barcode_struct, rec_size))
	{
		free(barcode_struct);
		return 0;
	}
	if(strlen(barcode_struct->barcode) > 0)
	{
		strcpy(barcode_result, barcode_struct->barcode);
	}else
	{
		strcpy(barcode_result, "unknown");
	}

	free(barcode_struct);
	if(!NVM_CloseFileDesc(fid))
	{
		return 0;
	}
	LOGD("The size of barcode_struct:%d\n", sizeof(barcode_struct));
	LOGD("Barcode is %s\n", barcode_result);
	return 1;
}
#endif


int create_verinfo(char *info, int size)
{

	int fd=-1;
	int fd2=-1;
	int fd_dt = -1;
	char val[128]={0};
	int len = 0;
	unsigned int i;
	char ver[128]={0};
	char imei1[64]={0};
	char imei2[64]={0};
	char imei3[128]={0};
	char imei4[128]={0};
	char modem_ver[128] = "unknown";
	char modem_ver2[128] = "unknown";
	char modem_ver_dt[128] = "unknown";
	char barcode[128] = "unknown";
	char barcode2[128] = "unknown";
	char barcode_dt[128] = "unknown";


	char kernel_ver[256] = "unknown";
	char uboot_ver[128]  = "unknown";
	char uboot_build_ver[128]  = "unknown";
	char kernel_build_ver[128] = "unknown";
	char rootfs_build_ver[128]  = "unknown";
	int kernel_ver_fd = -1;
	int kernel_cli_fd = -1;
	char buffer[1024];
	char *ptr= NULL, *pstr = NULL;
	int reslt=0;
	int move_bit = 0;
	g_nr_lines = 0;


	if(is_support_modem(1))
	{
		LOGD(TAG "MTK_ENABLE_MD1\n");

		fd= openDevice();
		if(-1 == fd) {
			LOGD(TAG "Fail to open CCCI interface\n");
			return 0;
		}
		for (i = 0; i<30; i++) usleep(50000); //sleep 1s wait for modem bootup
		send_at (fd, "AT\r\n");
		wait4_ack (fd, NULL, 3000);
	}





	if(is_support_modem(2)){
		LOGD(TAG "MTK_ENABLE_MD2\n");
		fd2= openDeviceWithDeviceName("/dev/ccci2_tty0");
		if(-1 == fd2) {
			LOGD(TAG "Fail to open ttyMT0 interface\n");
			return 0;
		}
		initTermIO(fd2,5);
		for (i = 0; i<30; i++) usleep(50000); //sleep 1s wait for modem bootup
		send_at (fd2, "AT\r\n");
		wait4_ack (fd2, NULL, 3000);
	}


#if defined(MTK_EXTERNAL_MODEM_SLOT) && !defined(EVDO_DT_SUPPORT)
	LOGD(TAG "MTK_DT_SUPPORT\n");
	fd_dt= openDeviceWithDeviceName("/dev/ttyMT0");
	if(-1 == fd_dt) {
		LOGD(TAG "Fail to open ttyMT0 interface\n");
		return 0;
	}
	for (i = 0; i<30; i++) usleep(50000); //sleep 1s wait for modem bootup
	send_at (fd_dt, "AT\r\n");
	wait4_ack (fd_dt, NULL, 3000);
#endif

#ifdef MTK_EXTERNAL_MODEM_SLOT

	if (!strcmp(MTK_EXTERNAL_MODEM_SLOT,"1"))
	{

		if(is_support_modem(1)){
			getIMEI(1, fd, imei2);

		}else if(is_support_modem(2)){
			getIMEI(1, fd2, imei2);
		}


#ifndef EVDO_DT_SUPPORT
		getIMEI(1, fd_dt, imei1);
#endif

	}
	else if(!strcmp(MTK_EXTERNAL_MODEM_SLOT, "2"))
	{

		if(is_support_modem(1)){
			getIMEI(1, fd, imei1);

		}else if(is_support_modem(2)){
			getIMEI(1, fd2, imei1);
		}

#ifndef EVDO_DT_SUPPORT
		getIMEI(1, fd_dt, imei2);
#endif

	}

#else


	if(is_support_modem(1)){
		getIMEI(1, fd, imei1);
#ifdef GEMINI

		if(is_support_modem(2)){
			getIMEI(1, fd2, imei2);
		}else{

			getIMEI(2, fd, imei2);
#if defined(MTK_GEMINI_3SIM_SUPPORT)
			getIMEI(3,fd, imei3);
#elif defined(MTK_GEMINI_4SIM_SUPPORT)
			getIMEI(3,fd, imei3);
			getIMEI(4,fd, imei4);
#endif
		}
#endif

	}else if(is_support_modem(2)){
		getIMEI(1, fd2, imei1);
#ifdef GEMINI
		getIMEI(2, fd2, imei2);
#endif

	}

#endif


	if(is_support_modem(1)){
		reslt = getModemVersion(fd,modem_ver);
		ptr = strchr(modem_ver, '\n');
		if (ptr != NULL) {
			*ptr = 0;
		}
		if(modem_ver[strlen(modem_ver)-1] == '\r')
		{
			modem_ver[strlen(modem_ver)-1] = 0;
		}

		reslt = getBarcode(fd,barcode);
		ptr = strchr(barcode, '\"');
		if (ptr != NULL) {
			*ptr = 0;
		}
		if(strlen(barcode) <= 0)
			strcpy(barcode, "unknown");
		closeDevice(fd);
	}



	if(is_support_modem(2)){
		reslt = getModemVersion(fd2,modem_ver2);
		ptr = strchr(modem_ver2, '\n');
		if (ptr != NULL) {
			*ptr = 0;
		}
		if(modem_ver2[strlen(modem_ver2)-1] == '\r')
		{
			modem_ver2[strlen(modem_ver2)-1] = 0;
		}
		reslt = getBarcode(fd2,barcode2);
		ptr = strchr(barcode2, '\"');
		if (ptr != NULL) {
			*ptr = 0;
		}
		if(strlen(barcode2) <= 0)
			strcpy(barcode2, "unknown");
		closeDevice(fd2);
	}


#if defined(MTK_EXTERNAL_MODEM_SLOT) && !defined(EVDO_DT_SUPPORT)
	reslt = getModemVersion(fd_dt,modem_ver_dt);
	ptr = strchr(modem_ver_dt, '\n');
	if (ptr != NULL) {
		*ptr = 0;
	}
	if(modem_ver_dt[strlen(modem_ver_dt)-1] == '\r')
	{
		modem_ver_dt[strlen(modem_ver_dt)-1] = 0;
	}
	reslt = getBarcode(fd_dt,barcode_dt);
	ptr = strchr(barcode_dt, '\"');
	if (ptr != NULL) {
		*ptr = 0;
	}
	if(strlen(barcode_dt) <= 0)
		strcpy(barcode_dt, "unknown");
	closeDevice(fd_dt);
#endif

#if defined(FEATURE_FTM_3GDATA_SMS) || defined(FEATURE_FTM_3GDATA_ONLY) || defined(FEATURE_FTM_WIFI_ONLY)

	get_barcode_from_nvram(barcode);

#endif

	kernel_ver_fd = open("/proc/version",O_RDONLY);
	if(kernel_ver_fd!=-1) {
		read(kernel_ver_fd, kernel_ver, 256);
		close(kernel_ver_fd);
	}

	kernel_cli_fd = open("/proc/cmdline",O_RDONLY);
	if(kernel_cli_fd!=-1) {
		read(kernel_cli_fd,buffer,128);
		ptr = buffer;
		pstr = strtok(ptr, ", =");
		while(pstr != NULL) {
			if(!strcmp(pstr, "uboot_build_ver")) {
				pstr = strtok(NULL, ", =");
				strcpy(uboot_build_ver, pstr);
			}
			if(!strcmp(pstr, "uboot_ver")) {
				pstr = strtok(NULL, ", =");
				strcpy(uboot_ver, pstr);
			}
			pstr = strtok(NULL, ", =");
		}
		close(kernel_cli_fd);
	}

	if(uboot_build_ver[strlen(uboot_build_ver)-1]=='\n') uboot_build_ver[strlen(uboot_build_ver)-1]=0;
	if(kernel_ver[strlen(kernel_ver)-1]=='\n') kernel_ver[strlen(kernel_ver)-1]=0;

	property_get("ro.mediatek.platform", val, "unknown");
	print_verinfo(info, &len,  "BB Chip     ", val);
	property_get("ro.product.device", val, "unknown");
	print_verinfo(info, &len,  "MS Board.   ", val);

#ifdef FEATURE_FTM_3GDATA_SMS
#elif defined FEATURE_FTM_3GDATA_ONLY
#elif defined FEATURE_FTM_WIFI_ONLY
#elif defined GEMINI
#ifndef EVDO_DT_SUPPORT
	print_verinfo(info, &len,  "IMEI1       ", imei1);
	print_verinfo(info, &len,  "IMEI2       ", imei2);
#if defined(MTK_GEMINI_3SIM_SUPPORT)
	print_verinfo(info, &len,  "IMEI3       ", imei3);
#elif defined(MTK_GEMINI_4SIM_SUPPORT)
	print_verinfo(info, &len,  "IMEI3       ", imei3);
	print_verinfo(info, &len,  "IMEI4       ", imei4);
#endif
#else
#ifdef MTK_EXTERNAL_MODEM_SLOT
	if(!strcmp(MTK_EXTERNAL_MODEM_SLOT, "1"))
	{
		print_verinfo(info, &len, "IMEI        ", imei2);
	}
	else if(!strcmp(MTK_EXTERNAL_MODEM_SLOT, "2"))
	{
		print_verinfo(info, &len, "IMEI        ", imei1);
	}
#endif
#endif
#else
	print_verinfo(info, &len,  "IMEI        ", imei1);
#endif

	if (!reslt) 
    {

		if(is_support_modem(1)){
			print_verinfo(info, &len,  "Modem Ver.  ", modem_ver);
			sprintf(return_data.version.modem_ver,"%s", modem_ver);

		}

		if(is_support_modem(2)){
			print_verinfo(info, &len,  "Modem2 Ver.  ", modem_ver2);
		}

#if defined(MTK_EXTERNAL_MODEM_SLOT) && !defined(EVDO_DT_SUPPORT)
		print_verinfo(info, &len, "Modem2 Ver.", modem_ver_dt);
#endif

		if(is_support_modem(1)){
			print_verinfo(info, &len,  "Bar code    ", barcode);

		}

		if(is_support_modem(2)){
			print_verinfo(info, &len,  "Bar code2    ", barcode2);

		}
#if defined(MTK_EXTERMAL_MODEM_SLOT) && !defined(EVDO_DT_SUPPORT)
		print_verinfo(info, &len,  "Bar code2  ", barcode_dt);
#endif
	} 
    else
    {
		LOGE(TAG "Fail to open device uart modem\n");
	}

#if defined(FEATURE_FTM_3GDATA_SMS) || defined(FEATURE_FTM_3GDATA_ONLY) || defined(FEATURE_FTM_WIFI_ONLY)

	LOGD(TAG "Entry barcode in wifi only");
	print_verinfo(info, &len,  "Bar code    ", barcode);
#endif

	property_get("ro.build.date", val, "TBD");
	print_verinfo(info, &len,  "Build Time  ", val);
	//    print_verinfo(info, &len,  "UBoot Ver.  ", uboot_ver);

	ptr = &(kernel_ver[0]);
	for(i=0;i<strlen(kernel_ver);i++) {
		if(kernel_ver[i]>='0' && kernel_ver[i]<='9') {
			ptr = &(kernel_ver[i]);
			break;
		}
	}
	print_verinfo(info, &len,  "Kernel Ver. ", ptr);
	property_get("ro.build.version.release", val, "unknown");
	print_verinfo(info, &len,  "Android Ver.", val);
	property_get("ro.mediatek.version.release", val, "unknown");
	print_verinfo(info, &len,  "SW Ver.     ", val);
	sprintf(return_data.version.sw_ver,"%s", val);
	property_get("ro.custom.build.version",val,"unknown");
	print_verinfo(info, &len,  "Custom Build Verno.", val);
 
	return 0;
}


int create_report(item_t *item, item_t *rpt_items, int maxitems, char *buf, int size)
{
	struct ftm_module *mod;
	int i = 0, len = 0;
	char *ptr = buf;
	char result[] = { ' ', 'O', 'X' };
	color_t bgc[] = { 0, COLOR_GREEN, COLOR_RED };
	//handle of testreport.log
	FILE *fp = NULL;

	while (i < maxitems && item->name) {
		mod = ftm_get_module(item->id);
		if (mod && mod->visible && len < size) {
			ptr = buf + len;
			len += sprintf(ptr, "[%c] %s ",
				(mod->test_result >= FTM_TEST_MAX) ?
				result[FTM_TEST_UNKNOWN] : result[mod->test_result], item->name);
			ptr[len++] = '\0';
			rpt_items[i].id = mod->id;
			rpt_items[i].name = ptr;
			rpt_items[i].background = (mod->test_result >= FTM_TEST_MAX) ?
				0 : bgc[mod->test_result];
			i++;
		}
		item++;
	}

	//add for saving test report
	fp = open_file(TEST_REPORT_SAVE_FILE);

	if(fp == NULL)
	{
		LOGD(TAG "TEST_REPORT_SAVE_FILE is null");
	}
	else
	{
		LOGD(TAG "TEST_REPORT_SAVE_FILE is not null");
		write_test_report(rpt_items, fp);
		fclose(fp);
	}
	//add for saving test report

	if (i < maxitems - 1) {
		rpt_items[i].id   = ITEM_MAX_IDS;
		rpt_items[i].name = uistr_info_test_report_back;
	}
	return ++i;
}


char ** trans_verinfo(const char *str, int *line)
{
	char **pstrs = (char**)malloc(g_nr_lines * sizeof(char*));
	int  len     = strlen(str) + 1;
	int  row     = 0;
	const char *start  = str;
	const char *end    = str;

	if (!pstrs) {
		LOGE("In factory mode: malloc failed\n");
		return NULL;
	}

	while (len--) {
		if ('\n' == *end) {
			pstrs[row] = (char*)malloc((end - start + 1) * sizeof(char));

			if (!pstrs[row]) {
				LOGE("In factory mode: malloc failed\n");
				return NULL;
			}

			strncpy(pstrs[row], start, end - start);
			pstrs[row][end - start] = '\0';
			start = end + 1;
			row++;
		}
		end++;
	}

	*line = row;
	return pstrs;
}

void tear_down(char **pstr, int row)
{
    int i;

    for (i = 0; i < row; i++) {
        if (pstr[i]) {
            free(pstr[i]);
            pstr[i] = NULL;
        }
    }
	
    if (pstr) {
        free(pstr);
        pstr = NULL;
    }
}


/*
    autoreturn:  if the function called by ata, then true;
    if called by main, then false;
*/
int display_version(int autoreturn)
{
	char *buf = NULL;
	struct textview vi;	 /* version info */
	text_t vi_title;
	int nr_line;
	text_t info;
	int avail_lines = 0;
	text_t rbtn;
	buf = malloc(BUFSZ);
	init_text(&vi_title, uistr_version, COLOR_YELLOW);
	init_text(&info, buf, COLOR_YELLOW);
	init_text(&info, buf, COLOR_YELLOW);

	avail_lines = get_avail_textline();
	init_text(&rbtn, uistr_key_back, COLOR_YELLOW);
	ui_init_textview(&vi, textview_key_handler, &vi);
	vi.set_btn(&vi, NULL, NULL, &rbtn);
	create_verinfo(buf, BUFSZ);
	LOGE("after create_verinfo");
	vi.set_title(&vi, &vi_title);
	vi.set_text(&vi, &info);
	vi.m_pstr = trans_verinfo(info.string, &nr_line);
	vi.m_nr_lines = g_nr_lines;
	LOGE("g_nr_lines is %d, avail_lines is %d\n", g_nr_lines, avail_lines);
	vi.m_start = 0;
	vi.m_end = (nr_line < avail_lines ? nr_line : avail_lines);
	LOGE("vi.m_end is %d\n", vi.m_end);

    if(autoreturn)
    {
    	vi.redraw(&vi);
    }
    else
    {
        vi.run(&vi);
    }

	LOGE("Before tear_down\n");
	tear_down(vi.m_pstr, nr_line);
	if (buf)
		free(buf);
	LOGE("The version is %s\n", test_data);
	return 0;
}

   
