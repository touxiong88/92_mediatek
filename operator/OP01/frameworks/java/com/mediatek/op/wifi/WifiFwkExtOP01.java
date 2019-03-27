package com.mediatek.op.wifi;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;

import com.mediatek.common.wifi.IWifiFwkExt;
import com.mediatek.xlog.Xlog;

import java.util.List;

public class WifiFwkExtOP01 extends DefaultWifiFwkExt {
    private static final String TAG = "WifiFwkExtOP01";
    private static final int DEFAULT_FRAMEWORK_SCAN_INTERVAL_MS = 15000;
    private static final long SUSPEND_NOTIFICATION_DURATION = 60 * 60 * 1000;
    private static final String WIFISETTINGS_CLASSNAME = "com.android.settings.Settings$WifiSettingsActivity";

    private ConnectTypeObserver mConnectTypeObserver;
    private long mSuspendNotificationTime = 0;
    private int mConnectPolicy = Settings.System.WIFI_CONNECT_AP_TYPE_AUTO;
    private int mCellToWiFiPolicy = Settings.System.WIFI_CONNECT_TYPE_AUTO;
    private ConnectivityManager mCm;
    private ActivityManager mAm;

    public WifiFwkExtOP01(Context context) {
        super(context);
    }

    public void init() {
        super.init();
        mCm = (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        mAm = (ActivityManager)mContext.getSystemService(Context.ACTIVITY_SERVICE);
        mConnectPolicy = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.WIFI_CONNECT_AP_TYPE, Settings.System.WIFI_CONNECT_AP_TYPE_AUTO);
        mCellToWiFiPolicy = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.WIFI_CONNECT_TYPE, Settings.System.WIFI_CONNECT_TYPE_AUTO);
        mConnectTypeObserver = new ConnectTypeObserver(new Handler(), mContext);
    }

    public boolean shouldEnableAllNetworksForScreenOn() {
        return false;
    }

    public boolean shouldEnableAllNetworksForCompletion() {
        int connectPolicy = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.WIFI_CONNECT_AP_TYPE, Settings.System.WIFI_CONNECT_AP_TYPE_AUTO);
        int cellToWiFiPolicy = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.WIFI_CONNECT_TYPE, Settings.System.WIFI_CONNECT_TYPE_AUTO);
        Xlog.d(TAG, "shouldEnableAllNetworksForCompletion, connectPolicy:" + connectPolicy
                + ", cellToWiFiPolicy:" + cellToWiFiPolicy);
        return (connectPolicy == Settings.System.WIFI_CONNECT_AP_TYPE_AUTO
                && cellToWiFiPolicy == Settings.System.WIFI_CONNECT_TYPE_AUTO);
    }

    public boolean hasCustomizedAutoConnect() {
        return true;
    }

    public boolean shouldAutoConnect() {
        if (mConnectPolicy == Settings.System.WIFI_CONNECT_AP_TYPE_AUTO
            && mCellToWiFiPolicy == Settings.System.WIFI_CONNECT_TYPE_AUTO) {
            Xlog.d(TAG, "Should auto connect.");
            return true;
        } else {
            Xlog.d(TAG, "Shouldn't auto connect.");
            return false;
        }
    }

    public boolean isWifiConnecting(int connectingNetworkId, List<Integer> disconnectNetworks) {
        Xlog.d(TAG, "isWifiConnecting, mConnectPolicy:" + mConnectPolicy
            + ", mCellToWiFiPolicy:" + mCellToWiFiPolicy + ", connectingNetworkId:" + connectingNetworkId);
        boolean isConnecting = false;
        boolean autoConnect = false;
        NetworkInfo info = mCm.getActiveNetworkInfo();
        if (null == info) {
            Xlog.d(TAG, "No active network.");
        } else {
            Xlog.d(TAG, "Active network type:" + info.getTypeName());
        }
        String highestPriorityNetworkSSID = null;
        int highestPriority = -1;
        int highestPriorityNetworkId = -1;
        List<WifiConfiguration> networks = mWifiManager.getConfiguredNetworks();
        List<ScanResult> scanResults = mWifiManager.getScanResults();
        if (null != networks && null != scanResults) {
            for (WifiConfiguration network : networks) {
                for (ScanResult scanresult : scanResults) {
                    //Xlog.d(TAG, "network.SSID=" + network.SSID + ", scanresult.SSID=" + scanresult.SSID);
                    //Xlog.d(TAG, "network.security=" + getSecurity(network)
                    //      + ", scanresult.security=" + getSecurity(scanresult));
                    if ((network.SSID != null) && (scanresult.SSID != null)
                        && network.SSID.equals("\"" + scanresult.SSID + "\"")
                        && (getSecurity(network) == getSecurity(scanresult))) {
                        if (network.priority > highestPriority) {
                            highestPriority = network.priority;
                            highestPriorityNetworkId = network.networkId;
                            highestPriorityNetworkSSID = network.SSID;
                        }
                        if (network.networkId == connectingNetworkId) {
                            isConnecting = true;
                        }
                    }
                }
            }
        }
        Xlog.d(TAG, "highestPriorityNetworkId:" + highestPriorityNetworkId
            + ", highestPriorityNetworkSSID:" + highestPriorityNetworkSSID
            + ", highestPriority:" + highestPriority
            + ", currentTimeMillis:" + System.currentTimeMillis()
            + ", mSuspendNotificationTime:" + mSuspendNotificationTime);
        if (!isConnecting) {
            if (null != info && info.getType() == ConnectivityManager.TYPE_MOBILE) {
                if (mConnectPolicy == Settings.System.WIFI_CONNECT_AP_TYPE_AUTO) {
                    if (mCellToWiFiPolicy == Settings.System.WIFI_CONNECT_TYPE_ASK) {
                        if (highestPriorityNetworkId != -1 && !TextUtils.isEmpty(highestPriorityNetworkSSID)
                            && (System.currentTimeMillis() - mSuspendNotificationTime > SUSPEND_NOTIFICATION_DURATION)) {
                            Intent intent = new Intent(WifiManager.WIFI_NOTIFICATION_ACTION);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.putExtra(WifiManager.EXTRA_NOTIFICATION_SSID, highestPriorityNetworkSSID);
                            intent.putExtra(WifiManager.EXTRA_NOTIFICATION_NETWORKID, highestPriorityNetworkId);
                            mContext.startActivity(intent);
                        }
                    } else if (mCellToWiFiPolicy == Settings.System.WIFI_CONNECT_TYPE_AUTO) {
                        highestPriorityNetworkSSID = null;
                        highestPriority = -1;
                        highestPriorityNetworkId = -1;
                        if (null != networks && null != scanResults) {
                            for (WifiConfiguration network : networks) {
                                if (!disconnectNetworks.contains(network.networkId)
                                    && !isDisableForFailure(network.disableReason)) {
                                    for (ScanResult scanresult : scanResults) {
                                        //Xlog.d(TAG, "network.SSID=" + network.SSID
                                        //      + ", scanresult.SSID=" + scanresult.SSID);
                                        //Xlog.d(TAG, "network.security=" + getSecurity(network)
                                        //      + ", scanresult.security=" + getSecurity(scanresult));
                                        if ((network.SSID != null) && (scanresult.SSID != null)
                                            && network.SSID.equals("\"" + scanresult.SSID + "\"")
                                            && (getSecurity(network) == getSecurity(scanresult))) {
                                            if (network.priority > highestPriority) {
                                                highestPriority = network.priority;
                                                highestPriorityNetworkId = network.networkId;
                                                highestPriorityNetworkSSID = network.SSID;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Xlog.d(TAG, "Mobile connected, highestPriorityNetworkId:" + highestPriorityNetworkId
                            + ", highestPriorityNetworkSSID:" + highestPriorityNetworkSSID
                            + ", highestPriority:" + highestPriority);
                        if (highestPriorityNetworkId != -1 && !TextUtils.isEmpty(highestPriorityNetworkSSID)) {
                            Xlog.d(TAG, "Enable all networks for mobile is connected.");
                            sendEnableAllNetworksBroadcast();
                            autoConnect = true;
                        }
                    } else if (mCellToWiFiPolicy == Settings.System.WIFI_CONNECT_TYPE_MANUL) {
                        ComponentName cn = null;
                        String classname = null;
                        if (mAm.getRunningTasks(1) != null && mAm.getRunningTasks(1).get(0) != null) {
                            cn = mAm.getRunningTasks(1).get(0).topActivity;
                        }
                        if (cn != null) {
                            classname = cn.getClassName();
                            Xlog.d(TAG, "Class Name:" + classname);
                        } else {
                            Xlog.e(TAG, "ComponentName is null!");
                        }
                        if (!(classname != null && classname.equals(WIFISETTINGS_CLASSNAME))
                            && highestPriorityNetworkId != -1 && !TextUtils.isEmpty(highestPriorityNetworkSSID)
                            && (System.currentTimeMillis() - mSuspendNotificationTime > SUSPEND_NOTIFICATION_DURATION)) {
                            Intent intent = new Intent(WifiManager.WIFI_NOTIFICATION_ACTION);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.putExtra(WifiManager.EXTRA_NOTIFICATION_SSID, highestPriorityNetworkSSID);
                            intent.putExtra(WifiManager.EXTRA_NOTIFICATION_NETWORKID, highestPriorityNetworkId);
                            mContext.startActivity(intent);
                        }
                    }
                } else {
                    ComponentName cn = null;
                    String classname = null;
                    if (mAm.getRunningTasks(1) != null && mAm.getRunningTasks(1).get(0) != null) {
                        cn = mAm.getRunningTasks(1).get(0).topActivity;
                    }
                    if (cn != null) {
                        classname = cn.getClassName();
                        Xlog.d(TAG, "Class Name:" + classname);
                    } else {
                        Xlog.e(TAG, "ComponentName is null!");
                    }
                    if (mCellToWiFiPolicy == Settings.System.WIFI_CONNECT_TYPE_ASK &&
                        !(classname != null && classname.equals(WIFISETTINGS_CLASSNAME))) {
                        if (highestPriorityNetworkId != -1 && !TextUtils.isEmpty(highestPriorityNetworkSSID)
                            && (System.currentTimeMillis() - mSuspendNotificationTime > SUSPEND_NOTIFICATION_DURATION)) {
                            Intent intent = new Intent(WifiManager.WIFI_NOTIFICATION_ACTION);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.putExtra(WifiManager.EXTRA_NOTIFICATION_SSID, highestPriorityNetworkSSID);
                            intent.putExtra(WifiManager.EXTRA_NOTIFICATION_NETWORKID, highestPriorityNetworkId);
                            mContext.startActivity(intent);
                        }
                    }
                }
            } else {
                if (mConnectPolicy == Settings.System.WIFI_CONNECT_AP_TYPE_AUTO
                    && mCellToWiFiPolicy == Settings.System.WIFI_CONNECT_TYPE_AUTO) {
                    highestPriorityNetworkSSID = null;
                    highestPriority = -1;
                    highestPriorityNetworkId = -1;
                    if (null != networks && null != scanResults) {
                        for (WifiConfiguration network : networks) {
                            if (!disconnectNetworks.contains(network.networkId)
                                && !isDisableForFailure(network.disableReason)) {
                                for (ScanResult scanresult : scanResults) {
                                    //Xlog.d(TAG, "network.SSID=" + network.SSID
                                    //      + ", scanresult.SSID=" + scanresult.SSID);
                                    //Xlog.d(TAG, "network.security=" + getSecurity(network)
                                    //      + ", scanresult.security=" + getSecurity(scanresult));
                                    if ((network.SSID != null) && (scanresult.SSID != null)
                                        && network.SSID.equals("\"" + scanresult.SSID + "\"")
                                        && (getSecurity(network) == getSecurity(scanresult))) {
                                        if (network.priority > highestPriority) {
                                            highestPriority = network.priority;
                                            highestPriorityNetworkId = network.networkId;
                                            highestPriorityNetworkSSID = network.SSID;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Xlog.d(TAG, "Mobile isn't connected, highestPriorityNetworkId:" + highestPriorityNetworkId
                        + ", highestPriorityNetworkSSID:" + highestPriorityNetworkSSID
                        + ", highestPriority:" + highestPriority);
                    if (highestPriorityNetworkId != -1 && !TextUtils.isEmpty(highestPriorityNetworkSSID)) {
                        Xlog.d(TAG, "Enable all networks for mobile is not connected.");
                        sendEnableAllNetworksBroadcast();
                        autoConnect = true;
                    }
                }
            }
        }
        Xlog.d(TAG, "isWifiConnecting, isConnecting:" + isConnecting + ", autoConnect:" + autoConnect);
        return (isConnecting || autoConnect);
    }

    public boolean hasConnectableAp() {
        if (mWifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {
            Xlog.d(TAG, "Scan for checking connectable AP.");
            mWifiManager.startScanActive();
            return true;
        } else {
            return false;
        }
    }

    public void suspendNotification() {
        mSuspendNotificationTime = System.currentTimeMillis();
        Xlog.d(TAG, "suspendNotification, mSuspendNotificationTime:" + mSuspendNotificationTime);
    }

    public int defaultFrameworkScanIntervalMs() {
        return DEFAULT_FRAMEWORK_SCAN_INTERVAL_MS;
    }

    private void sendUpdateSettingsBroadcast() {
        Intent intent = new Intent(IWifiFwkExt.AUTOCONNECT_SETTINGS_CHANGE);
        intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT);
        mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    private void sendEnableAllNetworksBroadcast() {
        Intent intent = new Intent(IWifiFwkExt.AUTOCONNECT_ENABLE_ALL_NETWORKS);
        intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT);
        mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    private boolean isDisableForFailure(int reason) {
        return reason == WifiConfiguration.DISABLED_DNS_FAILURE
                || reason == WifiConfiguration.DISABLED_DHCP_FAILURE
                || reason == WifiConfiguration.DISABLED_AUTH_FAILURE;
    }

    public String getApDefaultSsid() {
        return mContext.getString(com.mediatek.internal.R.string.wifi_tether_configure_ssid_default_for_cmcc);
    }

    private class ConnectTypeObserver extends ContentObserver {
        private Context mMyContext;
        public ConnectTypeObserver(Handler handler, Context context) {
            super(handler);
            mMyContext = context;
            ContentResolver cr = mMyContext.getContentResolver();
            cr.registerContentObserver(Settings.System.getUriFor(
                Settings.System.WIFI_CONNECT_AP_TYPE), false, this);
            cr.registerContentObserver(Settings.System.getUriFor(
                Settings.System.WIFI_CONNECT_TYPE), false, this);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            int connectType = Settings.System.getInt(mMyContext.getContentResolver(),
                Settings.System.WIFI_CONNECT_AP_TYPE, Settings.System.WIFI_CONNECT_AP_TYPE_AUTO);
            int cellToWlan = Settings.System.getInt(mMyContext.getContentResolver(),
                Settings.System.WIFI_CONNECT_TYPE, Settings.System.WIFI_CONNECT_TYPE_AUTO);
            if (mCellToWiFiPolicy != cellToWlan
                || (mCellToWiFiPolicy == Settings.System.WIFI_CONNECT_TYPE_ASK && mConnectPolicy != connectType)) {
                mSuspendNotificationTime = 0;
            }
            mConnectPolicy = connectType;
            mCellToWiFiPolicy = cellToWlan;
            Xlog.d(TAG, "ConnectTypeObserver, mConnectPolicy:" + mConnectPolicy
                    + ", mCellToWiFiPolicy:" + mCellToWiFiPolicy);
            sendUpdateSettingsBroadcast();
        }
    }
}
