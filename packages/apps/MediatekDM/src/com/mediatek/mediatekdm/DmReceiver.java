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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.ServiceState;
import android.util.Log;

import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.mediatekdm.DmConst.TAG;
import com.mediatek.mediatekdm.DmOperationManager.IOperationScannerHandler;
import com.mediatek.mediatekdm.util.Utilities;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DmReceiver extends BroadcastReceiver {
    public static final String BOOT_TIME_FILE_NAME = "bootTimeStamp.ini";
    private static ExecutorService sExecutorService = null;

    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null || context == null) {
            Log.w(TAG.RECEIVER, "Invalid arguments. Exit.");
            return;
        }

        Log.i(TAG.RECEIVER, "Received intent: " + intent);
        String intentAction = intent.getAction();

        if (intentAction.equalsIgnoreCase(TelephonyIntents.ACTION_SIM_INDICATOR_STATE_CHANGED)) {
            /**
             * ACTION_SIM_INDICATOR_STATE_CHANGED is used to launch
             * RebootChecker when network is ready. This intent is MTK internal.
             */
            int listenSim = intent.getIntExtra(TelephonyIntents.INTENT_KEY_ICC_SLOT, -1);
            Log.i(TAG.RECEIVER, "Intent from sim " + listenSim);
            if (listenSim == Utilities.getRegisteredSimId(context)) {
                int simState = intent.getIntExtra(TelephonyIntents.INTENT_KEY_ICC_STATE, PhoneConstants.SIM_INDICATOR_UNKNOWN);
                Log.i(TAG.RECEIVER, "Phone state is " + simState);
                if (simState == PhoneConstants.SIM_INDICATOR_NORMAL || simState == PhoneConstants.SIM_INDICATOR_ROAMING) {
                    // We ignore SIM_INDICATOR_CONNECTED & SIM_INDICATOR_ROAMINGCONNECTED here.
                    Log.i(TAG.RECEIVER, "Phone state is either normal or roaming. Proceed.");
                    Log.i(TAG.RECEIVER, "Launch reboot checker.");
                    checkPendingOperations(context);
                } else if (simState == PhoneConstants.SIM_INDICATOR_CONNECTED
                        || simState == PhoneConstants.SIM_INDICATOR_ROAMINGCONNECTED) {
                    Log.i(TAG.RECEIVER, "GENIMI SUPPORT is " + FeatureOption.MTK_GEMINI_SUPPORT);
                    if (FeatureOption.MTK_GEMINI_SUPPORT) {
                        DmOperationManager manager = DmOperationManagerFactory.getInstance();
                        Log.i(TAG.RECEIVER, "isInRecovery is " + manager.isInRecovery());
                        Log.i(TAG.RECEIVER, "hasNext is " + manager.hasNext());
                        if (manager.isInRecovery() || manager.hasNext()) {
                            Intent serviceIntent = new Intent(intent);
                            serviceIntent.setClass(context, DmService.class);
                            serviceIntent.setAction(DmConst.IntentAction.GEMINI_DATA_RECOVERED);
                            context.startService(serviceIntent);
                        }
                    }
                } else {
                    Log.i(TAG.RECEIVER, "Invalid phone state. Do nothing.");
                }
            } else {
                Log.i(TAG.RECEIVER, "Sim id not match, ignore");
            }
        } else if (intentAction.equals(TelephonyIntents.ACTION_SERVICE_STATE_CHANGED)) {
            if (FeatureOption.MTK_GEMINI_SUPPORT) {
                Log.i(TAG.RECEIVER, "Received ACTION_SERVICE_STATE_CHANGED.");
                int listenSim = intent.getIntExtra(PhoneConstants.GEMINI_SIM_ID_KEY, -1);

                Log.i(TAG.RECEIVER, "Intent from sim " + listenSim);
                if (listenSim == Utilities.getRegisteredSimId(context)) {
                    ServiceState serviceState = ServiceState.newFromBundle(intent.getExtras());
                    Log.i(TAG.RECEIVER, "State is " + serviceState.getState());
                    if (serviceState.getState() == ServiceState.STATE_IN_SERVICE) {
                        Log.i(TAG.RECEIVER, "State is STATE_IN_SERVICE");
                        DmOperationManager manager = DmOperationManagerFactory.getInstance();
                        Log.i(TAG.RECEIVER, "isInRecovery is " + manager.isInRecovery());
                        Log.i(TAG.RECEIVER, "hasNext is " + manager.hasNext());
                        if (manager.isInRecovery() || manager.hasNext()) {
                            Intent serviceIntent = new Intent(intent);
                            serviceIntent.setClass(context, DmService.class);
                            serviceIntent.setAction(DmConst.IntentAction.GEMINI_DATA_RECOVERED);
                            context.startService(serviceIntent);
                        } else {
                            Log.i(TAG.RECEIVER, "No need to start service.");
                        }
                    } else {
                        Log.i(TAG.RECEIVER, "State is not STATE_IN_SERVICE.");
                    }
                } else {
                    Log.i(TAG.RECEIVER, "Sim id not match");
                }
            }
        } else if (intentAction.equals(DmConst.IntentAction.DM_BOOT_COMPLETE)) {
            /**
             * Save boot time stamp for reboot checker.
             */
            FileOutputStream out = null;
            try {
                String timeBoot = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date());
                Log.i(TAG.RECEIVER, "Device boot time is " + timeBoot);

                Context wriContext = DmApplication.getInstance();
                out = wriContext.openFileOutput(BOOT_TIME_FILE_NAME, Context.MODE_PRIVATE);
                out.write(timeBoot.getBytes());
                out.flush();
                out.close();

                Log.i(TAG.RECEIVER, "Write boot time " + timeBoot + " to file " + BOOT_TIME_FILE_NAME);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Log.e(TAG.RECEIVER, "Not found time stamp file: " + e);
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG.RECEIVER, "Failed to write time stamp file: " + e);
            } finally {
                try {
                    if (out != null) {
                        out.close();
                        out = null;
                    }
                } catch (IOException e) {
                    Log.e(TAG.RECEIVER, e.getMessage());
                }
            }
        } else if (intentAction.equals(DmConst.IntentAction.DM_SWUPDATE)) {
            /**
             * User clicked update in system settings preference.
             */
            if (DmFeatureSwitch.DM_FUMO) {
                Log.i(TAG.RECEIVER, "Launch system update UI.");
                Class<?> dmEntryClass;
                try {
                    dmEntryClass = Class.forName("com.mediatek.mediatekdm.fumo.DmEntry");
                } catch (ClassNotFoundException e) {
                    throw new Error(e);
                }
                Intent activityIntent = new Intent(context, dmEntryClass);
                activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(activityIntent);
            }
        } else {
            Log.w(TAG.RECEIVER, "Normal intent. Forward it to service.");
            Intent serviceIntent = new Intent(intent);
            serviceIntent.setClass(context, DmService.class);
            serviceIntent.setAction(intentAction);
            context.startService(serviceIntent);
        }
    }

    private synchronized void checkPendingOperations(Context context) {
        if (sExecutorService == null) {
            sExecutorService = Executors.newSingleThreadExecutor();
        }
        sExecutorService.execute(new OperationChecker(context));
        // We process LAWMO first because sExecutorService will execute the executor one by one and may
        // be pending WIPE request left (due to power failure for example).
        if (DmFeatureSwitch.DM_LAWMO) {
            try {
                Class<?> checkerClass = Class.forName("com.mediatek.mediatekdm.lawmo.RebootChecker");
                Constructor<?> checkerConstructor = checkerClass.getConstructor(Context.class);
                sExecutorService.execute((RebootChecker) checkerConstructor.newInstance(context));
            } catch (Exception e) {
                throw new Error(e);
            }
        }
        if (DmFeatureSwitch.DM_FUMO) {
            try {
                Class<?> checkerClass = Class.forName("com.mediatek.mediatekdm.fumo.RebootChecker");
                Constructor<?> checkerConstructor = checkerClass.getConstructor(Context.class);
                sExecutorService.execute((RebootChecker) checkerConstructor.newInstance(context));
            } catch (Exception e) {
                throw new Error(e);
            }
        }
    }

    private class OperationChecker extends RebootChecker {

        public OperationChecker(Context context) {
            super(context);
        }

        public void run() {
            // Check for pending operations
            DmOperationManagerFactory.getInstance().scanPendingOperations(new IOperationScannerHandler() {
                @Override
                public void notify(boolean hasPendingOperations) {
                    if (hasPendingOperations) {
                        Intent intent = new Intent(DmConst.IntentAction.REBOOT_CHECK);
                        intent.setClass(mContext, DmService.class);
                        Bundle bundle = new Bundle();
                        bundle.putString("operation", "true");
                        intent.putExtras(bundle);
                        mContext.startService(intent);
                    }
                }
            });

        }
    }
}
