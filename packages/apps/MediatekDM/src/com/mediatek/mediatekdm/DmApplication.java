
package com.mediatek.mediatekdm;

import android.app.Application;
import android.os.RemoteException;
import android.util.Log;

import com.mediatek.mediatekdm.DmConst.TAG;
import com.mediatek.mediatekdm.conn.DmDatabase;
import com.mediatek.mediatekdm.ext.MTKPhone;
import com.mediatek.mediatekdm.util.Utilities;

public class DmApplication extends Application {
    private static DmApplication sInstance;

    public void onCreate() {
        super.onCreate();
        sInstance = this;

        try {
            byte[] switchValue = MTKPhone.getDmAgent().getDmSwitchValue();
            if (switchValue != null && (new String(switchValue)).equals("1")) {
                Log.d(TAG.APPLICATION, "There is a pending DM flag.");
                Utilities.removeDirectoryRecursively(getFilesDir());
                MTKPhone.getDmAgent().setDmSwitchValue("0".getBytes());
                DmDatabase.clearDB(this);
                Log.d(TAG.APPLICATION, "Data folder cleared.");
            } else {
                Log.d(TAG.APPLICATION, "There is no pending DM flag.");
            }
        } catch (RemoteException e) {
            throw new Error(e);
        }
    }

    public static DmApplication getInstance() {
        return sInstance;
    }
}
