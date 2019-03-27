
package com.mediatek.factorymode.camera;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.mediatek.factorymode.AppDefine;
import com.mediatek.factorymode.R;
import com.mediatek.factorymode.Utils;

public class FlashLight extends Activity implements OnClickListener {

    private Button mBtOk;

    private Button mBtFailed;

    private SharedPreferences mSp;
    private Camera mCamera = null;
    private Camera.Parameters mParams;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.flashlight);
        mSp = getSharedPreferences("FactoryMode", Context.MODE_PRIVATE);
        mBtOk = (Button) findViewById(R.id.flash_bt_ok);
        mBtOk.setOnClickListener(this);
        mBtFailed = (Button) findViewById(R.id.flash_bt_failed);
        mBtFailed.setOnClickListener(this);
        if (mCamera == null) {
            mCamera = Camera.open();
        }
    }

    public void onResume() {
        super.onResume();

        mParams = mCamera.getParameters();
        mParams.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        mCamera.setParameters(mParams);
    }

    public void onPause() {
        super.onPause();

        mParams = mCamera.getParameters();
        mParams.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        mCamera.setParameters(mParams);
    }

    public void onDestroy() {
        super.onDestroy();
        mCamera.release();
        mCamera = null;
    }

    public void onClick(View v) {
        Utils.SetPreferences(this, mSp, R.string.flash_name,
                (v.getId() == mBtOk.getId()) ? AppDefine.FT_SUCCESS : AppDefine.FT_FAILED);
        finish();
    }
}
