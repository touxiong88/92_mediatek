package com.mediatek.factorymode.sensor;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.mediatek.factorymode.AppDefine;
import com.mediatek.factorymode.R;
import com.mediatek.factorymode.Utils;

public class Gyroscope extends Activity implements OnClickListener,
        SensorEventListener {
    private TextView xView;
    private TextView yView;
    private TextView zView;

    private Button mBtOk;

    private Button mBtFailed;

    SharedPreferences mSp;

    SensorManager mSm = null;

    Sensor mGyroscopeSensor;

    private static final float NS2S = 1.0f / 1000000000.0f;
    private float timestamp;

    private float angle[] = new float[3];

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gyroscope);
        mSp = getSharedPreferences("FactoryMode", Context.MODE_PRIVATE);
        xView = (TextView) findViewById(R.id.x_id);
        yView = (TextView) findViewById(R.id.y_id);
        zView = (TextView) findViewById(R.id.z_id);
        mSm = (SensorManager) getSystemService(SENSOR_SERVICE);
        mGyroscopeSensor = mSm.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mBtOk = (Button) findViewById(R.id.gyroscope_bt_ok);
        mBtOk.setOnClickListener(this);
        mBtFailed = (Button) findViewById(R.id.gyroscope_bt_failed);
        mBtFailed.setOnClickListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSm.unregisterListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSm.registerListener(this, mGyroscopeSensor,
                SensorManager.SENSOR_MAX);
    }

    @Override
    public void onClick(View v) {
        Utils.SetPreferences(this, mSp, R.string.gyroscope_name,
                (v.getId() == mBtOk.getId()) ? AppDefine.FT_SUCCESS
                        : AppDefine.FT_FAILED);
        finish();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent e) {
        if (timestamp != 0) {
            int x = (int) e.values[SensorManager.DATA_X];
            int y = (int) e.values[SensorManager.DATA_Y];
            int z = (int) e.values[SensorManager.DATA_Z];

            final float dT = (e.timestamp - timestamp) * NS2S;
            angle[0] += x * dT;
            angle[1] += y * dT;
            angle[2] += z * dT;
            xView.setText(String.valueOf(getString(R.string.gyroscope_azimuth)
                    + e.values[SensorManager.DATA_X]));
            yView.setText(String.valueOf(getString(R.string.gyroscope_pitch)
                    + e.values[SensorManager.DATA_Y]));
            zView.setText(String.valueOf(getString(R.string.gyroscope_roll)
                    + e.values[SensorManager.DATA_Z]));
        }
        timestamp = e.timestamp;
    }
}
