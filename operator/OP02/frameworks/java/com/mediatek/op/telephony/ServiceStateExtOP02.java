package com.mediatek.op.telephony;

import android.content.Context;
import android.content.res.Resources;
import android.telephony.TelephonyManager;

import com.android.internal.telephony.PhoneConstants;

/// M: The OP02 implementation of ServiceState.
public class ServiceStateExtOP02 extends ServiceStateExt {
    public ServiceStateExtOP02() {
    }

    public ServiceStateExtOP02(Context context) {
    }

    @Override
    public String getEccPlmnValue() {
        for (int i = 0; i < PhoneConstants.GEMINI_SIM_NUM; i++) {
            if (TelephonyManager.SIM_STATE_READY == TelephonyManager.getDefault().getSimStateGemini(i)) {
                return Resources.getSystem().getText(com.android.internal.R.string.emergency_calls_only).toString();
            }
        }
        return Resources.getSystem().getText(com.android.internal.R.string.lockscreen_carrier_default).toString();
    }
}
