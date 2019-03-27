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

package com.mtk.telephony;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;

import com.mediatek.common.featureoption.FeatureOption;
import com.android.internal.telephony.gemini.GeminiPhone;

public class HotSwapDevToolActivity extends Activity {
    private static final String LOG_TAG = "PHONE";

    private static final String AT_CMD_SIM_PLUG_OUT = "AT+ESIMTEST=17";
    private static final String AT_CMD_SIM_PLUG_IN = "AT+ESIMTEST=18";
    private static final String AT_CMD_SIM_PLUG_IN_ALL = "AT+ESIMTEST=19";
    private static final String AT_CMD_SIM_MISSING = "AT+ESIMTEST=65";
    private static final String AT_CMD_SIM_RECOVERY = "AT+ESIMTEST=66";
    private static final int MAX_GEMINI_SIM_NUM = 4;
    private Phone mPhone;
    private static Button[] mPlugOutSim = new Button[MAX_GEMINI_SIM_NUM];
    private static Button[] mPlugInSim = new Button[MAX_GEMINI_SIM_NUM];
    private static Button[] mMissingSim = new Button[MAX_GEMINI_SIM_NUM];
    private static Button[] mRecoverySim = new Button[MAX_GEMINI_SIM_NUM];
    private Button mPlugOutAllSims;
    private Button mPlugInAllSims;
    
    private OnClickListener mOnClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == mPlugOutSim[0]) {
                logd("Plug out SIM1");
                String cmdStr[] = {AT_CMD_SIM_PLUG_OUT, ""};
                if (FeatureOption.MTK_GEMINI_SUPPORT)
					((GeminiPhone)mPhone).invokeOemRilRequestStringsGemini(cmdStr, null, PhoneConstants.GEMINI_SIM_1);
                else
                    mPhone.invokeOemRilRequestStrings(cmdStr, null);
            } else if (v == mPlugOutSim[1]) {
                logd("Plug out SIM2");
                String cmdStr[] = {AT_CMD_SIM_PLUG_OUT, ""};
                if (FeatureOption.MTK_GEMINI_SUPPORT)
					((GeminiPhone)mPhone).invokeOemRilRequestStringsGemini(cmdStr, null, PhoneConstants.GEMINI_SIM_2);
                else
                    mPhone.invokeOemRilRequestStrings(cmdStr, null);
            } else if (v == mPlugOutSim[2]) {
                logd("Plug out SIM3");
                String cmdStr[] = {AT_CMD_SIM_PLUG_OUT, ""};
                if (FeatureOption.MTK_GEMINI_SUPPORT)
					((GeminiPhone)mPhone).invokeOemRilRequestStringsGemini(cmdStr, null, PhoneConstants.GEMINI_SIM_3);
                else
                    mPhone.invokeOemRilRequestStrings(cmdStr, null);
            } else if (v == mPlugOutSim[3]) {
                logd("Plug out SIM4");
                String cmdStr[] = {AT_CMD_SIM_PLUG_OUT, ""};
                if (FeatureOption.MTK_GEMINI_SUPPORT)
					((GeminiPhone)mPhone).invokeOemRilRequestStringsGemini(cmdStr, null, PhoneConstants.GEMINI_SIM_4);
                else
                    mPhone.invokeOemRilRequestStrings(cmdStr, null);
            } else if (v == mPlugOutAllSims) {
                logd("Plug out all SIMs");
                String cmdStr[] = {AT_CMD_SIM_PLUG_OUT, ""};
                if (FeatureOption.MTK_GEMINI_SUPPORT) {
                    ((GeminiPhone)mPhone).invokeOemRilRequestStringsGemini(cmdStr, null, 0);
                } else {
                    mPhone.invokeOemRilRequestStrings(cmdStr, null);
                }
            } else if (v == mPlugInSim[0]) {
                logd("Plug in SIM1");
                String cmdStr[] = {AT_CMD_SIM_PLUG_IN, ""};
                if (FeatureOption.MTK_GEMINI_SUPPORT)
					((GeminiPhone)mPhone).invokeOemRilRequestStringsGemini(cmdStr, null, PhoneConstants.GEMINI_SIM_1);
                else
                    mPhone.invokeOemRilRequestStrings(cmdStr, null);
            } else if (v == mPlugInSim[1]) {
                logd("Plug in SIM2");
                String cmdStr[] = {AT_CMD_SIM_PLUG_IN, ""};
                if (FeatureOption.MTK_GEMINI_SUPPORT)
					((GeminiPhone)mPhone).invokeOemRilRequestStringsGemini(cmdStr, null, PhoneConstants.GEMINI_SIM_2);
                else
                    mPhone.invokeOemRilRequestStrings(cmdStr, null);
            }  else if (v == mPlugInSim[2]) {
                logd("Plug in SIM3");
                String cmdStr[] = {AT_CMD_SIM_PLUG_IN, ""};
                if (FeatureOption.MTK_GEMINI_SUPPORT)
					((GeminiPhone)mPhone).invokeOemRilRequestStringsGemini(cmdStr, null, PhoneConstants.GEMINI_SIM_3);
                else
                    mPhone.invokeOemRilRequestStrings(cmdStr, null);
            }  else if (v == mPlugInSim[3]) {
                logd("Plug in SIM4");
                String cmdStr[] = {AT_CMD_SIM_PLUG_IN, ""};
                if (FeatureOption.MTK_GEMINI_SUPPORT)
					((GeminiPhone)mPhone).invokeOemRilRequestStringsGemini(cmdStr, null, PhoneConstants.GEMINI_SIM_4);
                else
                    mPhone.invokeOemRilRequestStrings(cmdStr, null);
            } else if (v == mPlugInAllSims) {
                logd("Plug in all SIMs");
                String cmdStr[] = {AT_CMD_SIM_PLUG_IN_ALL, ""};
                if (FeatureOption.MTK_GEMINI_SUPPORT) {
                    ((GeminiPhone)mPhone).invokeOemRilRequestStringsGemini(cmdStr, null, 0);
                } else {
                    mPhone.invokeOemRilRequestStrings(cmdStr, null);
                }
            } else if (v == mMissingSim[0]) {
                logd("SIM1 missing");
                String cmdStr[] = {AT_CMD_SIM_MISSING, ""};
                if (FeatureOption.MTK_GEMINI_SUPPORT)
					((GeminiPhone)mPhone).invokeOemRilRequestStringsGemini(cmdStr, null, PhoneConstants.GEMINI_SIM_1);
                else
                    mPhone.invokeOemRilRequestStrings(cmdStr, null);
            } else if (v == mMissingSim[1]) {
                logd("SIM2 missing");
                String cmdStr[] = {AT_CMD_SIM_MISSING, ""};
                if (FeatureOption.MTK_GEMINI_SUPPORT)
					((GeminiPhone)mPhone).invokeOemRilRequestStringsGemini(cmdStr, null, PhoneConstants.GEMINI_SIM_2);
                else
                    mPhone.invokeOemRilRequestStrings(cmdStr, null);
            } else if (v == mRecoverySim[0]) {
                logd("SIM1 recovering");
                String cmdStr[] = {AT_CMD_SIM_RECOVERY, ""};
                if (FeatureOption.MTK_GEMINI_SUPPORT)
					((GeminiPhone)mPhone).invokeOemRilRequestStringsGemini(cmdStr, null, PhoneConstants.GEMINI_SIM_1);
                else
                    mPhone.invokeOemRilRequestStrings(cmdStr, null);
            } else if (v == mRecoverySim[1]) {
                logd("SIM2 recovering");
                String cmdStr[] = {AT_CMD_SIM_RECOVERY, ""};
                if (FeatureOption.MTK_GEMINI_SUPPORT)
					((GeminiPhone)mPhone).invokeOemRilRequestStringsGemini(cmdStr, null, PhoneConstants.GEMINI_SIM_2);
                else
                    mPhone.invokeOemRilRequestStrings(cmdStr, null);
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.hot_swap_dev_tool);

        mPlugOutSim[0] = (Button) findViewById(R.id.btn_plug_out_sim1);
        mPlugOutSim[1] = (Button) findViewById(R.id.btn_plug_out_sim2);
        mPlugOutSim[2] = (Button) findViewById(R.id.btn_plug_out_sim3);
        mPlugOutSim[3] = (Button) findViewById(R.id.btn_plug_out_sim4);
        
        mPlugInSim[0] = (Button) findViewById(R.id.btn_plug_in_sim1);
        mPlugInSim[1] = (Button) findViewById(R.id.btn_plug_in_sim2);
        mPlugInSim[2] = (Button) findViewById(R.id.btn_plug_in_sim3);
        mPlugInSim[3] = (Button) findViewById(R.id.btn_plug_in_sim4);
        
        mMissingSim[0] = (Button) findViewById(R.id.btn_missing_sim1);
        mMissingSim[1] = (Button) findViewById(R.id.btn_missing_sim2);
        
        mRecoverySim[0] = (Button) findViewById(R.id.btn_recovery_sim1);
        mRecoverySim[1] = (Button) findViewById(R.id.btn_recovery_sim2);

        mPlugOutAllSims = (Button) findViewById(R.id.btn_plug_out_all_sims);
        mPlugInAllSims = (Button) findViewById(R.id.btn_plug_in_all_sims);

        for (int i=0; i < PhoneConstants.GEMINI_SIM_NUM; i++) {
            logd("OnClickListener plug out sim" + i);
            logd("OnClickListener plug in sim" + i);
            mPlugOutSim[i].setOnClickListener(mOnClickListener);
            mPlugInSim[i].setOnClickListener(mOnClickListener);
            // SIM missing & recovery is not supported is gemini+
            if (i < 2) {
                logd("OnClickListener missing sim" + i);
                logd("OnClickListener recover sim" + i);
                mMissingSim[i].setOnClickListener(mOnClickListener);
                mRecoverySim[i].setOnClickListener(mOnClickListener);
            }
        }
        mPlugOutAllSims.setOnClickListener(mOnClickListener);
        mPlugInAllSims.setOnClickListener(mOnClickListener);
        
        if (!FeatureOption.MTK_SIM_HOT_SWAP_COMMON_SLOT) {
            mPlugOutAllSims.setVisibility(View.GONE);
            mPlugInAllSims.setVisibility(View.GONE);
            for (int i=PhoneConstants.GEMINI_SIM_NUM; i < MAX_GEMINI_SIM_NUM; i++) {
                logd("View GONE plug out sim" + i);
                mPlugOutSim[i].setVisibility(View.GONE);
            }
        } else {
            for (int i=0; i < MAX_GEMINI_SIM_NUM; i++) {
                logd("View GONE plug out sim" + i);
                mPlugOutSim[i].setVisibility(View.GONE);
            }
        }
        
        for (int i=PhoneConstants.GEMINI_SIM_NUM; i < MAX_GEMINI_SIM_NUM; i++) {
            logd("View GONE plug in sim" + i);
            mPlugInSim[i].setVisibility(View.GONE);
            if (i < 2) {
                logd("View GONE missing sim" + i);
                logd("View GONE recover sim" + i);
                mMissingSim[i].setVisibility(View.GONE);
                mRecoverySim[i].setVisibility(View.GONE);
            }
        }
        mPhone = PhoneFactory.getDefaultPhone();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private static void logd(String msg) {
        Log.d(LOG_TAG, "[HotSwapTool]" + msg);
    }
}
