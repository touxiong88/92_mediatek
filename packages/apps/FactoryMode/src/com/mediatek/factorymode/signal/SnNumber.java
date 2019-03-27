package com.mediatek.factorymode.signal;

import com.mediatek.factorymode.AppDefine;
import com.mediatek.factorymode.R;
import com.mediatek.factorymode.Utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.telephony.TelephonyManager;

public class SnNumber extends Activity {

    SharedPreferences mSp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSp = getSharedPreferences("FactoryMode", Context.MODE_PRIVATE);
        TelephonyManager tm = (TelephonyManager) this
                .getSystemService(Context.TELEPHONY_SERVICE);
        AlertDialog.Builder builder = new AlertDialog.Builder(SnNumber.this);
        builder.setTitle(R.string.sn_number_title);
        builder.setMessage(tm.getSN());
	 builder.setCancelable(false);
        builder.setPositiveButton(R.string.Success,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Utils.SetPreferences(SnNumber.this, mSp, R.string.snnumber_name,
                                AppDefine.FT_SUCCESS);
                        finish();
                    }
                });
        builder.setNegativeButton(R.string.Failed, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                Utils.SetPreferences(SnNumber.this, mSp, R.string.snnumber_name,
                        AppDefine.FT_FAILED);
                finish();
            }
        });
        builder.create().show();
    }
}
