
package com.mediatek.factorymode.lcd;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.TextView;

import com.mediatek.factorymode.AppDefine;
import com.mediatek.factorymode.R;
import com.mediatek.factorymode.Utils;

public class LCD extends Activity {
    private TextView mText1 = null;

    private int mNum = 0;

    private Timer timer;

    SharedPreferences mSp;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSp = getSharedPreferences("FactoryMode", Context.MODE_PRIVATE);
        setContentView(R.layout.lcd);
        timer = new Timer();
        initView();

    }

    protected void onDestroy() {
        super.onDestroy();
        timer.cancel();
    }

    Handler myHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                mNum++;
                if (mNum >= 4) {
                    timer.cancel();
                    myHandler.removeMessages(0);
                    AlertDialog.Builder builder = new AlertDialog.Builder(LCD.this);
                    builder.setTitle(R.string.FMRadio_notice);
                    builder.setPositiveButton(R.string.Success,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    Utils.SetPreferences(getApplicationContext(), mSp,
                                            R.string.lcd_name, AppDefine.FT_SUCCESS);
                                    finish();
                                }
                            });
                    builder.setNegativeButton(getResources().getString(R.string.Failed),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    Utils.SetPreferences(getApplicationContext(), mSp,
                                            R.string.lcd_name, AppDefine.FT_FAILED);
                                    finish();
                                }
                            });
                    builder.create().show();
                } else {
                    changeColor(mNum);
                }
            }
        }
    };

    private void initView() {
        mText1 = (TextView) findViewById(R.id.test_color_text1);

        timer.schedule(new TimerTask() {
            public void run() {
                Message msg = new Message();
                msg.what = 0;
                myHandler.sendMessage(msg);
            }
        }, 1000, 1000);
    }

    private void changeColor(int num) {
        switch (num % 4) {
            case 0:
                mText1.setBackgroundColor(Color.RED);
                break;
            case 1:
                mText1.setBackgroundColor(Color.GREEN);
                break;
            case 2:
                mText1.setBackgroundColor(Color.BLUE);
                break;
            case 3:
                mText1.setBackgroundColor(Color.WHITE);
                break;
        }
    }
}
