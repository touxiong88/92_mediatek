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

package com.mediatek.weather3dwidget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RemoteViews.RemoteView;

import com.mediatek.common.widget.IMtkWidget;

@RemoteView
public class WeatherWidgetView extends FrameLayout implements IMtkWidget {
    private static final String TAG = "W3D/WeatherWidgetView";

    private static final int SCREEN_UNKNOWN = -10000;

    private WeatherView mWeatherView;
    private ImageView mImageView;

    private int mAppWidgetScreen = SCREEN_UNKNOWN;
    private int mAppWidgetId = -1;

    private int mLauncherState = 0;
    private static final int LAUNCHER_STATE_START_DRAG = 1;
    private static final int LAUNCHER_STATE_STOP_DRAG = -1;
    private static final int LAUNCHER_STATE_MOVE_OUT = 2;
    private static final int LAUNCHER_STATE_MOVE_IN = -2;
    private static final int LAUNCHER_STATE_START_COVERED = 3;
    private static final int LAUNCHER_STATE_STOP_COVERED = -3;
    private static final int LAUNCHER_STATE_PAUSE = 4;
    private static final int LAUNCHER_STATE_RESUME = -4;

    public WeatherWidgetView(Context context) {
        super(context);
        LogUtil.v(TAG, "WeatherWidgetView - 1");
    }

    public WeatherWidgetView(Context context, AttributeSet attrs) {
        super(context, attrs);
        LogUtil.v(TAG, "WeatherWidgetView - 2");
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        LogUtil.i(TAG, "onFinishInflate");

        mWeatherView = (WeatherView)this.findViewWithTag("weather_view");
        mImageView = (ImageView)this.findViewWithTag("snapshot");
        mImageView.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        LogUtil.v(TAG, "onAttachedToWindow() - id = " + mAppWidgetId);
        if (getWidgetId() != AppWidgetManager.INVALID_APPWIDGET_ID) {
            // send out intent to notify WeatherWidget -> UpdateService for ready to update screen
            sendOnAttachedIntent();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mWeatherView.setVisibility(View.INVISIBLE);
        LogUtil.v(TAG, "onDetachedFromWindow() - id = " + mAppWidgetId);
    }

    private void sendOnAttachedIntent() {
        LogUtil.v(TAG, "sendOnAttachedIntent, id = " + getWidgetId());
        Intent intent = new Intent(WeatherWidgetAction.ACTION_WEATHER_WIDGET_VIEW_ATTACH);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, getWidgetId());
        getContext().sendBroadcast(intent);
        mWeatherView.setVisibility(View.VISIBLE);
    }

    /**
     * The count could be installed in launcher.
     * @return
     */
    public int getPermittedCount() {
        LogUtil.i(TAG, "getPermittedCount");
        return 1;
    }

    /**
     * The screen index of current AppWidget.
     * @return
     */
    public int getScreen() {
        LogUtil.i(TAG, "getScreen = " + mAppWidgetScreen);
        return mAppWidgetScreen;
    }

    /**
     * Set the screen index of current AppWidget.
     * @param screen
     */
    public void setScreen(int screen) {
        LogUtil.i(TAG, "setScreen(" + screen + "), mAppWidgetScreen = " + mAppWidgetScreen);
        mAppWidgetScreen = screen;
    }

    /**
     * The AppWidgetId of current AppWidget.
     * @return
     */
    public int getWidgetId() {
        LogUtil.i(TAG, "getWidgetId = " + mAppWidgetId);
        return mAppWidgetId;
    }

    /**
     * Set the AppWidgetId of current AppWidget.
     * @param widgetId
     */
    public void setWidgetId(int widgetId) {
        mAppWidgetId = widgetId;
        LogUtil.i(TAG, "setWidgetId(" + widgetId + ")");
    }

    private boolean startPause(int curScreen) {
        LogUtil.i(TAG, "startPause() - curScreen = " + curScreen);
        //when in move state,
        //launcher will use the return value of moveOut().
        mWeatherView.pauseRendering();
        LogUtil.i(TAG, "startPause, pauseRendering");
        mWeatherView.setVisibility(View.INVISIBLE);

        Bitmap bitmap = mWeatherView.getBitmap();
        mImageView.setImageBitmap(bitmap);
        LogUtil.i(TAG, "startPause, setImageBitmap");
        mImageView.setVisibility(View.VISIBLE);
        return true;
    }

    private void stopPause(int curScreen) {
        LogUtil.i(TAG, "stopPause() - curScreen = " + curScreen);

        mImageView.setVisibility(View.INVISIBLE);
        LogUtil.i(TAG, "stopPause, ImageView - INVISIBLE");
        mWeatherView.resumeRendering();
        LogUtil.i(TAG, "stopPause, resumeRendering");
        mWeatherView.setVisibility(View.VISIBLE);
        LogUtil.i(TAG, "stopPause, WeatherView - VISIBLE");
        // resume 3D Stage View again
        LogUtil.i(TAG, "stop pause ");
    }

    private void pauseWeatherViewRendering() {
        mWeatherView.pauseRendering();
        LogUtil.i(TAG, "pauseWeatherViewRendering, WeatherView - INVISIBLE");
    }

    private void resumeWeatherViewRendering() {
        mWeatherView.resumeRendering();
        mImageView.setVisibility(View.INVISIBLE);
        mWeatherView.setVisibility(View.VISIBLE);
        LogUtil.i(TAG, "resumeWeatherViewRendering, WeatherView - VISIBLE");
    }

    /**
     * Will be called when user start to drag current AppWidget.
     */
    public void startDrag() {
        LogUtil.i(TAG, "startDrag(), mLauncherState = " + mLauncherState);

        if (mLauncherState == 0) {
            mLauncherState = LAUNCHER_STATE_START_DRAG;
            startPause(mAppWidgetScreen);
        } else if (mLauncherState > 0) {
            mLauncherState = LAUNCHER_STATE_START_DRAG;
            LogUtil.i(TAG, "change mLauncherState to " + mLauncherState);
        } else {
            //do nothing when it is not correct.
            LogUtil.w(TAG, "handle startDrag. mLauncherState = " + mLauncherState);
        }
    }

    /**
     * Will be called when user stop to drag current AppWidget.
     */
    public void stopDrag() {
        LogUtil.i(TAG, "stopDrag(), mLauncherState = " + mLauncherState);

        if ((mLauncherState + LAUNCHER_STATE_STOP_DRAG) == 0) {
            mLauncherState = 0;
            stopPause(mAppWidgetScreen);
        } else {
            //do nothing when it is not correct.
            LogUtil.w(TAG, "handle stopDrag. mLauncherState = " + mLauncherState);
        }
    }

    /**
     * Will be called when user leave the screen which current AppWidget locates in.
     * @param curScreen which side's screen user will be seen.
     * -1 means move to left, +1 means move to right.
     * @return if IMtkWidget's implemention is ready for moving out, it will return true.
     * otherwise, return false.
     * <br/>Note: while return true, the Launcher will
     */
    public boolean moveOut(int curScreen) {
        LogUtil.i(TAG, "moveOut(" + curScreen + "), mLauncherState = " + mLauncherState);
        return true;
    }

    /**
     * Will be called when the screen which AppWidget locates in will be seen by user.
     * @param curScreen the screen AppWidget locates in.
     */
    public void moveIn(int curScreen) {
        LogUtil.i(TAG, "moveIn(" + curScreen + ") , mLauncherState = " + mLauncherState);
    }

    /**
     * Will be called when the current AppWidget will be not seen
     * before launcher makes other views cover the current AppWidget.
     * @param curScreen
     */
    public void startCovered(int curScreen) {
        LogUtil.i(TAG, "startCovered(" + curScreen + "), mLauncherState = " + mLauncherState);
        if (mLauncherState == 0) {
            mLauncherState = LAUNCHER_STATE_START_COVERED;
            pauseWeatherViewRendering();
        } else if (mLauncherState > 0) {
            mLauncherState = LAUNCHER_STATE_START_COVERED;
            LogUtil.i(TAG, "change mLauncherState to " + mLauncherState);
        } else {
            //do nothing when it is not correct.
            LogUtil.w(TAG, "handle startCovered. mLauncherState = " + mLauncherState);
        }
    }

    /**
     * Will be called when the current AppWidget will be seen
     * after launcher moves away other views on the top of current AppWidget.
     * @param curScreen
     */
    public void stopCovered(int curScreen) {
        LogUtil.i(TAG, "stopCovered(" + curScreen + "), mLauncherState = " + mLauncherState);
        if ((mLauncherState + LAUNCHER_STATE_STOP_COVERED) == 0) {
            mLauncherState = 0;
            resumeWeatherViewRendering();
        } else {
            //do nothing when it is not correct.
            LogUtil.w(TAG, "handle stopCovered. mLauncherState = " + mLauncherState);
        }
    }

    /**
     * Will be called when launcher's onPause be called.
     * @param curScreen
     */
    public void onPauseWhenShown(int curScreen) {
        LogUtil.i(TAG, "onPauseWhenShown(" + curScreen + "), mLauncherState = " + mLauncherState);
        if (mLauncherState == 0) {
            mLauncherState = LAUNCHER_STATE_PAUSE;
            pauseWeatherViewRendering();
        } else if (mLauncherState > 0) {
            mLauncherState = LAUNCHER_STATE_PAUSE;
            LogUtil.i(TAG, "change mLauncherState to " + mLauncherState);
        } else {
            //do nothing when it is not correct.
            LogUtil.w(TAG, "handle onPauseWhenShown. mLauncherState = " + mLauncherState);
        }
    }

    /**
     * Will be called when launcher's onResume be called.
     * @param curScreen
     */
    public void onResumeWhenShown(int curScreen) {
        LogUtil.i(TAG, "onResumeWhenShown(" + curScreen + "), mLauncherState = " + mLauncherState);
        if ((mLauncherState + LAUNCHER_STATE_RESUME) == 0) {
            mLauncherState = 0;
            resumeWeatherViewRendering();
        } else {
            //do nothing when it is not correct.
            LogUtil.w(TAG, "handle onResumeWhenShown. mLauncherState = " + mLauncherState);
        }
    }

    /**
     * Will be called after launcher's onSaveInstanceState is called.
     * @param outSate
     */
    public void onSaveInstanceState(Bundle outSate) {
        LogUtil.i(TAG, "onSaveInstanceState");
    }

    /**
     * Will be called after launcher's onRestoreInstanceState is called.
     * @param state
     */
    public void onRestoreInstanceState(Bundle state) {
        LogUtil.i(TAG, "onRestoreInstanceState");
    }

    public void leaveAppwidgetScreen() {
        LogUtil.i(TAG, "leaveAppwidgetScreen");
        pauseWeatherViewRendering();
    }

    public void enterAppwidgetScreen() {
        LogUtil.i(TAG, "enterAppwidgetScreen");
        resumeWeatherViewRendering();
        mLauncherState = 0;
    }
}

