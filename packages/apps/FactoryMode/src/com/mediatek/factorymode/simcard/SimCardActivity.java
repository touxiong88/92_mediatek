package com.mediatek.factorymode.simcard;

import com.mediatek.factorymode.AppDefine;
import com.mediatek.factorymode.R;
import com.mediatek.factorymode.Utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

public class SimCardActivity extends Activity {
    SharedPreferences mSp;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent ActivityIntent = new Intent();
        ActivityIntent.setClassName(this,
                "com.mediatek.factorymode.simcard.SimCard");
        startActivityForResult(ActivityIntent, AppDefine.FT_FMRADIOSETID);
        mSp = getSharedPreferences("FactoryMode", Context.MODE_PRIVATE);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            finish();
            return;
        }

        boolean Result = data.getBooleanExtra("result", false);
        Utils.SetPreferences(getApplicationContext(), mSp, R.string.sim_name,
                (Result) ? AppDefine.FT_SUCCESS : AppDefine.FT_FAILED);
        finish();
    }
}
