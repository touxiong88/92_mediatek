package com.mediatek.factorymode.buildversion;

import com.mediatek.factorymode.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.SystemProperties;
import android.content.SharedPreferences;
import com.mediatek.factorymode.Utils;
import com.mediatek.factorymode.AppDefine;
public class Buildversion extends Activity {
private String boardinfo;
private String KEY_BUILDVERSION ="ro.build.display.id";
SharedPreferences mSp;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSp = getSharedPreferences("FactoryMode", Context.MODE_PRIVATE);
        boardinfo = SystemProperties.get(KEY_BUILDVERSION);
        AlertDialog.Builder builder = new AlertDialog.Builder(Buildversion.this);
        builder.setTitle(R.string.build_version);
        builder.setMessage(boardinfo);
	 builder.setCancelable(false);
        builder.setPositiveButton(R.string.Success,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Utils.SetPreferences(Buildversion.this, mSp, R.string.build_version, AppDefine.FT_SUCCESS);
                        finish();
                    }
                });
        builder.create().show();
    }
}
