
package com.mediatek.factorymode.memory;

import java.io.IOException;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.mediatek.factorymode.AppDefine;
import com.mediatek.factorymode.R;
import com.mediatek.factorymode.ShellExe;
import com.mediatek.factorymode.Utils;

public class Memory extends Activity implements OnClickListener {
    private TextView mCommInfo;

    private TextView mBtOk;

    private TextView mBtFailed;

    SharedPreferences mSp;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.memory);
        mSp = getSharedPreferences("FactoryMode", Context.MODE_PRIVATE);
        mBtOk = (TextView) findViewById(R.id.memory_bt_ok);
        mBtOk.setOnClickListener(this);
        mBtFailed = (TextView) findViewById(R.id.memory_bt_failed);
        mBtFailed.setOnClickListener(this);
        mCommInfo = (TextView) findViewById(R.id.comm_info);
        mCommInfo.setText(getInfo("cat /proc/driver/nand"));
    }

    private String getInfo(String cmd) {
        String result = null;
        try {
            String[] cmdx = {
                    "/system/bin/sh", "-c", cmd
            };
            int ret = ShellExe.execCommand(cmdx);
            if (0 == ret) {
                result = ShellExe.getOutput();
            } else {
                result = "Can not get flash info.";
            }

        } catch (IOException e) {
            result = e.toString();
        }
        return result;
    }

    @Override
    public void onClick(View v) {
        Utils.SetPreferences(this, mSp, R.string.memory_name,
                (v.getId() == mBtOk.getId()) ? AppDefine.FT_SUCCESS : AppDefine.FT_FAILED);
        finish();
    }
}
