
package com.mediatek.factorymode.sensor;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.mediatek.factorymode.AppDefine;
import com.mediatek.factorymode.FactoryMode;
import com.mediatek.factorymode.R;
import com.mediatek.factorymode.Utils;

public class LSensor extends Activity implements SensorEventListener {
    /** Called when the activity is first created. */
    SensorManager mSensorManager = null;

    Sensor mLightSensor = null;

    TextView mAccuracyView = null;

    TextView mValueX = null;

    Button mBtOk;

    Button mBtFailed;

    SharedPreferences mSp;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lsensor);
        if (FactoryMode.mHavePsensor == false) {
            finish();
            return;
        }
        mSp = getSharedPreferences("FactoryMode", Context.MODE_PRIVATE);
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        mAccuracyView = (TextView) findViewById(R.id.lsensor_accuracy);
        mValueX = (TextView) findViewById(R.id.lsensor_value);
        mBtOk = (Button) findViewById(R.id.lsensor_bt_ok);
        mBtOk.setOnClickListener(cl);
        mBtFailed = (Button) findViewById(R.id.lsensor_bt_failed);
        mBtFailed.setOnClickListener(cl);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this, mLightSensor);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mLightSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        if (sensor.getType() == Sensor.TYPE_LIGHT) {
            mAccuracyView.setText(getString(R.string.LSensor_accuracy) + accuracy);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            float[] values = event.values;
            mValueX.setText(getString(R.string.LSensor_value) + values[0]);
        }
    }

    public View.OnClickListener cl = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Utils.SetPreferences(getApplicationContext(), mSp, R.string.lsensor_name,
                    (v.getId() == mBtOk.getId()) ? AppDefine.FT_SUCCESS : AppDefine.FT_FAILED);
            finish();
        }
    };
}
