package com.mediatek.mediatekdm.util;

import android.content.Context;
import android.os.Environment;
import android.os.Message;
import android.os.RemoteException;
import android.os.StatFs;
import android.util.Log;

import com.mediatek.common.dm.DmAgent;
import com.mediatek.mediatekdm.DmConst;
import com.mediatek.mediatekdm.DmConst.TAG;
import com.mediatek.mediatekdm.DmFeatureSwitch;
import com.mediatek.mediatekdm.ext.MTKPhone;
import com.mediatek.mediatekdm.xml.DmXMLParser;
import com.mediatek.telephony.SimInfoManager;
import com.mediatek.telephony.SimInfoManager.SimInfoRecord;
import com.mediatek.telephony.TelephonyManagerEx;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.List;

public class Utilities {
    private static final long MIN_STORAGE_NEEDED = 1 * 1024 * 1024; // 1MB

    /**
     * Dump data in hex format to android log.
     * 
     * @param data
     */
    public static void hexdump(byte[] data) {
        final int rowBytes = 16;
        final int rowQtr1 = 3;
        final int rowHalf = 7;
        final int rowQtr2 = 11;
        int rows;
        int residue;
        int i;
        int j;
        byte[] saveBuf = new byte[rowBytes + 2];
        char[] hexBuf = new char[4];
        char[] idxBuf = new char[8];
        char[] hexChars = new char[20];

        hexChars[0] = '0';
        hexChars[1] = '1';
        hexChars[2] = '2';
        hexChars[3] = '3';
        hexChars[4] = '4';
        hexChars[5] = '5';
        hexChars[6] = '6';
        hexChars[7] = '7';
        hexChars[8] = '8';
        hexChars[9] = '9';
        hexChars[10] = 'A';
        hexChars[11] = 'B';
        hexChars[12] = 'C';
        hexChars[13] = 'D';
        hexChars[14] = 'E';
        hexChars[15] = 'F';

        rows = data.length >> 4;
        residue = data.length & 0x0000000F;
        for (i = 0; i < rows; i++) {
            StringBuilder sb = new StringBuilder();
            int hexVal = (i * rowBytes);
            idxBuf[0] = hexChars[((hexVal >> 12) & 15)];
            idxBuf[1] = hexChars[((hexVal >> 8) & 15)];
            idxBuf[2] = hexChars[((hexVal >> 4) & 15)];
            idxBuf[3] = hexChars[(hexVal & 15)];

            String idxStr = new String(idxBuf, 0, 4);
            sb.append(idxStr + ": ");

            for (j = 0; j < rowBytes; j++) {
                saveBuf[j] = data[(i * rowBytes) + j];

                hexBuf[0] = hexChars[(saveBuf[j] >> 4) & 0x0F];
                hexBuf[1] = hexChars[saveBuf[j] & 0x0F];

                sb.append(hexBuf[0]);
                sb.append(hexBuf[1]);
                sb.append(' ');

                if (j == rowQtr1 || j == rowHalf || j == rowQtr2) {
                    sb.append(" ");
                }

                if (saveBuf[j] < 0x20 || saveBuf[j] > 0x7E) {
                    saveBuf[j] = (byte) '.';
                }
            }

            String saveStr = new String(saveBuf, 0, j);
            sb.append(" | " + saveStr + " |");
            Log.d(TAG.COMMON, sb.toString());
        }

        if (residue > 0) {
            StringBuilder sb = new StringBuilder();
            int hexVal = (i * rowBytes);
            idxBuf[0] = hexChars[((hexVal >> 12) & 15)];
            idxBuf[1] = hexChars[((hexVal >> 8) & 15)];
            idxBuf[2] = hexChars[((hexVal >> 4) & 15)];
            idxBuf[3] = hexChars[(hexVal & 15)];

            String idxStr = new String(idxBuf, 0, 4);
            sb.append(idxStr + ": ");

            for (j = 0; j < residue; j++) {
                saveBuf[j] = data[(i * rowBytes) + j];

                hexBuf[0] = hexChars[(saveBuf[j] >> 4) & 0x0F];
                hexBuf[1] = hexChars[saveBuf[j] & 0x0F];

                sb.append((char) hexBuf[0]);
                sb.append((char) hexBuf[1]);
                sb.append(' ');

                if (j == rowQtr1 || j == rowHalf || j == rowQtr2) {
                    sb.append(" ");
                }

                if (saveBuf[j] < 0x20 || saveBuf[j] > 0x7E) {
                    saveBuf[j] = (byte) '.';
                }
            }

            for ( /* j INHERITED */; j < rowBytes; j++) {
                saveBuf[j] = (byte) ' ';
                sb.append("   ");
                if (j == rowQtr1 || j == rowHalf || j == rowQtr2) {
                    sb.append(" ");
                }
            }

            String saveStr = new String(saveBuf, 0, j);
            sb.append(" | " + saveStr + " |");
            // output
            Log.d(TAG.COMMON, sb.toString());
        }
    }

    /**
     * Decode NIA message and return the UI mode.
     * 
     * @param msg
     * @return
     */
    public static int extractUIModeFromNIA(byte[] msg) {
        Log.d(TAG.COMMON, "+extractUIModeFromNIA()");
        int uiMode = -1;
        if (msg == null || msg.length <= 0) {
            Log.d(TAG.COMMON, "-extractUIModeFromNIA()");
            return uiMode;
        }
        ByteBuffer buffer = ByteBuffer.wrap(msg);
        if (buffer == null) {
            Log.d(TAG.COMMON, "-extractUIModeFromNIA()");
            return uiMode;
        }
        buffer.getDouble();
        buffer.getDouble();
        buffer.get();// skip one byte
        byte b2 = buffer.get();
        uiMode = ((b2 << 2) >>> 6) & 3;
        Log.d(TAG.COMMON, "-extractUIModeFromNIA()");
        return uiMode;
    }

    /**
     * For the messages which will be used for FUMO or SCOMO listeners and will never be sent to handlers, please use this
     * method instead.
     * 
     * @param what
     * @param arg1
     * @param arg2
     * @param obj
     * @return
     */
    public static Message obtainMessage(int what, int arg1, int arg2, Object obj) {
        Message msg = new Message();
        msg.what = what;
        msg.arg1 = arg1;
        msg.arg2 = arg2;
        msg.obj = obj;
        return msg;
    }

    /**
     * For the messages which will be used for FUMO or SCOMO listeners and will never be sent to handlers, please use this
     * method instead.
     * 
     * @param what
     * @return
     */
    public static Message obtainMessage(int what) {
        Message msg = new Message();
        msg.what = what;
        return msg;
    }

    /**
     * For the messages which will be used for FUMO or SCOMO listeners and will never be sent to handlers, please use this
     * method instead.
     * 
     * @param what
     * @param obj
     * @return
     */
    public static Message obtainMessage(int what, Object obj) {
        Message msg = new Message();
        msg.what = what;
        msg.obj = obj;
        return msg;
    }

    public static String getOperatorName() {
        String opName = null;
        File configFileInSystem = new File(DmConst.Path.getPathInSystem(DmConst.Path.DM_CONFIG_FILE));
        if (configFileInSystem.exists()) {
            DmXMLParser xmlParser = new DmXMLParser(configFileInSystem.getAbsolutePath());
            opName = xmlParser.getValByTagName("op");
            Log.i(TAG.COMMON, "operator = " + opName);
        }
        return opName;
    }

    /**
     * getRegisteredSimId
     * 
     * @param context
     * @return: -1 -- no sim registered 0 -- Device support one sim and is registered other -- registered sim ID.
     */
    public static int getRegisteredSimId(Context context) {
        String registerIMSI = null;
        TelephonyManagerEx telMgr = TelephonyManagerEx.getDefault();
        if (telMgr == null) {
            Log.e(TAG.COMMON, "Get TelephonyManager failed.");
            return -1;
        }

        try {
            DmAgent agent = MTKPhone.getDmAgent();
            if (agent == null) {
                Log.e(TAG.COMMON, "get dm_agent_binder failed.");
                return -1;
            }
            byte[] imsiData = agent.readImsi();
            if (imsiData == null) {
                Log.e(TAG.COMMON, "get imsi failed.");
                return -1;
            }
            registerIMSI = new String(imsiData);
        } catch (RemoteException e) {
            Log.e(TAG.COMMON, "get registered IMSI failed", e);
        }

        if (registerIMSI == null) {
            Log.e(TAG.COMMON, "get registered IMSI failed");
            return -1;
        }

        Log.i(TAG.COMMON, "registered imsi=" + registerIMSI);

        List<SimInfoRecord> simList = SimInfoManager.getInsertedSimInfoList(context);
        for (SimInfoRecord sim : simList) {
            String imsi;
            imsi = telMgr.getSubscriberId(sim.mSimSlotId);
            Log.d(TAG.COMMON, "Sim" + sim.mSimSlotId + ":imsi=" + imsi);
            if (imsi != null && imsi.equals(registerIMSI)) {
                Log.d(TAG.COMMON, "registered SIM card is SIM" + sim.mSimSlotId);
                return sim.mSimSlotId;
            }
        }

        Log.d(TAG.COMMON, "getRegisteredSimId error!");
        return -1;
    }

    public static boolean isLawmoLocked() {
        boolean isLocked = false;
        if (DmFeatureSwitch.DM_LAWMO) {
            try {
                isLocked = MTKPhone.getDmAgent().isLockFlagSet();
            } catch (RemoteException e) {
                Log.w(TAG.COMMON, "RemoteException when check is lawmo lock, think as unlock");
            }
        }
        Log.i(TAG.COMMON, "isLawmoLocked : " + isLocked);
        return isLocked;
    }

    public static long getAvailableInternalMemorySize() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long availableBlock = stat.getAvailableBlocks();
        // Reserve MIN_STORAGE_NEEDED for minimum requirements of DM application
        long availableSize = (long) (availableBlock * blockSize) - MIN_STORAGE_NEEDED;
        return availableSize;
    }

    public static void removeDirectoryRecursively(File directory) {
        Log.w(TAG.SERVICE, "+removeDirectoryRecursively(" + directory + ")");
        File[] children = directory.listFiles();
        for (File child : children) {
            if (child.isFile()) {
                Log.d(TAG.SERVICE, "Remove file " + child);
                child.delete();
            } else if (child.isDirectory()) {
                removeDirectoryRecursively(child);
                child.delete();
            }
        }
        Log.w(TAG.SERVICE, "-removeDirectoryRecursively()");
    }
}
