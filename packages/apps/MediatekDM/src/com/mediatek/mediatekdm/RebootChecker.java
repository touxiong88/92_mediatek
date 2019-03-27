package com.mediatek.mediatekdm;

import android.content.Context;

public abstract class RebootChecker implements Runnable {
    protected final Context mContext;
    
    public RebootChecker(Context context) {
        mContext = context;
    }
}
