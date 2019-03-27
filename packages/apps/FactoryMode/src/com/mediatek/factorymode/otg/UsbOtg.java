package com.mediatek.factorymode.otg;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.text.format.Formatter;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.mediatek.factorymode.AppDefine;
import com.mediatek.factorymode.R;
import com.mediatek.factorymode.Utils;

public class UsbOtg extends Activity implements OnClickListener {
    private TextView mInfo;

    private Button mBtOk;

    private Button mBtFailed;

    private SharedPreferences mSp;

    private StorageManager mStorageManager;

    private String mPath = "/mnt/usbotg";

    private MyHandler myHandler;

    private MyThread myThread;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.otg);
        mSp = getSharedPreferences("FactoryMode", Context.MODE_PRIVATE);
        mInfo = (TextView) findViewById(R.id.otg_info);
        mBtOk = (Button) findViewById(R.id.otg_bt_ok);
        mBtOk.setOnClickListener(this);
        mBtFailed = (Button) findViewById(R.id.otg_bt_failed);
        mBtFailed.setOnClickListener(this);
        mStorageManager = StorageManager.from(this);
        myHandler = new MyHandler();
        myThread = new MyThread();
        myHandler.post(myThread);
    }

    public void onResume() {
        super.onResume();
        mStorageManager.registerListener(mStorageListener);
    }

    public void onPause() {
        super.onPause();
        mStorageManager.unregisterListener(mStorageListener);
    }

    private void otgTest() {
        String[] mPathList = mStorageManager.getVolumePaths();
        StorageVolume[] volumes = mStorageManager.getVolumeList();

        int len = mPathList.length;
        for (int i = 0; i < len; i++) {
            if (volumes[i].getPath().equals(mPath)) {
                File otgFile = new File(mPath);
                if (mStorageManager.getVolumeState(mPathList[i]).equals("mounted")) {
                    String otgTotalSize = Formatter.formatShortFileSize(this,
                            otgFile.getTotalSpace());

                    String otgFreeSize = Formatter.formatShortFileSize(this,
                            otgFile.getUsableSpace());

                    mInfo.setText(getString(R.string.otg_tips_success) + "\n\n"
                            + getString(R.string.otg_totalsize) + otgTotalSize + "\n\n"
                            + getString(R.string.otg_freesize) + otgFreeSize);
                } else {
                    mInfo.setText(getString(R.string.otg_tips_failed));
                }
            }
        }
    }

    StorageEventListener mStorageListener = new StorageEventListener() {
        @Override
        public void onStorageStateChanged(String path, String oldState, String newState) {
            myHandler.postDelayed(myThread, 2000);
        }
    };

    class MyHandler extends Handler {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    }

    class MyThread implements Runnable {

        @Override
        public void run() {
            otgTest();
        }

    }

    @Override
    public void onClick(View v) {
        Utils.SetPreferences(this, mSp, R.string.otg_name,
                (v.getId() == mBtOk.getId()) ? AppDefine.FT_SUCCESS : AppDefine.FT_FAILED);
        finish();
    }
}
