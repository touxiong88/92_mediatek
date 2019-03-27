
package com.mediatek.factorymode.fuelgauge;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import com.mediatek.factorymode.AppDefine;
import com.mediatek.factorymode.FactoryMode;
import com.mediatek.factorymode.R;
import com.mediatek.factorymode.Utils;
import android.widget.LinearLayout;
import android.graphics.Color;

public class FuelGauge extends Activity implements OnClickListener {

    TextView mFuelGaugeModel;
    TextView mFuelGaugeCapacity;
    TextView mFuelGaugeStatus;
    LinearLayout mFuelGaugeModelLayout;
    LinearLayout mFuelGaugeCapacityLayout;
    Button mBtnOk;
    Button mBtnFailed;
    private SharedPreferences mSp;

    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);
        setContentView(R.layout.fuelgauge);
        if (FactoryMode.mHaveFuelGauge == false) {
            finish();
            return;
        }
        mFuelGaugeModel = (TextView) findViewById(R.id.fuelgauge_model);
        mFuelGaugeCapacity = (TextView) findViewById(R.id.fuelgauge_capacity);
        mFuelGaugeStatus = (TextView) findViewById(R.id.fuelgauge_status);
        mFuelGaugeModelLayout = (LinearLayout) findViewById(R.id.fuelgauge_model_layout);
        mFuelGaugeCapacityLayout = (LinearLayout) findViewById(R.id.fuelgauge_capacity_layout);
        mBtnOk = (Button) findViewById(R.id.fuelgauge_bt_ok);
        mBtnFailed = (Button) findViewById(R.id.fuelgauge_bt_failed);
        mSp = getSharedPreferences("FactoryMode", Context.MODE_PRIVATE);
        mBtnOk.setOnClickListener(this);
        mBtnFailed.setOnClickListener(this);
        getFuelGaugeInfo();
    }

    private void getFuelGaugeInfo() {
        File fileFuelGaugeCapacity = new File("/sys/bus/platform/drivers/bq27520/bq27520_capacity");
        String fuelGaugeCapacity = fuelGaugeReader(fileFuelGaugeCapacity) + " mAh";
        if ((null != fuelGaugeReader(fileFuelGaugeCapacity)) && (Integer.valueOf(fuelGaugeReader(fileFuelGaugeCapacity)) > 0)) {
            mFuelGaugeStatus.setText(R.string.fuelgauge_status_ok);
            mFuelGaugeStatus.setTextColor(Color.GREEN);
            mFuelGaugeModel.setText("2700");
            mFuelGaugeCapacity.setText(fuelGaugeCapacity);
        } else {
            mFuelGaugeStatus.setText(R.string.fuelgauge_status_fail);
            mFuelGaugeStatus.setTextColor(Color.RED);
            mFuelGaugeModelLayout.setVisibility(View.GONE);
            mFuelGaugeCapacityLayout.setVisibility(View.GONE);
        }
    }

    private String fuelGaugeReader(File file) {
        String readResult = null;
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));
            readResult = br.readLine();
        } catch (Exception e) {
            readResult = null;
            e.printStackTrace();
        } finally {
            if (null != br) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                br = null;
            }
        }
        return readResult;
    }

    @Override
    public void onClick(View v) {
        Utils.SetPreferences(this, mSp, R.string.fuelgauge_name,
                (v.getId() == mBtnOk.getId()) ? AppDefine.FT_SUCCESS : AppDefine.FT_FAILED);
        finish();
    }
}
