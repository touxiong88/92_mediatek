package com.android.smsregister;

import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Service;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Log;
import android.view.WindowManager;

public class TimerService extends Service {
    private int mStartId = -1;
    private MsgSender mSender;
    private Handler mHandler = new Handler();
    private boolean mCanStopService = false;
    private static final String NvRAMKey = "ro.nid.salesreg";
    public static final String START_SERVICE = "com.android.register.action.MESSAGE_SERVICE";
    public static final String RESTART_SERVICE = "com.android.intent.sms.register.RESTART_SERVICE";

    private Runnable mSendThread = new Runnable() {
        public void run() {
            Log.i(MsgSender.TAG, "mSendThread runing, current Time : " + new Date(System.currentTimeMillis()));
            if (mSender == null) {
                mSender = new MsgSender(TimerService.this);
            }
            if (!mSender.sendMsg()) {
                Log.i(MsgSender.TAG, "Fail is no SIM card.");
                timerCancelAndStop();
            }
        }
    };

    private void timerCancelAndStop() {
        Log.i(MsgSender.TAG, "Stop the register service.");
        mCanStopService = true;
        stopSelf(mStartId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mStartId = startId;
        String action = intent.getAction();
        Log.i(MsgSender.TAG, "action : " + action);
        if (action.equals(MsgSender.ACTION_MSG_SENT)) {
            handleSmsSent(intent);
        } else if (action.equals(RESTART_SERVICE)
                || action.equals(START_SERVICE)) {
            byte[] prop = readFlag();
            if (prop == null || (prop != null && prop[0] == 0 && prop[1] < 5)) {
                Log.i(MsgSender.TAG, "SMS register readyed, current Time : " + new Date(System.currentTimeMillis()));
                mHandler.postDelayed(mSendThread, 1000);
            } else {
                timerCancelAndStop();
            }
        }
        return START_STICKY;
    }

    public void handleSmsSent(Intent intent) {
        int resultCode = intent.getIntExtra("result", 0);
        byte[] prop = readFlag();
        if (prop == null) {
            prop = new byte[]{0, 0, 0, 0};
        }
        if (resultCode == Activity.RESULT_OK) {
            Log.i(MsgSender.TAG, "RESULT_OK, sent success");
            prop[0] = 1;
            timerCancelAndStop();
            if (SmsConfig.isShowDialog()) {
                AlertDialog.Builder builder = new AlertDialog.Builder(TimerService.this);
                builder.setTitle(R.string.welcome);
                builder.setMessage(R.string.prompt_text);
                builder.setNegativeButton(android.R.string.ok, new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                AlertDialog mDialog=builder.create();
                mDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                mDialog.show();
            }
        } else {
            Log.i(MsgSender.TAG, "RESULT_FAILE");
            if (prop[1] < 5) {
                Log.i(MsgSender.TAG, "1 hours after the Re-send");
            } else {
                timerCancelAndStop();
            }
        }
        prop[1] += 1;
        writeFlag(prop);
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(MsgSender.TAG, "onDestroy");
        if (!mCanStopService) {
            Intent intent = new Intent(RESTART_SERVICE);
            Log.i(MsgSender.TAG, "restart service");
            startService(intent);
        }
    }

    public void writeFlag(byte[] value) {
        SharedPreferences result = getSharedPreferences("result", 0);
        result.edit().putInt("sent", value[0]).commit();
        result.edit().putInt("fail_count", value[1]).commit();
    }

    public byte[] readFlag() {
        byte[] flag = new byte[]{0, 0, 0, 0};;
        SharedPreferences result = getSharedPreferences("result", 0);
        flag[0] = (byte) result.getInt("sent", 0);
        flag[1] = (byte) result.getInt("fail_count", 0);
        return flag;
    }
/*
    public static void writeFlag(byte[] value) {
        IBinder binder = ServiceManager.getService("NvRAMAgent");
        NvRAMAgent agent = NvRAMAgent.Stub.asInterface(binder);
        binder = ServiceManager.getService("NvRAMBackup");
        NvRAMBackup agent2 = NvRAMBackup.Stub.asInterface(binder);
        int file_lid = SystemProperties.getInt(NvRAMKey, 31);

        try {
            agent.writeFile(file_lid, value);
            agent2.saveToBin();
            Log.i(MsgSender.TAG, "write File Success, Re-check file");
            readFlag();
        } catch (RemoteException e) {
            Log.i(MsgSender.TAG, "write File Failed");
            e.printStackTrace();
        }
    }

    public static byte[] readFlag() {
        IBinder binder = ServiceManager.getService("NvRAMAgent");
        NvRAMAgent agent = NvRAMAgent.Stub.asInterface(binder);
        int file_lid = SystemProperties.getInt(NvRAMKey, 31);
        byte[] flag = null;
        Log.i(MsgSender.TAG, "file_lid = " + file_lid);

        try {
            flag = agent.readFile(file_lid);
        } catch (RemoteException e) {
            Log.i(MsgSender.TAG, "read File Failed");
            e.printStackTrace();
        }
        Log.i(MsgSender.TAG, "flag[0] = " + (flag != null ? flag[0] : "null"));
        Log.i(MsgSender.TAG, "flag[1] = " + (flag != null ? flag[1] : "null"));
        return flag;
    }
*/
}
