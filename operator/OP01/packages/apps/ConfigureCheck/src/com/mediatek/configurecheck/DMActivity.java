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
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;


public class DMActivity extends MyListActivity {
    private static final String TAG = "CheckTool.DMActivity";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.out.println("DMActivity---onCreate()");

    }

    public void onResume() {
        super.onResume();
        System.out.println("DMActivity---onResume()");
        // items.add(getSMSSetting());
        addItem(getDMAutoReg());
        addItem(getDMNumber());
        addItem(getDMServer());
        addItem(getDMManufacturer());
        addItem(getDMModel());
        addItem(getDMVersion());
    }

    public ItemConfigure getSMSSetting() {

        String autoRegister = "";
        String name = getResources().getString(R.string.SMS_autoregister);
        String resultString;
        int resultImg;
        String note = null;

        try {
            autoRegister = ProtocolActivity.smsAutoRegister();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        if ("1".equals(autoRegister)) {
            resultString = getResources().getString(R.string.Enable);
            resultImg = R.drawable.ic_configurecheck_right;
        } else if ("0".equals(autoRegister)) {
            note = getResources().getString(R.string.turn_on_SMS);
            resultString = getResources().getString(R.string.Disable);
            resultImg = R.drawable.ic_configurecheck_wrong;
        } else {
            note = getResources().getString(R.string.no_support);
            resultString = getResources().getString(R.string.no_support);
            resultImg = R.drawable.ic_configurecheck_warning;
        }
        ItemConfigure item = new ItemConfigure(name, resultString, resultImg,
                note);
        return item;
    }

    public ItemConfigure getDMServer() {
        Uri dmServerUri = Uri
                .parse("content://com.mediatek.providers.mediatekdm/OMSAcc");
        StringBuilder sb = new StringBuilder("");
        int resultImg = R.drawable.ic_configurecheck_warning;
        String note = null;
        String result = null;
        try {
            Cursor cursor = getContentResolver().query(dmServerUri, null,
                    null, null, null);
            while (cursor.moveToNext()) {
                sb.append(cursor.getString(cursor.getColumnIndex("Addr")));
            }
            cursor.close();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        result = sb.toString();
        Log.i(TAG, "getDMServer() ----->" + result);
        if ("".equals(result)) {
            result = getResources().getString(R.string.no_support);
            note = getResources().getString(R.string.dm_content_no_found);
        }
        ItemConfigure item = new ItemConfigure(getResources().getString(
                R.string.DM_server), result, resultImg, note);
        return item;
    }

    public String getSmsRegProvider(String name) {
        StringBuilder sb = new StringBuilder("");
        Uri dmSmsregUri = Uri
                .parse("content://com.mediatek.providers.smsreg");
        try {
            Cursor cursor = getContentResolver().query(dmSmsregUri, null,
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

    public ItemConfigure getDMNumber() {
        String result = getSmsRegProvider("smsNumber");
        int resultImg = R.drawable.ic_configurecheck_warning;
        String note = null;
        if ("".equals(result)) {
            result = getResources().getString(R.string.no_support);
            note = getResources().getString(R.string.smsReg_content_no_found);
        }
        ItemConfigure item = new ItemConfigure(getResources().getString(
                R.string.DM_number), result, resultImg, note);
        return item;
    }

    public ItemConfigure getDMVersion() {
        String result = getSmsRegProvider("version");
        int resultImg = R.drawable.ic_configurecheck_warning;
        String note = null;
        if ("".equals(result)) {
            result = getResources().getString(R.string.no_support);
            note = getResources().getString(R.string.smsReg_content_no_found);
        }
        ItemConfigure item = new ItemConfigure(getResources().getString(
                R.string.DM_version), result, resultImg, note);
        return item;
    }

    public ItemConfigure getDMModel() {
        String result = getSmsRegProvider("product");
        int resultImg = R.drawable.ic_configurecheck_warning;
        String note = null;
        if ("".equals(result)) {
            result = getResources().getString(R.string.no_support);
            note = getResources().getString(R.string.smsReg_content_no_found);
        }
        ItemConfigure item = new ItemConfigure(getResources().getString(
                R.string.DM_model), result, resultImg, note);
        return item;
    }

    public ItemConfigure getDMAutoReg() {
        String result = getSmsRegProvider("enable");
        int resultImg = R.drawable.ic_configurecheck_warning;
        String note = null;
        if ("".equals(result)) {
            result = getResources().getString(R.string.no_support);
            note = getResources().getString(R.string.smsReg_content_no_found);
            note += ". " + getResources().getString(R.string.turn_on_SMS);
        } else if ("yes".equals(result)) {
            resultImg = R.drawable.ic_configurecheck_right;
        } else {
            resultImg = R.drawable.ic_configurecheck_wrong;
            note = getResources().getString(R.string.turn_on_SMS);
        }
        ItemConfigure item = new ItemConfigure(getResources().getString(
                R.string.SMS_autoregister), result, resultImg, note);
        return item;
    }

    public ItemConfigure getDMManufacturer() {
        String result = getSmsRegProvider("manufacturer");
        int resultImg = R.drawable.ic_configurecheck_warning;
        String note = null;
        if ("".equals(result)) {
            result = getResources().getString(R.string.no_support);
            note = getResources().getString(R.string.smsReg_content_no_found);
        }
        ItemConfigure item = new ItemConfigure(getResources().getString(
                R.string.DM_company), result, resultImg, note);
        return item;
    }
};
