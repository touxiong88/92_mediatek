
package com.mediatek.factorymode;

import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Log;

public class NvRAMAdapter {

    private static int FILE_ID = SystemProperties.getInt("ro.nid.productinfo", 25);

    private static int offset = 544;

    private static String TAG = "NvRAMAdapter";

    public static int readNvRAM() {
        IBinder binder = ServiceManager.getService("NvRAMAgent");
        NvRAMAgent agent = NvRAMAgent.Stub.asInterface(binder);
        binder = ServiceManager.getService("NvRAMBackup");
        byte[] readBuff = null;
        try {
            readBuff = agent.readFile(FILE_ID);
            Log.e(TAG, "readBuff size is >>> " + readBuff.length);
        } catch (RemoteException e) {
            Log.e(TAG, "readbuff can't read...");
        }
        byte[] data = new byte[8];
        for (int i = 0; i < data.length; i++) {
            data[i] = readBuff[offset + i];
            Log.e(TAG, "readbuff[i] = " + data[i]);
        }
        return data[0];
    }

    public static void writeNvRam(int resultFlag) {
        IBinder binder = ServiceManager.getService("NvRAMAgent");
        NvRAMAgent agent = NvRAMAgent.Stub.asInterface(binder);
        binder = ServiceManager.getService("NvRAMBackup");
        byte[] readBuff = null;
        try {
            readBuff = agent.readFile(FILE_ID);
            Log.e(TAG, "readBuff size is >>> " + readBuff.length);
        } catch (RemoteException e) {
            Log.e(TAG, "readbuff can't read...");
        }
        readBuff[offset] = (byte) resultFlag;
        try {
            int flag = agent.writeFile(FILE_ID, readBuff);
            Log.e(TAG, ".... Write IMEI ....");
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
