package com.mediatek.factorymode.boardinfo;

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
public class Boardinfo extends Activity {
private String boardinfo;
private String KEY_BOARDINFO ="ro.build.flavor";
SharedPreferences mSp;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSp = getSharedPreferences("FactoryMode", Context.MODE_PRIVATE);
        boardinfo = SystemProperties.get(KEY_BOARDINFO);
        AlertDialog.Builder builder = new AlertDialog.Builder(Boardinfo.this);
        builder.setTitle(R.string.board_info);
        builder.setMessage(boardinfo);
	 builder.setCancelable(false);
        builder.setPositiveButton(R.string.Success,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Utils.SetPreferences(Boardinfo.this, mSp, R.string.board_info, AppDefine.FT_SUCCESS);
                        finish();
                    }
                });
        builder.create().show();
    }
}


