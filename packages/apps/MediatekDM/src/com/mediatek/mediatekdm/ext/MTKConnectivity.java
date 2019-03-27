
package com.mediatek.mediatekdm.ext;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;


public final class MTKConnectivity {
    public static final int TYPE_MOBILE_DM = ConnectivityManager.TYPE_MOBILE_DM;

    public static final String EXTRA_SIM_ID = ConnectivityManager.EXTRA_SIM_ID;

    public static int getSimId(NetworkInfo info) {
        return info.getSimId();
    }

    public static int startUsingNetworkFeature(ConnectivityManager connMgr, int type,
            String feature, int simId) {
        if (MTKOptions.MTK_GEMINI_SUPPORT) {
            return connMgr.startUsingNetworkFeatureGemini(type, feature, simId);    
        } else {
            return connMgr.startUsingNetworkFeature(type, feature);
        }
    }

    public static int stopUsingNetworkFeatureGemini(ConnectivityManager connMgr, int type,
            String feature, int simId) {
        if (MTKOptions.MTK_GEMINI_SUPPORT) {
            return connMgr.stopUsingNetworkFeatureGemini(type, feature, simId);
        } else {
            return connMgr.stopUsingNetworkFeature(type, feature);
        }
    }
}
