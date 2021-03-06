/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2012. All rights reserved.
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

package com.mediatek.rcse.plugin.message;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.IMessenger;
import android.os.RemoteException;
import android.telephony.TelephonyManager;

import com.mediatek.mms.ipmessage.ActivitiesManager;
import com.mediatek.mms.ipmessage.ChatManager;
import com.mediatek.mms.ipmessage.ContactManager;
import com.mediatek.mms.ipmessage.IpMessagePluginImpl;
import com.mediatek.mms.ipmessage.MessageManager;
import com.mediatek.mms.ipmessage.NotificationsManager;
import com.mediatek.mms.ipmessage.ResourceManager;
import com.mediatek.mms.ipmessage.ServiceManager;
import com.mediatek.rcse.activities.widgets.ContactsListManager;
import com.mediatek.rcse.activities.widgets.PhotoLoaderManager;
import com.mediatek.rcse.api.Logger;
import com.mediatek.rcse.api.RegistrationApi;
import com.mediatek.rcse.service.ApiManager;
import com.mediatek.rcse.service.PluginApiManager;
import com.mediatek.rcse.service.binder.IRemoteWindowBinder;

import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.provider.settings.RcsSettings;

/**
 * Used to access RCSe plugin
 */
public class IpMessagePluginExt extends IpMessagePluginImpl implements PluginApiManager.RegistrationListener {

    private static final String TAG = "IpMessagePluginExt";
    private IpMessageManager mMessageManager;
    private PluginChatWindowManager mPluginChatWindowManager;
    private IpMessageServiceMananger mIpMessageServiceManager = null;
    private IpMessageContactManager mIpMessageContactManager = null;
    private IpNotificationsManager mIpNotificationsManager = null;
    private IRemoteWindowBinder mRemoteWindowBinder;
    private ActivitiesManager mActiviesMananger = null;
    private ChatManager mChatManager = null;
    private final boolean mIsSimCardAvailable;
    private Context mHostContext = null;
    private IpMessageResourceMananger mResourceMananger = null;

    public IpMessagePluginExt(Context context) {
        super(context);
        Logger.initialize(context);
        PluginApiManager.initialize(context);
        PluginApiManager.getInstance().addRegistrationListener(this);
        mIsSimCardAvailable = isSimCardAvailable(context);
        if (mIsSimCardAvailable) {
            PluginUtils.initializeSimIdFromTelephony(context);
        } else {
            Logger.d(TAG,
                    "IpMessagePluginExt mIsSimCardAvailable is false, no need to initialize simId");
        }
    }
    
    @Override
    public ResourceManager getResourceManager(Context context) {
        Logger.d(TAG, "getResourceManager() entry! context = "
                + context.getApplicationInfo().packageName);
        if (mIsSimCardAvailable && Logger.getIsIntegrationMode()) {
            initialize(context);
            return mResourceMananger;
        } else {
            Logger.d(TAG, "getResourceManager() in chat app mode");
            return super.getResourceManager(context);
        }
    }
    
    @Override
    public ActivitiesManager getActivitiesManager(Context context) {
        Logger.d(TAG, "getActivitiesManager() entry! context = "
                + context.getApplicationInfo().packageName);
        if (mIsSimCardAvailable && Logger.getIsIntegrationMode()) {
            initialize(context);
            return mActiviesMananger;
        } else {
            Logger.d(TAG, "getActivitiesManager() in chat app mode");
            return super.getActivitiesManager(context);
        }
    }

    @Override
    public ChatManager getChatManager(Context context) {
        Logger.d(TAG, "getChatManager() entry! context = "
                + context.getApplicationInfo().packageName);
        if (mIsSimCardAvailable && Logger.getIsIntegrationMode()) {
            initialize(context);
            return mChatManager;
        } else {
            Logger.d(TAG, "getActivitiesManager() in chat app mode");
            return super.getChatManager(context);
        }
    }

    private void initialize(Context context) {
        if (mHostContext == null) {
            mHostContext = context;
            AndroidFactory.setApplicationContext(context);
            mMessageManager = new IpMessageManager(context);
            mResourceMananger = new IpMessageResourceMananger(context);
            mIpMessageContactManager = new IpMessageContactManager(context);
            mIpNotificationsManager = new IpNotificationsManager(context);
            mActiviesMananger = new IpMessageActivitiesManager(context);
            mChatManager = new IpMessageChatManger(context);
            mIpMessageServiceManager = new IpMessageServiceMananger(context);
            PhotoLoaderManager.initialize(context);
            if (!ApiManager.initialize(context)) {
                Logger.e(TAG, "IpMessageServiceMananger() ApiManager initialization failed!");
            }
            RcsSettings.createInstance(context);

            RegistrationApi registrationApi = ApiManager.getInstance().getRegistrationApi();
            if (registrationApi != null) {
                mIpMessageServiceManager.serviceIsReady();
            } else {
                Logger.d(TAG, "getServiceManager() registrationApi is null, has not connected");
            }
            bindRcseService(context);
            ContactsListManager.initialize(context);
        } else {
            Logger.d(TAG, "setContext(), context is exist!");
        }
    }

    private void bindRcseService(Context context) {
        Logger.d(TAG, "bindRcseService() context = " + context);
        if (context == null) {
            Logger.e(TAG, "bindRcseService() context is null");
            return;
        }
        boolean result = context.bindService(new Intent(IRemoteWindowBinder.class.getName()), mServiceConnection,
                Context.BIND_AUTO_CREATE);
        Logger.d(TAG, "bindRcseService() bindService connect result = " + result);
    }

    public MessageManager getMessageManager(Context context) {
        Logger.d(TAG, "getMessageManager() entry! context = "
                + context.getApplicationInfo().packageName);
        if (mIsSimCardAvailable && Logger.getIsIntegrationMode()) {
            initialize(context);
            return mMessageManager;
        } else {
            Logger.d(TAG, "getMessageManager() in chat app mode");
            return super.getMessageManager(context);
        }
    }

    @Override
    public ServiceManager getServiceManager(Context context) {
        Logger.d(TAG, "getServiceManager() entry! context = " + context);
        if (mIsSimCardAvailable && Logger.getIsIntegrationMode()) {
            initialize(context);
            return mIpMessageServiceManager;
        } else {
            Logger.d(TAG, "getServiceManager() in chat app mode");
            return super.getServiceManager(context);
        }
    }

    public NotificationsManager getNotificationsManager(Context context) {
        Logger.d(TAG, "getNotificationsManager() entry! context = " + context);
        if (mIsSimCardAvailable && Logger.getIsIntegrationMode()) {
            initialize(context);
            return mIpNotificationsManager;
        } else {
            Logger.d(TAG, "getNotificationsManager() in chat app mode");
            return super.getNotificationsManager(context);
        }
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        
        @Override
        public void onServiceDisconnected(ComponentName className) {
            Logger.v(TAG, "onServiceDisconnected() entry");
            mRemoteWindowBinder = null;
            PluginController.destroyInstance();
            Logger.v(TAG, "onServiceDisconnected() exit");
        }
        
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Logger.v(TAG, "onServiceConnected() entry className = " + className);
            mRemoteWindowBinder = IRemoteWindowBinder.Stub.asInterface(service);
            mPluginChatWindowManager = new PluginChatWindowManager(mMessageManager);
            try {
                Logger.d(TAG, "onServiceConnected(), mPluginChatWindowManager = " + mPluginChatWindowManager
                        + " windowBinder = " + mRemoteWindowBinder);
                mRemoteWindowBinder.addChatWindowManager(mPluginChatWindowManager, true);
                IMessenger messenger = IMessenger.Stub.asInterface(mRemoteWindowBinder.getController());
                PluginController.initialize(messenger);
                Logger.d(TAG, "onServiceConnected(), messenger = " + messenger);
                PluginUtils.reloadRcseMessages();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    };

    @Override
    public ContactManager getContactManager(Context context) {
        
        Logger.d(TAG, "getContactManager(), context = " + context.getApplicationInfo().packageName);
        if (mIsSimCardAvailable && Logger.getIsIntegrationMode()) {
            initialize(context);
            return mIpMessageContactManager;
        } else {
            Logger.d(TAG, "getContactManager() in chat app mode");
            return super.getContactManager(context);
        }
    }

    /**
     * Check whether SIM card is available in the device
     * 
     * @return False if no SIM card is available in the device
     */
    private boolean isSimCardAvailable(Context context) {
        Logger.d(TAG, "isSimCardAvailable() entry, context is " + context);
        TelephonyManager manager =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        int state = TelephonyManager.SIM_STATE_ABSENT;
        if (manager == null) {
            Logger.e(TAG, "isSimCardAvailable() entry, manager is " + null);
        } else {
            state = manager.getSimState();
            Logger.d(TAG, "isSimCardAvailable() state is " + state);
        }
        if ((TelephonyManager.SIM_STATE_ABSENT) == state) {
            Logger.d(TAG, "isSimCardAvailable() ,no SIM card found");
            return false;
        } else {
            Logger.w(TAG, "isSimCardAvailable() ,SIM card found");
            return true;
        }
    }

    @Override
    public boolean isActualPlugin() {
        return true;
    }

    @Override
    public void onApiConnectedStatusChanged(boolean isConnected) {
        Logger.d(TAG, "onApiConnectedStatusChanged() entry isConnected is " + isConnected);
        if (isConnected) {
            Logger.d(TAG, "onApiConnectedStatusChanged(), rebind rcse service");
            bindRcseService(mHostContext);
        } else {
            Logger.d(TAG, "onApiConnectedStatusChanged(), disconnect!");
        }
    }

    @Override
    public void onRcsCoreServiceStatusChanged(int status) {
        Logger.d(TAG, "onRcsCoreServiceStatusChanged() entry status is " + status);
    }

    @Override
    public void onStatusChanged(boolean status) {
        Logger.d(TAG, "onStatusChanged() entry status is " + status);

    }
}
