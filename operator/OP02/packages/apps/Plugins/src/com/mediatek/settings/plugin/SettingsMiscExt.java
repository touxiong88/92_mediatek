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
package com.mediatek.settings.plugin;

import android.app.Activity;
import android.content.Context;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.ContextWrapper;
import android.database.SQLException;
import android.database.Cursor;
import android.net.Uri;
import android.preference.DialogPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.Telephony;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TabHost.TabSpec;
import android.widget.TabWidget;
import android.widget.TextView;

import com.android.internal.telephony.TelephonyProperties;
import com.mediatek.settings.ext.DefaultSettingsMiscExt;
import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.op02.plugin.R;
import com.mediatek.xlog.Xlog;

import java.util.ArrayList;
import java.util.HashMap;



public class SettingsMiscExt extends DefaultSettingsMiscExt {
    Context mContext;

    private static final int SIM_CARD_1 = 0;
    private static final int SIM_CARD_2 = 1;
    
    private long mApnToUseId = -1;
    private boolean mResult;
    
    private static final String CU_3GNET_NAME = "3gnet";
    private static final String CU_3GWAP_NAME = "3gwap";    
    private static final String CU_MMS_TYPE = "mms";     
    
    private static final String TAG = "SettingsMiscExt";
    
    public SettingsMiscExt(Context context) {
           super(context);
           mContext = getBaseContext();
   }
   
    /*
      *@ return if wifi toggle button could be disabled. 
     */
    public boolean isWifiToggleCouldDisabled(Context ctx) {
        return true;
    }

    /*
     *return tether wifi string.
     */
    public String getTetherWifiSSID(Context ctx) {
        return mContext.getString(
                    com.android.internal.R.string.wifi_tether_configure_ssid_default);
    }
    
    public void setTimeoutPrefTitle(Preference pref) {
        DialogPreference dialogPref = (DialogPreference)pref;
        pref.setTitle(mContext.getString(R.string.screen_timeout_CU));
        dialogPref.setDialogTitle(mContext.getString(R.string.screen_timeout_CU));
    }

    private boolean IsDefaultApn(String apn, String type) {

        if (((apn != null) && (apn.equals(CU_3GNET_NAME)))
            && ((type == null) || (!type.equals(CU_MMS_TYPE)))) {

            return true;
        } else if ((apn != null) && (apn.equals(CU_3GWAP_NAME))) {

            return true;
        }               
        return false;
            
    }
}
