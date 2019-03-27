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

import android.os.Bundle;
import android.util.Log;

import com.mediatek.custom.CustomProperties;


public class EigenvalueActivity extends MyListActivity {
    /** Called when the activity is first created. */
    private static final String TAG = "CheckTool.GeneralActivity";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.out.println("GeneralActivity---onCreate()");

    }

    public void onResume() {
        super.onResume();
        System.out.println("GeneralActivity---onResume()");
        
        addItem(getBrowserUA());
        addItem(getBrowserUAUrl());
        addItem(getMmsUA());
        addItem(getMmsUAUrl());
        addItem(getHttpUA());
        addItem(getHttpUAUrl());
        addItem(getRtspUA());
        addItem(getRtspUAUrl());
        addItem(getFMRds());
    }

    public ItemConfigure getBrowserUA() {
        String value = null;
        int resultImg = R.drawable.ic_configurecheck_warning;
        String note = null;
        try {
            value = CustomProperties.getString("browser", "UserAgent");
        } catch (java.lang.NoClassDefFoundError ex) {
            Log.i(TAG, "can't found class: CustomProperties");
            ex.printStackTrace();
        }
        if (value == null) {
            value = getResources().getString(R.string.no_support);
            note = getResources()
                    .getString(R.string.custom_properties_no_found);
        }
        ItemConfigure item = new ItemConfigure(getResources().getString(
                R.string.broswer_UA), value, resultImg, note);
        return item;
    }

    public ItemConfigure getBrowserUAUrl() {
        String value = null;
        int resultImg = R.drawable.ic_configurecheck_warning;
        String note = null;
        try {
            value = CustomProperties.getString("browser", "UAProfileURL");
        } catch (java.lang.NoClassDefFoundError ex) {
            Log.i(TAG, "can't found class: CustomProperties");
            ex.printStackTrace();
        }
        if (value == null) {
            value = getResources().getString(R.string.no_support);
            note = getResources()
                    .getString(R.string.custom_properties_no_found);
        }
        ItemConfigure item = new ItemConfigure(getResources().getString(
                R.string.broswer_UA_profile_URL), value, resultImg, note);
        return item;
    }

    public ItemConfigure getMmsUA() {
        String value = null;
        int resultImg = R.drawable.ic_configurecheck_warning;
        String note = null;
        try {
            value = CustomProperties.getString("mms", "UserAgent");
        } catch (java.lang.NoClassDefFoundError ex) {
            Log.i(TAG, "can't found class: CustomProperties");
            ex.printStackTrace();
        }
        if (value == null) {
            value = getResources().getString(R.string.no_support);
            note = getResources()
                    .getString(R.string.custom_properties_no_found);
        }
        ItemConfigure item = new ItemConfigure(getResources().getString(
                R.string.MMS_UA), value, resultImg, note);
        return item;
    }

    public ItemConfigure getMmsUAUrl() {
        String value = null;
        int resultImg = R.drawable.ic_configurecheck_warning;
        String note = null;
        try {
            value = CustomProperties.getString("mms", "UAProfileURL");
        } catch (java.lang.NoClassDefFoundError ex) {
            Log.i(TAG, "can't found class: CustomProperties");
            ex.printStackTrace();
        }
        if (value == null) {
            value = getResources().getString(R.string.no_support);
            note = getResources()
                    .getString(R.string.custom_properties_no_found);
        }
        ItemConfigure item = new ItemConfigure(getResources().getString(
                R.string.MMS_UA_profile_URL), value, resultImg, note);
        return item;
    }

    public ItemConfigure getHttpUA() {
        String value = null;
        int resultImg = R.drawable.ic_configurecheck_warning;
        String note = null;
        try {
            value = CustomProperties.getString("http_streaming", "UserAgent");
        } catch (java.lang.NoClassDefFoundError ex) {
            Log.i(TAG, "can't found class: CustomProperties");
            ex.printStackTrace();
        }
        if (value == null) {
            value = getResources().getString(R.string.no_support);
            note = getResources()
                    .getString(R.string.custom_properties_no_found);
        }
        ItemConfigure item = new ItemConfigure(getResources().getString(
                R.string.http_streaming_UA), value, resultImg, note);
        return item;
    }

    public ItemConfigure getHttpUAUrl() {
        String value = null;
        int resultImg = R.drawable.ic_configurecheck_warning;
        String note = null;
        try {
            value = CustomProperties
                    .getString("http_streaming", "UAProfileURL");
        } catch (java.lang.NoClassDefFoundError ex) {
            Log.i(TAG, "can't found class: CustomProperties");
            ex.printStackTrace();
        }
        if (value == null) {
            value = getResources().getString(R.string.no_support);
            note = getResources().getString(
                    R.string.custom_properties_no_found);
        }
        ItemConfigure item = new ItemConfigure(getResources().getString(
                R.string.http_streaming_UA_profile_URL), value, resultImg, note);
        return item;
    }

    public ItemConfigure getRtspUA() {
        String value = null;
        int resultImg = R.drawable.ic_configurecheck_warning;
        String note = null;
        try {
            value = CustomProperties.getString("rtsp_streaming", "UserAgent");
        } catch (java.lang.NoClassDefFoundError ex) {
            Log.i(TAG, "can't found class: CustomProperties");
            ex.printStackTrace();
        }
        if (value == null) {
            value = getResources().getString(R.string.no_support);
            note = getResources()
                    .getString(R.string.custom_properties_no_found);
        }
        ItemConfigure item = new ItemConfigure(getResources().getString(
                R.string.rtsp_streaming_UA), value, resultImg, note);
        return item;
    }

    public ItemConfigure getRtspUAUrl() {
        String value = null;
        int resultImg = R.drawable.ic_configurecheck_warning;
        String note = null;
        try {
            value = CustomProperties
                    .getString("rtsp_streaming", "UAProfileURL");
        } catch (java.lang.NoClassDefFoundError ex) {
            Log.i(TAG, "can't found class: CustomProperties");
            ex.printStackTrace();
        }
        if (value == null) {
            value = getResources().getString(R.string.no_support);
            note = getResources()
                    .getString(R.string.custom_properties_no_found);
        }
        ItemConfigure item = new ItemConfigure(getResources().getString(
                R.string.rtsp_streaming_UA_profile_URL), value, resultImg, note);
        return item;
    }

    public ItemConfigure getFMRds() {
        String value = null;
        int resultImg = R.drawable.ic_configurecheck_warning;
        String note = null;
        try {
            value = CustomProperties.getString("fmtransmitter", "RDSValue");
        } catch (java.lang.NoClassDefFoundError ex) {
            Log.i(TAG, "can't found class: CustomProperties");
            ex.printStackTrace();
        }
        if (value == null) {
            value = getResources().getString(R.string.no_support);
            note = getResources()
                    .getString(R.string.custom_properties_no_found);
        }
        ItemConfigure item = new ItemConfigure(getResources().getString(
                R.string.FM_RDS), value, resultImg, note);
        return item;
    }
};
