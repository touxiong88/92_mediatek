package com.mediatek.factorymode;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class IntouchTest extends Activity implements OnClickListener {

    private Button mBtOk;

    private Button mBtFailed;

    SharedPreferences mSp;


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.intouch);
        mSp = getSharedPreferences("FactoryMode", Context.MODE_PRIVATE);
        mBtOk = (Button) findViewById(R.id.intouch_bt_ok);
        mBtOk.setOnClickListener(this);
        mBtFailed = (Button) findViewById(R.id.intouch_bt_failed);
        mBtFailed.setOnClickListener(this);
        Intent intent = new Intent();
        intent.setClassName("com.check.sensor", "com.check.sensor.CheckActivity");
        startActivity(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onClick(View v) {
        Utils.SetPreferences(this, mSp, R.string.intouch_name,
                (v.getId() == mBtOk.getId()) ? AppDefine.FT_SUCCESS
                        : AppDefine.FT_FAILED);
        finish();
    }

}
