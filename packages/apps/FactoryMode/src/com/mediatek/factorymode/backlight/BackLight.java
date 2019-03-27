
package com.mediatek.factorymode.backlight;

import java.io.IOException;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.mediatek.factorymode.AppDefine;
import com.mediatek.factorymode.R;
import com.mediatek.factorymode.ShellExe;
import com.mediatek.factorymode.Utils;

public class BackLight extends Activity implements OnClickListener {

    private Button mBtnLcdON;

    private Button mBtnLcdOFF;

    private Button mBtOk;

    private Button mBtFailed;

    private SharedPreferences mSp;

    private String lcdCmdON = "echo 250 > /sys/class/leds/lcd-backlight/brightness";

    private String lcdCmdOFF = "echo 50 > /sys/class/leds/lcd-backlight/brightness";

    private final int ERR_OK = 0;

    private final int ERR_ERR = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.backlight);

        mBtnLcdON = (Button) findViewById(R.id.Display_lcd_on);
        mBtnLcdOFF = (Button) findViewById(R.id.Display_lcd_off);
        mBtOk = (Button) findViewById(R.id.display_bt_ok);
        mBtFailed = (Button) findViewById(R.id.display_bt_failed);

        mBtnLcdON.setOnClickListener(this);
        mBtnLcdOFF.setOnClickListener(this);
        mBtOk.setOnClickListener(this);
        mBtFailed.setOnClickListener(this);
        mSp = getSharedPreferences("FactoryMode", Context.MODE_PRIVATE);
    }

    private void setLastError(int err) {
        System.out.print(err);
    }

    public void onClick(View arg0) {
        try {

            if (arg0.getId() == mBtnLcdON.getId()) {
                String[] cmd = {
                        "/system/bin/sh", "-c", lcdCmdON
                };
                int ret = ShellExe.execCommand(cmd);
                if (0 == ret) {
                    setLastError(ERR_OK);
                } else {
                    setLastError(ERR_ERR);
                }

            } else if (arg0.getId() == mBtnLcdOFF.getId()) {
                String[] cmd = {
                        "/system/bin/sh", "-c", lcdCmdOFF
                };
                int ret = ShellExe.execCommand(cmd);
                if (0 == ret) {
                    setLastError(ERR_OK);
                } else {
                    setLastError(ERR_ERR);
                }

            } else if (arg0.getId() == mBtOk.getId()) {
                Utils.SetPreferences(this, mSp, R.string.backlight_name, AppDefine.FT_SUCCESS);
                finish();
            } else if (arg0.getId() == mBtFailed.getId()) {
                Utils.SetPreferences(this, mSp, R.string.backlight_name, AppDefine.FT_FAILED);
                finish();
            }
        } catch (IOException e) {
            setLastError(ERR_ERR);
        }
    }
}
