
package com.mediatek.factorymode.sensor;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.mediatek.factorymode.AppDefine;
import com.mediatek.factorymode.FactoryMode;
import com.mediatek.factorymode.R;
import com.mediatek.factorymode.Utils;

@SuppressWarnings("deprecation")
public class MSensor extends Activity implements SensorListener {

    private ImageView mImgCompass = null;

    private TextView mOrientText = null;

    private TextView mOrientValue = null;

    private SensorManager mSm = null;

    private RotateAnimation mMyAni = null;

    private float mDegressQuondam = 0.0f;

    private SharedPreferences mSp;

    private Button mBtOk;

    private Button mBtFailed;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.msensor);
        if (FactoryMode.mHaveMsensor == false) {
            finish();
            return;
        }
        mSp = getSharedPreferences("FactoryMode", Context.MODE_PRIVATE);
        mOrientText = (TextView) findViewById(R.id.OrientText);
        mImgCompass = (ImageView) findViewById(R.id.ivCompass);
        mOrientValue = (TextView) findViewById(R.id.OrientValue);
        mBtOk = (Button) findViewById(R.id.msensor_bt_ok);
        mBtOk.setOnClickListener(cl);
        mBtFailed = (Button) findViewById(R.id.msensor_bt_failed);
        mBtFailed.setOnClickListener(cl);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void onStart() {
        super.onStart();
        mSm = (SensorManager) getSystemService(SENSOR_SERVICE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSm.registerListener(this, SensorManager.SENSOR_ORIENTATION,
                SensorManager.SENSOR_DELAY_FASTEST);

    }

    @Override
    protected void onStop() {
        if (mSm != null) {
            mSm.unregisterListener(this);
        }
        super.onStop();
    }

    public void onDestroy() {
        if (mSm != null) {
            mSm.unregisterListener(this);
        }
        super.onDestroy();
    }

    private void AniRotateImage(float fDegress) {
        if (Math.abs(fDegress - mDegressQuondam) < 1) {
            return;
        }
        mMyAni = new RotateAnimation(mDegressQuondam, fDegress, Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        mMyAni.setDuration(200);
        mMyAni.setFillAfter(true);

        mImgCompass.startAnimation(mMyAni);
        mDegressQuondam = fDegress;
    }

    public void onSensorChanged(int sensor, float[] values) {
        synchronized (this) {
            if (sensor == SensorManager.SENSOR_ORIENTATION) {
                if (null == mOrientText || null == mOrientValue || null == mImgCompass) {
                    return;
                }
                if (Math.abs(values[0] - mDegressQuondam) < 1) {
                    return;
                }

                switch ((int) values[0]) {
                    case 0: // North
                        mOrientText.setText(R.string.MSensor_North);
                        break;
                    case 90: // East
                        mOrientText.setText(R.string.MSensor_East);
                        break;
                    case 180: // South
                        mOrientText.setText(R.string.MSensor_South);
                        break;
                    case 270: // West
                        mOrientText.setText(R.string.MSensor_West);
                        break;
                    default: {
                        int v = (int) values[0];
                        if (v > 0 && v < 90) {
                            mOrientText.setText(getString(R.string.MSensor_north_east) + v);
                        }

                        if (v > 90 && v < 180) {
                            v = 180 - v;
                            mOrientText.setText(getString(R.string.MSensor_south_east) + v);
                        }

                        if (v > 180 && v < 270) {
                            v = v - 180;
                            mOrientText.setText(getString(R.string.MSensor_south_west) + v);
                        }
                        if (v > 270 && v < 360) {
                            v = 360 - v;
                            mOrientText.setText(getString(R.string.MSensor_north_west) + v);
                        }
                    }
                }

                mOrientValue.setText(String.valueOf(values[0]));

                if (mDegressQuondam != -values[0])
                    AniRotateImage(-values[0]);
            }

        }
    }

    @Override
    public void onAccuracyChanged(int sensor, int accuracy) {
    }

    private View.OnClickListener cl = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Utils.SetPreferences(getApplicationContext(), mSp, R.string.msensor_name,
                    (v.getId() == mBtOk.getId()) ? AppDefine.FT_SUCCESS : AppDefine.FT_FAILED);
            finish();
        }
    };
}
