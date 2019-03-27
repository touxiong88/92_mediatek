package com.mediatek.mediatekdm.fumo;

import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;
import android.util.Log;

import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.mediatekdm.DmConst;
import com.mediatek.mediatekdm.DmConst.TAG;
import com.mediatek.mediatekdm.DmReceiver;
import com.mediatek.mediatekdm.DmService;
import com.mediatek.mediatekdm.ext.MTKPhone;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class RebootChecker extends com.mediatek.mediatekdm.RebootChecker {

    public RebootChecker(Context context) {
        super(context);
    }

    @Override
    public void run() {
        // compare boot time file & fota flag file
        FileInputStream in = null;
        try {
            final String stampFilePath = DmConst.Path.getPathInData(mContext, DmReceiver.BOOT_TIME_FILE_NAME);
            File stampFile = new File(stampFilePath);
            File fotaFile = new File(DmConst.Path.getPathInData(mContext, DmConst.Path.FOTA_FLAG_FILE));

            if (stampFile.exists() && fotaFile.exists()) {
                Log.d(TAG.RECEIVER, "Boot file & fota flag file exist.");
                // read boot time, then write into file
                in = new FileInputStream(stampFilePath);
                byte[] buf = new byte[in.available()];
                in.read(buf);
                in.close();

                in = new FileInputStream(fotaFile);
                byte[] bufFota = new byte[in.available()];
                in.read(bufFota);
                in.close();

                String timeBoot = new String(buf);
                String timeFota = new String(bufFota);

                if (timeBoot.equalsIgnoreCase(timeFota)) {
                    Log.d(TAG.RECEIVER, "Not reboot, do nothing.");
                    return;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG.RECEIVER, "Failed to touch flag file: " + e);
        } catch (ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
            Log.e(TAG.RECEIVER, "Array out of bounds " + e);
        } finally {
            try {
                if (null != in) {
                    in.close();
                    in = null;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Check whether we need to report FUMO update result
        if (isUpdateReboot()) {
            // Get the result
            boolean isUpdateSuccessful = false;
            if (FeatureOption.MTK_EMMC_SUPPORT) {
                int otaResult = 0;
                try {
                    otaResult = MTKPhone.getDmAgent().readOtaResult();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                isUpdateSuccessful = (otaResult == 1);
            } else {
                isUpdateSuccessful = FotaDeltaFiles.verifyUpdateStatus();
            }

            Intent intent = new Intent(DmConst.IntentAction.REBOOT_CHECK);
            intent.setClass(mContext, DmService.class);
            intent.putExtra("UpdateSucceeded", isUpdateSuccessful);
            mContext.startService(intent);
        }
    }

    private boolean isUpdateReboot() {
        Log.i(TAG.RECEIVER, "Check the existence of update flag file.");
        boolean ret = false;
        try {
            File updateFile = new File(DmConst.Path.getPathInData(mContext, DmConst.Path.FOTA_FLAG_FILE));
            if (updateFile.exists()) {
                Log.d(TAG.RECEIVER, "FOTA flag file exists.");
                ret = true;
            }
        } catch (SecurityException e) {
            Log.e(TAG.RECEIVER, e.toString());
            e.printStackTrace();
        }
        return ret;
    }
}
