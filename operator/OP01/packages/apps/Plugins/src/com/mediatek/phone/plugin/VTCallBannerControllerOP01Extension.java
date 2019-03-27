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

package com.mediatek.phone.plugin;



import java.util.Date;

import android.content.Context;

import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;

import android.text.TextUtils;
import android.text.format.DateFormat;

import android.util.Config;
import android.util.Log;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallerInfo;
import com.android.internal.telephony.Connection;
import com.mediatek.phone.ext.VTCallBannerControllerExtension;
import com.mediatek.op01.plugin.R;

public class VTCallBannerControllerOP01Extension extends VTCallBannerControllerExtension {

    private static final String LOG_TAG = "VTCallBannerControllerOP01Extension";
    private static final boolean DBG = true;
    private ViewGroup mVtCallBanner;
    private Context mContext;

    public void initialize(Context context, ViewGroup vtCallBanner) {
        mContext = context;
        mVtCallBanner = vtCallBanner;
    }

    public boolean updateCallStateWidgets(Call call) {
        if (DBG) {
            log("updateCallStateWidgets(call " + call + ")...");
        }
        final Call.State state = call.getState();

        String callStateLabel = null;
        switch (state) {
            case DISCONNECTING:
            case DISCONNECTED:
                Resources resource = mContext.getResources();
                String packageName = mContext.getPackageName();
                TextView callStateLabelView =
                    (TextView) mVtCallBanner.findViewById(resource.getIdentifier("callStateLabel", "id", packageName));
                if (null == callStateLabelView) {
                    if (DBG) {
                        log("callStateLabelView is null, just return false");
                    }
                    break;
                }
                if (VTInCallScreenFlagsOP01Extension.getInstance().getCallStartDate() > 0) {
                    String sTime = "00";
                    try {
                        Context context = mContext.createPackageContext("com.mediatek.op01.plugin",Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
                        sTime = context.getString(R.string.vt_start_time_from) 
                        + " " + DateFormat.getTimeFormat(mContext).format(new Date(VTInCallScreenFlagsOP01Extension.getInstance()
                            .getCallStartDate()));
                    } catch(NameNotFoundException e){
                        log("no com.mediatek.op01.plugin packages");
                    }
                    if (!TextUtils.isEmpty(callStateLabelView.getText())) {
                        callStateLabel = ", ";
                    }
                    callStateLabel += sTime;
                    if (DBG) {
                        log("updateCallStateWidgets(), callStateLabel = " + callStateLabel);
                    }
                    callStateLabelView.setText(callStateLabelView.getText() + callStateLabel);
                }
                break;
            default:
               break;
        }
        return false;
    }

    public boolean updateState(Call call) {
        if (!VTInCallScreenFlagsOP01Extension.getInstance().getVTFullScreenFlag()) {
            mVtCallBanner.setVisibility(View.VISIBLE);
        }
        return false;
    }
    public void updateDisplayForPerson(CallerInfo info, int presentation,
            boolean isTemporary, Call call, Connection conn) {
        if ((Call.State.DISCONNECTING == call.getState() || Call.State.DISCONNECTED == call.getState())
                && VTInCallScreenFlagsOP01Extension.getInstance().getCallStartDate() > 0) {
             Resources resource = mContext.getResources();
             String packageName = mContext.getPackageName();
             TextView phoneNumberGeoDescription =
                (TextView) mVtCallBanner.findViewById(resource
                        .getIdentifier("phoneNumberGeoDescription", "id", packageName));
             if (null != phoneNumberGeoDescription) {
                 phoneNumberGeoDescription.setVisibility(View.INVISIBLE);
             }
        }
    }

    private static void log(String msg) {
        if (Config.LOGD) {
            Log.d(LOG_TAG, msg);
        }
    }
}
