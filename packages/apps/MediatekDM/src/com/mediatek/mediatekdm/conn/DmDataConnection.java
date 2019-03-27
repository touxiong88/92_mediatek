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

/*
 * Copyright (C) 2007-2008 Esmertec AG.
 * Copyright (C) 2007-2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mediatek.mediatekdm.conn;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import com.android.internal.telephony.PhoneConstants;
import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.mediatekdm.DmConst;
import com.mediatek.mediatekdm.DmConst.TAG;
import com.mediatek.mediatekdm.DmService.IServiceMessage;
import com.mediatek.mediatekdm.ext.MTKConnectivity;
import com.mediatek.mediatekdm.ext.MTKPhone;
import com.mediatek.mediatekdm.mdm.MdmEngine;
import com.mediatek.mediatekdm.mdm.SessionInitiator;
import com.mediatek.mediatekdm.mdm.SessionStateObserver;
import com.mediatek.mediatekdm.option.Options;
import com.mediatek.mediatekdm.util.ScreenLock;
import com.mediatek.mediatekdm.util.Utilities;
import com.mediatek.telephony.SimInfoManager;
import com.mediatek.telephony.SimInfoManager.SimInfoRecord;

import junit.framework.Assert;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

public class DmDataConnection implements SessionStateObserver {
    public static final int GEMINI_SIM_1 = 0;
    public static final int GEMINI_SIM_2 = 1;

    private ConnectivityReceiver mConnectivityReceiver = null;
    private ConnectivityManager mConnMgr;
    private Context mContext;
    private DmDatabase mDmDatabase;

    private int mSimId = -1;
    private boolean mIsStealDataConn = false;

    private static DmDataConnection sInstance = null;

    private Set<DataConnectionListener> mListeners = new HashSet<DataConnectionListener>();

    private DmDataConnection(Context context) {
        mContext = context;
        mConnMgr = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        // register the CONNECTIVITY_ACTION receiver
        mConnectivityReceiver = new ConnectivityReceiver();
        // init DmDatabase
        mDmDatabase = new DmDatabase(context);

        IntentFilter intent = new IntentFilter();
        intent.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        intent.addAction(DmConst.IntentAction.NET_DETECT_TIMEOUT);
        mContext.registerReceiver(mConnectivityReceiver, intent);
    }

    public static DmDataConnection getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new DmDataConnection(context);
            Log.i(TAG.CONNECTION, "-- sInstance is null, new it.");
        }
        Log.i(TAG.CONNECTION, "-- sInstance not null.");
        return sInstance;
    }

    private boolean isLawmoLocked() {
        boolean isLocked = false;
        try {
            isLocked = MTKPhone.getDmAgent().isLockFlagSet();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        Log.i(TAG.CONNECTION, "isLawmoLocked : " + isLocked);
        return isLocked;
    }

    private void ensureDmDataConnectivity(int simId) {
        Log.i(TAG.CONNECTION, ">>>>>>>ensureDmDataConnectivity");
        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (!cm.getMobileDataEnabled()) {
            Log.i(TAG.CONNECTION, "mobile data not enable, enable it");

            mIsStealDataConn = true;
            MdmEngine.getInstance().registerSessionStateObserver(this);

            if (FeatureOption.MTK_GEMINI_SUPPORT) {
                Intent intent = new Intent(Intent.ACTION_DATA_DEFAULT_SIM_CHANGED);
                SimInfoRecord simInfo = SimInfoManager.getSimInfoBySlot(mContext, simId);
                Log.i(TAG.CONNECTION, "we will enable simInfo.mSimInfoId : " + simInfo.mSimInfoId);
                intent.putExtra(PhoneConstants.MULTI_SIM_ID_KEY, simInfo.mSimInfoId);
                mContext.sendBroadcast(intent);
            } else {
                cm.setMobileDataEnabled(true);
            }
        }
    }

    private void checkDmDataConnectivity(int simId) {
        Log.i(TAG.CONNECTION, ">>>>>>>checkDmDataConnectivity");
        if (isLawmoLocked()) {
            Log.i(TAG.CONNECTION, "lawmo locked, need to ensure the mobile is connected");
            ensureDmDataConnectivity(simId);
        }
    }

    public int startDmDataConnectivity() throws IOException {
        Assert.assertFalse("startDmDataConnectivity MUST NOT be called in direct internet conn.",
                Options.USEDIRECTINTERNET);
        // if gemini is set
        int result = -1;
        mSimId = Utilities.getRegisteredSimId(mContext);
        if (mSimId == -1) {
            Log.e(TAG.CONNECTION, "Get Register SIM ID error in start data connection");
            return result;
        }

        if (!mDmDatabase.dmApnReady(mSimId)) {
            Log.e(TAG.CONNECTION, "Dm apn mTable is not ready!");
            return result;
        }

        checkDmDataConnectivity(mSimId);

        result = beginDmDataConnectivity(mSimId);

        if (result == MTKPhone.APN_TYPE_NOT_AVAILABLE || result == MTKPhone.APN_REQUEST_FAILED) {
            Log.e(TAG.CONNECTION, "start Dmdate Connectivity error");
        }

        // for test begin
        if (result == MTKPhone.APN_ALREADY_ACTIVE) {
            Log.i(TAG.CONNECTION, "DataConnection is already exist and send MSG_WAP_CONNECTION_SUCCESS to client");
            notifyHandlers(IServiceMessage.MSG_WAP_CONNECTION_SUCCESS);
        }

        // for test end
        return result;
    }

    public void stopDmDataConnectivity() {
        Assert.assertFalse("stopDmDataConnectivity MUST NOT be called in direct internet conn.",
                Options.USEDIRECTINTERNET);
        Log.v(TAG.CONNECTION, "stopDmDataConnectivity");
        try {
            mSimId = Utilities.getRegisteredSimId(mContext);
            if (mSimId == -1) {
                Log.e(TAG.CONNECTION, "Get Register SIM ID error in stop data connection");
                return;
            }
            endDmConnectivity(mSimId);
            ScreenLock.releaseWakeLock(mContext);
            ScreenLock.enableKeyguard(mContext);
        } finally {
            Log.v(TAG.CONNECTION, "stopUsingNetworkFeature end");
        }
    }

    public class ConnectivityReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (context == null || intent == null) {
                return;
            }
            if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                Log.d(TAG.CONNECTION,
                        "ConnectivityReceiver Receive android.net.conn.CONNECTIVITY_CHANGE");
                Bundle bundle = intent.getExtras();
                if (bundle != null) {
                    @SuppressWarnings("deprecation")
                    NetworkInfo info = (NetworkInfo) bundle
                            .get(ConnectivityManager.EXTRA_NETWORK_INFO);
                    if (info == null) {
                        Log.e(TAG.CONNECTION, "[dm-conn]->Get NetworkInfo error");
                        return;
                    }
                    mSimId = Utilities.getRegisteredSimId(mContext);
                    if (mSimId == -1) {
                        Log.e(TAG.CONNECTION,
                                "[dm-conn]->Get Register SIM ID error in connetivity receiver");
                        return;
                    }
                    int networkSimId = MTKConnectivity.getSimId(info);
                    int intentSimId = intent.getIntExtra(MTKConnectivity.EXTRA_SIM_ID, 0);
                    int networkType = info.getType();

                    if (intentSimId == mSimId && networkType == MTKConnectivity.TYPE_MOBILE_DM) {
                        Log.i(TAG.CONNECTION, "[dm-conn]->type == " + info.getTypeName() + "("
                                + networkType + ")");
                        Log.i(TAG.CONNECTION, "[dm-conn]->intent_sim_Id == " + intentSimId);
                        Log.i(TAG.CONNECTION, "[dm-conn]->network_sim_Id == " + networkSimId);
                        Log.i(TAG.CONNECTION, "[dm-conn]->registered_sim_Id == " + mSimId);

                        State state = info.getState();
                        if (state == State.CONNECTED) {
                            Log.i(TAG.CONNECTION, "[dm-conn]->state == CONNECTED");
                            try {
                                ensureRouteToHost();
                                notifyHandlers(IServiceMessage.MSG_WAP_CONNECTION_SUCCESS);
                            } catch (IOException ex) {
                                Log.e(TAG.CONNECTION, "[dm-conn]->ensureRouteToHost() failed:", ex);
                            }
                        } else if (state == State.CONNECTING) {
                            Log.i(TAG.CONNECTION, "[dm-conn]->state == CONNECTING");
                            return;
                        } else if (state == State.DISCONNECTED) {
                            Log.i(TAG.CONNECTION, "[dm-conn]->state == DISCONNECTED");
                            return;
                        }
                    }
                }
            } else if (intent.getAction().equalsIgnoreCase(DmConst.IntentAction.NET_DETECT_TIMEOUT)) {
                Log.i(TAG.CONNECTION, "[dm-conn]->action == com.mediatek.mediatekdm.NETDETECTTIMEOUT");
                Log.i(TAG.CONNECTION, ">>>sending msg WAP_CONN_TIMEOUT");
                notifyHandlers(IServiceMessage.MSG_WAP_CONNECTION_TIMEOUT);
            }
        }
    }

    private int beginDmDataConnectivity(int simId) throws IOException {
        int result = MTKConnectivity.startUsingNetworkFeature(mConnMgr,
                ConnectivityManager.TYPE_MOBILE,
                MTKPhone.FEATURE_ENABLE_DM, simId);

        Log.i(TAG.CONNECTION, "beginDmDataConnectivity: simId = " + simId + "\t result="
                + result);

        if (result == MTKPhone.APN_ALREADY_ACTIVE) {
            Log.w(TAG.CONNECTION, "[dm-conn]->APN_ALREADY_ACTIVE");
            ScreenLock.releaseWakeLock(mContext);
            ScreenLock.acquirePartialWakelock(mContext);
            ensureRouteToHost();
        } else if (result == MTKPhone.APN_REQUEST_STARTED) {
            Log.w(TAG.CONNECTION,
                    "[dm-conn]->APN_REQUEST_STARTED, waiting for intent.");
            ScreenLock.releaseWakeLock(mContext);
            ScreenLock.acquirePartialWakelock(mContext);
        } else if (result == MTKPhone.APN_REQUEST_FAILED) {
            Log.e(TAG.CONNECTION, "[dm-conn]->APN_REQUEST_FAILED");
        } else {
            throw new IOException("[dm-conn]:Cannot establish Dm Data connectivity");
        }
        return result;
    }

    // add for gemini
    private void endDmConnectivity(int simId) {
        try {
            Log.i(TAG.CONNECTION, "endDmDataConnectivityGemini: simId = " + simId);

            if (mConnMgr != null) {
                MTKConnectivity.stopUsingNetworkFeatureGemini(mConnMgr,
                        ConnectivityManager.TYPE_MOBILE,
                        MTKPhone.FEATURE_ENABLE_DM, simId);
            }
        } finally {
            Log.v(TAG.CONNECTION, "stopUsingNetworkFeature end");
        }
    }

    private void ensureRouteToHost() throws IOException {
        Log.v(TAG.CONNECTION, "Begin ensureRouteToHost");
        // call getApnInfoFromSettings
        String proxyAddr = mDmDatabase.getApnProxyFromSettings();
        int inetAddr = lookupHost(proxyAddr);
        Log.i(TAG.CONNECTION, "inetAddr = " + inetAddr);

        // get the addr form setting
        if (!mConnMgr.requestRouteToHost(MTKConnectivity.TYPE_MOBILE_DM, inetAddr)) {
            throw new IOException("Cannot establish route to proxy " + inetAddr);
        }

    }

    public static int lookupHost(String hostname) {
        InetAddress inetAddress;
        try {
            inetAddress = InetAddress.getByName(hostname);
        } catch (UnknownHostException e) {
            return -1;
        }
        byte[] addrBytes;
        int addr;
        addrBytes = inetAddress.getAddress();
        addr = ((addrBytes[3] & 0xff) << 24) | ((addrBytes[2] & 0xff) << 16)
                | ((addrBytes[1] & 0xff) << 8)
                | (addrBytes[0] & 0xff);
        return addr;
    }

    public interface DataConnectionListener {
        void notifyStatus(int status);
    }

    public void registerListener(DataConnectionListener listener) {
        mListeners.add(listener);
    }

    public void unregisterListener(DataConnectionListener listener) {
        mListeners.remove(listener);
    }

    private void notifyHandlers(int msgCode) {
        for (DataConnectionListener l : mListeners) {
            l.notifyStatus(msgCode);
        }
    }

    private void destroyDataConnection() {
        try {
            mContext.unregisterReceiver(mConnectivityReceiver);
        } catch (IllegalArgumentException e) {
            Log.v(TAG.CONNECTION, "Exception in destroyDataConnection.");
            e.printStackTrace();
        }
        mContext = null;
    }

    public static void destroyInstance() {
        sInstance.destroyDataConnection();
        sInstance = null;
        Log.i(TAG.CONNECTION, "-- destroyInstance.");
    }

    public void notify(SessionType type, SessionState state, int lastError, SessionInitiator initiator) {
        Log.i(TAG.CONNECTION, "DmConnSessionStateObserver");
        Log.i(TAG.CONNECTION, "SessionType = " + type);
        Log.i(TAG.CONNECTION, "SessionState = " + state);
        Log.i(TAG.CONNECTION, "lastError = " + lastError);

        if (type == SessionType.DM && state == SessionState.COMPLETE &&
                mIsStealDataConn && !isLawmoLocked()) {
            Log.i(TAG.CONNECTION, "dm enable mobile data privately, now we should close it");
            ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            cm.setMobileDataEnabled(false);
            mIsStealDataConn = false;
            MdmEngine.getInstance().unregisterSessionStateObserver(this);
        }
    }
}
