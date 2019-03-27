package com.mediatek.op.policy;

import android.content.Context;
import android.view.View;

import com.android.internal.R;
import com.mediatek.common.policy.IDualClock;
import com.mediatek.xlog.Xlog;

public class DefaultDualClock implements IDualClock {
    private static final String TAG = "DefaultDualClock";

    /** {@inheritDoc} */
    @Override
    public View createClockView(Context context, View container) {
        Xlog.d(TAG, "createClockView context = " + context + " container = " + container + " return null");
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public int getStatusViewLayout() {
        Xlog.d(TAG, "getStatusViewLayout layout res id = " + R.layout.keyguard_status_view);
        return R.layout.keyguard_status_view;
    }

    /** {@inheritDoc} */
    @Override
    public void resetPhonelistener() {
    }

    /** {@inheritDoc} */
    @Override
    public void updateClockLayout() {
    }
}
