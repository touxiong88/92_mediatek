/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
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

package com.mediatek.mediatekdm;

import android.content.Context;
import android.os.RemoteException;

import com.mediatek.mediatekdm.ext.MTKPhone;
import com.mediatek.common.dm.DmAgent;

import java.io.File;

public final class DmConst {
    public static final class TAG {
        public static final String APPLICATION = "DM/Application";
        public static final String CLIENT = "DM/Client";
        public static final String SERVICE = "DM/Service";
        public static final String RECEIVER = "DM/Receiver";
        public static final String COMMON = "DM/Common";
        public static final String CONTROLLER = "DM/Controller";
        public static final String DATABASE = "DM/Database";
        public static final String CONNECTION = "DM/Connection";
        public static final String XML = "DM/XML";
        public static final String NOTIFICATION = "DM/Notification";
        public static final String NODEIOHANDLER = "DM/NodeIOHandler";

        public static final String SESSION = "DM/SessionHandler";
        public static final String PL = "DM/PL";
        public static final String BOOTSTRAP = "DM/Bootstrap";
        public static final String CP = "DM/CP";
        public static final String FUMO = "DM/Fumo";
        public static final String LAWMO = "DM/Lawmo";
        public static final String MMI = "DM/MMI";
        public static final String SCOMO = "DM/Scomo";
        public static final String SMSREG = "DM/SmsReg";
        public static final String DL = "DM/DL";

        public static final String DEBUG = "DM/Debug";
        public static final String PROVIDER = "DM/ContentProvider";
    }

    public static final class TagName {
        public static final String SETTING = "Setting";
        public static final String NAME = "name";
        public static final String NODE = "node";
        public static final String TEXT = "text";
        public static final String TIMING = "timing";
    }

    public static final class Path {
        public static final String PATH_IN_SYSTEM = "/system/etc/dm/";
        public static final String TEST_PATH_IN_SYSTEM = PATH_IN_SYSTEM + "test/";
        public static final String PRODUCTIVE_PATH_IN_SYSTEM = PATH_IN_SYSTEM + "productive/";

        // SmsReg(Keep this sync with SmsReg)
        // TODO Use content provider to access information of SmsReg.
        public static final String SMSREG_CONFIG_FILE = "smsSelfRegConfig.xml";

        // Common
        public static final String DM_TREE_FILE = "tree.xml";
        public static final String DM_CONFIG_FILE = "config.xml";
        public static final String DM_APN_INFO_FILE = "DmApnInfo.xml";
        public static final String DM_OPERATION_FOLDER = "operations";

        // FUMO
        public static final String REMINDER_CONFIG_FILE = "reminder.xml";
        public static final String DELTA_FILE = "delta.zip";
        public static final String RESUME_FILE = "dlresume.dat";
        public static final String FOTA_FLAG_FILE = "fota_executing";
        
        // LAWMO
        public static final String WIPE_FILE = "wipe";
        
        public static String getSwitchValue() {
            try {
                byte[] switchValue = MTKPhone.getDmAgent().getSwitchValue();
                if (switchValue != null) {
                    return new String(switchValue);
                } else {
                    return "0";
                }
            } catch (RemoteException e) {
                throw new Error(e);
            }
        }
        
        public static String getPathInSystem(String fileName) {
            if (getSwitchValue().equals("1")) {
                return PRODUCTIVE_PATH_IN_SYSTEM + fileName;
            } else {
                return TEST_PATH_IN_SYSTEM + fileName;
            }
        }
        
        public static String getPathInData(Context context, String fileName){
            return context.getFilesDir().getAbsolutePath() + File.separator + fileName;
        }
    }

    public static final class NodeUri {
        public static final String FUMO_ROOT = "./FwUpdate";
        public static final String LAWMO_ROOT = "./LAWMO";
        public static final String SCOMO_ROOT = "./SCOMO";
        public static final String DEVDETAIL_SWV = "./DevDetail/SwV";
        public static final String DEVINFO_DEVID = "./DevInfo/DevId";
    }

    public static final class IntentAction {
        public static final String DM_WAP_PUSH = "android.provider.Telephony.WAP_PUSH_RECEIVED";
        public static final String DM_BOOT_COMPLETE = "android.intent.action.BOOT_COMPLETED";
        /** remind user to update firmware at time user specified after package is download */
        public static final String DM_REMINDER = "com.mediatek.mediatekdm.REMINDER";
        public static final String DM_DL_FOREGROUND = "com.mediatek.mediatekdm.DMDOWNLOADINGFOREGROUND";
        public static final String DM_SWUPDATE = "com.mediatek.DMSWUPDATE";
        public static final String PROXY_CHANGED = "android.intent.action.PROXY_CHANGE";
        public static final String NIA_RECEIVED = "com.mediatek.mediatekdm.NIA_RECEIVED";
        public static final String DM_START = "com.mediatek.mediatekdm.DM_STARTED";
        /** start process NIA */
        public static final String DM_NOTIFICATION_RESPONSE = "com.mediatek.mediatekdm.NOTIFICATION_RESPONSE";
        public static final String DM_NOTIFICATION_TIMEOUT = "com.mediatek.mediatekdm.NOTIFICATION_TIMEOUT";
        public static final String DM_ALERT_RESPONSE = "com.mediatek.mediatekdm.ALERT_RESPONSE";
        public static final String DM_ALERT_TIMEOUT = "com.mediatek.mediatekdm.ALERT_TIMEOUT";
        /** alert/NIA timeout without response from user */
        public static final String NET_DETECT_TIMEOUT = "com.mediatek.mediatekdm.NETDETECTTIMEOUT";
        public static final String DM_FACTORYSET = "com.mediatek.mediatekdm.FACTORYSET";
        public static final String ACTION_DM_SERVE = "com.mediatek.mediatekdm.DMSERVE";
        /**
         * Broadcast from {link @DmService} to notify UI to close the NIA information.
         */
        public static final String DM_CLOSE_DIALOG = "com.mediatek.mediatekdm.CLOSE_DIALOG";
        /**
         * Continue uncompleted operations before reboot. All the param is boolean type 
         * and the valid value is true.
         * @param "update": 
         * @param "operation"
         * @param "wipe"
         */
        public static final String REBOOT_CHECK = "com.mediatek.mediatekdm.REBOOT_CHECK";
        public static final String SCOMO_SCAN_PACKAGE = "com.mediatek.mediatekdm.SCOMO_SCAN_PKG";
        /**
         * Trigger FUMO session by client polling
         */
        public static final String FUMO_CLIENT_POLLING = "com.mediatek.mediatekdm.FUMO_CI";

        public static final String GEMINI_DATA_RECOVERED = "com.mediatek.mediatekdm.GEMINI_DATA_RECOVERED";

        public static final String CHECK_NETWORK = "com.mediatek.mediatekdm.CHECK_NETWORK";

        public static final String ONLY_START_SERVICE = "com.mediatek.mediatekdm.ONLY_START_SERVICE";
    }

    public static final class IntentType {
        public static final String DM_NIA = "application/vnd.syncml.notification";
        public static final String BOOTSTRAP_NIA = "application/vnd.syncml.dm.wbxml";
    }

    public static final class LawmoResult {
        public static final int OPERATION_SUCCESSSFUL = 200;
    }

    public static final class LawmoStatus {
        public static final int FULLY_LOCK = 10;
        public static final int PARTIALY_LOCK = 20;
    }

    public static final class FumoResult {
        public static final int OPERATION_SUCCESSSFUL = 200;
    }

    public static final class NotificationInteractionType {
        // Alert
        public static final int TYPE_ALERT_1100 = 1100;
        public static final int TYPE_ALERT_1101 = 1101;
        public static final int TYPE_ALERT_1102 = 1102;
        public static final int TYPE_ALERT_1103 = 1103;
        public static final int TYPE_ALERT_1104 = 1104;
        // Notification
        public static final int TYPE_NOTIFICATION_VISIBLE = 3;
        public static final int TYPE_NOTIFICATION_INTERACT = 4;
        // FUMO
        public static final int TYPE_FUMO_NEW_VERSION = 11;
        public static final int TYPE_FUMO_DOWNLOADING = 12;
        public static final int TYPE_FUMO_DOWNLOAD_COMPLETED = 13;
        // SCOMO
        public static final int TYPE_SCOMO_NOTIFICATION = 21;
        
        public static final int TYPE_INVALID = -1;
    }
}
