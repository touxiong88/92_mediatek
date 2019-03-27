package com.mediatek.op.telephony;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.gemini.GeminiPhone;

import com.mediatek.common.telephony.IGsmDCTExt;


public class GsmDCTExtOP01 extends GsmDCTExt {
    private Context mContext;

    public GsmDCTExtOP01(Context context) {
    }

    public boolean isDataAllowedAsOff(String apnType) {
        if (TextUtils.equals(apnType, PhoneConstants.APN_TYPE_MMS)) {
            return true;
        }
        return false;
    }
    
    public boolean getFDForceFlag(boolean force_flag) {
        return force_flag;    	
    }    

    private int getTheOnlyInsertedSimId() {
        int simSlotId = -1;

        GeminiPhone phone = (GeminiPhone)PhoneFactory.getDefaultPhone();
        int count = 0;
        for (int i = 0; i < PhoneConstants.GEMINI_SIM_NUM; i++) {
            if (phone.isSimInsert(i)) {
                ++count;
                simSlotId = i;
            }
        }
        
        if (count != 1) {
            simSlotId = -1;
        }

        Log.d("GsmDCTExt", "getTheOnlyInsertedSimId:" + simSlotId);
        return simSlotId;
}
    
    public int getPsAttachSimWhenRadioOn() {
        return getTheOnlyInsertedSimId();
    }
    
    public boolean isPsDetachWhenAllPdpDeactivated() {
        return (getTheOnlyInsertedSimId() == -1);
    }
}

