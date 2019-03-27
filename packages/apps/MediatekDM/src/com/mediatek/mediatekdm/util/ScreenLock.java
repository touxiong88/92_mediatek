
package com.mediatek.mediatekdm.util;

import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.Context;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

@SuppressWarnings("deprecation")
public class ScreenLock {
    private static WakeLock sFullWakelock = null;
    private static WakeLock sPartialWakelock = null;
    private static KeyguardLock sKeyguardLock = null;
    private static final String TAG = "DM/ScreenLock";

    public static void acquirePartialWakelock(Context context) {
        // get a PowerManager instance
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (sPartialWakelock == null) {
            // get WakeLock
            sPartialWakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "dm_PartialLock");
            if (!sPartialWakelock.isHeld()) {
                Log.d(TAG, "need to aquire partial wake up");
                // wake lock
                sPartialWakelock.acquire();
            } else {
                sPartialWakelock = null;
                Log.d(TAG, "not need to aquire partial wake up");
            }
        }
    }

    public static void acquireFullWakelock(Context context) {
        // get a PowerManager instance
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

        if (sFullWakelock == null) {
            // get WakeLock
            sFullWakelock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK
                    | PowerManager.ACQUIRE_CAUSES_WAKEUP
                    | PowerManager.ON_AFTER_RELEASE, "dm_FullLock");
            if (!sFullWakelock.isHeld()) {
                Log.d(TAG, "need to aquire full wake up");
                // wake lock
                sFullWakelock.acquire();
            } else {
                sFullWakelock = null;
                Log.d(TAG, "not need to aquire full wake up");
            }
        }
    }

    public static void disableKeyguard(Context context) {
        // get a KeyguardManager instance
        KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);

        if (sKeyguardLock == null) {
            // get KeyguardLock
            sKeyguardLock = km.newKeyguardLock("dm_KL");
            if (km.inKeyguardRestrictedInputMode()) {
                Log.d(TAG, "need to disableKeyguard");
                // release key guard lock
                sKeyguardLock.disableKeyguard();
            } else {
                sKeyguardLock = null;
                Log.d(TAG, "not need to disableKeyguard");
            }
        }
    }

    public static void releaseFullWakeLock(Context context) {
        if (sFullWakelock != null) {
            if (sFullWakelock.isHeld()) {
                sFullWakelock.release();
                sFullWakelock = null;
                Log.d(TAG, "releaseFullWakeLock release");
            } else {
                Log.d(TAG, "releaseFullWakeLock mWakelock.isHeld() == false");
            }
        } else {
            Log.d(TAG, "releaseFullWakeLock mWakelock == null");
        }
    }

    public static void releasePartialWakeLock(Context context) {
        if (sPartialWakelock != null) {
            if (sPartialWakelock.isHeld()) {
                sPartialWakelock.release();
                sPartialWakelock = null;
                Log.d(TAG, "releasePartialWakeLock release");
            } else {
                Log.d(TAG, "releasePartialWakeLock mWakelock.isHeld() == false");
            }
        } else {
            Log.d(TAG, "releasePartialWakeLock mWakelock == null");
        }
    }

    public static void enableKeyguard(Context context) {
        if (sKeyguardLock != null) {
            sKeyguardLock.reenableKeyguard();
            sKeyguardLock = null;
            Log.d(TAG, "enableKeyguard reenableKeyguard");
        } else {
            Log.d(TAG, "enableKeyguard sKeyguardLock == null");
        }
    }

    public static void releaseWakeLock(Context context) {
        releasePartialWakeLock(context);
        releaseFullWakeLock(context);
    }

}
