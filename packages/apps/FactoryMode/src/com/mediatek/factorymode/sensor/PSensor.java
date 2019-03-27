
package com.mediatek.factorymode.sensor;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.mediatek.factorymode.AppDefine;
import com.mediatek.factorymode.FactoryMode;
import com.mediatek.factorymode.R;
import com.mediatek.factorymode.Utils;

public class PSensor extends Activity implements SensorEventListener, OnClickListener {
    /** Called when the activity is first created. */
    private SensorManager sensorManager;

    Sensor mProxmitySensor = null;

    private Button mBtOk;

    private Button mFailed;

    private TextView mPsensor;

    public static final String LOG_TAG = "Sensor";

    private int[] mAllPsensor = new int[1000];

    private static int mCount = 0;

    private int mPrePsensor = 0;

    private int mAverage = 0;

    private char[] mWrint = new char[1];

    private int mSumPsensor = 0;

    private Handler myHandler;

    CountDownTimer mCountDownTimer;

    SharedPreferences mSp;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.psensor);
        if (FactoryMode.mHavePsensor == false) {
            finish();
            return;
        }
        sensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);
        mSp = getSharedPreferences("FactoryMode", Context.MODE_PRIVATE);
        mBtOk = (Button) findViewById(R.id.psensor_bt_ok);
        mBtOk.setOnClickListener(this);
        mFailed = (Button) findViewById(R.id.psensor_bt_failed);
        mFailed.setOnClickListener(this);
        mPsensor = (TextView) findViewById(R.id.proximity);
        myHandler = new Handler();
        myHandler.post(myRunnable);
    }

    protected void onDestroy() {
        super.onDestroy();
        if (myHandler != null) {
            myHandler.removeCallbacks(myRunnable);
        }
    }

    public Runnable myRunnable = new Runnable() {
        @Override
        public void run() {
            File file = new File("/sys/bus/platform/drivers/als_ps/ps");
            if (!file.exists()) {
                file = new File("/sys/bus/platform/drivers/als_ps_cm3628/ps");
            }
            if (file.exists()) {
                String pSensorValues2 = readFile(file);
                if (pSensorValues2.trim().contains("-")) {
                    return;
                }
                mPrePsensor = Integer.parseInt(pSensorValues2.trim());
                mAllPsensor[mCount] = mPrePsensor;
                mCount++;
                mPsensor.setText(getResources().getString(R.string.proximity) + " "
                        + mPrePsensor);
            }
            for (int i = 0; i < mCount; i++) {
                mSumPsensor = mSumPsensor + mAllPsensor[i];
                mAllPsensor[i] = 0;
            }
            if (mCount > 0) {
                mAverage = mSumPsensor / mCount + 1;
                mWrint[0] = (char) mAverage;
            }
            mCount = 0;
            mSumPsensor = 0;
            myHandler.postDelayed(myRunnable, 200);
        }
    };

    protected void onResume() {
        super.onResume();
        mProxmitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        sensorManager.registerListener(this, mProxmitySensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    protected void onStop() {
        super.onStop();
        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
        }
        sensorManager.unregisterListener(this);
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private static String readFile(File fn) {
        FileReader f;
        int len;

        f = null;
        try {
            f = new FileReader(fn);
            String s = "";
            char[] cbuf = new char[200];
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


    public void onSensorChanged(SensorEvent event) {
    }

    @Override
    public void onClick(View v) {
        Utils.SetPreferences(this, mSp, R.string.psensor_name,
                (v.getId() == mBtOk.getId()) ? AppDefine.FT_SUCCESS : AppDefine.FT_FAILED);
        finish();
    }
}
