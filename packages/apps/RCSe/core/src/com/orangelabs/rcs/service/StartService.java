/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
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
 ******************************************************************************/

package com.orangelabs.rcs.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.XmlResourceParser;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Telephony.SIMInfo;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.rcse.api.FlightModeApi;
import com.mediatek.rcse.plugin.apn.RcseOnlyApnUtils;
import com.mediatek.rcse.service.NetworkChangedReceiver;
import com.mediatek.xlog.Xlog;
import com.orangelabs.rcs.R;
import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.platform.registry.AndroidRegistryFactory;
import com.orangelabs.rcs.provider.eab.ContactsManager;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.provider.settings.RcsSettingsData;
import com.orangelabs.rcs.provisioning.https.HttpsProvisioningService;
import com.orangelabs.rcs.service.api.client.ClientApiIntents;
import com.orangelabs.rcs.utils.PhoneUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * RCS start service.
 *
 * @author hlxn7157
 */
public class StartService extends Service {
    /**
     * Service name
     */
    public static final String SERVICE_NAME = "com.orangelabs.rcs.service.START";

    /**
     * M:  Add for storing 3 SIM card user data info @{
     */
    /**
     * Indicates the last first user account used
     */
    public static final String REGISTRY_LAST_FIRST_USER_ACCOUNT = "LastFirstUserAccount";
    
    /**
     * Indicates the last second user account used
     */
    public static final String REGISTRY_LAST_SECOND_USER_ACCOUNT = "LastSecondUserAccount";
    
    /**
     * Indicates the last third user account used
     */
    public static final String REGISTRY_LAST_THIRD_USER_ACCOUNT = "LastThirdUserAccount";
    

	/**
     * Intent broadcasted when the RCS configuration status has changed (see constant attribute "status").
     * 
     * <p>The intent will have the following extra values:
     * <ul>
     *   <li><em>status</em> - Configuration status.</li>
     * </ul>
     * </ul>
     */
	public final static String CONFIGURATION_STATUS = "com.orangelabs.rcs.CONFIGURATION_STATUS";

    /**
     * Indicates the last User icci account
     */
    private ArrayList<String> lastUserIcci = null;
    
    /**
     * Indicates the user account index included in lastUserAccount
     */
    private final static int LAST_FIRST_USER_ACCOUNT_INDEX = 0; 
    private final static int LAST_SECOND_USER_ACCOUNT_INDEX = 1; 
    private final static int LAST_THIRD_USER_ACCOUNT_INDEX = 2; 
    /** 
     * @} 
     */

    /**
     * Current user account used
     */
    public static final String REGISTRY_CURRENT_USER_ACCOUNT = "CurrentUserAccount";

    /**
     * RCS new user account
     */
    public static final String REGISTRY_NEW_USER_ACCOUNT = "NewUserAccount";

    /**
     * Connection manager
     */
    private ConnectivityManager connMgr = null;

    /**
     * Network state listener
     */
    private BroadcastReceiver networkStateListener = null;

    /**
     * Current User account
     */
    private String currentUserAccount = null;

    /**
     * Launch boot flag
     */
    private boolean boot = false;

    /**
     * M: 
     */
    /**
     * Indicate whether network state listener has been registered.
     */
    private final AtomicBoolean mIsRegisteredAtomicBoolean = new AtomicBoolean();
    
    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    @Override
    public void onCreate() {
        if (logger.isActivated()) {
            logger.debug("onCreate() called");
        }
        // Instantiate RcsSettings
        RcsSettings.createInstance(getApplicationContext());
        /**
         * M: rcse only apn feature @{
         */
        // because LauncherUtils may be called by UI process, so there
        // may not createInstance in core process.
        if (RcseOnlyApnUtils.getInstance() == null) {
            RcseOnlyApnUtils.createInstance(getApplicationContext());
        }
        /** @} */
        // Use a network listener to start RCS Core when the data will be ON 
        if (PhoneUtils.isUsingDummyAccount()) {
            // Get connectivity manager
            if (connMgr == null) {
                connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            }
            
            // Instantiate the network listener
	        networkStateListener = new BroadcastReceiver() {
	            @Override
	            public void onReceive(Context context, final Intent intent) {
	                Thread t = new Thread() {
	                    public void run() {
	                        connectionEvent(intent.getAction());
	                    }
	                };
	                t.start();
	            }
	        };
	
	        // Register network state listener
	        IntentFilter intentFilter = new IntentFilter();
	        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
	        registerReceiver(networkStateListener, intentFilter);
	        mIsRegisteredAtomicBoolean.set(true);
            /**
             * M: Modified for doing unregister when device go to flight mode. @{
             */
            new FlightModeApi(getApplicationContext()).connectApi();
            /**
             * @}
             */
        }
        registerNetworkReceiver();
    }

    @Override
    public void onDestroy() {
        // finalize the RcseOnlyApnUtils instance
        RcseOnlyApnUtils.getInstance().destroy();
        
        // Unregister network state listener
        if (networkStateListener != null && mIsRegisteredAtomicBoolean.compareAndSet(true, false)) {
            unregisterReceiver(networkStateListener);
            networkStateListener = null;
        }
        unregisterNetworkReceiver();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (logger.isActivated()) {
            logger.debug("Start RCS service");
        }

        // Check boot
        if (intent != null) {
            boot = intent.getBooleanExtra("boot", false);
        }

        /**
         * M: Check account in a background thread, fix an ANR issue. @{
         */
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                // if it should use only apn due to network status, and it
                // is not already started or route fail, then do start to
                // request to use only apn
                if (RcsSettings.getInstance().isRcseOnlyApnEnabled()
                        && !RcseOnlyApnUtils.getInstance().isRcsOnlyApnStarted()) {
                    RcseOnlyApnUtils.getInstance().switchRcseOnlyApn();
                }
                boolean accountAvailable = checkAccount();
                if (logger.isActivated()) {
                    logger.debug("accountAvailable = " + accountAvailable);
                }
                if (accountAvailable) {
                    launchRcsService(boot);
                } else {
                    // User account can't be initialized (no radio to read IMSI, .etc)
                    if (logger.isActivated()) {
                        logger.error("Can't create the user account");
                    }

                    // Send service intent 
                    Intent stopIntent = new Intent(ClientApiIntents.SERVICE_STATUS);
                    stopIntent.putExtra("status", ClientApiIntents.SERVICE_STATUS_STOPPED);
                    sendBroadcast(stopIntent);

                    // Exit service
                    stopSelf();
                }
            }
        });
        /** 
         * @} 
         */
        
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    /**
     * Connection event
     *
     * @param action Connectivity action
     */
    private void connectionEvent(String action) {
        if (logger.isActivated()) {
            logger.debug("Connection event " + action);
        }
        // Try to start the service only if a data connectivity is available
        if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
            if ((networkInfo != null) && networkInfo.isConnected()) {
                if (logger.isActivated()) {
                    logger.debug("Device connected - Launch RCS service");
                }
                boolean accountAvailable = checkAccount();
                if (logger.isActivated()) {
                    logger.debug("accountAvailable = " + accountAvailable);
                }
                if (accountAvailable) {
                /**
                 * M:  Add for displaying roaming notification @{
                 */
                // Start the RCS service
                launchRcseCoreServie();
                /** 
                 * @} 
                 */
                
                // Stop Network listener
                    if (networkStateListener != null
                            && mIsRegisteredAtomicBoolean.compareAndSet(true, false)) {
                	unregisterReceiver(networkStateListener);
                	networkStateListener = null;
                }
                } else {
                    // Exit service
                    stopSelf();
                }
            }
        }
    }

    /**
     * Set the country code
     */
    private void setCountryCode() {
        // Get country code 
        TelephonyManager mgr = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        String countryCodeIso = mgr.getSimCountryIso();
        if (countryCodeIso == null) {
        	if (logger.isActivated()) {
        		logger.error("Can't read country code from SIM");
        	}
            return;
        }

        // Parse country table to resolve the area code and country code
        try {
            XmlResourceParser parser = getResources().getXml(R.xml.country_table);
            parser.next();
            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if (parser.getName().equals("Data")) {
                        if (parser.getAttributeValue(null, "code").equalsIgnoreCase(countryCodeIso)) {
                        	String countryCode = parser.getAttributeValue(null, "cc");
                            if (countryCode != null) {
                                if (!countryCode.startsWith("+")) {
                                    countryCode = "+" + countryCode;
                                }
                                if (logger.isActivated()) {
                                    logger.info("Set country code to " + countryCode);
                                }
                                /**
                                 * M: Add for storing 3 SIM card user data info @{
                                 */
                                // Used to avoid ANR while do I/O operation
                                final String finalCountryCode = countryCode;
                                AsyncTask.execute(new Runnable(){
                                    public void run() {
                                        RcsSettings.getInstance().setCountryCode(finalCountryCode);
                                    }
                                });
                                /**
                                 * @}
                                 */
                            }

                        	final String areaCode = parser.getAttributeValue(null, "tc");
                            if (areaCode != null) {
                                if (logger.isActivated()) {
                                    logger.info("Set area code to " + areaCode);
                                }
                                /**
                                 * M: Add for storing 3 SIM card user data info @{
                                 */
                                // Used to avoid ANR while do I/O operation
                                AsyncTask.execute(new Runnable(){
                                    public void run() {
                                        RcsSettings.getInstance().setCountryAreaCode(areaCode);
                                    }
                                });
                                /**
                                 * @}
                                 */
                            }
                            return;
                        }
                    }
                }
                eventType = parser.next();
            }
        } catch (XmlPullParserException e) {
        	if (logger.isActivated()) {
        		logger.error("Can't parse country code from XML file", e);
        	}
        } catch (IOException e) {
        	if (logger.isActivated()) {
        		logger.error("Can't read country code from XML file", e);
        	}
        }
    }

    /**
     * Check account
     *
     * @return true if an account is available
     */
    private boolean checkAccount() {
        AndroidFactory.setApplicationContext(getApplicationContext());
        
        // Read the current and last end user account
        currentUserAccount = getIcciFromSimCard();
        
        /**
         * M: Added to check whether the SIM card has been insert into device. @{
         */
        if(TextUtils.isEmpty(currentUserAccount)){
            if (logger.isActivated()) {
                logger.debug("currentUserAccount is null");
            }
            return false;
        }
        /**
         * @}
         */
        
        /** 
         * M: Check whether system has been changed. @{ 
         */
        String curentFingerPrint = Build.FINGERPRINT;
        String lastFingerprint = RcsSettings.getInstance().getSystemFingerprint();
        if (logger.isActivated()) {
            logger.debug("curentFingerPrint = " + curentFingerPrint + ", lastFingerprint = "
                    + lastFingerprint);
        }
        //If last finger print is null or "", it means that is the fist time launch RCS-e client
        if (!curentFingerPrint.equals(lastFingerprint) && lastFingerprint != null
                && !lastFingerprint.equals("") && boot) {
            if (logger.isActivated()) {
                logger.debug("Reset config, because the system has been changed");
            }
            // Reset config
            LauncherUtils.resetAllRcsConfig(getApplicationContext());
            // Update the system load's finger print
            RcsSettings.getInstance().setSystemFingerprint(curentFingerPrint);
        } else if (!curentFingerPrint.equals(lastFingerprint)
                && (lastFingerprint == null || lastFingerprint.equals("")) && boot) {
            if (logger.isActivated()) {
                logger.debug("Update the system load's finger print");
            }
            // Update the system load's finger print
            RcsSettings.getInstance().setSystemFingerprint(curentFingerPrint);
        }
        /** 
         * @} 
         */
        
        lastUserIcci = getLastUserIcciList();
                if (logger.isActivated()) {
            for (int i = 0; i < lastUserIcci.size(); i++) {
                String tmpIcci = lastUserIcci.get(i);
                logger.debug("Last user icci[" + i + "]=" + (tmpIcci == null ? "null" : tmpIcci));
            }
            }
        boolean isFisrtLaunch = isFirstLaunch();
            if (logger.isActivated()) {
            logger.debug("isFisrtLaunch = " + (isFisrtLaunch ? "true" : "false"));
        }
        // On the first launch and if SIM card has changed
        if (isFisrtLaunch) {
            // Save the current end user account
            setLastUserIcci(currentUserAccount);
            // Set the country code
            setCountryCode();     
            // Set New user to true
            setNewIcci(true);
        } else {
            boolean isNewIcci = isNewIcci();
            if (logger.isActivated()) {
                logger.debug("isNewIcci = " + (isNewIcci ? "true" : "false"));
            }
            if (isNewIcci) {
                // Save the current end user account
                setLastUserIcci(currentUserAccount);
                // Set the default information to the specify user
                RcsSettings.getInstance().setDefaultUserProfile();
            // Set the country code
            setCountryCode();        
            // Activate service if new account
            RcsSettings.getInstance().setServiceActivationState(true);         
            // Set New user to true
                setNewIcci(true);
                // Account has changed (i.e. new SIM card): delete the current account and create a new one
                ContactsManager.createInstance(getApplicationContext());
                ContactsManager.getInstance().deleteRCSEntries();
        } else {
            // Set New user to false
                setNewIcci(false);
        }
        }
        RcsSettings.getInstance().init();
        return true;
    }

    /**
     * Launch the RCS service.
     *
     * @param boot indicates if RCS is launched from the device boot
     */
    private void launchRcsService(boolean boot) {
    	int mode = RcsSettings.getInstance().getAutoConfigMode();
    	
        if (logger.isActivated()) {
            logger.debug("Launch RCS service: HTTPS="
                + (mode == RcsSettingsData.HTTPS_AUTO_CONFIG)
                + ", boot=" + boot);
        }
        if (mode == RcsSettingsData.HTTPS_AUTO_CONFIG) {
            // HTTPS auto config
            // Check the last provisioning version
            if (RcsSettings.getInstance().getProvisionVersion().equals("-1")) {
                if (hasIcciSwapped()) {
                    // Reset provisioning version
                    // Start provisioning as a first launch
                    Intent intent = new Intent(HttpsProvisioningService.SERVICE_NAME);
                    intent.putExtra("first", true);
                    startService(intent);
                } else {
                    if (logger.isActivated()) {
                        logger.debug("Provisioning is blocked with this account");
                    }
                }
            } 
		   /**
                   * M:  Modified For Auto configuration if Version is 0 @{
                   */
                   // Start the RCS service
             else 
                 if (RcsSettings.getInstance().getProvisionVersion().equals("0")) {
                    Intent intent = new Intent(HttpsProvisioningService.SERVICE_NAME);
                    intent.putExtra("first", true);
                    startService(intent);
             	}
                    /** 
                     * @} 
                     */
             else {
                if (isFirstLaunch() || hasIcciSwapped()) {
                    // First launch: start the auto config service with special tag
                    Intent intent = new Intent(HttpsProvisioningService.SERVICE_NAME);
                    intent.putExtra("first", true);
                    startService(intent);
                } else if (boot) {
                    // Boot: start the auto config service
                    startService(new Intent(HttpsProvisioningService.SERVICE_NAME));
                } else {
                    /**
                     * M:  Modified for displaying roaming notification @{
                     */
                    // Start the RCS service
                    launchRcseCoreServie();
                    /** 
                     * @} 
                     */
                }
            }
        } else {
            /**
             * M:  Modified for displaying roaming notification @{
             */
            // No auto configuration: directly start the RCS service
            launchRcseCoreServie();
            /** 
             * @} 
             */
        }
    }

    /**
     * M: Modified for storing 3 SIM card user data info @{
     */
    /**
     * Get the last user account    
     */
    public static ArrayList<String> getLastUserIcciList() {
        ArrayList<String> lastUserIcciList = new ArrayList<String>(3);
        Context context = AndroidFactory.getApplicationContext();
        SharedPreferences preferences = context.getSharedPreferences(
                AndroidRegistryFactory.RCS_PREFS_NAME, Activity.MODE_PRIVATE);
        lastUserIcciList.add(preferences.getString(REGISTRY_LAST_FIRST_USER_ACCOUNT, null));
        lastUserIcciList.add(preferences.getString(REGISTRY_LAST_SECOND_USER_ACCOUNT, null));
        lastUserIcciList.add(preferences.getString(REGISTRY_LAST_THIRD_USER_ACCOUNT, null));
        return lastUserIcciList;
    }

    /**
     * Set the last user icci account
     *
     * @param value last user icci account
     */
    private void setLastUserIcci(String value) {
        String insertKey = null;
        SharedPreferences preferences = getSharedPreferences(AndroidRegistryFactory.RCS_PREFS_NAME, Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        // First user account
        String first = null;
        // Second user account
        String second = null;
        // Third user account
        String third = null;
        // Get the first user account
        first = preferences.getString(REGISTRY_LAST_FIRST_USER_ACCOUNT, null);
        if(first == null){
            // Set the insert index
            insertKey = REGISTRY_LAST_FIRST_USER_ACCOUNT;
        }else{
            if(first.equals(value)){
                return;
            }
            second = preferences.getString(REGISTRY_LAST_SECOND_USER_ACCOUNT, null);
            if(second == null){
                insertKey = REGISTRY_LAST_FIRST_USER_ACCOUNT;
                editor.putString(REGISTRY_LAST_SECOND_USER_ACCOUNT, first);
            }else{
                if(second.equals(value)){
                    return;
                }
                third = preferences.getString(REGISTRY_LAST_THIRD_USER_ACCOUNT, null);
                if(third != null && third.equals(value)){
                    return;
                }else{
                    insertKey = REGISTRY_LAST_FIRST_USER_ACCOUNT;
                    editor.putString(REGISTRY_LAST_SECOND_USER_ACCOUNT, first);
                    editor.putString(REGISTRY_LAST_THIRD_USER_ACCOUNT, second);
                }
            }
        }
        editor.putString(insertKey, value);
        editor.commit();
    }
    /**
     * @}
     */

    /**
     * Initiate the current user account from the imsi
     */
    public static String getIcciFromSimCard() {
        String icci = null;
        Context context = AndroidFactory.getApplicationContext();
        ITelephony telephony = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
        Logger logger = Logger.getLogger("StartService");
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            if (logger.isActivated()) {
                logger.debug("getIcciFromSimCard()-MTK_GEMINI_SUPPORT is true");
            }
            SIMInfo siminfo = SIMInfo.getSIMInfoBySlot(context, PhoneConstants.GEMINI_SIM_1);
            if (siminfo != null) {
                icci = siminfo.mICCId;
            } else {
                siminfo = SIMInfo.getSIMInfoBySlot(context, PhoneConstants.GEMINI_SIM_2);
                if (siminfo != null) {
                    icci = siminfo.mICCId;
                }
            }
        } else {
            if (logger.isActivated()) {
                logger.debug("getIcciFromSimCard()-MTK_GEMINI_SUPPORT is false");
            }
            SIMInfo siminfo = SIMInfo.getSIMInfoBySlot(context, PhoneConstants.GEMINI_SIM_1);
            if (siminfo != null) {
                icci = siminfo.mICCId;
            }
        }
        if (logger.isActivated()) {
            logger.debug("getIcciFromSimCard()-currentUserAccount is "
                    + (icci == null ? "null" : icci));
        }
        return icci;
    }

    /**
     * Is the first RCs is launched ?
     *
     * @return true if it's the first time RCS is launched
     */
    private boolean isFirstLaunch() {
        for(int i=0;i<lastUserIcci.size();i++){
            if(lastUserIcci.get(i) != null){
                return false;
            }
        }
        return true;
    }

    /**
     * M: Changed and added for auto configuration.@{
     */
    /**
     * Check if RCS sim card has changed since the last time we started the service
     *
     * @return true if the active account was changed
     */
    private boolean hasIcciSwapped() {
        String lastFirstUserIcci= lastUserIcci.get(LAST_FIRST_USER_ACCOUNT_INDEX);
        return currentUserAccount.equals(lastFirstUserIcci) ? false: true;
        }
    
            /**
     * Check whether a new sim card plugged in(means never plugged in before).
     *
     * @return true if a new sim card plugged in
             */
    private boolean isNewIcci() {
        return lastUserIcci.contains(currentUserAccount) ? false : true;
                }
            /**
             * @}
             */

    /**
     * Set true if new user account
     *
     * @param value true if new user account
     */
    private void setNewIcci(boolean value) {
        SharedPreferences preferences = getSharedPreferences(AndroidRegistryFactory.RCS_PREFS_NAME, Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(REGISTRY_NEW_USER_ACCOUNT, value);
        editor.commit();
    }

    /**
     * Check if new user account
     *
     * @param context Application context
     * @return true if new user account
     */
    public static boolean getNewUserAccount(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(AndroidRegistryFactory.RCS_PREFS_NAME, Activity.MODE_PRIVATE);
        return preferences.getBoolean(REGISTRY_NEW_USER_ACCOUNT, false);
    }
    
    /** 
     * M: Added to indicate whether the receiver is registered. @{ 
     */

    /**
     * NetworkChangedReceiver instance
     */
    private final NetworkChangedReceiver mNetworkChangedReceiver = new NetworkChangedReceiver();

    private void launchRcseCoreServie() {
                    LauncherUtils.launchRcsCoreService(getApplicationContext());
                }

    private synchronized void registerNetworkReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        this.registerReceiver(mNetworkChangedReceiver, filter);
        }

    private synchronized void unregisterNetworkReceiver() {
        this.unregisterReceiver(mNetworkChangedReceiver);
    }
    /** 
     * @} 
     */
}
