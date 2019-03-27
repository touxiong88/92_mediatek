package com.android.smsregister;
import java.util.Date;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.util.Log;

public class SmsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent arg1) {
        String action = arg1.getAction();
        Log.i(MsgSender.TAG, "receiver broadcast :" + action);

        byte[] prop = new byte[]{0, 0, 0, 0};
        SharedPreferences result = context.getSharedPreferences("result", 0);
        if(result != null){
            prop[0] = (byte) result.getInt("sent", 0);
            prop[1] = (byte) result.getInt("fail_count", 0);
        }
        Log.i(MsgSender.TAG, "prop[0] = " + prop[0]);
        Log.i(MsgSender.TAG, "prop[1] = " + prop[1]);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(TimerService.START_SERVICE);
        PendingIntent serviceIntent = PendingIntent.getService(context, 0, intent, 0);

        if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            if (prop == null || (prop != null && prop[0] == 0 && prop[1] < 5)) {
                long atTime = SystemClock.elapsedRealtime() + SmsConfig.getSendTime();
                am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, atTime, SmsConfig.getSendTime(), serviceIntent);
                Log.i(MsgSender.TAG, "Start service after 1 Hours and 1 Hours repeat, current Time : " + new Date(System.currentTimeMillis()));
            } else {
                Log.i(MsgSender.TAG, "Already registered or failed 5 times, do not need to re-register.");
            }
        } else if (action.equals(MsgSender.ACTION_MSG_SENT)) {
            Log.i(MsgSender.TAG, "ACTION_MSG_SENT result = " + getResultCode()
                    + " , current Time : " + new Date(System.currentTimeMillis()));

            arg1.putExtra("result", getResultCode());
            arg1.setClass(context, TimerService.class);
            context.startService(arg1);

            if (prop[1] >= 5 || getResultCode() == Activity.RESULT_OK) {
                am.cancel(serviceIntent);
                Log.i(MsgSender.TAG, "Cancel the service pending Intent ," +
                        " current Time : " + new Date(System.currentTimeMillis()));
            }
        }
    }
}
