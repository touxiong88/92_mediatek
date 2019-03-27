
package com.mediatek.mediatekdm.iohandler.setting;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Log;

import com.mediatek.mediatekdm.DmConst.TAG;
import com.mediatek.mediatekdm.DmService;
import com.mediatek.mediatekdm.mdm.MdmException;
import com.mediatek.mediatekdm.mdm.NodeIoHandler;

public class DmMailNodeIoHandler implements NodeIoHandler {

    protected Context mContext = null;
    protected Uri mUri = null;
    protected String mRecordToWrite = null;
    protected String mMccmnc = null;
    private boolean mIsDataReady = true;
    private final Object mLock = new Object();
    private final IntentFilter mFilter = new IntentFilter("android.intent.action.PUSHMAIL_PROFILE");
    private PushMailReceiver mReceiver = new PushMailReceiver();

    private static final String[] ITEM = {
            "apn", "smtp_server", "smtp_port", "smtp_ssl", "pop3_server", "pop3_port", "pop3_ssl",
            "recv_protocol"
    };

    static String[] sContent = new String[8];
    static String[] sSetArr = new String[8];

    public DmMailNodeIoHandler(Context ctx, Uri treeUri, String mccMnc) {
        Log.i(TAG.NODEIOHANDLER, "Mail constructed");

        mContext = ctx;
        mUri = treeUri;
        mMccmnc = mccMnc;
    }

    public int read(int arg0, byte[] arg1) throws MdmException {
        String recordToRead = null;
        String uriPath = mUri.getPath();
        Log.i(TAG.NODEIOHANDLER, "mUri: " + uriPath);
        Log.i(TAG.NODEIOHANDLER, "arg0: " + arg0);

        if (DmService.sCCStoredParams.containsKey(uriPath)) {
            recordToRead = DmService.sCCStoredParams.get(uriPath);
            Log.d(TAG.NODEIOHANDLER, "get valueToRead from mCCStoredParams, the value is "
                    + recordToRead);
        } else {
            recordToRead = new String();
            for (int i = 0; i < getItem().length; i++) {
                if (mUri.getPath().contains(getItem()[i])) {
                    if (sContent[i] != null) {
                        recordToRead = sContent[i];
                    } else {
                        HandlerThread thread = new HandlerThread("pushmail");
                        thread.start();
                        mContext.registerReceiver(mReceiver, mFilter, null,
                                new Handler(thread.getLooper()));
                        Intent intent = new Intent();
                        intent.setAction("android.intent.action.PUSHMAIL_GET_PROFILE");
                        mContext.sendBroadcast(intent);
                        Log.i(TAG.NODEIOHANDLER,
                                "[MailNode] send broadcast intent 'PUSHMAIL_GET_PROFILE' ====>");

                        Log.i(TAG.NODEIOHANDLER,
                                "[MailNode] blocking here to wait for intent 'PUSHMAIL_PROFILE'...");
                        synchronized (mLock) {
                            while (mIsDataReady) {
                                try {
                                    // FIXME!!!
                                    // CC procedure will hang if the intent has
                                    // no response.
                                    // Do we need set a timeout here?
                                    mLock.wait();
                                    Log.i(TAG.NODEIOHANDLER,
                                            "[MailNode] skip waiting when got intent back");
                                    break;
                                } catch (InterruptedException e) {
                                    Log.e(TAG.NODEIOHANDLER, "[MailNode] waiting interrupted.");
                                }
                            }
                        }
                        mIsDataReady = true;
                        recordToRead = sContent[i];
                        mContext.unregisterReceiver(mReceiver);
                    }
                }
            }
            DmService.sCCStoredParams.put(uriPath, recordToRead);
            Log.d(TAG.NODEIOHANDLER, "put valueToRead to mCCStoredParams, the value is "
                    + recordToRead);
        }
        if (TextUtils.isEmpty(recordToRead)) {
            return 0;
        } else {
            byte[] temp = recordToRead.getBytes();
            if (arg1 == null) {
                return temp.length;
            }
            int numberRead = 0;
            for (; numberRead < arg1.length - arg0; numberRead++) {
                if (numberRead < temp.length) {
                    arg1[numberRead] = temp[arg0 + numberRead];
                } else {
                    break;
                }
            }
            if (numberRead < arg1.length - arg0) {
                recordToRead = null;
            } else if (numberRead < temp.length) {
                recordToRead = recordToRead.substring(arg1.length - arg0);
            }
            return numberRead;
        }
    }

    public void write(int arg0, byte[] arg1, int arg2) throws MdmException {

        Log.i(TAG.NODEIOHANDLER, "mUri: " + mUri.getPath());
        Log.i(TAG.NODEIOHANDLER, "arg1: " + new String(arg1));
        Log.i(TAG.NODEIOHANDLER, "arg0: " + arg0);
        Log.i(TAG.NODEIOHANDLER, "arg2: " + arg2);

        if (mRecordToWrite == null) {
            mRecordToWrite = new String();
        }
        mRecordToWrite += new String(arg1);
        if (mRecordToWrite.length() == arg2) {
            for (int i = 0; i < getItem().length; i++) {
                if (mUri.getPath().contains(getItem()[i])) {
                    sSetArr[i] = mRecordToWrite;

                    boolean needToBroadcast = true;
                    for (String s : sSetArr) {
                        if (s == null) {
                            needToBroadcast = false;
                        }
                    }

                    if (needToBroadcast) {

                        Intent intent = new Intent();
                        intent.setAction("android.intent.action.PUSHMAIL_SET_PROFILE");
                        for (int k = 0; k < sSetArr.length; k++) {
                            intent.putExtra(getItem()[k], sSetArr[k]);
                            Log.i(TAG.NODEIOHANDLER, getItem()[k] + ": " + sSetArr[k]);
                        }

                        mContext.sendBroadcast(intent);
                        mRecordToWrite = null;
                        for (int j = 0; j < sContent.length; j++) {
                            sContent[j] = null;
                            sSetArr[j] = null;
                        }
                    }
                    break;
                }
            }
        }
    }

    protected String[] getItem() {
        return ITEM;
    };

    class PushMailReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String intentAction = intent.getAction();
            if (intentAction.equals("android.intent.action.PUSHMAIL_PROFILE")) {
                Log.i(TAG.NODEIOHANDLER,
                        "[MailNode] received broadcast intent 'PUSHMAIL_PROFILE' <====");
                for (int i = 0; i < sContent.length; i++) {
                    sContent[i] = intent.getStringExtra(ITEM[i]);
                    Log.i(TAG.NODEIOHANDLER, ITEM[i] + ":" + sContent[i]);
                }
                mIsDataReady = true;
                synchronized (mLock) {
                    mLock.notify();
                    Log.i(TAG.NODEIOHANDLER, "[MailNode] notifying the wait lock.");
                }
            }
        }

    }
}
