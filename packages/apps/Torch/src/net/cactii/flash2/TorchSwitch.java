
package net.cactii.flash2;

import java.io.File;
import java.util.List;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.provider.Settings;

public class TorchSwitch extends BroadcastReceiver {

    public static final String TOGGLE_FLASHLIGHT = "net.cactii.flash2.TOGGLE_FLASHLIGHT";
    public static final String TORCH_STATE_CHANGED = "net.cactii.flash2.TORCH_STATE_CHANGED";
    public static final String TORCH_STATE = "torch_state";

    private SharedPreferences mPrefs;

/* Vanzo:huangchaojun on: Mon, 27 Jun 2011 11:45:17 +0800
 */
    private static final String DEVICE = "/sys/bus/platform/drivers/kd_camera_flashlight/lighton";
// End of Vanzo:huangchaojun

    @Override
    public void onReceive(Context context, Intent receivingIntent) {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (receivingIntent.getAction().equals(TOGGLE_FLASHLIGHT)) {
            // bright setting can come from intent or from prefs depending on
            // on what send the broadcast
            //
            // Unload intent extras if they exist:

/* Vanzo:huangchaojun on: Mon, 27 Jun 2011 11:45:43 +0800
 */
            File deviceFile = new File(DEVICE);
            if (!deviceFile.exists()) {
                Intent intent = new Intent(context, FlashlightActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                Settings.System.putInt(context.getContentResolver(), TORCH_STATE, 0);
                return;
            }
// End of Vanzo:huangchaojun
            boolean bright = receivingIntent.getBooleanExtra("bright", false) |
                    mPrefs.getBoolean("bright", false);
            boolean strobe = receivingIntent.getBooleanExtra("strobe", false) |
                    mPrefs.getBoolean("strobe", false);
            int period = receivingIntent.getIntExtra("period", 200);
            Intent i = new Intent(context, TorchService.class);
            if (this.TorchServiceRunning(context)) {
                context.stopService(i);
            } else {
                i.putExtra("bright", bright);
                i.putExtra("strobe", strobe);
                i.putExtra("period", period);
                context.startService(i);
            }
        }
        if (receivingIntent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            Settings.System.putInt(context.getContentResolver(), TorchSwitch.TORCH_STATE, 0);
        }
    }

    private boolean TorchServiceRunning(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Activity.ACTIVITY_SERVICE);

        List<ActivityManager.RunningServiceInfo> svcList = am.getRunningServices(100);

        if (!(svcList.size() > 0))
            return false;
        for (RunningServiceInfo serviceInfo : svcList) {
            ComponentName serviceName = serviceInfo.service;
            if (serviceName.getClassName().endsWith(".TorchService")
                    || serviceName.getClassName().endsWith(".RootTorchService"))
                return true;
        }
        return false;
    }
}
