
package com.mediatek.factorymode.fmradio;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;

import com.mediatek.factorymode.AppDefine;
import com.mediatek.factorymode.R;
import com.mediatek.factorymode.Utils;

public class FMRadio extends Activity {
    SharedPreferences mSp;

    static Cursor mCursor = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSp = getSharedPreferences("FactoryMode", Context.MODE_PRIVATE);
        Intent fmActivityIntent = new Intent("com.mediatek.FMRadio.test");
        startActivityForResult(fmActivityIntent, AppDefine.FT_FMRADIOSETID);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode != RESULT_OK){
            finish();
            return;
        }
        boolean fmResult = data.getBooleanExtra("result", false);
        Utils.SetPreferences(getApplicationContext(), mSp, R.string.fmradio_name,
                (fmResult) ? AppDefine.FT_SUCCESS : AppDefine.FT_FAILED);
        finish();
    }
}
