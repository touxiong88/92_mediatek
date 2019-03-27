
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

import com.mediatek.factorymode.AppDefine;
import com.mediatek.factorymode.R;
import com.mediatek.factorymode.Utils;

public class GSensor extends Activity implements OnClickListener {
    private ImageView ivimg;

    private Button mBtOk;

    private Button mBtFailed;

    SharedPreferences mSp;

    SensorManager mSm = null;

    Sensor mGravitySensor;

    private final static int OFFSET = 2;
    private final static int THRESHOLD = 5;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gsensor);
        mSp = getSharedPreferences("FactoryMode", Context.MODE_PRIVATE);
        ivimg = (ImageView) findViewById(R.id.gsensor_iv_img);
        mBtOk = (Button) findViewById(R.id.gsensor_bt_ok);
        mBtOk.setOnClickListener(this);
        mBtFailed = (Button) findViewById(R.id.gsensor_bt_failed);
        mBtFailed.setOnClickListener(this);
        mSm = (SensorManager) getSystemService(SENSOR_SERVICE);
        mGravitySensor = mSm.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER);
        mSm.registerListener(lsn, mGravitySensor, SensorManager.SENSOR_DELAY_GAME);
    }

    protected void onDestroy() {
        mSm.unregisterListener(lsn);
        super.onDestroy();
    }

    SensorEventListener lsn = new SensorEventListener() {
        public void onAccuracyChanged(android.hardware.Sensor sensor, int accuracy) {
        }

        public void onSensorChanged(SensorEvent e) {
            if (e.sensor == mGravitySensor) {
                int x = (int) e.values[SensorManager.DATA_X];
                int y = (int) e.values[SensorManager.DATA_Y];
                int z = (int) e.values[SensorManager.DATA_Z];

                if (x - OFFSET > y && x - OFFSET > z) {
                    ivimg.setBackgroundResource(R.drawable.gsensor_x);
                } else if (y - OFFSET > x && y - OFFSET > z) {
                    ivimg.setBackgroundResource(R.drawable.gsensor_y);
                } else if (x + THRESHOLD <= -OFFSET && 0 <= y && y <= THRESHOLD && 0 <= z && z <= THRESHOLD + OFFSET) {
                    ivimg.setBackgroundResource(R.drawable.gsensor_x_2);
                } else if ((x >= -THRESHOLD && x <= THRESHOLD) && y <=0 && z <= THRESHOLD * 2) {
                    ivimg.setBackgroundResource(R.drawable.gsensor_z);
                    Log.e("hw", ">>>>>x = " + x  + ">>>>>>y =" + y + ">>>>>>z =" + z);
                }
            }
        }
    };

    @Override
    public void onClick(View v) {
        Utils.SetPreferences(this, mSp, R.string.gsensor_name,
                (v.getId() == mBtOk.getId()) ? AppDefine.FT_SUCCESS : AppDefine.FT_FAILED);
        finish();
    }
}
