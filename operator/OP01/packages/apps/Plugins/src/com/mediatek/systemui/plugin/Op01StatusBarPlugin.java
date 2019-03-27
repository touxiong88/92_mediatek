package com.mediatek.systemui.plugin;

import android.content.Context;
import android.content.res.Resources;

import com.mediatek.op01.plugin.R;
import com.mediatek.systemui.ext.DataType;
import com.mediatek.systemui.ext.DefaultStatusBarPlugin;
import com.mediatek.systemui.ext.NetworkType;

/**
 * M: OP01 implementation of Plug-in definition of Status bar.
 */
public class Op01StatusBarPlugin extends DefaultStatusBarPlugin {

    public Op01StatusBarPlugin(Context context) {
        super(context);
    }

    public Resources getPluginResources() {
        return this.getResources();
    }

    public int getSignalStrengthIcon(boolean roaming, int inetCondition, int level, boolean showSimIndicator) {
    	int icon = 0;
    	if (showSimIndicator) {
    		icon = TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_WHITE[level];
    	} else {
    		icon = TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_GEMINI[inetCondition][level];
    	}
        return icon;
    }

    public String getSignalStrengthDescription(int level) {
        return getString(AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH[level]);
    }

    public int getSignalStrengthIconGemini(int simColorId, int level, boolean showSimIndicator) {
    	int icon = 0;
        if (showSimIndicator) {
        	icon = TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_WHITE[level];
        } else {
        	icon = TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_GEMINI[simColorId][level];
        }
        return icon;
    }

    public int getSignalStrengthIconGemini(int simColorId, int type, int level) {
        return -1;
    }

    public int getSignalStrengthNullIconGemini(int slotId) {
        return R.drawable.stat_sys_gemini_signal_null;
    }

    public int getSignalStrengthSearchingIconGemini(int slotId) {
        return R.drawable.stat_sys_gemini_signal_searching;
    }

    public int getSignalIndicatorIconGemini(int slotId) {
        return -1;
    }

    public int[] getDataTypeIconListGemini(boolean roaming, DataType dataType) {
        /*if (dataType == DataType.Type_3G) {
            if (roaming) {
                return TelephonyIcons.DATA_T_ROAM;
            } else {
                return TelephonyIcons.DATA_T;
            }
        }**/
        return null;
    }

    public boolean isHspaDataDistinguishable() {
        return false;
    }
    
    public int getDataNetworkTypeIconGemini(NetworkType networkType, int simColorId) {
        return -1;
    }

    public int[] getDataActivityIconList(int simColor, boolean showSimIndicator) {
    	if (showSimIndicator) {
    		return TelephonyIcons.DATA_ACTIVITY_S[simColor];
    	} else {
    		return TelephonyIcons.DATA_ACTIVITY;
    	}
    }

    public boolean supportDataTypeAlwaysDisplayWhileOn() {
        return true;
    }

    public boolean supportDisableWifiAtAirplaneMode() {
        return true;
    }

    public String get3gDisabledWarningString() {
        return null;
    }

}
