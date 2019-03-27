
package com.mediatek.factorymode.sdcard;

import java.io.File;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
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

public class SDCard extends Activity implements OnClickListener {
    private TextView mInfo;

    private Button mBtOk;

    private Button mBtFailed;

    private SharedPreferences mSp;

    private StorageManager mStorageManager;

    private boolean mSdcard0 = false;

    private boolean mSdcard1 = false;

    private String mPath0 = "/storage/sdcard0";

    private String mPath1 = "/storage/sdcard1";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sdcard);
        mSp = getSharedPreferences("FactoryMode", Context.MODE_PRIVATE);
        mInfo = (TextView) findViewById(R.id.sdcard_info);
        mBtOk = (Button) findViewById(R.id.sdcard_bt_ok);
        mBtOk.setOnClickListener(this);
        mBtFailed = (Button) findViewById(R.id.sdcard_bt_failed);
        mBtFailed.setOnClickListener(this);
        mStorageManager = StorageManager.from(this);
        SDCardSizeTest();
    }

    public void SDCardSizeTest() {
        String[] mPathList = mStorageManager.getVolumePaths();
        StorageVolume[] volumes = mStorageManager.getVolumeList();

        int len = mPathList.length;
        for (int i = 0; i < len; i++) {
            if (mStorageManager.getVolumeState(mPathList[i]).equals("mounted")
                    && volumes[i].getPath().equals("/storage/sdcard0")) {
                mSdcard0 = true;
            } else if (mStorageManager.getVolumeState(mPathList[i]).equals("mounted")
                    && volumes[i].getPath().equals("/storage/sdcard1")) {
                mSdcard1 = true;
            }
        }

        if (mSdcard0 && mSdcard1) {
            File f0 = new File(mPath0);
            File f1 = new File(mPath1);
            showSdcardInfo(f0, f1);
        } else if (mSdcard0) {
            File f0 = new File(mPath0);
            showSdcardInfo(f0);
        } else {
            mInfo.setText(getString(R.string.sdcard_tips_failed));
        }
    }

    private void showSdcardInfo(File file0, File file1) {
        String sdcard0TotalSize = Formatter.formatShortFileSize(this, file0.getTotalSpace());

        String sdcard0FreeSize = Formatter.formatShortFileSize(this, file0.getUsableSpace());

        String sdcard1TotalSize = Formatter.formatShortFileSize(this, file1.getTotalSpace());

        String sdcard1FreeSize = Formatter.formatShortFileSize(this, file1.getUsableSpace());

        mInfo.setText(getString(R.string.sdcard_tips_success_ex) + "\n\n"
                + getString(R.string.sdcard_totalsize) + sdcard0TotalSize + "\n\n"
                + getString(R.string.sdcard_freesize) + sdcard0FreeSize + "\n\n"
                + "=========================\n\n"
                + getString(R.string.sdcard_tips_success) + "\n\n"
                + getString(R.string.sdcard_totalsize) + sdcard1TotalSize + "\n\n"
                + getString(R.string.sdcard_freesize) + sdcard1FreeSize);
    }

    private void showSdcardInfo(File file) {
        String sdcardTotalSize = Formatter.formatShortFileSize(this, file.getTotalSpace());

        String sdcardFreeSize = Formatter.formatShortFileSize(this, file.getUsableSpace());

        mInfo.setText(getString(R.string.sdcard_tips_success) + "\n\n"
                + getString(R.string.sdcard_totalsize) + sdcardTotalSize + "\n\n"
                + getString(R.string.sdcard_freesize) + sdcardFreeSize + "\n\n"
                + "=========================\n\n"
                + getString(R.string.sdcard_tips_failed_ex));
    }

    @Override
    public void onClick(View v) {
        Utils.SetPreferences(this, mSp, R.string.sdcard_name,
                (v.getId() == mBtOk.getId()) ? AppDefine.FT_SUCCESS : AppDefine.FT_FAILED);
        finish();
    }
}
