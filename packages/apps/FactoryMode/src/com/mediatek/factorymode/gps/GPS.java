
package com.mediatek.factorymode.gps;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;

import com.mediatek.factorymode.AppDefine;
import com.mediatek.factorymode.R;
import com.mediatek.factorymode.Utils;

public class GPS extends Activity implements OnClickListener {

    private TextView mStView;

    private TextView mSatelliteNumView;

    private TextView mSignalView;

    private Chronometer mTimeView;

    private TextView mResultView;

    private Button mBtOk;

    private Button mBtFailed;

    private SharedPreferences mSp;

    private GpsUtil mGpsUtil;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gps);
        mGpsUtil = new GpsUtil(this);
        mSp = getSharedPreferences("FactoryMode", Context.MODE_PRIVATE);
        mStView = (TextView) findViewById(R.id.gps_state_id);
        mSatelliteNumView = (TextView) findViewById(R.id.gps_satellite_id);
        mSignalView = (TextView) findViewById(R.id.gps_signal_id);
        mResultView = (TextView) findViewById(R.id.gps_result_id);
        mTimeView = (Chronometer) findViewById(R.id.gps_time_id);
        mBtOk = (Button) findViewById(R.id.gps_bt_ok);
        mBtFailed = (Button) findViewById(R.id.gps_bt_failed);
        mBtOk.setOnClickListener(this);
        mBtFailed.setOnClickListener(this);
        mTimeView.setFormat(getResources().getString(R.string.GPS_time));
        mStView.setText(R.string.GPS_connect);
        mTimeView.start();
        getSatelliteInfo();
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 0:
                    getSatelliteInfo();
                    break;
            }
        }
    };

    protected void onDestroy() {
        mGpsUtil.closeLocation();
        super.onDestroy();
    }

    public void onClick(View v) {
        Utils.SetPreferences(this, mSp, R.string.gps_name,
                (v.getId() == mBtOk.getId()) ? AppDefine.FT_SUCCESS : AppDefine.FT_FAILED);
        finish();
    }

    public void getSatelliteInfo() {
        int num = 0;
        if ((num = mGpsUtil.getSatelliteNumber()) >= 2) {
            mHandler.removeMessages(0);
            mTimeView.stop();
            mResultView.setText(R.string.GPS_Success);
        } else {
            mHandler.sendEmptyMessageDelayed(0, 3000);
        }
        mSatelliteNumView.setText(getString(R.string.GPS_satelliteNum) + num);
        mSignalView.setText(getString(R.string.GPS_Signal)
                + mGpsUtil.getSatelliteSignals());
    }
}
