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

public class CMMBActivity extends MyListActivity {
    /** Called when the activity is first created. */
    private static final String TAG = "CheckTool.CMMBActivity";
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.out.println("CMMBActivity---onCreate()");

    }

    public void onResume() {
        super.onResume();
        System.out.println("ProtocolActivity---onResume()");
        // add items bellow
        addItem(getCmmbUA());
        addItem(getCmmbNote());

    }

    public ItemConfigure getCmmbUA() {
        String resultStr = null;
        int resultImg = R.drawable.ic_configurecheck_warning;
        String note = null;
        try {
            resultStr = CustomProperties.getString("cmmb", "UserAgent");
        } catch (java.lang.NoClassDefFoundError ex) {
            Log.i(TAG, "can't found class: CustomProperties");
            ex.printStackTrace();
        }
        if (resultStr == null) {
            resultStr = getResources().getString(R.string.no_support);
            note = getResources().getString(R.string.custom_properties_no_found);
        }
        ItemConfigure item = new ItemConfigure(getResources().getString(
                R.string.CMMB_UA), resultStr, resultImg, note);
        return item;
    }
    
    public ItemConfigure getCmmbNote() {
        String title = getResources().getString(R.string.CMMB_RF);
        String resultStr = getResources().getString(R.string.press_for_detail);
        int resultImg = R.drawable.ic_configurecheck_warning;
        String note = getResources().getString(R.string.CMMB_RF_detail);
        
        ItemConfigure item = new ItemConfigure(title, resultStr, resultImg, note);
        return item;
    }
    
};
