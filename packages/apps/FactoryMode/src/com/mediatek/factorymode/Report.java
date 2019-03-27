
package com.mediatek.factorymode;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Button;
import android.content.Intent;

public class Report extends Activity {
    private SharedPreferences mSp;

    private TextView mSuccess;

    private TextView mFailed;

    private TextView mDefault;

    private Button mResetFactory;

    private List<String> mOkList;

    private List<String> mFailedList;

    private List<String> mDefaultList;

    final int itemString[] = {
            R.string.touchscreen1_name,
            R.string.touchscreen2_name, R.string.lcd_name, R.string.gps_name,
            R.string.battery_name, R.string.KeyCode_name,
            R.string.speaker_name, R.string.headset_name,
            R.string.microphone_name, R.string.earphone_name, R.string.WiFi,
            R.string.bluetooth_name, R.string.vibrator_name,
            R.string.telephone_name, R.string.backlight_name,
            R.string.gsensor_name, R.string.msensor_name,
            R.string.lsensor_name, R.string.psensor_name, R.string.sdcard_name,
            R.string.camera_name, R.string.subcamera_name,
            R.string.fmradio_name, R.string.sim_name, R.string.headsethook_name };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.report);
        mSp = getSharedPreferences("FactoryMode", Context.MODE_PRIVATE);
        mSuccess = (TextView) findViewById(R.id.report_success);
        mFailed = (TextView) findViewById(R.id.report_failed);
        mDefault = (TextView) findViewById(R.id.report_default);
        mResetFactory = (Button) findViewById(R.id.reset_factory);
        mResetFactory.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View arg0) {
                    Intent intent = new Intent();
                    intent.setClassName("com.android.settings", "com.android.settings.ResetFactoryActivity");
                    startActivity(intent);
                }
            });
        mOkList = new ArrayList<String>();
        mFailedList = new ArrayList<String>();
        mDefaultList = new ArrayList<String>();

        for (int i = 0; i < FactoryMode.itemString.size(); i++) {
            String item = getString(FactoryMode.itemString.get(i));
            if ((AppDefine.FT_SUCCESS).equals(mSp.getString(item, null))) {
                mOkList.add(item);
            } else if ((AppDefine.FT_FAILED).equals(mSp.getString(item, null))) {
                mFailedList.add(item);
            } else {
                mDefaultList.add(item);
            }
        }
        ShowInfo();
    }

    protected void ShowInfo() {
        String okItem = "\n" + getString(R.string.report_ok) + "\n";
        for (int i = 0; i < mOkList.size(); i++) {
            okItem += mOkList.get(i) + " | ";
        }

        mSuccess.setText(okItem);

        String failedItem = "\n" + getString(R.string.report_failed) + "\n";
        for (int j = 0; j < mFailedList.size(); j++) {
            failedItem += mFailedList.get(j) + " | ";
        }
        mFailed.setText(failedItem);

        String defaultItem = "\n" + getString(R.string.report_notest) + "\n";
        for (int k = 0; k < mDefaultList.size(); k++) {
            defaultItem += mDefaultList.get(k) + " | ";
        }
        mDefault.setText(defaultItem);
    }
}
