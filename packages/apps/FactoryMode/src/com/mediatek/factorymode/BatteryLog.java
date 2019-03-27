
package com.mediatek.factorymode;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class BatteryLog extends Activity implements OnClickListener {

    private TextView mStatus;

    private TextView mLevel;

    private TextView mScale;

    private TextView mHealth;

    private TextView mVoltage;

    private TextView mTemperature;

    private TextView mTechnology;

    private TextView mUptime;

    private TextView mCurrent;


    private Button mBtOK;

    private Button mBtFailed;

    private SharedPreferences mSp;

    private static final int EVENT_TICK = 1;

    private static final int EVENT_UPDATE = 2;

    String flag = "";

    boolean mStart = false;

/* Vanzo:libing on: Wed, 29 May 2013 19:29:53 +0800
 *
 */
    public class BatteryInfo {
        public String current;
        public String consumption;

        public BatteryInfo (String cur, String con) {
            this.current = cur;
            this.consumption = con;
        }
    }
// End of Vanzo:libing
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_TICK:
                    updateBatteryStats();
                    sendEmptyMessageDelayed(EVENT_TICK, 1000);
                    break;
/* Vanzo:libing on: Wed, 29 May 2013 19:30:54 +0800
 *
                case EVENT_UPDATE:
                    mCurrent.setText(msg.arg1 + "mA");
                    mCurrentConsumption.setText(msg.arg2 + "mA");
                    break;
 */

                case EVENT_UPDATE:
                    BatteryInfo info = (BatteryInfo) msg.obj;
                    mCurrent.setText(info.current + "mA");
                    break;
// End of Vanzo:libing
            }
        }

        private void updateBatteryStats() {
            long uptime = SystemClock.elapsedRealtime();
            mUptime.setText(DateUtils.formatElapsedTime(uptime / 1000));
        }
    };

    private final String tenthsToFixedString(int x) {
        int tens = x / 10;
        return new String("" + tens + "." + (x - 10 * tens));
    }

    private IntentFilter mIntentFilter;

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context arg0, Intent arg1) {
            String action = arg1.getAction();
            if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                int plugType = arg1.getIntExtra("plugged", 0);

                mLevel.setText("" + arg1.getIntExtra("level", 0));
                mScale.setText("" + arg1.getIntExtra("scale", 0));
                mVoltage.setText("" + arg1.getIntExtra("voltage", 0) + " "
                        + getString(R.string.battery_info_voltage_units));
                mTemperature.setText("" + tenthsToFixedString(arg1.getIntExtra("temperature", 0))
                        + getString(R.string.battery_info_temperature_units));
                mTechnology.setText("" + arg1.getStringExtra("technology"));

                int status = arg1.getIntExtra("status", BatteryManager.BATTERY_STATUS_UNKNOWN);
                String statusString;
                switch (status) {
                    case BatteryManager.BATTERY_STATUS_CHARGING:
                        statusString = getString(R.string.battery_info_status_charging);
                        if (plugType > 0) {
                            statusString = statusString
                                    + " "
                                    + getString((plugType == BatteryManager.BATTERY_PLUGGED_AC) ? R.string.battery_info_status_charging_ac
                                            : R.string.battery_info_status_charging_usb);
                        }
                        break;
                    case BatteryManager.BATTERY_STATUS_DISCHARGING:
                        statusString = getString(R.string.battery_info_status_discharging);
                        break;
                    case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                        statusString = getString(R.string.battery_info_status_not_charging);
                        break;
                    case BatteryManager.BATTERY_STATUS_FULL:
                        statusString = getString(R.string.battery_info_status_full);
                        break;
                    default:
                        statusString = getString(R.string.battery_info_status_unknown);
                        break;
                }
                mStatus.setText(statusString);

                int health = arg1.getIntExtra("health", BatteryManager.BATTERY_HEALTH_UNKNOWN);
                String healthString;
                switch (health) {
                    case BatteryManager.BATTERY_HEALTH_GOOD:
                        healthString = getString(R.string.battery_info_health_good);
                        break;
                    case BatteryManager.BATTERY_HEALTH_OVERHEAT:
                        healthString = getString(R.string.battery_info_health_overheat);
                        break;
                    case BatteryManager.BATTERY_HEALTH_DEAD:
                        healthString = getString(R.string.battery_info_health_dead);
                        break;
                    case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
                        healthString = getString(R.string.battery_info_health_over_voltage);
                        break;
                    case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE:
                        healthString = getString(R.string.battery_info_health_unspecified_failure);
                        break;
                    default:
                        healthString = getString(R.string.battery_info_health_unknown);
                        break;
                }
                mHealth.setText(healthString);
            }

        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.battery_info);
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
    }

    @Override
    public void onResume() {
        super.onResume();

        mStatus = (TextView) findViewById(R.id.status);
        mLevel = (TextView) findViewById(R.id.level);
        mScale = (TextView) findViewById(R.id.scale);
        mHealth = (TextView) findViewById(R.id.health);
        mTechnology = (TextView) findViewById(R.id.technology);
        mVoltage = (TextView) findViewById(R.id.voltage);
        mTemperature = (TextView) findViewById(R.id.temperature);
        mUptime = (TextView) findViewById(R.id.uptime);
        mCurrent = (TextView) findViewById(R.id.current);
        mBtOK = (Button) findViewById(R.id.battery_bt_ok);
        mBtOK.setOnClickListener(this);
        mBtFailed = (Button) findViewById(R.id.battery_bt_failed);
        mBtFailed.setOnClickListener(this);

        mSp = getSharedPreferences("FactoryMode", Context.MODE_PRIVATE);
        mHandler.sendEmptyMessageDelayed(EVENT_TICK, 1000);
        mStart = true;
        registerReceiver(mIntentReceiver, mIntentFilter);
        new runThread().start();
    }

    @Override
    public void onPause() {
        super.onPause();
        mHandler.removeMessages(EVENT_TICK);
        // we are no longer on the screen stop the observers
        mStart = false;
        unregisterReceiver(mIntentReceiver);
    }

    public void onClick(View v) {
        Utils.SetPreferences(this, mSp, R.string.battery_name,
                (v.getId() == mBtOK.getId()) ? AppDefine.FT_SUCCESS : AppDefine.FT_FAILED);
        finish();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 1, R.string.menu_exit);
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        setResult(Activity.RESULT_FIRST_USER);
        finish();
        return true;
    }

    class runThread extends Thread {
        public void run() {
            while (mStart) {
                File currentFile = new File(
                        "/sys/devices/platform/battery/power_supply/battery/BatteryAverageCurrent");
                File consumptionFile = new File(
                        "/sys/devices/platform/battery/FG_Battery_CurrentConsumption");
                if (consumptionFile.exists() && currentFile.exists()) {
/* Vanzo:libing on: Wed, 29 May 2013 17:37:10 +0800
 *
                    mHandler.obtainMessage(EVENT_UPDATE,
                            Integer.parseInt(readFile(currentFile).trim()),
                            Integer.parseInt(readFile(consumptionFile).trim())).sendToTarget();
 */
                    BatteryInfo bi = new BatteryInfo(readFile(currentFile).trim(), readFile(consumptionFile).trim());
                    mHandler.obtainMessage(EVENT_UPDATE, bi).sendToTarget();
// End of Vanzo:libing
                }
                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                }
            }
        }
    }

    private static String readFile(File fn) {
        FileReader f = null;
        int len;
        try {
            f = new FileReader(fn);
            String s = "";
            char[] cbuf = new char[20];
            while ((len = f.read(cbuf, 0, cbuf.length)) >= 0) {
                s += String.valueOf(cbuf, 0, len);
            }
            return s;
        } catch (IOException ex) {
            return "0";
        } finally {
            if (f != null) {
                try {
                    f.close();
                } catch (IOException ex) {
                    return "0";
                }
            }
        }
    }

}
