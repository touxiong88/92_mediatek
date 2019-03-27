/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.mediatek.media3d;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.widget.FrameLayout;

import com.mediatek.media3d.photo.PhotoPage;
import com.mediatek.media3d.portal.PortalPage;
import com.mediatek.media3d.video.VideoPage;
import com.mediatek.media3d.weather.WeatherPage;
import com.mediatek.ngin3d.Stage;
import com.mediatek.ngin3d.animation.AnimationLoader;

import java.util.Locale;

public class Main extends Activity implements PortalPage.PageQueryCallback {
    private static final String TAG = "Media3D.Main";
    public static final boolean ON_DRAG_MODE = true;

    private Stage mStage;

    private Media3DView mMedia3DView;
    private PortalPage mPortalPage;
    private WeatherPage mWeatherPage;
    private PhotoPage mPhotoPage;
    private VideoPage mVideoPage;
    private GestureDetector mGestureDetector;
    private boolean mIsLaunchedByLandscapeLauncher;

    public Media3DView getMedia3DView() {
        return mMedia3DView;
    }

    public Main() {
        Media3D.setDemoMode(false);
    }

    public PhotoPage getPhotoPage() {
        return mPhotoPage;
    }

    public WeatherPage getWeatherPage() {
        return mWeatherPage;
    }

    public VideoPage getVideoPage() {
        return mVideoPage;
    }

    public PortalPage getPortalPage() {
        return mPortalPage;
    }

    private void loadPage(Page page, Bundle savedInstanceState) {
        if (Media3D.DEBUG) {
            Log.v(TAG, "Load page " + page);
        }

        page.onAttach(this);
        page.onCreate(savedInstanceState);
        mMedia3DView.addPage(page);
        mMedia3DView.loadPage(page);
    }

    private void loadPageAsync(Bundle savedInstanceState, Page... pages) {
        final int count = pages.length;
        for (int i = 0; i < count; i++) {
            pages[i].onAttach(this);
            pages[i].onCreate(savedInstanceState);
            mMedia3DView.addPage(pages[i]);
        }
        mMedia3DView.loadPageAsync(pages);
    }

    private void unloadPage(Page page) {
        if (Media3D.DEBUG) {
            Log.v(TAG, "Unload page " + page);
        }

        mMedia3DView.removePage(page);
        page.onDestroy();
        page.onDetach();
    }

    private LayoutManager mLM;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Media3D.DEBUG) {
            Log.v(TAG, "onCreate()");
        }
        setContentView(R.layout.media3d);

        AnimationLoader.setCacheDir(getCacheDir());

        SimpleVideoView backgroundVideo = (SimpleVideoView) findViewById(R.id.bg_video);

        mGestureDetector = new GestureDetector(this, new MyGestureListener());

        mStage = new Stage();
        mStage.setBackgroundColor(new com.mediatek.ngin3d.Color(0x00, 0x00, 0x00, 0x00));
        mStage.addTextureAtlas(getResources(), R.raw.media3d_atlas, R.raw.media3d);
        mStage.setMaxFPS(60);

        // Media3D was designed with a system that used left-handed coordinates.
        // The graphics engine now uses a conventional right-handed system so we
        // use a 'special' projection to compensate for this.
        mStage.setProjection(Stage.UI_PERSPECTIVE, 2.0f, 3000.0f, -1111.0f);
        mLocale = getResources().getConfiguration().locale;

        mMedia3DView = new Media3DView(this, mStage, backgroundVideo);
        backgroundVideo.setZOrderMediaOverlay(false);
        mMedia3DView.setZOrderMediaOverlay(true);
        mLM = LayoutManager.realize(this, R.xml.attrs);

        mPortalPage = new PortalPage(mStage);
        loadPage(mPortalPage, savedInstanceState);
        mPortalPage.setPageQueryCallback(this);

        mWeatherPage = new WeatherPage(mStage);
        mPhotoPage = new PhotoPage(mStage);
        mVideoPage = new VideoPage(mStage);

        // The three pages onLoad() revised to non-time consuming function,
        // Therefore, change them as synchronous.
        loadPage(mWeatherPage, savedInstanceState);
        loadPage(mPhotoPage, savedInstanceState);
        loadPage(mVideoPage, savedInstanceState);

        FrameLayout frame = (FrameLayout) findViewById(R.id.stage_root);
        frame.addView(mMedia3DView);

        mOrientationListener =
            new OrientationListener(this, SensorManager.SENSOR_DELAY_NORMAL);

        if (mOrientationListener.canDetectOrientation()) {
            mOrientationListener.enable();
        }

        Intent intent = getIntent();
        // Only system send android.intent.action.ROTATED_MAIN and
        // it isn't from history list, we count it into landscape launching.
        if (intent.getAction() != null &&
            intent.getAction().equals("android.intent.action.ROTATED_MAIN") &&
            (intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == 0) {
            mIsLaunchedByLandscapeLauncher = true;
        }

        mMedia3DView.reload(savedInstanceState);
    }

    public LayoutManager getLayoutManager() {
        return mLM;
    }

    private boolean isDeviceLandscape() {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        Log.v(TAG, "isDeviceLandscape : " + rotation);
        return (rotation == Surface.ROTATION_90) ||
               (rotation == Surface.ROTATION_270);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        mMedia3DView.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        if (Media3D.DEBUG) {
            Log.d(TAG, "onDestroy");
        }
        unloadPage(mPortalPage);
        mPortalPage = null;

        unloadPage(mWeatherPage);
        mWeatherPage = null;

        unloadPage(mPhotoPage);
        mPhotoPage = null;

        unloadPage(mVideoPage);
        mVideoPage = null;

        mOrientationListener.disable();
        mOrientationListener = null;

        // Remove references of all actors, especially for Text bitmap
        mStage.removeAll();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Media3D.DEBUG) {
            Log.d(TAG, "onResume");
        }
        if (mOrientationListener != null) {
            mOrientationListener.enable();
        }
        mMedia3DView.onResume();
    }

    @Override
    public void onTrimMemory(int level) {
        if (mMedia3DView != null) {
            Log.v(TAG, "onTrimMemory level : " + level);
            mMedia3DView.saveStatus();
        }
    }

    @Override
    protected void onPause() {
        if (Media3D.DEBUG) {
            Log.d(TAG, "onPause");
        }
        mMedia3DView.onPause();
        if (mOrientationListener != null) {
            mOrientationListener.disable();
        }
        super.onPause();
    }

    private class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        private int getFlingDirection(float velocityX, float velocityY) {
            int direction = FlingEvent.NONE;

            if (Math.abs(velocityX) >= Math.abs(velocityY)) {
                if (velocityX < -FlingEvent.THRESHOLD) {
                    direction = FlingEvent.LEFT;
                } else if (velocityX > FlingEvent.THRESHOLD) {
                    direction = FlingEvent.RIGHT;
                }
            } else {
                if (velocityY > FlingEvent.THRESHOLD) {
                    direction = FlingEvent.DOWN;
                } else if (velocityY < -FlingEvent.THRESHOLD) {
                    direction = FlingEvent.UP;
                }
            }

            if (Media3D.DEBUG) {
                Log.v(TAG, "getFling(): " + direction);
            }
            return direction;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent event) {
            if (Media3D.DEBUG) {
                Log.v(TAG, "onSingleTapConfirmed()");
            }
            return mMedia3DView.onSingleTapConfirmed(event);
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (ON_DRAG_MODE) {
                return false;
            } else {
                int direction = getFlingDirection(velocityX, velocityY);
                mMedia3DView.onFling(direction);
                return direction != FlingEvent.NONE;
            }
        }

        // TODO: Try to use onTouchEvent instead of onScroll
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (ON_DRAG_MODE) {
                return mMedia3DView.onScroll(distanceX, distanceY);
            } else {
                return false;
            }
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent m) {
        boolean handled = super.dispatchTouchEvent(m);
        if (!handled) {
            handled = mGestureDetector.onTouchEvent(m);
        }
        return handled;
    }

    @Override
    public boolean onTouchEvent(MotionEvent m) {
        mMedia3DView.onTouchEvent(m);
        return false;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (Media3D.DEBUG) {
            Log.v(TAG, "onKeyUp - " + keyCode);
        }
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            Page backPage = mMedia3DView.getBackPage();
            if (backPage == null) {
                if (mPortalPage.isShowLoading()) {
                    mPortalPage.cancelLoading();
                    Log.v(TAG, "cancel loading");
                } else {
                    // If user exits Media3D with normal procedure,
                    // last save page in onTrimMemory() doesn't has meaning anymore.
                    // Therefore, clean up saved page.
                    mMedia3DView.clearStatus();
                    onBackPressed();
                    Log.v(TAG, "back key");
                }
            } else {
                mMedia3DView.enterPage(backPage);
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_MENU) {
            return mMedia3DView.onBarShowHide();
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mMedia3DView.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    Locale mLocale;
    private boolean isLocaleChanged(Locale locale) {
        if (mLocale != locale) {
            mLocale = locale;
            return true;
        }
        return false;
    }

    private void updateLocale(Locale locale) {
        if (isLocaleChanged(locale)) {
            mPortalPage.updateLocale(locale);
            mWeatherPage.updateLocale(locale);
            mPhotoPage.updateLocale(locale);
            mVideoPage.updateLocale(locale);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        if (Media3D.DEBUG) {
            Log.v(TAG, "onConfigurationChanged");
        }

        // handle configuration change to avoid drawing on lock screen
        // and the surface memory which makes gpu oom.
        if (mMedia3DView == null) {
            return;
        }

        updateLocale(config.locale);

        mMedia3DView.onConfigurationChanged(config);
        mMedia3DView.setVisibility(View.VISIBLE);
    }

    public Page queryWeatherPage() {
        return getWeatherPage();
    }

    public Page queryPhotoPage() {
        return getPhotoPage();
    }

    public Page queryVideoPage() {
        return getVideoPage();
    }

    private OrientationListener mOrientationListener;
    private final class OrientationListener extends OrientationEventListener {
        private static final int ORIENTATION_UNKNOWN = -1;
        private static final int ORIENTATION_PORTRAIT = 0;
        private static final int ORIENTATION_LANDSCAPE = 1;
        private int mLaunchOrientation = ORIENTATION_UNKNOWN;

        public OrientationListener(Context context, int rate) {
            super(context, rate);
        }

        @Override
        public void onOrientationChanged(int degree) {
            if (mIsLaunchedByLandscapeLauncher &&
                isPortraitScope(degree)) {
                // If user exits Media3D with rotation close, regarding it as normal exit.
                // Therefore, clean up saved page.
                mMedia3DView.clearStatus();
                finish();
            }
        }

        private boolean isBetween(int x, int lower, int upper) {
            return (x >= lower && x <= upper);
        }

        private boolean isPortraitScope(int degree) {
            return isBetween(degree, 0, 10) || isBetween(degree, 350, 359);
        }
    }
}