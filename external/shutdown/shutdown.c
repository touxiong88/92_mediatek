/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 * 
 * MediaTek Inc. (C) 2010. All rights reserved.
 * 
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

#include <stdio.h>
#include <stdlib.h>
#include <ctype.h>
#include <fcntl.h>
#include <string.h>

#include <sys/stat.h>
#include <sys/types.h>
#include <sys/reboot.h>
#include <sys/types.h>
#include <sys/mount.h>
#include <errno.h>

#include <signal.h>
#include <dirent.h>
#include <cutils/properties.h>
#include <utils/Log.h>

#define BKL_LCD_PATH "/sys/class/leds/lcd-backlight/brightness"

extern void kill_process(const char *path);

char *service_name[]={"runtime", "zygote", "keystore", "adbd","pppd",
			"wpa_supplicant", "p2p_supplicant1", "p2p_supplicant0",
			"nvram_daemon","NvRAMAgent","bluetoothd","racoon",
			"mtpd","systemkeys","ipod", "bootlogoupdater",
			"ueventd","pvrsrvinit","mtpd","mnld",
			"netd","netdiag", "mobile_log_d","debuggerd",
			"media","bootanim","dbus", "6620_launcher","mtkbt",
			"ccci_fsd", "ccci_mdinit", "pppd_gprs","gsm0710muxd",
			"muxreport-daemon", "ril-daemon", "atci-daemon",
			"audio-daemon", "installd", "wlaninit", "dhcpcd",
			"agpsd", "emsvr", "afmsvr", "mdlogger","" };

void turn_off_backlight()
{
    int fd = open(BKL_LCD_PATH, O_RDWR);
    if (fd == -1) {
		reboot(RB_POWER_OFF);
    }
    write(fd, "0", 1);	
    close(fd);
}

#define FSCK_TUNE
#ifdef FSCK_TUNE
#define NORMAL_UMOUNT_FLAG_LEN 4
#define NORMAL_UMOUNT_BLOCK_LEN 1024
#define NORMAL_UMOUNT_FLAG_POSTION NORMAL_UMOUNT_BLOCK_LEN - NORMAL_UMOUNT_FLAG_LEN
unsigned char NORMAL_UMOUNT_FLAG_ARRAY[5] = {0x11,0x22,0x33,0x44};
#define DATA_PATH "/emmc@usrdata" 
#endif
#ifdef FSCK_TUNE
void mark_shutdown()
{
	int i = 0;
	int dev = -1;
	int len = 0;
	int normal_umount_flag =0 ;
	char *p;
	char *s;
	unsigned char data_buf[NORMAL_UMOUNT_BLOCK_LEN];

	memset(data_buf,0,sizeof(data_buf));
				
	dev = open(DATA_PATH,O_RDWR | O_SYNC);
	if (dev < 0)
	{
		return;
	}
	else
	{
		lseek(dev,0,SEEK_SET);
		len = read(dev,data_buf,NORMAL_UMOUNT_BLOCK_LEN);
		if (len == NORMAL_UMOUNT_BLOCK_LEN)
		{
			memcpy(&data_buf[NORMAL_UMOUNT_FLAG_POSTION],NORMAL_UMOUNT_FLAG_ARRAY,NORMAL_UMOUNT_FLAG_LEN);
			lseek(dev,0,SEEK_SET);
			len = write(dev,data_buf,NORMAL_UMOUNT_BLOCK_LEN);
			if (len != NORMAL_UMOUNT_BLOCK_LEN)
			{
				return;
			}
		}
	}
	close(dev);
	return;
}
#endif
int main(int argc, char **argv)
{
    int i = 0;
    
    turn_off_backlight();
	
    while ( strcmp (service_name[i],"") ) {
        property_set("ctl.stop", service_name[i]);
	i ++;
    }
        
    sleep(1);

    kill_process("/data");    
    sleep(1);
    sync();
    umount2("/data",2);
    umount2("/cache",2);

#ifdef FSCK_TUNE
	mark_shutdown();
#endif

    reboot(RB_POWER_OFF);
    return 0;
}

