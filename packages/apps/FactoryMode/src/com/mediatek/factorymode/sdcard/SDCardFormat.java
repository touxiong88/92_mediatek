
package com.mediatek.factorymode.sdcard;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.content.ContentResolver;
import android.content.Context;
import android.content.IContentProvider;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.storage.StorageVolume;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import com.mediatek.factorymode.AppDefine;
import com.mediatek.factorymode.Utils;
import com.mediatek.factorymode.R;
import com.android.internal.os.storage.ExternalStorageFormatter;

public class SDCardFormat extends Activity implements OnClickListener {

    private StorageVolume mStorageVolume;
    private String mVolumeDescription;
    private String mVolumePath;
    private ContentResolver mContentResolver;
    private Button btformat;
    private Button btok;
    private Button btfailed;
    private SharedPreferences sp;
    private boolean mIsInternalSD;
    private final String[] MTP_PROJECTION = {
        MediaStore.MTP_TRANSFER_FILE_PATH
    };

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        setContentView(R.layout.sdcard_format);
        sp = getSharedPreferences("FactoryMode", Context.MODE_PRIVATE);
        btok = (Button) findViewById(R.id.sdcardformat_bt_ok);
        btok.setOnClickListener(this);
        btfailed = (Button) findViewById(R.id.sdcardformat_bt_failed);
        btfailed.setOnClickListener(this);
        btformat = (Button) findViewById(R.id.sdcardformat_start);
        btformat.setOnClickListener(this);
        mStorageVolume = getIntent().getParcelableExtra(StorageVolume.EXTRA_STORAGE_VOLUME);
        if (mStorageVolume != null) {
            mVolumeDescription = mStorageVolume.getDescription(this);
            mVolumePath = mStorageVolume.getPath();
            mIsInternalSD = !mStorageVolume.isRemovable();
        }
        mContentResolver = getContentResolver();
    }

    private String getMtpPath() {
        Cursor cur = null;
        String path = null;

        cur = mContentResolver.query(MediaStore.getMtpTransferFileUri(), MTP_PROJECTION, null,
                null, null);
        if (cur != null) {
            if (cur.getCount() == 0 || cur.getCount() > 1) {
                path = "ERROR";
            } else {
                cur.moveToFirst();
                path = cur.getString(0);
            }
        }
        if (cur != null) {
            cur.close();
        }
        return path;
    }

    private boolean getMtpStatus() {
        String MtpPath = getMtpPath();
        if (MtpPath != null && !(MtpPath.equals("ERROR"))) {
            if (MtpPath.contains(mVolumePath + "/")) {
                return true;
            }
        }

        return false;
    }

    private String getVolumeString(int StringId) {
        if (mVolumeDescription == null || !mIsInternalSD) {
            return getString(StringId);
        }

        String sdCardString;
        if (getString(StringId).getBytes().length - getString(StringId).length() > 0) {
            sdCardString = " " + getString(R.string.sdcard);
        } else {
            sdCardString = getString(R.string.sdcard);
        }

        String str = getString(StringId).replace(sdCardString, mVolumeDescription);
        if (str != null && str.equals(sdCardString)) {
            sdCardString = sdCardString.substring(0, 1).toLowerCase() + sdCardString.substring(1);
            return getString(StringId).replace(sdCardString, mVolumeDescription);
        } else {
            return str;
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == btformat.getId()) {
            if (ActivityManager.isUserAMonkey()) {
                return;
            }

            if (getMtpStatus()) {
                Toast.makeText(SDCardFormat.this,
                        getVolumeString(R.string.mtp_transfer_erase_error_text), Toast.LENGTH_SHORT)
                        .show();
            } else {
                Intent intent = new Intent(ExternalStorageFormatter.FORMAT_ONLY);
                intent.setComponent(ExternalStorageFormatter.COMPONENT_NAME);
                intent.putExtra(StorageVolume.EXTRA_STORAGE_VOLUME, mStorageVolume);
                startService(intent);
            }
        } else {
            Utils.SetPreferences(this, sp, R.string.sdcardformat_name,
                    (v.getId() == btok.getId()) ? AppDefine.FT_SUCCESS : AppDefine.FT_FAILED);
            finish();
        }
    }
}
