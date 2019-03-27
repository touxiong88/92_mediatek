package com.mediatek.op.policy;

import android.content.Context;
import android.view.View;

import com.mediatek.common.policy.IOperatorSIMString;

public class DefaultOperatorSIMString implements IOperatorSIMString {
    @Override
    public String getOperatorSIMString(String sourceStr, int slotId, SIMChangedTag simChangedTag, Context context) {
        return sourceStr;
    }

    @Override
    public String getOperatorSIMStringForSIMDetection(String sourceStr, int newSimSlot, int newSimNumber, Context context) {
        return sourceStr;
    }
}
