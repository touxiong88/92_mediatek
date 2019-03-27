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

package com.mediatek.configurecheck;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;

import com.mediatek.custom.CustomProperties;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Properties;

public class GeneralActivity extends MyListActivity {
    /** Called when the activity is first created. */
    private static final String TAG = "CheckTool.GeneralActivity";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.out.println("GeneralActivity---onCreate()");

    }

    public void onResume() {
        super.onResume();
        System.out.println("GeneralActivity---onResume()");
        
        addItem(checkDownload());
        addItem(checkRestoreFactory()); 
        addItem(getWIFIAddr());
        addItem(getRootState());
        addItem(getWIFISSID());
        if (Build.VERSION.SDK_INT >= 11) {
            addItem(getWifiSleepPolicy());
        } else {
            addItem(getGB2WifiSleepPolicy());   
        }
        //      addItem(getLogPath());
        addItem(getAPNSetting());
        addItem(getGPRSMode());
        addItem(getTargetMode());
        addItem(getTargetVersion());
        addItem(getLoadType());
        addItem(getBTName());
        addItem(getEigenvalue());
        addItem(checkFeatureTest());
        addItem(checkbasicApk());
        addItem(check3rdApkPath());
        addItem(checkBgConnect());

        
    }

    public ItemConfigure getRootState() {
        String title = getResources().getString(R.string.root_state);
        String resultStr;
        int resultImg = R.drawable.ic_configurecheck_warning;
        String info = getResources().getString(R.string.root_note);

        String roSecure = SystemProperties.get("ro.secure");
        if ("0".equals(roSecure)) {
            resultStr = getResources().getString(R.string.rooted);           
        } else if ("1".equals(roSecure)) {
            resultStr = getResources().getString(R.string.not_root);
        } else {
            resultStr = "Unknown";
            info = roSecure.toString();
        }

        ItemConfigure item = new ItemConfigure(title, resultStr, resultImg,
                info);
        return item;
    }

    public ItemConfigure getWIFIAddr() {
        String title = getResources().getString(R.string.WLAN_ADDR);
        String resultStr;
        int resultImg = R.drawable.ic_configurecheck_warning;     
        String info = getResources().getString(R.string.different_wifi);

        WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifi.getConnectionInfo(); 
        String macAddress = wifiInfo == null ? null : wifiInfo.getMacAddress();
        resultStr = macAddress; 
        
        if (macAddress == null && !wifi.isWifiEnabled()) {
            resultStr = getResources().getString(R.string.turn_on_wifi);
        }
        ItemConfigure item = new ItemConfigure(title, resultStr, resultImg,
                info);
        return item;
    }

    public ItemConfigure getWIFISSID() {
        String title = getResources().getString(R.string.WLAN_SSID);
        String resultStr;
        int resultImg = R.drawable.ic_configurecheck_warning;
        String info = getResources().getString(R.string.wifi_ssid);

        WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiConfiguration config = wifi.getWifiApConfiguration();
        // WifiInfo info = wifi.getConnectionInfo();
        String ssid = config == null ? null : config.SSID;
        resultStr = ssid;
        if (ssid == null && !wifi.isWifiEnabled()) {
            resultStr = getResources().getString(R.string.turn_on_wifi);
        }
        ItemConfigure item = new ItemConfigure(title, resultStr, resultImg,
                info);
        return item;
    }

    public ItemConfigure getWifiSleepPolicy() {
        String title = getResources().getString(R.string.WLAN_SLEEP_POLICY);
        String resultStr;
        int resultImg = R.drawable.ic_configurecheck_warning;
        String info = null;
        
        int value = Settings.System.getInt(getContentResolver(), 
                                Settings.System.WIFI_SLEEP_POLICY, 
                                Settings.System.WIFI_SLEEP_POLICY_DEFAULT);
        
        if (value == 0) {
            resultImg = R.drawable.ic_configurecheck_right;
            resultStr = getResources().getString(R.string.never);
        } else if (value == 1) {
            resultImg = R.drawable.ic_configurecheck_wrong;
            resultStr = getResources().getString(R.string.in_charge);
            info = getResources().getString(R.string.sleep_plocy_setting);
        } else {
            resultImg = R.drawable.ic_configurecheck_wrong;
            resultStr = getResources().getString(R.string.always);
            info = getResources().getString(R.string.sleep_plocy_setting);
        }
        
        ItemConfigure item = new ItemConfigure(title, resultStr, resultImg,
                info);
        return item;
    }

    public ItemConfigure getGB2WifiSleepPolicy() {
        String title = getResources().getString(R.string.WLAN_SLEEP_POLICY_GB2);
        String resultStr;
        int resultImg = R.drawable.ic_configurecheck_warning;
        String info = null;
        
        int value = Settings.System.getInt(getContentResolver(), 
                                Settings.System.WIFI_SLEEP_POLICY, 
                                Settings.System.WIFI_SLEEP_POLICY_DEFAULT);
        
        if (value == 0) {
            resultImg = R.drawable.ic_configurecheck_right;
            resultStr = getResources().getString(R.string.screen_off_sleep);
        } else if (value == 1) {
            resultImg = R.drawable.ic_configurecheck_wrong;
            resultStr = getResources().getString(R.string.no_sleep_in_charge);
            info = getResources().getString(R.string.sleep_plocy_setting_GB2);
        } else {
            resultImg = R.drawable.ic_configurecheck_wrong;
            resultStr = getResources().getString(R.string.never_sleep);
            info = getResources().getString(R.string.sleep_plocy_setting_GB2);
        }
        
        ItemConfigure item = new ItemConfigure(title, resultStr, resultImg,
                info);
        return item;
    }

    public ItemConfigure getLogPath() {
        String title = getResources().getString(R.string.log_path);
        String resultStr;
        int resultImg;
        String info = null;

        String logPath = SystemProperties.get("persist.radio.log2sd.path");

        if (null != logPath && logPath.equals("/mnt/sdcard2")) {
            resultImg = R.drawable.ic_configurecheck_right;
            resultStr = "external SD card";
        } else if (null != logPath && logPath.equals("/mnt/sdcard")) {
            resultImg = R.drawable.ic_configurecheck_wrong;
            resultStr = "internal SD card";
            info = getResources().getString(R.string.log_file_default_path);
        } else if (null != logPath && logPath.equals("/data")) {
            resultImg = R.drawable.ic_configurecheck_wrong;
            resultStr = "storage path is /data ";
            info = getResources().getString(R.string.log_file_default_path);
        } else {
            logPath = getConfigStoragePath();
            if (null != logPath && logPath.equals("/mnt/sdcard2")) {
                resultImg = R.drawable.ic_configurecheck_right;
                resultStr = "external SD card";
            } else {
               resultImg = R.drawable.ic_configurecheck_wrong;
               resultStr = "internal SD card";
               info = getResources().getString(R.string.log_file_default_path);
            }
        }

        ItemConfigure item = new ItemConfigure(title, resultStr, resultImg,
                info);
        return item;
    }

    private String getConfigStoragePath() {
        String storagePath = null;
        Properties myProps = new Properties();
        FileInputStream mInputStream = null;
        try {
            mInputStream = new FileInputStream("/system/etc/mtklog-config.prop");
            myProps.load(mInputStream);
            storagePath = myProps.getProperty("persist.radio.log2sd.path");
        } catch (FileNotFoundException e) {
            Log.e(TAG, "File Not Found Exception " + e.toString());
        } catch (IOException e) {
            Log.e(TAG, "IOException " + e.toString());
        } finally {
            try {
                mInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return storagePath;
    }

    public ItemConfigure getAPNSetting() {

        String title = getResources().getString(R.string.APN_data_link);
        String resultStr = null;
        int resultImg = R.drawable.ic_configurecheck_wrong;
        String info = getResources().getString(R.string.set_mobile_network);

        ConnectivityManager conMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        Class<?> conMgrClass = null; // ConnectivityManager
        Field iConMgrField = null;
        Object iConMgr = null;
        Class<?> iConMgrClass = null;
        Method getMobileDataEnabledMethod = null;
        try {
            conMgrClass = Class.forName(conMgr.getClass().getName());
            iConMgrField = conMgrClass.getDeclaredField("mService");
            iConMgrField.setAccessible(true);
            iConMgr = iConMgrField.get(conMgr);
            iConMgrClass = Class.forName(iConMgr.getClass().getName());
            getMobileDataEnabledMethod = iConMgrClass
                    .getDeclaredMethod("getMobileDataEnabled");
            getMobileDataEnabledMethod.setAccessible(true);
            boolean connect = (Boolean) getMobileDataEnabledMethod
                    .invoke(iConMgr);
            if (connect) {
                resultImg = R.drawable.ic_configurecheck_right;
                resultStr = getResources().getString(R.string.data_set);

            } else {
                resultImg = R.drawable.ic_configurecheck_wrong;
                resultStr = getResources().getString(R.string.data_unset);
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        ItemConfigure item = new ItemConfigure(title, resultStr, resultImg,
                info);
        return item;
    }

    public ItemConfigure getGPRSMode() {
        String title = getResources().getString(R.string.GPRS_mode);
        String resultStr;
        int resultImg;
        String info = getResources().getString(R.string.set_call_high_priority);

        int flag = Settings.System.getInt(getContentResolver(),
                Settings.System.GPRS_TRANSFER_SETTING,
                Settings.System.GPRS_TRANSFER_SETTING_DEFAULT);
        if (flag == 1) {
            resultImg = R.drawable.ic_configurecheck_right;
            resultStr = getResources()
                    .getString(R.string.call_high_priority);
        } else {
            resultImg = R.drawable.ic_configurecheck_wrong;
            resultStr = getResources()
                    .getString(R.string.data_high_priority);
        }
        ItemConfigure item = new ItemConfigure(title, resultStr, resultImg,
                info);
        return item;
    }

    public ItemConfigure getTargetMode() {
        Uri customerServiceUri = Uri
                .parse("content://com.mediatek.cmcc.provider/phoneinfo");
        StringBuilder sb = new StringBuilder("");
        int resultImg = R.drawable.ic_configurecheck_warning;
        String info = null;
        String resultStr = null;
        try {
            Cursor cursor = getContentResolver().query(customerServiceUri, null,
                    null, null, null);
            while (cursor.moveToNext()) {
                sb.append(cursor.getString(cursor.getColumnIndex("phone_model")));
            }
            cursor.close();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        resultStr = sb.toString();
        Log.i(TAG, "phone_model ----->" + resultStr);
        if ("".equals(resultStr)) {
            resultStr = getResources().getString(R.string.no_support);
            info = getResources().getString(R.string.cmcc_content_no_found);
        }
        ItemConfigure item = new ItemConfigure(getResources().getString(
                R.string.target_model), resultStr, resultImg, info);
        return item;
    }

    public ItemConfigure getTargetVersion() {
        ItemConfigure item = new ItemConfigure(getResources().getString(
                R.string.target_version), Build.VERSION.RELEASE,
                R.drawable.ic_configurecheck_warning, null);
        return item;
    }

    public ItemConfigure getLoadType() {
        String buildType = Build.TYPE;
        int resultImg = R.drawable.ic_configurecheck_right;
        String info = null;
        if ("user".equals(buildType)) {
            resultImg = R.drawable.ic_configurecheck_right;
        } else {
            resultImg = R.drawable.ic_configurecheck_wrong;
            info = getResources().getString(R.string.user_load);
        }
        ItemConfigure item = new ItemConfigure(getResources().getString(
                R.string.load_type), Build.TYPE, resultImg, info);
        return item;
    }

    public ItemConfigure getBTName() {
        String title = getResources().getString(R.string.BT_name);
        String resultStr = null;
        int resultImg = R.drawable.ic_configurecheck_warning;
        String info = getResources().getString(R.string.check_BT_name);

        BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();
        if (bluetooth == null) {
            resultStr = getResources().getString(R.string.no_support_BT);
        } else {
            resultStr = bluetooth.getName();
            if (bluetooth.getName() == null && !bluetooth.isEnabled()) {
                resultStr = getResources().getString(R.string.turn_on_BT);
            }
        }
        ItemConfigure item = new ItemConfigure(title, resultStr, resultImg,
                info);
        return item;
    }

    public ItemConfigure checkFeatureTest() {
        String title = getResources().getString(R.string.feature_check);
        String resultStr = getResources().getString(R.string.press_for_detail);
        int resultImg = R.drawable.ic_configurecheck_warning;
        String info = getResources().getString(R.string.feature_detail);

        ItemConfigure item = new ItemConfigure(title, resultStr, resultImg,
                info);
        return item;
    }

    public ItemConfigure checkDownload() {
        String title = getResources().getString(R.string.download_test);
        String resultStr = getResources().getString(R.string.press_for_detail);
        int resultImg = R.drawable.ic_configurecheck_warning;
        String info = getResources().getString(R.string.download_detail);

        ItemConfigure item = new ItemConfigure(title, resultStr, resultImg,
                info);
        return item;
    }

    public ItemConfigure checkRestoreFactory() {
        String title = getResources().getString(R.string.restore_test);
        String resultStr = getResources().getString(R.string.restore_detail);
        int resultImg = R.drawable.ic_configurecheck_warning;
        String info = null;

        ItemConfigure item = new ItemConfigure(title, resultStr, resultImg,
                info);
        return item;
    }

    public ItemConfigure checkbasicApk() {
        String title = getResources().getString(R.string.app_check);
        String resultStr = getResources().getString(R.string.press_for_detail);
        int resultImg = R.drawable.ic_configurecheck_warning;
        String info = getResources().getString(R.string.app_detail);

        ItemConfigure item = new ItemConfigure(title, resultStr, resultImg,
                info);
        return item;
    }

    public ItemConfigure check3rdApkPath() {
        String title = getResources().getString(R.string.cmcc_apk_path_check);
        String resultStr = getResources().getString(R.string.vendor_path);
        int resultImg = R.drawable.ic_configurecheck_right;
        String info = getResources().getString(R.string.cmcc_apk_path_check_note);
        boolean underSystem = false; 
        StringBuilder string = new StringBuilder("");
        PackageManager pm = getPackageManager();
        
        String[] vendorApps = { "cn.com.fetion", "cmccwm.mobilemusic",
                "cn.emagsoftware.gamehall", "com.aspire.mm",
                "com.aspire.g3wlan.client", "com.cmcc.mv",
                "com.teleav.app.entry", "com.mediatek.modileportal",
                "com.UCMobile.cmcc", "com.sina.weibog3",
                "com.ophone.reader.ui", "com.cmcc.mobilevideo",
                "com.sohu.inputmethod.sogou", "com.huawei.pisa.activity" };
        for (int i = 0; i < vendorApps.length; i++) {
            try {
                ApplicationInfo appInfo = pm.getApplicationInfo(vendorApps[i],
                        PackageManager.GET_META_DATA);
                if (appInfo.sourceDir.contains("system/app")) {
                    underSystem = true;
                    string.append(vendorApps[i] + " ");
                }
            } catch (NameNotFoundException e) {
                e.printStackTrace();
            }
        }
        if (underSystem) {
            string.append(" under " + getResources().getString(R.string.system_path));
            resultStr = string.toString();
            resultImg = R.drawable.ic_configurecheck_wrong;
        }
        ItemConfigure item = new ItemConfigure(title, resultStr, resultImg,
                info);
        return item;
    }

    public ItemConfigure checkBgConnect() {
        String title = getResources().getString(R.string.bg_connect_check);
        String resultStr = getResources().getString(R.string.press_for_detail);
        int resultImg = R.drawable.ic_configurecheck_warning;
        String info = getResources().getString(R.string.bg_connect_check_note);

        ItemConfigure item = new ItemConfigure(title, resultStr, resultImg,
                info);
        return item;
    }

    public ItemConfigure getEigenvalue() {
        String resultStr = null;
        int resultImg = R.drawable.ic_configurecheck_enter;
        String info = null;
        try {
            resultStr = CustomProperties.getString("browser", "UserAgent");
        } catch (java.lang.NoClassDefFoundError ex) {
            Log.i(TAG, "can't found class: CustomProperties");
            ex.printStackTrace();
        }
        if (resultStr == null) {
            resultStr = getResources().getString(R.string.no_support);
            info = getResources()
                    .getString(R.string.custom_properties_no_found);
        } else {
            resultStr = getResources().getString(R.string.press_enter);
        }
        ItemConfigure item = new ItemConfigure(getResources().getString(
                R.string.eigenvalue), resultStr, resultImg, info);
        return item;
    }

};
