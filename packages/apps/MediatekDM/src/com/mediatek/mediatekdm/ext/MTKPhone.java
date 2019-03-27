
package com.mediatek.mediatekdm.ext;

import android.net.Uri;
import android.os.IBinder;
import android.os.ServiceManager;
import android.provider.Telephony.Carriers;
import android.util.Log;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.mediatek.common.dm.DmAgent;
import com.mediatek.telephony.TelephonyManagerEx;

public final class MTKPhone {
    public static final int APN_ALREADY_ACTIVE = PhoneConstants.APN_ALREADY_ACTIVE;
    public static final int APN_TYPE_NOT_AVAILABLE = PhoneConstants.APN_TYPE_NOT_AVAILABLE;
    public static final int APN_REQUEST_FAILED = PhoneConstants.APN_REQUEST_FAILED;
    public static final int APN_REQUEST_STARTED = PhoneConstants.APN_REQUEST_STARTED;

    public static final String FEATURE_ENABLE_DM = Phone.FEATURE_ENABLE_DM;

    private static final Uri CONTENT_URI_DM = Carriers.CONTENT_URI_DM;
    private static final Uri CONTENT_URI_DM_GEMINI = Carriers.GeminiCarriers.CONTENT_URI_DM;

    public static Uri getDMUri(int simId) {
        if (simId < 0) {
            return null;
        }

        return sPhone.getDMUri(simId);
    }
    
    public static Uri getQureryTable(int simId) {
        String uriString = sPhone.getDmUriString(simId);
        if (uriString == null) {
            Log.d("MTKPhone", "getQureryTable(" + simId + ") returns null");
            return null;
        }
        
        Log.d("MTKPhone", "getQureryTable(" + simId + ") returns " + Uri.parse(uriString));
        return Uri.parse(uriString);
    }
    
    public static String getDmImei() {
        /* Use IMEI of one sim slot as IMEI of device. I think it's 
         * weird that one device have two IMEI. 
         */
        return TelephonyManagerEx.getDefault().getDeviceId(PhoneConstants.GEMINI_SIM_1);
    }
    
    public static DmAgent getDmAgent() {
        IBinder binder = ServiceManager.getService("DmAgent");
        if (binder == null) {
            Log.e("MTKPhone", "ServiceManager.getService(DmAgent) failed.");
            return null;
        }
        DmAgent agent = DmAgent.Stub.asInterface(binder);
        return agent;
    }
    
    /* Gemini support */
    private static MtkPhone sPhone;
    
    static {
        Log.d("MTKPhone", "MTKOptions.MTK_GEMINI_SUPPORT: " + MTKOptions.MTK_GEMINI_SUPPORT);
        if (MTKOptions.MTK_GEMINI_SUPPORT) {
            sPhone = new PhoneGemini();
        } else {
            sPhone = new PhoneNorm();
        }
    }
    
    private interface MtkPhone {
        Uri getDMUri(int slot);
        String getDmUriString(int slot);
    }

    private static class PhoneGemini implements MtkPhone {
        public Uri getDMUri(int slot) {
            if (slot > 0) {
                return CONTENT_URI_DM_GEMINI;
            } else {
                return CONTENT_URI_DM;
            }
        }
        
        public String getDmUriString(int slot) {
            String uriString = null;
            if (slot == 0) {
                uriString = "content://telephony/carriers_sim1";
            } else if (slot == 1) {
                uriString = "content://telephony/carriers_sim2";
            }
            
            return uriString;
        }
    }

    private static class PhoneNorm implements MtkPhone {
        public Uri getDMUri(int slot) {
            return CONTENT_URI_DM;
        }

        public String getDmUriString(int slot) {
            return "content://telephony/carriers";
        }
    }

}
