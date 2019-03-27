
package com.mediatek.factorymode.camera;

import com.mediatek.factorymode.AppDefine;
import com.mediatek.factorymode.R;
import com.mediatek.factorymode.Utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;

import java.io.IOException;
import java.util.List;

public class SubCamera extends Activity implements OnClickListener {

    private SurfaceView surfaceView;

    private SurfaceHolder surfaceHolder;

    private Camera mCamera;

    private Button mBtFinish;

    private Button mBtOk;

    private Button mBtFailed;

    SharedPreferences mSp;

    Camera.CameraInfo cameraInfo;

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
        mCamera.takePicture(null, null, pictureCallback);
    }

    Camera.PictureCallback pictureCallback = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
        }
    };

    SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {
        public void surfaceCreated(SurfaceHolder holder) {
            int cameraCount = 0;
            int result = 0;
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            int degrees = 0;
            switch (rotation) {
                case Surface.ROTATION_0:
                    degrees = 0;
                    break;
                case Surface.ROTATION_90:
                    degrees = 90;
                    break;
                case Surface.ROTATION_180:
                    degrees = 180;
                    break;
                case Surface.ROTATION_270:
                    degrees = 270;
                    break;
            }
            cameraInfo = new Camera.CameraInfo();
            cameraCount = Camera.getNumberOfCameras();
            for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
                Camera.getCameraInfo(camIdx, cameraInfo);
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    result = (cameraInfo.orientation + degrees) % 360;
                    result = (360 - result) % 360;
                    try {
                        mCamera = Camera.open(camIdx);
                        mCamera.setDisplayOrientation(result);
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
                Utils.SetPreferences(getApplicationContext(), mSp, R.string.subcamera_name,
                        AppDefine.FT_FAILED);
                finish();
                return;
            }

            Camera.Parameters parameters = mCamera.getParameters();
            Size size = parameters.getPictureSize();

            List<Size> sizes = parameters.getSupportedPreviewSizes();
            Size optimalSize = getOptimalPreviewSize(sizes, (double) size.width / size.height);
            if (optimalSize != null) {
                int camOr = cameraInfo.orientation;
                if (camOr == 0 || camOr == 180) {
                    parameters.setPreviewSize(optimalSize.height, optimalSize.width);
                } else {
                    parameters.setPreviewSize(optimalSize.width, optimalSize.height);
                }
            }
            mCamera.setParameters(parameters);
            mCamera.startPreview();
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            if (mCamera != null) {
                mCamera.stopPreview();
            }
        }
    };

    @Override
    protected void onPause() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
        super.onPause();
    }

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
            if (targetHeight > size.height)
                continue;

            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }
        if (optimalSize == null) {
            for (Size size : sizes) {
                double ratio = (double) size.width / size.height;
                if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                    continue;
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
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
    public void onClick(View v) {
        if (v.getId() == mBtFinish.getId()) {
            takePic();
        } else if (v.getId() == mBtOk.getId()) {
            Utils.SetPreferences(getApplicationContext(), mSp, R.string.subcamera_name,
                    AppDefine.FT_SUCCESS);
            finish();
        } else if (v.getId() == mBtFailed.getId()) {
            Utils.SetPreferences(getApplicationContext(), mSp, R.string.subcamera_name,
                    AppDefine.FT_FAILED);
            finish();
        }
    }
}
