
package net.cactii.flash2;

import java.util.List;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

public class MainActivity extends Activity {

    private TorchWidgetProvider mWidgetProvider;

    // On button
    private ToggleButton buttonOn;

    // Strobe toggle
    private CheckBox buttonStrobe;

    private CheckBox buttonBright;

    private boolean bright;

    private boolean mTorchOn;

    // Strobe frequency slider.
    private SeekBar slider;

    // Period of strobe, in milliseconds
    private int strobeperiod;

    private Context context;

    // Label showing strobe frequency
    private TextView strobeLabel;

    // Preferences
    private SharedPreferences mPrefs;

    private SharedPreferences.Editor mPrefsEditor = null;

    // Labels
    private String labelOn = null;
    private String labelOff = null;
/* Vanzo:hexiongjievanzo on: Tue, 13 Sep 2011 15:54:53 +0800
 * the program need a global variable
 */
    View mMyback;
// End of Vanzo: hexiongjievanzo
    private static boolean useBrightSetting = !Build.DEVICE.equals("crespo");

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
/* Vanzo:hexiongjievanzo on: Fri, 19 Aug 2011 16:05:51 +0800
 * The torch wanna to full the screen
 */
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
// End of Vanzo: hexiongjievanzo
        setContentView(R.layout.mainnew);

/* Vanzo:hexiongjievanzo on: Fri, 19 Aug 2011 16:05:51 +0800
 * The torch wanna to full the screen
 */
        mMyback = findViewById(R.id.mainback);
// End of Vanzo: hexiongjievanzo

        context = this.getApplicationContext();
        buttonOn = (ToggleButton) findViewById(R.id.buttonOn);

/* Vanzo:hexiongjievanzo on: Fri, 19 Aug 2011 16:05:51 +0800
 * not necessery
        buttonStrobe = (CheckBox) findViewById(R.id.strobe);
        strobeLabel = (TextView) findViewById(R.id.strobeTimeLabel);
        slider = (SeekBar) findViewById(R.id.slider);
        buttonBright = (CheckBox) findViewById(R.id.bright);
 */
// End of Vanzo: hexiongjievanzo

        strobeperiod = 100;
        mTorchOn = false;

        labelOn = this.getString(R.string.label_on);
        labelOff = this.getString(R.string.label_off);

        mWidgetProvider = TorchWidgetProvider.getInstance();

        // Preferences
        this.mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        // preferenceEditor
        this.mPrefsEditor = this.mPrefs.edit();

/* Vanzo:hexiongjievanzo on: Fri, 19 Aug 2011 16:08:35 +0800
 * not necessery
        if (useBrightSetting) {
            bright = this.mPrefs.getBoolean("bright", false);
            buttonBright.setChecked(bright);
            buttonBright.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked && mPrefs.getBoolean("bright", false))
                        MainActivity.this.bright = true;
                    else if (isChecked)
                        openBrightDialog();
                    else {
                        bright = false;
                        mPrefsEditor.putBoolean("bright", false);
                        mPrefsEditor.commit();
                    }
                }
            });
        } else {
            buttonBright.setEnabled(false);
        }
        strobeLabel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonStrobe.setChecked(!buttonStrobe.isChecked());
            }
        });
 */
// End of Vanzo: hexiongjievanzo

        buttonOn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TorchSwitch.TOGGLE_FLASHLIGHT);
/* Vanzo:hexiongjievanzo on: Fri, 19 Aug 2011 16:08:35 +0800
 * not necessery
                intent.putExtra("strobe", buttonStrobe.isChecked());
                intent.putExtra("period", strobeperiod);
 */
// End of Vanzo: hexiongjievanzo
                intent.putExtra("bright", bright);
                context.sendBroadcast(intent);
/* Vanzo:hexiongjievanzo on: Fri, 19 Aug 2011 16:08:35 +0800
 * called when the touch state changed
 */
                mMyback.setBackgroundResource(mTorchOn ? R.drawable.bulb_off : R.drawable.bulb_on);
                mTorchOn = !mTorchOn;
// End of Vanzo: hexiongjievanzo
            }
        });


        // Strobe frequency slider bar handling
/* Vanzo:hexiongjievanzo on: Fri, 19 Aug 2011 16:10:23 +0800
 * not necessery
        setProgressBarVisibility(true);
        slider.setHorizontalScrollBarEnabled(true);
        slider.setProgress(400 - this.mPrefs.getInt("strobeperiod", 100));
        strobeperiod = this.mPrefs.getInt("strobeperiod", 100);
        final String strStrobeLabel = this.getString(R.string.setting_frequency_title);
        strobeLabel.setText(strStrobeLabel + ": " +
                666 / strobeperiod + "Hz / " + 40000 / strobeperiod + "BPM");
        slider.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                strobeperiod = 401 - progress;
                if (strobeperiod < 20)
                    strobeperiod = 20;

                strobeLabel.setText(strStrobeLabel + ": " +
                        666 / strobeperiod + "Hz / " + 40000 / strobeperiod + "BPM");

                Intent intent = new Intent("net.cactii.flash2.SET_STROBE");
                intent.putExtra("period", strobeperiod);
                sendBroadcast(intent);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

        });
 */
// End of Vanzo: hexiongjievanzo
        registerReceiver(mShutDownReceiver, new IntentFilter(Intent.ACTION_SHUTDOWN));
/* Vanzo:huangchaojun on: Fri, 19 Aug 2011 12:37:09 +0800
        // Show the about dialog, the first time the user runs the app.
        if (!this.mPrefs.getBoolean("aboutSeen", false)) {
            this.openAboutDialog();
            this.mPrefsEditor.putBoolean("aboutSeen", true);
        }

 */
// End of Vanzo:huangchaojun
    }

    public void onPause() {
        this.mPrefsEditor.putInt("strobeperiod", this.strobeperiod);
        this.mPrefsEditor.commit();
        this.updateWidget();
        super.onPause();
    }

    public void onDestroy() {
        this.updateWidget();
/* Vanzo:wangyi on: Wed, 09 Jan 2013 10:45:42 +0800
        unregisterReceiver(mShutDownReceiver);
 */
        if (null != mShutDownReceiver) {
            unregisterReceiver(mShutDownReceiver);
        }
// End of Vanzo: wangyi
        super.onDestroy();
    }

    public void onResume() {
/* Vanzo:wangyi on: Fri, 19 Apr 2013 16:20:26 +0800
 * bugfix #34240
 */
        if ((!torchServiceRunning(context))
                && (Settings.System
                        .getInt(context.getContentResolver(), TorchSwitch.TORCH_STATE, 0) == 1)) {
            Settings.System.putInt(context.getContentResolver(), TorchSwitch.TORCH_STATE, 0);
        }
// End of Vanzo: wangyi
/* Vanzo:hexiongjievanzo on: Tue, 13 Sep 2011 15:56:02 +0800
 * ensure the right state when resume
 */
        updateBigButtonState();
        mMyback.setBackgroundResource(mTorchOn ? R.drawable.bulb_on : R.drawable.bulb_off);
        buttonOn.setChecked(mTorchOn);
// End of Vanzo: hexiongjievanzo
        updateBigButtonState();
        this.updateWidget();
        super.onResume();
    }

/* Vanzo:huangchaojun on: Fri, 19 Aug 2011 12:35:46 +0800
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean supRetVal = super.onCreateOptionsMenu(menu);
        menu.addSubMenu(0, 0, 0, this.getString(R.string.about_btn));
        return supRetVal;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        boolean supRetVal = super.onOptionsItemSelected(menuItem);
        this.openAboutDialog();
        return supRetVal;
    }

    private void openAboutDialog() {
        LayoutInflater li = LayoutInflater.from(this);
        View view = li.inflate(R.layout.aboutview, null);
        new AlertDialog.Builder(MainActivity.this).setTitle(this.getString(R.string.about_title)).setView(view)
                .setNegativeButton(this.getString(R.string.about_close), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Log.d(MSG_TAG, "Close pressed");
                    }
                }).show();
    }

 */
// End of Vanzo:huangchaojun
    private void openBrightDialog() {
        LayoutInflater li = LayoutInflater.from(this);
        View view = li.inflate(R.layout.brightwarn, null);
        new AlertDialog.Builder(MainActivity.this).setTitle(this.getString(R.string.brightwarn_title))
                .setView(view)
                .setNegativeButton(this.getString(R.string.brightwarn_negative), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        MainActivity.this.buttonBright.setChecked(false);
                    }
                }).setNeutralButton(this.getString(R.string.brightwarn_accept), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        MainActivity.this.bright = true;
                        mPrefsEditor.putBoolean("bright", true);
                        mPrefsEditor.commit();
                    }
                }).show();
    }

    public void updateWidget() {
        this.mWidgetProvider.updateAllStates(context);
    }

    private void updateBigButtonState() {
        if (Settings.System.getInt(context.getContentResolver(),
                TorchSwitch.TORCH_STATE, 0) == 1) {
            mTorchOn = true;
            buttonOn.setChecked(true);
/* Vanzo:hexiongjievanzo on: Sat, 20 Aug 2011 11:35:33 +0800
 * the button was not come true
            buttonBright.setEnabled(false);
            buttonStrobe.setEnabled(false);
            if (!buttonStrobe.isChecked()) {
                slider.setEnabled(false);
            }
 */
// End of Vanzo: hexiongjievanzo
        } else {
            mTorchOn = false;
            buttonOn.setChecked(false);
/* Vanzo:hexiongjievanzo on: Sat, 20 Aug 2011 11:36:15 +0800
 * the button was not come true
            buttonBright.setEnabled(useBrightSetting);
            buttonStrobe.setEnabled(true);
            slider.setEnabled(true);
 */
// End of Vanzo: hexiongjievanzo
        }
    }

    private BroadcastReceiver mShutDownReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    };

/* Vanzo:wangyi on: Fri, 19 Apr 2013 16:22:51 +0800
 * bugfix #34240
 */
    private boolean torchServiceRunning(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Activity.ACTIVITY_SERVICE);

        List<ActivityManager.RunningServiceInfo> svcList = am.getRunningServices(100);

        if (!(svcList.size() > 0)) return false;
        for (RunningServiceInfo serviceInfo : svcList) {
            ComponentName serviceName = serviceInfo.service;
            if (serviceName.getClassName().endsWith(".TorchService")
                    || serviceName.getClassName().endsWith(".RootTorchService")) return true;
        }
        return false;
    }
// End of Vanzo: wangyi
}
