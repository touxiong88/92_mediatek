package com.mediatek.security.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.mediatek.xlog.Xlog;

public class PermControlReceiver extends BroadcastReceiver {
    private static final String TAG = "PermControlBootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Xlog.d(TAG, "action = " + action);
        if (Intent.ACTION_PACKAGE_ADDED.equals(action) ||
                   Intent.ACTION_PACKAGE_FULLY_REMOVED.equals(action)) {
            context.startService(getIntent(context,action,getPackageName(intent)));
        }
    }
    
    private Intent getIntent(Context context, String action) {
        Intent serviceIntent = new Intent(context, PermControlService.class);
        serviceIntent.setAction(action);
        return serviceIntent;
    }
    
    private Intent getIntent(Context context, String action, String pkgName) {
        Intent intent = getIntent(context,action);
        intent.putExtra(PermControlUtils.PACKAGE_NAME, pkgName);
        return intent;
    }
    
    private String getPackageName(Intent intent) {
        Uri data = intent.getData();
        String pkgName = data.getEncodedSchemeSpecificPart();
        Xlog.d(TAG,"getPackageName() pkgNAme = " + pkgName);
        return pkgName;
    }
}
