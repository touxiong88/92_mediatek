
package com.mediatek.factorymode.camera;

import com.mediatek.factorymode.AppDefine;
import com.mediatek.factorymode.R;
import com.mediatek.factorymode.Utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.view.Display;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;

import java.io.IOException;
import java.util.List;

public class CameraTest extends Activity implements OnClickListener {

    private SurfaceView surfaceView;

    private SurfaceHolder surfaceHolder;

    private Camera mCamera;

    private ImageView mAutoFocus;

    private Button mBtFinish;

    private Button mBtOk;

    private Button mBtFailed;

    private boolean mIsPreview = false;

    SharedPreferences mSp;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.camera);
        mSp = getSharedPreferences("FactoryMode", Context.MODE_PRIVATE);
        setupViews();
    }

    private void setupViews() {
        surfaceView = (SurfaceView) findViewById(R.id.camera_view);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(surfaceCallback);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mBtFinish = (Button) findViewById(R.id.camera_take);
        mBtFinish.setOnClickListener(this);
        mBtOk = (Button) findViewById(R.id.camera_btok);
        mBtOk.setOnClickListener(this);
        mBtFailed = (Button) findViewById(R.id.camera_btfailed);
        mBtFailed.setOnClickListener(this);
        mAutoFocus = (ImageView) findViewById(R.id.autofocus_img);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_CAMERA || keyCode == KeyEvent.KEYCODE_SEARCH) {
            takePic();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void takePic() {
        mBtFinish.setEnabled(false);
        mAutoFocus.setImageResource(R.drawable.focus_focusing);
        mCamera.autoFocus(autofocusCallback);
    }

    Camera.PictureCallback pictureCallback = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            mAutoFocus.setImageDrawable(null);
        }
    };

    Camera.AutoFocusCallback autofocusCallback = new Camera.AutoFocusCallback() {

        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            if (success) {
                mAutoFocus.setImageResource(R.drawable.focus_focused);
                mCamera.takePicture(null, null, pictureCallback);
            } else {
                mAutoFocus.setImageResource(R.drawable.focus_focus_failed);
            }
        }
    };

    SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {
        public void surfaceCreated(SurfaceHolder holder) {
            int cameraCount = 0;
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            cameraCount = Camera.getNumberOfCameras();
            for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
                Camera.getCameraInfo(camIdx, cameraInfo);
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    try {
                        mCamera = Camera.open(camIdx);
                        mCamera.setPreviewDisplay(holder);
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            if (mCamera == null) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Utils.SetPreferences(getApplicationContext(), mSp, R.string.camera_name,
                        AppDefine.FT_FAILED);
                finish();
                return;
            }
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPictureFormat(PixelFormat.JPEG);
            //parameters.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
            parameters.setFocusMode("auto");
            Size size = parameters.getPictureSize();

            List<Size> sizes = parameters.getSupportedPreviewSizes();
            Size optimalSize = null;
            if (size != null && size.height != 0)
                optimalSize = getOptimalPreviewSize(sizes, (double) size.width / size.height);
            if (optimalSize != null) {
                parameters.setPreviewSize(optimalSize.width, optimalSize.height);
            }
            mCamera.setParameters(parameters);
            try {
                mCamera.startPreview();
            } catch (Exception e) {
                if (mCamera != null) {
                    mCamera = null;
                }
            }
            mIsPreview = true;
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            if (mCamera != null) {
                if (mIsPreview) {
                    mCamera.stopPreview();
                }
            }
        }
    };

    private Size getOptimalPreviewSize(List<Size> sizes, double targetRatio) {
        final double ASPECT_TOLERANCE = 0.05;
        if (sizes == null)
            return null;

        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        Display display = getWindowManager().getDefaultDisplay();
        int targetHeight = Math.min(display.getHeight(), display.getWidth());

        if (targetHeight <= 0) {
            WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            targetHeight = windowManager.getDefaultDisplay().getHeight();
        }

        for (Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    @Override
    protected void onPause() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
        super.onPause();
    }

    public void onDestory() {
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == mBtFinish.getId()) {
            takePic();
        } else if (v.getId() == mBtOk.getId()) {
            Utils.SetPreferences(getApplicationContext(), mSp, R.string.camera_name,
                    AppDefine.FT_SUCCESS);
            finish();
        } else if (v.getId() == mBtFailed.getId()) {
            Utils.SetPreferences(getApplicationContext(), mSp, R.string.camera_name,
                    AppDefine.FT_FAILED);
            finish();
        }
    }
}
