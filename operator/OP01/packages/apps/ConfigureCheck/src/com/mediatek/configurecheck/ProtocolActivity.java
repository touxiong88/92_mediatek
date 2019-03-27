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

import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;

import com.android.internal.telephony.gemini.GeminiPhone;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneBase;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.PhoneProxy;
import com.android.internal.telephony.RIL;

import com.mediatek.common.featureoption.FeatureOption;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProtocolActivity extends MyListActivity {
    /** Called when the activity is first created. */
    private static final String TAG = "CheckTool.ProtocolActivity";

    private final Handler mModemATHander = new Handler() {
        public final void handleMessage(Message msg) {
            Log.i(TAG, "Receive msg form Modem mode query");
            String title = getResources().getString(R.string.modem_mode);
            String resultString = null;
            int resultImg = R.drawable.ic_configurecheck_wrong;
            String note = null;

            boolean isJB2 = (Build.VERSION.SDK_INT >= 17);
            AsyncResult ar = (AsyncResult) msg.obj;
            if (ar.exception == null) {
                String data[] = (String[]) ar.result;

                if (null != data) {
                    Log.i(TAG, "data length is " + data.length);
                } else {
                    Log.i(TAG, "The returned data is wrong.");
                }
                int i = 0;
                for (String str : data) {
                    Log.i(TAG, "data[" + i + "] is : " + str);
                    i++;
                }
                if (data[0].length() > 6) {
                    String mode = data[0].substring(7, data[0].length());
                    Log.i(TAG, "mode is " + mode);
                    if (mode.length() >= 3) {
                       String subMode = mode.substring(0, 1);
                       String subCtaMode = mode.substring(2, mode.length());
                       Log.d(TAG, "subMode is " + subMode);
                       Log.d(TAG, "subCtaMode is " + subCtaMode);
                        if ("0".equals(subMode)) {
                            resultString = "None";
                            resultImg = R.drawable.ic_configurecheck_right;
                        } else if ("1".equals(subMode)) {
                            if (isJB2) {
                                resultString = "Integrity Off";
                            } else {
                                resultString = "CTA";
                            }
                        } else if ("2".equals(subMode)) {
                            resultString = "FTA";
                        } else if ("3".equals(subMode)) {
                            resultString = "IOT";
                        } else if ("4".equals(subMode)) {
                            resultString = "OPERATOR";
                        } else if (isJB2 && "5".equals(subMode)) {
                            resultString = "Factory";
                        }                        
                    } else {
                        Log.i(TAG, "mode len is " + mode.length());
                    }
                } else {
                    Log.i(TAG, "The data returned is not right.");
                }
            } else {
                Log.i(TAG, "mModemATHander Query failed");
            }

            if(resultString == null) {
                resultString = "Query failed";
            }
        
            if(resultImg == R.drawable.ic_configurecheck_wrong) {
                note = getResources().getString(R.string.set_modem_mode);
            }
            
            ItemConfigure item = new ItemConfigure(title, resultString,
                resultImg, note);
            addItem(item);    
        }
    };

    private final Handler mNetworkQueryHandler = new Handler() {
        public final void handleMessage(Message msg) {
            Log.i(TAG, "Receive msg form network mode query");
            String title = getResources().getString(R.string.network_mode);
            String resultString = "Query failed";
            int resultImg = R.drawable.ic_configurecheck_warning;
            String note = null;
            
            AsyncResult ar = (AsyncResult) msg.obj;
            if (ar.exception == null) {
                int type = ((int[]) ar.result)[0];
                Log.d(TAG, "Get Preferred Type " + type);
                switch (type) {
                    case 0: // TD-SCDMA preferred
                        resultString = "TD-SCDMA preferred";
                        break;
                    case 1: // GSM only
                        resultString = "GSM only";
                        break;
                    case 2: // TD-SCDMA only
                        resultString = "TD-SCDMA only";
                        break;
                    case 3: // GSM/TD-SCDMA(auto)
                        resultString = "GSM/TD-SCDMA(auto)";
                        break;
                    default:
                        break;
                }
            } else {
                Log.i(TAG, "mNetworkQueryHandler Query failed");
            }
            
            ItemConfigure item = new ItemConfigure(title, resultString,
                resultImg, note);
            addItem(item);
        }
    };

    private final Handler mNetworkSelectionModeHandler = new Handler() {
        public final void handleMessage(Message msg) {
            Log.i(TAG, "Receive msg form network slection mode");
            String title = getResources().getString(R.string.PLMN_setting);
            String resultString = "Query failed";
            int resultImg = R.drawable.ic_configurecheck_wrong;
            String note = null;
            
            AsyncResult ar = (AsyncResult) msg.obj;
            if (ar.exception == null) {
                int auto = ((int[]) ar.result)[0];
                Log.d(TAG, "Get Selection Type " + auto);
                if(auto == 0) {
                    resultString = getResources().getString(R.string.auto_select);
                    resultImg = R.drawable.ic_configurecheck_right;
                } else {
                    resultString = getResources().getString(R.string.manual_select);
                    note = getResources().getString(R.string.set_auto_select);           
                }   
            } else {
                Log.i(TAG, "mNetworkSelectionModeHandler Query failed");
            }
            
            ItemConfigure item = new ItemConfigure(title, resultString,
                resultImg, note);
            addItem(item);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "ProtocolActivity---onCreate()");

    }

    public void onResume() {
        super.onResume();
        Log.i(TAG, "ProtocolActivity---onResume()");

        addItem(turnOffAutoConnect());
        addItem(checkAntenna());
        addItem(getAutoTimeSetting());
        if (Build.VERSION.SDK_INT >= 11) {
          addItem(getAutoTimeZoneSetting());
        }
        addItem(getDMAutoReg());
        // addItem(getSMSSetting());
        // addItem(getIVSRSetting());
        addItem(getGPRSSetting());
        addItem(isAtciServiceRunning());
        getCFUSetting();
        getModemModeSetting();
        getNetworkMode();
        getNetworkSelectionMode();
    }

    public ItemConfigure turnOffAutoConnect() {
        String title = getResources().getString(R.string.turnoff_data);
        String resultStr = getResources().getString(R.string.turnoff_data_detail);
        int resultImg = R.drawable.ic_configurecheck_warning;
        String note = null;
        
        ItemConfigure item = new ItemConfigure(title, resultStr, resultImg, note);
        return item;
    }

    public ItemConfigure checkAntenna() {
        String title = getResources().getString(R.string.check_antenna);
        String resultStr = getResources().getString(R.string.check_antenna_detail);
        int resultImg = R.drawable.ic_configurecheck_warning;
        String note = null;
        
        ItemConfigure item = new ItemConfigure(title, resultStr, resultImg, note);
        return item;
    }

    public ItemConfigure getAutoTimeSetting() {
        String name = getResources().getString(R.string.time_auto_sync);
        String resultString;
        int resultImg;
        String note = null;
        boolean autoTimeEnabled = false;

        try {
            autoTimeEnabled = (Settings.System.getInt(getContentResolver(),
                    Settings.System.AUTO_TIME) > 0);
        } catch (SettingNotFoundException e) {
            e.printStackTrace();
        }
        if (autoTimeEnabled) {
            resultString = getResources().getString(R.string.Enable);
            resultImg = R.drawable.ic_configurecheck_wrong;
            note = getResources().getString(R.string.turn_off_auto_time);
        } else {
            resultString = getResources().getString(R.string.Disable);
            resultImg = R.drawable.ic_configurecheck_right;
        }
        ItemConfigure item = new ItemConfigure(name, resultString, resultImg,
                note);
        return item;
    }

    public ItemConfigure getAutoTimeZoneSetting() {
        String name = getResources().getString(R.string.time_zone_auto_sync);
        String resultString;
        int resultImg;
        String note = null;
        boolean autoTimeZoneEnabled = false;

        try {
            autoTimeZoneEnabled = (Settings.System.getInt(getContentResolver(),
                    "auto_time_zone") > 0);
        } catch (SettingNotFoundException e) {
            e.printStackTrace();
        }
        if (autoTimeZoneEnabled) {
            resultString = getResources().getString(R.string.Enable);
            resultImg = R.drawable.ic_configurecheck_wrong;
            note = getResources().getString(R.string.turn_off_auto_time_zone);
        } else {
            resultString = getResources().getString(R.string.Disable);
            resultImg = R.drawable.ic_configurecheck_right;
        }
        ItemConfigure item = new ItemConfigure(name, resultString, resultImg,
                note);
        return item;
    }

    public String getSmsRegProvider(String name) {
        StringBuilder sb = new StringBuilder("");
        Uri dmSmsregUrl = Uri
                .parse("content://com.mediatek.providers.smsreg");
        try {
            Cursor cursor = getContentResolver().query(dmSmsregUrl, null,
                    null, null, null);
            while (cursor.moveToNext()) {
                sb.append(cursor.getString(cursor.getColumnIndex(name)));
            }
            cursor.close();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }    
        Log.i(TAG, "getSmsRegProvider(" + name + ") ----->" + sb.toString());
        return sb.toString();
    }

    public ItemConfigure getDMAutoReg() {
        String result = getSmsRegProvider("enable");
        int resultImg;
        String note = null;
        if ("".equals(result)) {
            result = getResources().getString(R.string.no_support);
            resultImg = R.drawable.ic_configurecheck_warning;
            note = getResources().getString(R.string.smsReg_content_no_found);
            note += ". " + getResources().getString(R.string.turn_off_SMS);
        } else if ("no".equals(result)) {
            resultImg = R.drawable.ic_configurecheck_right;
        } else {
            resultImg = R.drawable.ic_configurecheck_wrong;
            note = getResources().getString(R.string.turn_off_SMS);
        }
        ItemConfigure item = new ItemConfigure(getResources().getString(
                R.string.SMS_autoregister), result, resultImg, note);
        return item;
    }

    public ItemConfigure getIVSRSetting() {
        String name = getResources().getString(R.string.IVSR_setting);
        String resultString;
        int resultImg;
        String note = null;

        long ivsrCheckValue = Settings.System.getLong(getContentResolver(),
                Settings.System.IVSR_SETTING,
                Settings.System.IVSR_SETTING_DISABLE);
        if (ivsrCheckValue == 0) {
            resultString = getResources().getString(R.string.Disable);
            resultImg = R.drawable.ic_configurecheck_right;
        } else {
            resultString = getResources().getString(R.string.Enable);
            resultImg = R.drawable.ic_configurecheck_wrong;
            note = getResources().getString(R.string.turn_off_IVSR);
        }
        ItemConfigure item = new ItemConfigure(name, resultString, resultImg,
                note);
        return item;
    }

    public ItemConfigure getGPRSSetting() {
        String name = getResources().getString(R.string.GPRS_always_on);
        String resultString;
        int resultImg;
        String note = null;

        int gprsAttachType = SystemProperties.getInt(
                "persist.radio.gprs.attach.type", 1);
        if (gprsAttachType == 1) {
            resultString = getResources().getString(R.string.Enable);
            resultImg = R.drawable.ic_configurecheck_right;
        } else {
            resultString = getResources().getString(R.string.Disable);
            resultImg = R.drawable.ic_configurecheck_wrong;
            note = getResources().getString(R.string.turn_on_GPRS);
        }
        ItemConfigure item = new ItemConfigure(name, resultString, resultImg,
                note);
        return item;
    }

    public ItemConfigure isAtciServiceRunning() {
        String name = getResources().getString(R.string.AT_command);
        String resultString = getResources().getString(R.string.AT_server_error);
        int resultImg = R.drawable.ic_configurecheck_wrong;
        String note = getResources().getString(R.string.AT_server_error);
        File file;

        if (Build.VERSION.SDK_INT >= 11) {
            file = new File("/system/bin", "atcid");
        } else {
            file = new File("/system/bin", "atci");
        }

       if (file.exists()) {
                resultImg = R.drawable.ic_configurecheck_right;
                resultString = getResources().getString(R.string.AT_server_ok);
                note = null;
        }
        
        ItemConfigure item = new ItemConfigure(name, resultString, resultImg,
                note);
        return item;
    }

    public void getCFUSetting() {
        Log.i(TAG, "getCFUSetting");
        String title = getResources().getString(R.string.CFU_setting);
        String resultString;
        int resultImg = R.drawable.ic_configurecheck_wrong;
        String note = null;
        
        String cfuSetting = SystemProperties.get(PhoneConstants.CFU_QUERY_TYPE_PROP, 
                PhoneConstants.CFU_QUERY_TYPE_DEF_VALUE);
        Log.i(TAG, "cfuSetting = " + cfuSetting);
        
        if (cfuSetting.equals("0")) {
            resultString = getResources().getString(R.string.default_value);
            resultImg = R.drawable.ic_configurecheck_right;
        } else if (cfuSetting.equals("1")) {
            resultString = getResources().getString(R.string.always_not_query);
            resultImg = R.drawable.ic_configurecheck_wrong;
        } else if (cfuSetting.equals("2")) {
            resultString = getResources().getString(R.string.always_query);
            resultImg = R.drawable.ic_configurecheck_wrong;
        } else {
            resultString = getResources().getString(R.string.invalid_return);
            resultImg = R.drawable.ic_configurecheck_wrong;
        }
        if (resultImg == R.drawable.ic_configurecheck_wrong) {
            note = getResources().getString(R.string.set_CFU);
        }
        ItemConfigure item = new ItemConfigure(title, resultString,
                resultImg, note);
        addItem(item);
    }

    public void getModemModeSetting() {
        Log.i(TAG, "getModemModeSetting");
        String cmd[] = new String[2];
        cmd[0] = "AT+EPCT?";
        cmd[1] = "+EPCT:";
        
        Phone mPhone = PhoneFactory.getDefaultPhone();
        mPhone.invokeOemRilRequestStrings(cmd, mModemATHander
                .obtainMessage());
    }

    public void getNetworkMode() {
        Log.i(TAG, "getNetworkMode");
        
        Phone mPhone = null;
        GeminiPhone mGeminiPhone = null;
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            mGeminiPhone = (GeminiPhone) PhoneFactory.getDefaultPhone();
            mGeminiPhone.getPreferredNetworkTypeGemini(mNetworkQueryHandler.obtainMessage(),
                    PhoneConstants.GEMINI_SIM_1);
        } else {
            mPhone = PhoneFactory.getDefaultPhone();
            mPhone.getPreferredNetworkType(mNetworkQueryHandler.obtainMessage());
        }
    }

    public void getNetworkSelectionMode() {
        Log.i(TAG, "getNetworkSelectionMode");
        Phone mPhone = null;
        
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            GeminiPhone mGeminiPhone = (GeminiPhone) PhoneFactory.getDefaultPhone();
            mPhone = mGeminiPhone.getDefaultPhone();
        } else {
            mPhone = PhoneFactory.getDefaultPhone();
        }
        ((PhoneBase)((PhoneProxy)mPhone).getActivePhone()).mCM.
            getNetworkSelectionMode(mNetworkSelectionModeHandler.obtainMessage());
     }

    public static String smsAutoRegister() throws android.os.RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        byte[] smsreg;
        String ret = "";
        IBinder mRemote = ServiceManager.getService("DMAgent");

        try {
            data.writeInterfaceToken("DMAgent");
            mRemote.transact(IBinder.FIRST_CALL_TRANSACTION + 8, data, reply, 0);
            smsreg = reply.createByteArray();
            if (smsreg != null) {
                ret = new String(smsreg, "UTF-8");
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } finally {
            reply.recycle();
            data.recycle();
        }
        Log.i(TAG, "ret =" + ret);
        return ret;
    }

    public ItemConfigure getSMSSetting() {
        String name = getResources().getString(R.string.SMS_autoregister);
        String resultString;
        int resultImg;
        String note = null;
        String autoRegister = "";

        try {
            autoRegister = smsAutoRegister();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "autoRegister =" + autoRegister);
        if ("1".equals(autoRegister)) {
            resultString = getResources().getString(R.string.Enable);
            resultImg = R.drawable.ic_configurecheck_wrong;
            note = getResources().getString(R.string.turn_off_SMS);
        } else if ("0".equals(autoRegister)) {
            resultString = getResources().getString(R.string.Disable);
            resultImg = R.drawable.ic_configurecheck_right;
        } else {
            note = getResources().getString(R.string.no_support);
            resultString = getResources().getString(R.string.no_support);
            resultImg = R.drawable.ic_configurecheck_warning;
        }
        ItemConfigure item = new ItemConfigure(name, resultString, resultImg,
                note);
        return item;
    }
}
