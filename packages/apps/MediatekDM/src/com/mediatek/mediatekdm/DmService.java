/*
 * Copyright Statement:
 * 
 * This software/firmware and related documentation ("MediaTek Software") are protected under
 * relevant copyright laws. The information contained herein is confidential and proprietary to
 * MediaTek Inc. and/or its licensors. Without the prior written permission of MediaTek inc. and/or
 * its licensors, any reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly prohibited.
 * 
 * MediaTek Inc. (C) 2010. All rights reserved.
 * 
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES THAT THE
 * SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE") RECEIVED FROM MEDIATEK AND/OR ITS
 * REPRESENTATIVES ARE PROVIDED TO RECEIVER ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS
 * ANY AND ALL WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT. NEITHER DOES MEDIATEK
 * PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED
 * BY, INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO
 * SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT
 * IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN
 * MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE RELEASES MADE
 * TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR STANDARD OR OPEN FORUM. RECEIVER'S SOLE
 * AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK
 * SOFTWARE RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK
 * SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 * 
 * The following software/firmware and/or related documentation ("MediaTek Software") have been
 * modified by MediaTek Inc. All revisions are subject to any receiver's applicable license
 * agreements with MediaTek Inc.
 */

package com.mediatek.mediatekdm;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Process;
import android.util.Log;

import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.mediatekdm.DmConst.IntentAction;
import com.mediatek.mediatekdm.DmConst.NotificationInteractionType;
import com.mediatek.mediatekdm.DmConst.TAG;
import com.mediatek.mediatekdm.DmController.DmAction;
import com.mediatek.mediatekdm.DmOperation.InteractionResponse;
import com.mediatek.mediatekdm.DmOperation.InteractionResponse.InteractionType;
import com.mediatek.mediatekdm.DmOperation.KEY;
import com.mediatek.mediatekdm.DmOperation.Type;
import com.mediatek.mediatekdm.DmOperationManager.IOperationStateObserver;
import com.mediatek.mediatekdm.DmOperationManager.State;
import com.mediatek.mediatekdm.DmOperationManager.TriggerResult;
import com.mediatek.mediatekdm.conn.DmDataConnection;
import com.mediatek.mediatekdm.conn.DmDataConnection.DataConnectionListener;
import com.mediatek.mediatekdm.ext.MTKFileUtil;
import com.mediatek.mediatekdm.ext.MTKOptions;
import com.mediatek.mediatekdm.ext.MTKPhone;
import com.mediatek.mediatekdm.mdm.MdmException.MdmError;
import com.mediatek.mediatekdm.mdm.MmiChoiceList;
import com.mediatek.mediatekdm.mdm.MmiConfirmation;
import com.mediatek.mediatekdm.mdm.MmiFactory;
import com.mediatek.mediatekdm.mdm.MmiInfoMsg;
import com.mediatek.mediatekdm.mdm.MmiInputQuery;
import com.mediatek.mediatekdm.mdm.MmiObserver;
import com.mediatek.mediatekdm.mdm.MmiProgress;
import com.mediatek.mediatekdm.mdm.MmiViewContext;
import com.mediatek.mediatekdm.mdm.SessionInitiator;
import com.mediatek.mediatekdm.mdm.SessionStateObserver;
import com.mediatek.mediatekdm.option.Options;
import com.mediatek.mediatekdm.util.Utilities;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

/**
 * @author mtk80963
 */
public class DmService extends Service {

    private final IBinder mBinder = new DmBinder();
    public static DmController sDmController = null;

    DmOperationManager mOperationManager = DmOperationManagerFactory.getInstance();

    static AlarmManager sAlarmMgr = null;
    DmNotification mDmNotification = null;
    private DmDataConnection mDataConnection = null;
    private boolean mFatalErrorOccurred = false;

    // NodeIoHandler data cache for operation recovery
    public static Map<String, String> sCCStoredParams = new HashMap<String, String>();;

    // All time out are defined by CMCC specification. in DM session, MAXDT=30
    private static final int DEFAULT_ALERT_1101_TIMEOUT = 30;

    // for source sync
    private static Semaphore sSemp;
    static {
        try {
            sSemp = new Semaphore(1, true);
            sSemp.acquire();
        } catch (InterruptedException e) {
            throw new Error(e);
        }
    }

    public DmNotification getNotification() {
        return mDmNotification;
    }

    /**
     * Override function of android.app.Service, initiate MDM controls.
     */
    public void onCreate() {
        Log.i(TAG.SERVICE, "On create service");
        super.onCreate();

        Notification fakeNotification = new Notification();
        fakeNotification.tickerText="Ticker .....";
        fakeNotification.flags = Notification.FLAG_AUTO_CANCEL;
        fakeNotification.setLatestEventInfo(this , "Title", "Notify " , null);

        startForeground(0, fakeNotification);
        Log.i(TAG.SERVICE, " Bring service to foreground");

        mFatalErrorOccurred = false;
        long availableStorage = Utilities.getAvailableInternalMemorySize();
        if (availableStorage < 0) {
            Log.e(TAG.SERVICE, "Storage is not enough: " + availableStorage + " available.");
            mFatalErrorOccurred = true;
            removeDirectoryRecursively(getFilesDir());
            Process.killProcessQuiet(Process.myPid());
            return;
        }

        try {
            isDmTreeReady();
            Log.i(TAG.SERVICE, "On create service done");
            if (sDmController == null) {
                initDmController();
            }
            initComponents();
            mDataConnection = DmDataConnection.getInstance(this);
        } catch (Error e) {
            e.printStackTrace();
            mFatalErrorOccurred = true;
            removeDirectoryRecursively(this.getFilesDir());
        }

        DmOperationManagerFactory.getInstance().registerObserver(
                (IOperationStateObserver)mServiceHandler);
    }

    private static void removeDirectoryRecursively(File directory) {
        Log.w(TAG.SERVICE, "+removeDirectoryRecursively(" + directory + ")");
        File[] children = directory.listFiles();
        for (File child : children) {
            if (child.isFile()) {
                Log.d(TAG.SERVICE, "Remove file " + child);
                child.delete();
            } else if (child.isDirectory()) {
                removeDirectoryRecursively(child);
                child.delete();
            }
        }
        Log.w(TAG.SERVICE, "-removeDirectoryRecursively()");
    }

    /**
     * Override function of android.app.Service, handle three types of intents:
     * 1. DM wap push 2. boot complete if system upgrades
     * 3. download foreground The service will restart once if it is killed by system.
     */
    @SuppressWarnings("unused")
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.d(TAG.SERVICE, "+onStartCommand()");

        if (mFatalErrorOccurred) {
            Log.e(TAG.SERVICE, "Fatal error happened. Exit.");
            stopSelf();
            return START_NOT_STICKY;
        }

        if (intent == null || intent.getAction() == null) {
            Log.w(TAG.SERVICE, "Intent or action is null");
            Log.d(TAG.SERVICE, "-onStartCommand()");
            return START_NOT_STICKY;
        }

        final String action = intent.getAction();

        // Check status of SMS registration
        if (Options.USESMSREGISTER) {
            int registeredSimId = Utilities.getRegisteredSimId(this);
            // 1. check whether we have registered
            if (registeredSimId == -1) {
                Log.w(TAG.SERVICE, "This phone has not registered yet. Ignore previously added operations.");
                // Set skip mark to ignore all pending operations before registered
                mOperationManager.ignoreOperationsBeforeTimestamp(System.currentTimeMillis());
                return START_NOT_STICKY;
            }
            // 2. check whether the NIA came from the registered SIM card
            if (MTKOptions.MTK_GEMINI_SUPPORT && action.equals(DmConst.IntentAction.DM_WAP_PUSH)) {
                int receivedSimId = intent.getIntExtra("simId", -1);
                if (receivedSimId != registeredSimId) {
                    Log.w(TAG.SERVICE, "NIA is not from the registered card). Do nothing.");
                    Log.d(TAG.SERVICE, "receivedSimId = " + receivedSimId + ", registeredSimId = " + registeredSimId);
                    return START_NOT_STICKY;
                }
            }
        }

        Log.d(TAG.SERVICE, "Received start id " + startId + " : " + intent + " action is " + action);
        if (mServiceHandler != null) {
            mServiceHandler.removeMessages(IServiceMessage.MSG_QUIT_SERVICE);
        }

        /*
         * Incoming intents can be organized in the following category: 1. Initial Trigger: SI or CI actions issued either by
         * DM server, user (via Settings) or device (polling & reboot check). This kind of intents will activate the network.
         * 2. Interactions with UI components: For example, alert or notification responses. This kind of intents will not
         * activate the network. 3. Miscellaneous:
         */
        if (action.equals(IntentAction.DM_WAP_PUSH)) {
            String type = intent.getType();
            Log.v(TAG.SERVICE, "WAP push message with type '" + type + "' received");
            if (type != null && type.equals(DmConst.IntentType.DM_NIA)) {
                DmOperation operation = new DmOperation();
                operation.initSI(intent.getByteArrayExtra("data"));
                operation.setProperty(DmOperation.KEY.INITIATOR, "Server");
                mOperationManager.enqueue(operation);
            }
        } else if (action.equals(IntentAction.REBOOT_CHECK)) {
            Log.d(TAG.SERVICE, "Reboot check");
            if (DmFeatureSwitch.DM_FUMO) {
                if (intent.hasExtra("UpdateSucceeded")) {
                    boolean succeeded = intent.getBooleanExtra("UpdateSucceeded", true);
                    int result = succeeded ? IFumoManager.RESULT_SUCCESSFUL : IFumoManager.RESULT_UPDATE_FAILED;
                    // Report to server & UI to user
                    mFumoManager.clearDlStateAndReport(0, result);
                    mFumoManager.initReportActivity(succeeded);

                    File updateFile = new File(DmConst.Path.getPathInData(this, DmConst.Path.FOTA_FLAG_FILE));
                    if (updateFile.exists()) {
                        Log.d(TAG.SERVICE, "Delete FUMO update flag file.");
                        updateFile.delete();
                    }
                }
            }
        } else if (action.equals(IntentAction.FUMO_CLIENT_POLLING)) {
            Log.d(TAG.SERVICE, "FUMO client polling in background");
            if (DmFeatureSwitch.DM_FUMO) {
                DmOperation operation = new DmOperation();
                operation.initCIFumo();
                operation.setProperty(DmOperation.KEY.INITIATOR, "Device");
                mOperationManager.enqueue(operation, false);
            } else {
                Log.w(TAG.SERVICE, "FUMO is not available. Do nothing.");
            }
        } else if (action.equals(IntentAction.DM_REMINDER)) {
            if (DmFeatureSwitch.DM_FUMO) {
                Log.d(TAG.SERVICE, "FUMO update reminder timeout, start DmClient");
                if (!mFumoManager.isDownloadComplete()) {
                    Log.w(TAG.SERVICE, "DL state is not STATE_DLPKGCOMPLETE, do nothing");
                    return START_NOT_STICKY;
                } else {
                    Class<?> dmClientClass;
                    try {
                        dmClientClass = Class.forName("com.mediatek.mediatekdm.fumo.DmClient");
                    } catch (ClassNotFoundException e) {
                        throw new Error(e);
                    }
                    Intent activityIntent = new Intent(this, dmClientClass);
                    activityIntent.setAction("com.mediatek.mediatekdm.DMCLIENT");
                    activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(activityIntent);
                }
                // DmClient should activate network by itself.
                return START_STICKY;
            }
        } else if (action.equals(IntentAction.DM_DL_FOREGROUND)) {
            if (DmFeatureSwitch.DM_FUMO) {
                Log.i(TAG.SERVICE, "Bring DmClient to foreground");
                Class<?> dmClientClass;
                try {
                    dmClientClass = Class.forName("com.mediatek.mediatekdm.fumo.DmClient");
                } catch (ClassNotFoundException e) {
                    throw new Error(e);
                }
                Intent activityIntent = new Intent(this, dmClientClass);
                activityIntent.setAction("com.mediatek.mediatekdm.DMCLIENT");
                activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(activityIntent);
                // Remain alive as we may be bound by DmClient in near future.
                return START_STICKY;
            }
        } else if (action.equals(IntentAction.SCOMO_SCAN_PACKAGE)) {
            Log.d(TAG.SERVICE, "Scan package information for SCOMO");
            if (DmFeatureSwitch.DM_SCOMO) {
                mScomoManager.scomoScanPackage();
            }
            return START_STICKY;
        } else if (action.equals(IntentAction.DM_NOTIFICATION_RESPONSE)) {
            cancelNiaAlertTimeout();
            sendBroadcast(new Intent(IntentAction.DM_CLOSE_DIALOG));
            if (mDmNotification != null) {
                mDmNotification.clear();
            }
            if (intent.getBooleanExtra("response", false)) {
                Log.d(TAG.SERVICE, "User confirmed notification, proceed");
                mOperationManager.current().addUIResponse(
                        new InteractionResponse(InteractionType.NOTIFICATION, InteractionResponse.POSITIVE, null));
                if (DmFeatureSwitch.DM_SCOMO) {
                    mScomoManager.setVerbose(false);
                }
                sDmController.triggerNiaDmSession(mOperationManager.current().getByteArrayProperty(KEY.NIA));
            } else {
                Log.d(TAG.SERVICE, "User canceled notification, proceed to next operation");
                mOperationManager.finishCurrent();
            }
            // Do NOT require network directly as it should be activated by DM_WAP_PUSH. If the
            // network is unavailable any more, we will facilitate the error recovery mechanism.
            return START_STICKY;
        } else if (action.equals(IntentAction.DM_ALERT_RESPONSE)) {
            cancelNiaAlertTimeout();
            sendBroadcast(new Intent(IntentAction.DM_CLOSE_DIALOG));
            if (mDmNotification != null) {
                mDmNotification.clear();
            }
            if (intent.getBooleanExtra("response", false)) {
                Log.d(TAG.SERVICE, "User confirmed alert");
                mOperationManager.current().addUIResponse(
                        new InteractionResponse(InteractionType.CONFIRMATION, InteractionResponse.POSITIVE, null));
                mAlertConfirm.confirm();
            } else {
                Log.d(TAG.SERVICE, "User canceled alert");
                mOperationManager.current().addUIResponse(
                        new InteractionResponse(InteractionType.CONFIRMATION, InteractionResponse.NEGATIVE, null));
                mAlertConfirm.cancel();
            }
            // Do NOT require network directly as it should be activated by DM_WAP_PUSH. If the
            // network is unavailable anymore, we will facilitate the error recovery mechanism.
            return START_STICKY;
        } else if (action.equals(IntentAction.DM_NOTIFICATION_TIMEOUT)) {
            int type = intent.getIntExtra("type", NotificationInteractionType.TYPE_INVALID);
            cancelNiaAlertTimeout();
            sendBroadcast(new Intent(IntentAction.DM_CLOSE_DIALOG));
            if (mDmNotification != null) {
                mDmNotification.clear();
            }
            if (type == NotificationInteractionType.TYPE_NOTIFICATION_INTERACT) {
                mOperationManager.finishCurrent();
            }
            // Do NOT require network directly as it should be activated by DM_WAP_PUSH. If the
            // network is unavailable any more, we will facilitate the error recovery mechanism.
            return START_STICKY;
        } else if (action.equals(IntentAction.DM_ALERT_TIMEOUT)) {
            cancelNiaAlertTimeout();
            sendBroadcast(new Intent(IntentAction.DM_CLOSE_DIALOG));
            if (mDmNotification != null) {
                mDmNotification.clear();
            }
            if (DmFeatureSwitch.CMCC_SPECIFIC) {
                mOperationManager.current().addUIResponse(
                        new InteractionResponse(InteractionType.CONFIRMATION, InteractionResponse.NEGATIVE, null));
                mAlertConfirm.cancel();
            } else {
                mOperationManager.current().addUIResponse(
                        new InteractionResponse(InteractionType.CONFIRMATION, InteractionResponse.TIMEOUT, null));
                mAlertConfirm.timeout();
            }
            // Do NOT require network directly as it should be activated by DM_WAP_PUSH. If the
            // network is unavailable any more, we will facilitate the error recovery mechanism.
            return START_STICKY;
        } else if (action.equals(DmConst.IntentAction.GEMINI_DATA_RECOVERED)) {
            Log.d(TAG.SERVICE, "Reconnect network...");
            mServiceHandler.removeMessages(IServiceMessage.MSG_CHECK_NETWORK);
        } else if (action.equals(DmConst.IntentAction.CHECK_NETWORK)) {
            Log.d(TAG.SERVICE, "Re check network...");
            mServiceHandler.removeMessages(IServiceMessage.MSG_CHECK_NETWORK);
        } else if(action.equals(DmConst.IntentAction.ONLY_START_SERVICE)) {
            Log.i(TAG.SERVICE, "Only start service...");
        } else {
            throw new Error("Unsupported intent " + intent);
        }

        // Activate network
        if (!Options.USEDIRECTINTERNET) {
            try {
                mDataConnection.registerListener(new DataConnectionListener() {
                    public void notifyStatus(int status) {
                        mServiceHandler.sendEmptyMessage(status);
                    }
                });
                int result = mDataConnection.startDmDataConnectivity();
                Log.i(TAG.SERVICE, "starting DM WAP conn...ret=" + result);
                if (result == MTKPhone.APN_ALREADY_ACTIVE) {
                    Log.i(TAG.SERVICE, "WAP is ready.");
                    mServiceHandler.sendEmptyMessage(IServiceMessage.MSG_OPERATION_PROCESS_NEXT);
                } else {
                    Log.i(TAG.SERVICE, "Net not ready, send MSG_CHECK_NETWORK again.");
                    mServiceHandler.sendEmptyMessageDelayed(IServiceMessage.MSG_CHECK_NETWORK,
                        DmServiceHandler.STAMP_CHECK_NET);
                }
                // Network is not available now, we will recovery/trigger the operation when network is ready.
            } catch (IOException e) {
                Log.e(TAG.SERVICE, "startDmDataConnectivity failed.", e);
            }
        } else {
            Log.i(TAG.SERVICE, "Process next operation directly when using internet.");
            mServiceHandler.sendEmptyMessage(IServiceMessage.MSG_OPERATION_PROCESS_NEXT);
        }

        return START_STICKY;
    }

    /**
     * Override function of android.app.Binder
     */
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG.SERVICE, "+onBind(): Fatal error flag is " + mFatalErrorOccurred);
        return mFatalErrorOccurred ? null : mBinder;
    }

    /**
     * Override function of android.app.Binder
     */
    public void onRebind(Intent intent) {
        Log.i(TAG.SERVICE, "On rebind service");
        super.onRebind(intent);
    }

    /**
     * Override function of android.app.Binder
     */
    public boolean onUnbind(Intent intent) {
        Log.i(TAG.SERVICE, "On unbind service");
        return super.onUnbind(intent);
    }

    /**
     * Override function of android.app.Service
     */
    public void onDestroy() {
        Log.d(TAG.SERVICE, "+onDestroy()");

        Log.d(TAG.SERVICE, "Exec stopForeground with para true.");
        stopForeground(true);

        DmOperationManagerFactory.getInstance().unregisterObserver(
                (IOperationStateObserver)mServiceHandler);

        try {
            if (sDmController != null) {
                sDmController.stop();
            }

            if (!Options.USEDIRECTINTERNET) {
                if (mDataConnection != null) {
                    mDataConnection.stopDmDataConnectivity();
                    DmDataConnection.destroyInstance();
                }
            }

            deinitComponents();
            if (sDmController != null) {
                sDmController.destroy();
                sDmController = null;
            }

            sAlarmMgr = null;
            if (mDmNotification != null) {
                mDmNotification.clear();
                mDmNotification = null;
            }
        } catch (Throwable e) {
            e.printStackTrace();
            Log.e(TAG.SERVICE, "Ignore all exceptions in onDestroy()");
        }

        super.onDestroy();
        Log.d(TAG.SERVICE, "-onDestroy()");
    }

    public class DmBinder extends Binder {
        public DmService getService() {
            return DmService.this;
        }
    }

    public void initDmController() {
        sDmController = new DmController(this, new DmSessionStateObserver(this), mNiaManager, mNiaManager, new MmiFactory() {

            public MmiChoiceList createChoiceListDlg(MmiObserver observer) {
                return new DmMmiChoiceList(DmService.this, observer);
            }

            public MmiConfirmation createConfirmationDlg(MmiObserver observer) {
                mAlertConfirm = new DmAlertConfirm(DmService.this, observer);
                return mAlertConfirm;
            }

            public MmiInfoMsg createInfoMsgDlg(MmiObserver observer) {
                return new DmMmiInfoMsg(DmService.this, observer);
            }

            public MmiInputQuery createInputQueryDlg(MmiObserver observer) {
                return new DmMmiInputQuery();
            }

            public MmiProgress createProgress(int total) {
                return new MmiProgress() {
                    public void update(int current, int total) {
                        DmOperation operation = mOperationManager.current();
                        if (operation != null) {
                            String type = operation.getProperty(KEY.TYPE);
                            if (type.equals(Type.TYPE_CI_FUMO) || operation.getBooleanProperty(KEY.FUMO_TAG, false)) {
                                // FUMO
                                mFumoManager.updateDownloadProgress(current, total);
                            } else if (operation.getBooleanProperty(KEY.SCOMO_TAG, false)) {
                                // SCOMO
                                mScomoManager.updateDownloadProgress(current, total);
                            }
                        }
                    }
                };

            }
        });
    }

    public boolean isInitDmController() {
        return (sDmController != null);
    }

    private boolean isDmTreeReady() {
        boolean ret = false;
        FileInputStream in = null;
        FileOutputStream out = null;
        try {
            File systemTree = new File(DmConst.Path.getPathInSystem(DmConst.Path.DM_TREE_FILE));
            File dataTree = new File(DmConst.Path.getPathInData(this, DmConst.Path.DM_TREE_FILE));
            File dataFilesDir = getFilesDir();
            if (!dataTree.exists()) {
                if (!systemTree.exists()) {
                    Log.e(TAG.SERVICE, "The tree in system does not exist");
                    return false;
                }
                if (!dataFilesDir.exists()) {
                    Log.e(TAG.SERVICE, "there is no /files dir in dm folder");
                    if (dataFilesDir.mkdir()) {
                        // chmod for recovery access?
                        MTKFileUtil.openPermission(dataFilesDir.getAbsolutePath());
                    } else {
                        Log.e(TAG.SERVICE, "Create files dir in dm folder error");
                        return false;
                    }
                }
                int length = 1024 * 50;
                in = new FileInputStream(systemTree);
                out = new FileOutputStream(dataTree);
                byte[] buffer = new byte[length];
                while (true) {
                    Log.i(TAG.SERVICE, "in while");
                    int ins = in.read(buffer);
                    if (ins == -1) {
                        in.close();
                        out.flush();
                        out.close();
                        Log.i(TAG.SERVICE, "there is no more data");
                        break;
                    } else {
                        out.write(buffer, 0, ins);
                    }
                }
                ret = true;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                    in = null;
                }
                if (out != null) {
                    out.close();
                    out = null;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    public Handler getHandler() {
        return mServiceHandler;
    }

    Handler mServiceHandler = new DmServiceHandler();

    private class DmServiceHandler extends Handler implements IOperationStateObserver {
        private static final int STAMP_CHECK_NET = 29000; // 29s
        private static final int HEART_BEAT = 5000; // 5s
        private static final int EXIT_DELAY = 120000; // 2min

        @Override
        public void handleMessage(Message msg) {
            // Dump queue.
            DmService.this.getMainLooper().getQueue().dumpMessageQueue();
            if (msg == null) {
                return;
            }
            Log.d(TAG.SERVICE, "DmServiceHandler.handleMessage(" + msg.what + ")");
            if (msg.what != IServiceMessage.MSG_QUIT_SERVICE) {
                removeMessages(IServiceMessage.MSG_QUIT_SERVICE);
            }
            DmOperation operation;
            switch (msg.what) {
                case IServiceMessage.MSG_OPERATION_PROCESS_NEXT:
                case IServiceMessage.MSG_OPERATION_RECOVER_CURRENT:
                case IServiceMessage.MSG_OPERATION_RETRY_CURRENT:
                    if (msg.what == IServiceMessage.MSG_OPERATION_PROCESS_NEXT) {
                        Log.d(TAG.SERVICE, "Process next operation in queue.");
                        TriggerResult result = mOperationManager.triggerNext();
                        if (result == TriggerResult.BUSY) {
                            Log.w(TAG.SERVICE, "The operation is busy. Retry later after the current is finished.");
                            // Clear MSG_OPERATION_PROCESS_NEXT in queue. When the current operation
                            // is finished, we will try to process the next later.
                            removeMessages(IServiceMessage.MSG_OPERATION_PROCESS_NEXT);
                            break;
                        } else if (result == TriggerResult.SKIPPED) {
                            sendEmptyMessage(IServiceMessage.MSG_OPERATION_PROCESS_NEXT);
                            break;
                        } else if (result == TriggerResult.NO_PENDING_OPERATION) {
                            Log.d(TAG.SERVICE, "Nothing to process.");
                            sendMessageDelayed(obtainMessage(IServiceMessage.MSG_QUIT_SERVICE), EXIT_DELAY);
                            break;
                        }
                        // Clear caches
                        sCCStoredParams.clear();
                    } else {
                        operation = mOperationManager.current();
                        Log.d(TAG.SERVICE, "Retry/recover current operation " + operation);
                        if (mOperationManager.isInRecovery()) {
                            if (operation.getId() == ((DmOperation) msg.obj).getId()) {
                                Log.w(TAG.SERVICE, "Sanity check passed. Retry.");
                                // Clear the pending timeout messages for the current operation.
                                removeMessages(IServiceMessage.MSG_OPERATION_TIME_OUT, operation);
                                removeMessages(IServiceMessage.MSG_OPERATION_RECOVER_CURRENT, operation);
                                removeMessages(IServiceMessage.MSG_OPERATION_RETRY_CURRENT, operation);
                                removeMessages(IServiceMessage.MSG_CHECK_NETWORK);
                                if (msg.what == IServiceMessage.MSG_OPERATION_RETRY_CURRENT) {
                                    mOperationManager.retryCurrent();
                                } else /* msg.what == IDmPersistentValues.MSG_OPERATION_RECOVER_CURRENT */ {
                                    mOperationManager.recoverCurrent();
                                }
                            } else {
                                throw new Error("Invalid operation state");
                            }
                        } else {
                            if (operation.getId() == ((DmOperation) msg.obj).getId()) {
                                Log.w(TAG.SERVICE, "Ignore duplicate retry/recover message.");
                                break;
                            } else {
                                throw new Error("Invalid operation state");
                            }
                        }
                    }

                    operation = mOperationManager.current();
                    if (operation.getProperty(KEY.TYPE).equals(Type.TYPE_SI)) {
                        byte[] message = operation.getByteArrayProperty(KEY.NIA);
                        if (Utilities.isLawmoLocked()) {
                            Log.w(TAG.MMI, "LAWMO locked, confirm notification without showing it.");
                            sDmController.triggerNiaDmSession(message);
                        } else {
                            int uiMode = Utilities.extractUIModeFromNIA(message);
                            Log.i(TAG.SERVICE, "UI mode is " + uiMode);
                            if (uiMode >= 0) {
                                InteractionResponse ir = null;
                                int lastResponse = InteractionResponse.INVALID;
                                switch (uiMode) {
                                    case 0:
                                    case 1:
                                        Log.d(TAG.SERVICE, "Trigger NIA in Handler");
                                        sDmController.triggerNiaDmSession(message);
                                        break;
                                    case 2:
                                        ir = operation.getNextUIResponse();
                                        if (ir != null) {
                                            if (ir.type != InteractionResponse.InteractionType.NOTIFICATION) {
                                                throw new Error("Invalid interaction response type!");
                                            } else {
                                                // Set a fake flag to skip notification of NIA.
                                                lastResponse = InteractionResponse.POSITIVE;
                                            }
                                        }
                                        if (lastResponse == InteractionResponse.INVALID) {
                                            mOperationManager.current().addUIResponse(
                                                    new InteractionResponse(
                                                            InteractionType.NOTIFICATION,
                                                            InteractionResponse.POSITIVE,
                                                            null));
                                            if (DmFeatureSwitch.DM_SCOMO) {
                                                mScomoManager.setVerbose(false);
                                            }
                                            mDmNotification.showNotification(
                                                    NotificationInteractionType.TYPE_NOTIFICATION_VISIBLE);
                                            setNiaAlertTimeout(INiaManager.DEFAULT_NOTIFICATION_VISIBLE_TIMEOUT,
                                                    NotificationInteractionType.TYPE_NOTIFICATION_VISIBLE);
                                        }
                                        sDmController.triggerNiaDmSession(message);
                                        break;
                                    case 3:
                                        ir = operation.getNextUIResponse();
                                        if (ir != null) {
                                            if (ir.type != InteractionResponse.InteractionType.NOTIFICATION) {
                                                throw new Error("Invalid interaction response type!");
                                            } else {
                                                lastResponse = ir.response;
                                            }
                                        }
                                        if (lastResponse == InteractionResponse.INVALID) {
                                            mDmNotification.showNotification(
                                                    NotificationInteractionType.TYPE_NOTIFICATION_INTERACT);
                                            setNiaAlertTimeout(INiaManager.DEFAULT_NOTIFICATION_INTERACT_TIMEOUT,
                                                    NotificationInteractionType.TYPE_NOTIFICATION_INTERACT);
                                        } else if (lastResponse == InteractionResponse.POSITIVE) {
                                            sDmController.triggerNiaDmSession(message);
                                        } else {
                                            // lastResponse CANNOT be InteractionResponse.NEGATIVE,
                                            // otherwise there should be nothing to recover.
                                            throw new Error("Invalid last response for notification " + lastResponse);
                                        }
                                        break;
                                    default:
                                        // Ignore it
                                        Log.e(TAG.SERVICE, "Invalid notification UI mode " + uiMode);
                                        break;
                                }
                            }
                        }
                    } else if (operation.getProperty(KEY.TYPE).equals(Type.TYPE_REPORT_FUMO)) {
                        // We deploy the same logic for both trigger and recovery
                        mFumoManager.reportResult(operation);
                    } else if (operation.getProperty(KEY.TYPE).equals(Type.TYPE_REPORT_LAWMO)) {
                        Log.w(TAG.SERVICE, "LAWMO report is not implemented!!!");
                    } else if (operation.getProperty(KEY.TYPE).equals(Type.TYPE_REPORT_SCOMO)) {
                        // We deploy the same logic for both trigger and recovery
                        mScomoManager.reportResult(operation);
                    } else if (operation.getProperty(KEY.TYPE).equals(Type.TYPE_CI_FUMO)) {
                        if (msg.what == IServiceMessage.MSG_OPERATION_PROCESS_NEXT) {
                            mFumoManager.queryNewVersion();
                        } else {
                            mFumoManager.retryQueryNewVersion();
                        }
                    } else if (operation.getProperty(KEY.TYPE).equals(Type.TYPE_DL)) {
                        if (msg.what == IServiceMessage.MSG_OPERATION_PROCESS_NEXT) {
                            throw new Error("DL operation should only be triggered by triggerNow().");
                        } else {
                            if (operation.getBooleanProperty(KEY.FUMO_TAG, false)) {
                                mFumoManager.recoverDlPkg();
                            } else if (operation.getBooleanProperty(KEY.SCOMO_TAG, false)) {
                                mScomoManager.recoverDlPkg();
                            }
                        }
                    }
                    break;
                case IServiceMessage.MSG_OPERATION_TIME_OUT:
                    operation = mOperationManager.current();
                    if (mOperationManager.isInRecovery() && operation == (DmOperation) msg.obj) {
                        if (operation.getRetry() > 0) {
                            sendMessage(obtainMessage(IServiceMessage.MSG_OPERATION_RETRY_CURRENT, msg.obj));
                        }
                    } else {
                        Log.e(TAG.SERVICE, "Invalid time out message with operation " + ((DmOperation) msg.obj));
                        Log.e(TAG.SERVICE, "Ignore it.");
                    }
                    break;
                case IServiceMessage.MSG_WAP_CONNECTION_SUCCESS:
                    Log.w(TAG.SERVICE, "Receive message is MSG_WAP_CONNECTION_SUCCESS");

                    Log.i(TAG.SERVICE, "Conn success, remove MSG_CHECK_NETWORK.");
                    removeMessages(IServiceMessage.MSG_CHECK_NETWORK);

                    if (mOperationManager.isBusy()) {
                        if (mOperationManager.isInRecovery()) {
                            sendMessage(obtainMessage(
                                    IServiceMessage.MSG_OPERATION_RECOVER_CURRENT, 0, 0, mOperationManager.current()));
                        } else {
                            Log.i(TAG.SERVICE, "There is already an operation running, ignore this event.");
                        }
                    } else {
                        sendEmptyMessage(IServiceMessage.MSG_OPERATION_PROCESS_NEXT);
                    }
                    break;
                case IServiceMessage.MSG_QUIT_SERVICE:
                    // TODO Quit service properly
                    if (!mOperationManager.isBusy() && !mOperationManager.hasNext()) {
                        stopSelf();
                    }
                    Log.w(TAG.SERVICE, "NOT IMPLEMENTED: Exit message if there is nothing to do.");
                    break;
                case IServiceMessage.MSG_CHECK_NETWORK:
                    Log.i(TAG.SERVICE, "Receive MSG_CHECK_NETWORK after " +
                            (STAMP_CHECK_NET/1000) + "s.");
                    Intent serviceIntent = new Intent();
                    serviceIntent.setClass(DmService.this, DmService.class);
                    serviceIntent.setAction(DmConst.IntentAction.CHECK_NETWORK);
                    startService(serviceIntent);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }

        @Override
        public void notify(State state, State previousState, Object extra) {
            if (state == State.IDLE && mOperationManager.hasNext()) {
                // WARNING: MSG_OPERATION_PROCESS_NEXT message MUST be sent after a certain delay, otherwise it will
                // close the window for triggerNow() invocations!
                sendEmptyMessageDelayed(IServiceMessage.MSG_OPERATION_PROCESS_NEXT, HEART_BEAT);
            } else if(state == State.RECOVERING ) {
                Log.i(TAG.SERVICE, "In recover, Send MSG_CHECK_NETWORK after " +
                        (STAMP_CHECK_NET/1000) + "s.");
                // WARNING: if in recover state, check network after a certain delay
                sendEmptyMessageDelayed(IServiceMessage.MSG_CHECK_NETWORK, STAMP_CHECK_NET);
            }
        }
    };

    static class DmSessionStateObserver implements SessionStateObserver {
        private DmService mService;

        DmSessionStateObserver(DmService service) {
            mService = service;
        }

        // Interface method of SessionStateObserver.
        // Called by engine when state of session changes.
        public void notify(SessionType type, SessionState state, int lastError, SessionInitiator initiator) {
            if (mService == null) {
                Log.w(TAG.SESSION, "receive notify with mService is null");
            }
            Log.i(TAG.SESSION, "---- session state notify ----");
            Log.i(TAG.SESSION, "[type] = " + type);
            Log.i(TAG.SESSION, "[state] = " + state);
            Log.i(TAG.SESSION, "[last error] = " + MdmError.fromInt(lastError) + "(" + lastError + ")");
            Log.i(TAG.SESSION, "[initiator]= " + initiator.getId());
            Log.i(TAG.SESSION, "---- session state dumped ----");

            String initiatorName = initiator.getId();
            DmOperationManager operationManager = DmOperationManagerFactory.getInstance();
            DmOperation operation = operationManager.current();
            Log.d(TAG.SERVICE, "Current operation is " + operation);

            SessionHandler session = null;
            DmAction action = new DmAction();
            action.fumoAction = DmFeatureSwitch.DM_FUMO ? mService.getFumoManager().queryActions() : 0;
            action.scomoAction = DmFeatureSwitch.DM_SCOMO ? mService.getScomoManager().queryActions() : 0;
            action.lawmoAction = DmFeatureSwitch.DM_LAWMO ? mService.getLawmoManager().queryActions() : 0;

            Log.d(TAG.SESSION, "DmAction is " + action);
            if (DmFeatureSwitch.DM_SCOMO && (mService.getScomoManager().isScomoInitiator(initiatorName)
                    || action.scomoAction != 0
                    || operationManager.current().getBooleanProperty(KEY.SCOMO_TAG, false))) {
                operationManager.current().setProperty(KEY.SCOMO_TAG, true);
                Log.d(TAG.SESSION, "SCOMO session");
                session = mService.mScomoManager.getSessionHandler();
            } else if (DmFeatureSwitch.DM_FUMO && (mService.getFumoManager().isFumoInitiator(initiatorName)
                    || action.fumoAction != 0
                    || operationManager.current().getBooleanProperty(KEY.FUMO_TAG, false))) {
                operationManager.current().setProperty(KEY.FUMO_TAG, true);
                Log.d(TAG.SESSION, "FUMO session");
                session = mService.mFumoManager.getSessionHandler();
            } else if (DmFeatureSwitch.DM_LAWMO && (mService.getLawmoManager().isLawmoInitiator(initiatorName)
                    || action.lawmoAction != 0
                    || operationManager.current().getBooleanProperty(KEY.LAWMO_TAG, false))) {
                operationManager.current().setProperty(KEY.LAWMO_TAG, true);
                Log.d(TAG.SESSION, "LAWMO session");
                session = mService.mLawmoManager.getSessionHandler();
            } else if (initiatorName.startsWith(INiaManager.INITIATOR)) {
                Log.d(TAG.SESSION, "Normal session");
                session = mService.mNiaManager;
            } else {
                Log.e(TAG.SESSION, "unknown initiator: " + initiator.getId());
                return;
            }

            // Throw it to main thread to run
            class SSORunnable implements Runnable {
                private SessionHandler mSession;
                private SessionType mType;
                private SessionState mState;
                private int mLastError;
                private DmAction mAction;

                public SSORunnable(
                        SessionHandler session,
                        SessionType type,
                        SessionState state,
                        int lastError,
                        DmAction action) {
                    mSession = session;
                    mType = type;
                    mState = state;
                    mLastError = lastError;
                    mAction = action;
                }

                @Override
                public void run() {
                    Log.i(TAG.SESSION, "SSORunnable run!");
                    mSession.onSessionStateChange(mType, mState, mLastError, mAction);

                    Log.i(TAG.SESSION, "SSO sSemp.release");
                    sSemp.release();
                }
            }

            Runnable r = new SSORunnable(session, type, state, lastError, action);
            mService.mServiceHandler.post(r);
            try {
                Log.i(TAG.SESSION, "Engine before acquire, permit number " + sSemp.availablePermits());
                sSemp.acquire();
                Log.i(TAG.SESSION, "Engine after acquire, permit number " + sSemp.availablePermits());
            } catch (InterruptedException e) {
                throw new Error(e);
            }
        }

    };

    /***************** DM SessionHandler end ********************/

    /********** Alert 1101 start **************/
    private DmAlertConfirm mAlertConfirm;
    private static MmiViewContext sAlertConfirmContext;

    /**
     * Get view context of alert1101
     * 
     * @return
     */
    public MmiViewContext getAlertConfirmContext() {
        return sAlertConfirmContext;
    }

    /**
     * Notify UI to show alert 1101.
     */
    public void showAlertConfirm(MmiViewContext context) {
        Log.i(TAG.SERVICE, "showNiaNotification");
        sAlertConfirmContext = context;
        if (mScomoManager != null) {
            Log.i(TAG.SERVICE, "verbose set to true");
            mScomoManager.setVerbose(true);
        }
        DmOperation operation = mOperationManager.current();
        InteractionResponse ir = operation.getNextUIResponse();
        if (ir != null) {
            if (ir.type == InteractionType.CONFIRMATION) {
                switch (ir.response) {
                    case InteractionResponse.POSITIVE:
                        mAlertConfirm.confirm();
                        break;
                    case InteractionResponse.NEGATIVE:
                        mAlertConfirm.cancel();
                        break;
                    case InteractionResponse.TIMEOUT:
                        mAlertConfirm.timeout();
                        break;
                    default:
                        throw new Error("Invalid interaction response " + ir.response);
                }
            } else {
                throw new Error("Invalid interaction response type " + ir.type);
            }
        } else {
            /* Alert message */
            mDmNotification.showNotification(NotificationInteractionType.TYPE_ALERT_1101);
            setNiaAlertTimeout(
                    (context.maxDT > 0 ? context.maxDT : DEFAULT_ALERT_1101_TIMEOUT),
                    NotificationInteractionType.TYPE_ALERT_1101);
        }
    }

    /********** Alert 1101 end **************/

    /*********** Alert ****************************/
    private PendingIntent mNiaAlertTimeoutIntent = null;

    /**
     * @param seconds
     * @param nia
     */
    private void setNiaAlertTimeout(long seconds, int type) {
        Log.i(TAG.SERVICE, "+setNiaAlertTimeout(" + seconds + ", " + type + ")");
        Intent intent = new Intent(this, DmService.class);
        intent.putExtra("type", type);
        switch (type) {
            case NotificationInteractionType.TYPE_ALERT_1101:
            case NotificationInteractionType.TYPE_ALERT_1102:
            case NotificationInteractionType.TYPE_ALERT_1103:
            case NotificationInteractionType.TYPE_ALERT_1104:
                intent.setAction(DmConst.IntentAction.DM_ALERT_TIMEOUT);
                break;
            case NotificationInteractionType.TYPE_NOTIFICATION_VISIBLE:
            case NotificationInteractionType.TYPE_NOTIFICATION_INTERACT:
                intent.setAction(DmConst.IntentAction.DM_NOTIFICATION_TIMEOUT);
                break;
            default:
                throw new Error("Invalid alert type " + type);
        }

        if (sAlarmMgr == null) {
            sAlarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        }
        mNiaAlertTimeoutIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        sAlarmMgr.cancel(mNiaAlertTimeoutIntent);
        sAlarmMgr.set(
                AlarmManager.RTC_WAKEUP,
                (System.currentTimeMillis() + seconds * 1000),
                mNiaAlertTimeoutIntent);
        Log.i(TAG.SERVICE, "-setNiaAlertTimeout()");
    }

    public void cancelNiaAlertTimeout() {
        Log.i(TAG.SERVICE, "+cancelNiaAlertTimeout()");
        if (sAlarmMgr != null && mNiaAlertTimeoutIntent != null) {
            sAlarmMgr.cancel(mNiaAlertTimeoutIntent);
            mNiaAlertTimeoutIntent = null;
        }
        Log.i(TAG.SERVICE, "-cancelNiaAlertTimeout()");
    }

    /*********** Alert end ****************************/

    /***************** Component management start *********************************/
    private interface DmComponent {
        String getName();

        void init(DmService service);

        void deinit(DmService service);
    };

    private static ArrayList<DmComponent> sComponents = new ArrayList<DmComponent>();

    private void initComponents() {
        mDmNotification = new DmNotification(DmService.this);
        for (DmComponent com : sComponents) {
            Log.i(TAG.SERVICE, "Init component: " + com.getName());
            com.init(this);
        }
    }

    private void deinitComponents() {
        for (DmComponent com : sComponents) {
            Log.i(TAG.SERVICE, "Deinit component: " + com.getName());
            com.deinit(this);
        }
    }

    private NiaSessionManager mNiaManager = new NiaSessionManager(this);
    private IScomoManager mScomoManager = null;
    private IFumoManager mFumoManager = null;
    private ILawmoManager mLawmoManager = null;

    public IFumoManager getFumoManager() {
        return mFumoManager;
    }

    public IScomoManager getScomoManager() {
        return mScomoManager;
    }

    public ILawmoManager getLawmoManager() {
        return mLawmoManager;
    }

    static {
        if (DmFeatureSwitch.DM_SCOMO) {
            sComponents.add(new DmComponent() {
                @Override
                public String getName() {
                    return "COM:SCOMO";
                }

                @Override
                public void init(DmService service) {
                    try {
                        Class<?> scomoManagerClass = Class.forName("com.mediatek.mediatekdm.scomo.ScomoManager");
                        Constructor<?> scomoManagerConstructor = scomoManagerClass.getConstructor(DmService.class);
                        service.mScomoManager = (IScomoManager) scomoManagerConstructor.newInstance(service);
                    } catch (Exception e) {
                        throw new Error(e);
                    }
                }

                @Override
                public void deinit(DmService service) {
                    if (service != null && service.mScomoManager != null) {
                        service.mScomoManager.destroy();
                    }
                }

            });
        }

        if (DmFeatureSwitch.DM_FUMO) {
            sComponents.add(new DmComponent() {
                @Override
                public String getName() {
                    return "COM:FUMO";
                }

                @Override
                public void init(DmService service) {
                    try {
                        Class<?> fumoManagerClass = Class.forName("com.mediatek.mediatekdm.fumo.FumoManager");
                        Constructor<?> fumoManagerConstructor = fumoManagerClass.getConstructor(DmService.class);
                        service.mFumoManager = (IFumoManager) fumoManagerConstructor.newInstance(service);
                    } catch (Exception e) {
                        throw new Error(e);
                    }
                }

                @Override
                public void deinit(DmService service) {
                    if (service != null && service.mFumoManager != null) {
                        service.mFumoManager.destroy();
                        service.mFumoManager = null;
                    }
                }

            });
        }
        
        if (DmFeatureSwitch.DM_LAWMO) {
            sComponents.add(new DmComponent() {
                @Override
                public String getName() {
                    return "COM:LAWMO";
                }

                @Override
                public void init(DmService service) {
                    try {
                        Class<?> lawmoManagerClass = Class.forName("com.mediatek.mediatekdm.lawmo.LawmoManager");
                        Constructor<?> lawmoManagerConstructor = lawmoManagerClass.getConstructor(DmService.class);
                        service.mLawmoManager = (ILawmoManager) lawmoManagerConstructor.newInstance(service);
                    } catch (Exception e) {
                        throw new Error(e);
                    }
                }

                @Override
                public void deinit(DmService service) {
                    service.mLawmoManager.destroy();
                    service.mLawmoManager = null;
                }

            });
        }
    }

    public void clearDmNotification() {
        if (mDmNotification != null) {
            mDmNotification.clear();
        }
    }

    public interface IServiceMessage {
        /// When recover, check whether network ready
        int MSG_CHECK_NETWORK = 100;
        /// Process next operation.
        int MSG_OPERATION_PROCESS_NEXT = 500;
        /// Recover current operation. This message should be parameterized with the operation to recover for sanity check.
        int MSG_OPERATION_RECOVER_CURRENT = 501;
        /// Retry current operation. This message should be parameterized with the operation to recover for sanity check.
        int MSG_OPERATION_RETRY_CURRENT = 502;
        /// Operation has timed out. This message should be parameterized with the operation timed out for sanity check.
        int MSG_OPERATION_TIME_OUT = 503;
        /// Quit service if we are idle.
        int MSG_QUIT_SERVICE = 600;
        int MSG_WAP_CONNECTION_SUCCESS = 103;
        int MSG_WAP_CONNECTION_TIMEOUT = 104;
    }
}
