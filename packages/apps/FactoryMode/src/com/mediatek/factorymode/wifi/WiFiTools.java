
package com.mediatek.factorymode.wifi;

import java.util.List;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.SystemProperties;
import android.util.Log;

import com.mediatek.factorymode.R;

public class WiFiTools {
    public static final int WIFI_STATE_DISABLING = 0;

    public static final int WIFI_STATE_DISABLED = 1;

    public static final int WIFI_STATE_ENABLING = 2;

    public static final int WIFI_STATE_ENABLED = 3;

    private static WifiManager mWifiManager = null;

    static boolean mResult = false;

    String info = "";

    Context context;

    WifiInfo wifiinfo;

    public final int SECURITY_NONE = 0;
    private final int SECURITY_WEP = 1;
    private final int SECURITY_PSK = 2;
    private final int SECURITY_WPA_PSK = 3;
    private final int SECURITY_WPA2_PSK = 4;
    private final int SECURITY_EAP = 5;
    private final int SECURITY_WAPI_PSK = 6;
    private final int SECURITY_WAPI_CERT = 7;

    private String mWFATestFlag = null;
    private final String KEY_PROP_WFA_TEST_VALUE = "true";
    private final String KEY_PROP_WFA_TEST_SUPPORT = "persist.radio.wifi.wpa2wpaalone";

    public WiFiTools(Context context) {
        this.context = context;
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        wifiinfo = mWifiManager.getConnectionInfo();
        mWifiManager.startScan();
    }

    public String GetState() {
        int state = -1;
        if (!mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(true);
            state = mWifiManager.getWifiState();
            switch (state) {
                case WIFI_STATE_DISABLING:
                    info = context.getString(R.string.WiFi_info_closeing);
                    break;
                case WIFI_STATE_DISABLED:
                    info = context.getString(R.string.WiFi_info_close);
                    break;
                case WIFI_STATE_ENABLING:
                    info = context.getString(R.string.WiFi_info_opening);
                    break;
                case WIFI_STATE_ENABLED:
                    info = context.getString(R.string.WiFi_info_open);
                    break;
                default:
                    info = context.getString(R.string.WiFi_info_unknown);
                    break;
            }
        } else {
            info = context.getString(R.string.WiFi_info_open);
        }
        return info;
    }

    /**
     * @author 2011-09-16
     */
    public boolean openWifi() {
        if (!mWifiManager.isWifiEnabled()) {
            boolean wifistate = mWifiManager.setWifiEnabled(true);
            return wifistate;
        } else {
            return true;
        }
    }

    public void closeWifi() {
        if (mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(false);
        }
    }

    public boolean addWifiConfig(List<ScanResult> wifiList, ScanResult srt, String pwd) {
        WifiConfiguration wc = new WifiConfiguration();
        wc.SSID = "\"" + srt.SSID + "\"";
        wc.allowedKeyManagement.set(KeyMgmt.NONE);
        wc.status = WifiConfiguration.Status.ENABLED;
        wc.networkId = mWifiManager.addNetwork(wc);
        return mWifiManager.enableNetwork(wc.networkId, true);
    }

    public List<ScanResult> scanWifi() {
        return mWifiManager.getScanResults();
    }

    public WifiInfo GetWifiInfo() {
        wifiinfo = mWifiManager.getConnectionInfo();
        return wifiinfo;
    }

    public Boolean IsConnection() {
        ConnectivityManager connec = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connec.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED) {
            return true;
        }
        return false;
    }

    public int getSecurity(ScanResult result) {
        Log.v("FactoryMode_Wifi", "the SSID is : " + result + "SECURITY is : " + result.capabilities);
        if (result.capabilities.contains("WAPI-PSK")) {
            return SECURITY_WAPI_PSK;
        } else if (result.capabilities.contains("WAPI-CERT")) {
            return SECURITY_WAPI_CERT;
        } else if (result.capabilities.contains("WEP")) {
            return SECURITY_WEP;
        } else if (result.capabilities.contains("PSK")) {
            if (isWFATestSupported()) {
                if (result.capabilities.contains("CCMP")) {
                    return SECURITY_WPA2_PSK;
                } else if (result.capabilities.contains("TKIP")) {
                    return SECURITY_WPA_PSK;
                }
            }
            return SECURITY_PSK;
        } else if (result.capabilities.contains("EAP")) {
            return SECURITY_EAP;
        }
        return SECURITY_NONE;
    }

    private boolean isWFATestSupported() {
        if (mWFATestFlag == null) {
            mWFATestFlag = SystemProperties.get(KEY_PROP_WFA_TEST_SUPPORT, "");
        }
        return KEY_PROP_WFA_TEST_VALUE.equals(mWFATestFlag);
    }
}
