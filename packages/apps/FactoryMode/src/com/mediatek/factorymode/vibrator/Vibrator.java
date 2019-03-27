
package com.mediatek.factorymode.vibrator;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.mediatek.factorymode.AppDefine;
import com.mediatek.factorymode.R;
import com.mediatek.factorymode.Utils;

public class Vibrator extends Activity implements OnClickListener {
    private android.os.Vibrator mVibrator;

    private Button mBtOk;

    private Button mBtFailed;

    private SharedPreferences mSp;

    private long[] pattern = {
            500, 1000
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.vibrator);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mSp = getSharedPreferences("FactoryMode", Context.MODE_PRIVATE);
        mBtOk = (Button) findViewById(R.id.vibrator_bt_ok);
        mBtOk.setOnClickListener(this);
        mBtFailed = (Button) findViewById(R.id.vibrator_bt_failed);
        mBtFailed.setOnClickListener(this);

        mVibrator = (android.os.Vibrator) getSystemService(VIBRATOR_SERVICE);
    }

    public void onResume() {
        super.onResume();
        mVibrator.vibrate(pattern, 0);
    }

    public void onPause() {
        super.onPause();
        mVibrator.cancel();
    }

    public void onDestroy() {
        super.onDestroy();
        mVibrator.cancel();
    }

    public void onClick(View v) {
        Utils.SetPreferences(this, mSp, R.string.vibrator_name,
                (v.getId() == mBtOk.getId()) ? AppDefine.FT_SUCCESS : AppDefine.FT_FAILED);
        finish();
    }
}
