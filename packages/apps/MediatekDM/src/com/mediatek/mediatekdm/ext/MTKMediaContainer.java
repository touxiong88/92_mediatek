
package com.mediatek.mediatekdm.ext;

import android.content.pm.PackageInfoLite;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.android.internal.app.IMediaContainerService;

public class MTKMediaContainer {
    private IMediaContainerService mImcs;

    public MTKMediaContainer(IBinder service) {
        mImcs = IMediaContainerService.Stub.asInterface(service);
    }

    public boolean isValid() {
        return mImcs != null;
    }

    public void finish() {
        mImcs = null;
    }

    public boolean checkSpace(String archive, long shreshold) {
        try {
            PackageInfoLite pkgLite = mImcs.getMinimalPackageInfo(archive, 0, shreshold);
            int loc = pkgLite.recommendedInstallLocation;
            if (loc != PackageManager.INSTALL_FAILED_INSUFFICIENT_STORAGE) {
                return true;
            }
        } catch (RemoteException e) {
            Log.e("MTKMediaContainer", "checkSpace exception", e);
        }
        return false;
    }

}
