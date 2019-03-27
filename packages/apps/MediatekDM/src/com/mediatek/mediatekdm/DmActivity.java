
package com.mediatek.mediatekdm;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.mediatek.mediatekdm.DmConst.TAG;

public class DmActivity extends Activity {
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.i(TAG.COMMON, "DmActivity->onServiceConnected(), got service reference");
            mService = ((DmService.DmBinder) service).getService();
            if (mService == null) {
                Log.e(TAG.COMMON, "onServiceConnected mService is null");
                return;
            }
            DmActivity.this.onServiceConnected();
        }

        public void onServiceDisconnected(ComponentName className) {
            DmActivity.this.onServiceDisconnected();
            mService = null;
        }
    };

    public DmService mService;

    public void onServiceConnected() {
    };

    public void onServiceDisconnected() {
    };

    public boolean bindService() {
        Log.d(TAG.COMMON, "+bindService()");
        Intent intent = new Intent(this, DmService.class);
        intent.setAction(DmConst.IntentAction.ACTION_DM_SERVE);
        boolean result = bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        Log.d(TAG.COMMON, "bindService() returns " + result);
        Log.d(TAG.COMMON, "-bindService()");
        return result;
    };

    public void unbindService() {
        if (DmOperationManagerFactory.getInstance().isBusy()) {
            Log.d(TAG.COMMON, "There's still operation running, start service again.");
            Intent serviceIntent = new Intent(this, DmService.class);
            serviceIntent.setAction(DmConst.IntentAction.ONLY_START_SERVICE);
            startService(serviceIntent);
        }
        unbindService(mConnection);
    };

}
