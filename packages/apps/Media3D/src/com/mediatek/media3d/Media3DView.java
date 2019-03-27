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
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Process;
import android.util.Log;
import android.util.Pair;
import android.view.MotionEvent;
import com.mediatek.media3d.photo.PhotoPage;
import com.mediatek.media3d.portal.PortalPage;
import com.mediatek.media3d.video.VideoPage;
import com.mediatek.media3d.weather.WeatherPage;
import com.mediatek.ngin3d.Actor;
import com.mediatek.ngin3d.Color;
import com.mediatek.ngin3d.Dimension;
import com.mediatek.ngin3d.Point;
import com.mediatek.ngin3d.Stage;
import com.mediatek.ngin3d.android.StageView;
import com.mediatek.ngin3d.animation.Animation;
import com.mediatek.ngin3d.animation.AnimationGroup;
import com.mediatek.ngin3d.animation.AnimationLoader;
import com.mediatek.ngin3d.animation.Mode;
import com.mediatek.ngin3d.animation.PropertyAnimation;

import javax.microedition.khronos.opengles.GL10;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Media3DView extends StageView {
    private static final String TAG = "Media3D.Media3DView";

    private final SimpleVideoView mBackgroundVideo;
    private VideoPlayer mVideoPlayer;
    private final List<Page> mPages = new ArrayList<Page>();
    private final Context mContext;

    private Page mCurrentPage;
    private Page mOldPage;
    private final NavigationBar mNavigationBar;
    private final ToolBar mToolBar;

    private static final float DEFAULT_Z_NEAR = 100.f;
    private static final float DEFAULT_Z_FAR = 3000.f;
    private static final float CAMERA_Z_POS = -1111;

    public static final int STATE_IDLE = 0;
    public static final int STATE_LEAVING_OLD_PAGE = 1;
    public static final int STATE_ENTERING_NEW_PAGE = 2;
    public static final int STATE_STOPPING_PAGE = 3;

    private boolean mStereo3DMode;
    private int mState;
    private int mLeavingTransitionType;
    private final HashMap<String, Animation> mAnimations = new HashMap<String, Animation>();

    public static final int BAR_STATE_HIDDEN = 0;
    public static final int BAR_STATE_ENTERING = 1;
    public static final int BAR_STATE_ENTERED = 2;
    public static final int BAR_STATE_ABOUT_TO_HIDE = 3;

    private static final int DELAY_HIDE_BAR = 3000;
    private static final int DELAY_ABOUT_TO_HIDE_BAR = 300;

    private static final Color TRANSPARENT_COLOR = new Color(0xFF, 0xFF, 0xFF, 0xFF);
    private static final Color OPAQUE_BLACK = new Color(0x00, 0x00, 0x00, 0xFF);
    private static final int TRANSITION_DURATION = 200;

    private int mBarState;
    private final PageDragHelper mPageDragHelper = new PageDragHelper();

    public Page getCurrentPage() {
        return mCurrentPage;
    }

    public Media3DView(Context context, Stage stage, SimpleVideoView videoView) {
        super(context, stage);
        mContext = context;
        mBackgroundVideo = videoView;
        mState = STATE_IDLE;

        getStage().setProjection(Stage.UI_PERSPECTIVE,
                DEFAULT_Z_NEAR, DEFAULT_Z_FAR, CAMERA_Z_POS);

        mToolBar = new ToolBar(context.getResources(), new ToolBar.Listener() {
            public void onButtonPressed(int buttonTag) {
                if (buttonTag == ToolBar.TAG_HOME) {
                    hideBarImmediately();
                    mPageHost.enterPage(PageHost.PORTAL);
                } else if (buttonTag == ToolBar.TAG_STEREO3D) {
                    enableStereo3DMode(true);
                    mToolBar.show3DIcon(false);
                } else if (buttonTag == ToolBar.TAG_NORMAL2D) {
                    enableStereo3DMode(false);
                    mToolBar.show3DIcon(true);
                }
            }

            public void onMenuItemSelected(NavigationBarMenuItem item) {
                hideBarImmediately();
                mCurrentPage.onBarMenuItemSelected(item);
            }

            public void onTimerStopped() {
                mHandler.removeCallbacks(mHideBarsRunnable);
            }

            public void onTimerStarted() {
                mHandler.removeCallbacks(mHideBarsRunnable);
                mHandler.postDelayed(mHideBarsRunnable, DELAY_HIDE_BAR);
            }
        });
        mToolBar.setVisible(false);
        getStage().add(mToolBar);

        mNavigationBar = new NavigationBar(context.getResources(), new NavigationBar.Listener() {
            public void onPageSelected(String page) {
                hideBarImmediately();
                mPageHost.enterPage(page);
            }

            public void onTimerStopped() {
                mHandler.removeCallbacks(mHideBarsRunnable);
            }

            public void onTimerStarted() {
                mHandler.removeCallbacks(mHideBarsRunnable);
                mHandler.postDelayed(mHideBarsRunnable, DELAY_HIDE_BAR);
            }
        });
        mNavigationBar.setVisible(false);
        getStage().add(mNavigationBar);
        generateNavigationAnimations();
    }

    public void generateNavigationAnimations() {
        final int duration = 330;
        Dimension screenDim = LayoutManager.getScreenResolution(getResources());
        Point topStart = new Point(screenDim.width/2, -(mToolBar.getHeight()/2), 0);
        Point topEnd = new Point(screenDim.width/2, (mToolBar.getHeight()/2), 0);
        PropertyAnimation topIn = new PropertyAnimation(mToolBar, "position", topStart, topEnd);
        topIn.setDuration(duration);

        Point bottomStart = new Point(screenDim.width/2, screenDim.height + (mNavigationBar.getHeight()/2), 0);
        Point bottomEnd = new Point(screenDim.width/2, screenDim.height - (mNavigationBar.getHeight()/2), 0);
        PropertyAnimation bottomIn = new PropertyAnimation(mNavigationBar, "position", bottomStart, bottomEnd);
        bottomIn.setDuration(duration);

        AnimationGroup barIn = new AnimationGroup();
        barIn.add(topIn).add(bottomIn).addListener(mAnimationListener);
        mAnimations.put("bar_in", barIn);
    }

    public class VideoPlayer implements VideoBackground {
        private boolean mIsVideoLoaded;
        private final Uri mUri;
        private final PropertyAnimation mFadeOut;
        private final PropertyAnimation mFadeIn;

        VideoPlayer(int resId) {
            mUri = Uri.parse("android.resource://com.mediatek.media3d/" + resId);
            final Color transparent = new Color(0x00, 0x00, 0x00, 0x00);
            final Color opaque = new Color(0x00, 0x00, 0x00, 0xFF);

            mFadeIn = new PropertyAnimation(getStage(), "background_color", opaque, transparent);
            mFadeIn.setDuration(TRANSITION_DURATION);

            mFadeOut = new PropertyAnimation(getStage(), "background_color", transparent, opaque);
            mFadeOut.setDuration(TRANSITION_DURATION);
        }

        public void setupSeekTable(HashMap<Integer,Pair<Integer, Integer>> seekTable) {
            if (!mIsVideoLoaded) {
                mBackgroundVideo.setLooping(true);
                mBackgroundVideo.setVideoURI(mUri, seekTable);
                mIsVideoLoaded = true;
            }
        }

        public void play(int segmentId) {
            mBackgroundVideo.start(segmentId);
            mFadeIn.start();
        }

        public void play(int startMsec, int endMsec) {
            mBackgroundVideo.seekTo(startMsec);
            mBackgroundVideo.start();
        }

        public void pause() {
            if (mBackgroundVideo.isPlaying()) {
                mFadeOut.start();
                mBackgroundVideo.pause();
            }
        }

        public void stop() {
            mFadeOut.start();
            mBackgroundVideo.stopPlayback();
            mIsVideoLoaded = false;
        }
    }

    private final PageHost mPageHost = new PageHost() {
        public void onPageLeft(Page page) {
            if (Media3D.DEBUG) {
                Log.v(TAG, "onPageLeft: " + page);
            }

            if (mState == STATE_LEAVING_OLD_PAGE || mState == STATE_STOPPING_PAGE) {
                if (mCurrentPage.getHost() == null) {
                    mState = STATE_IDLE;
                    if (Media3D.DEBUG) {
                        Log.v(TAG, "Entering new page: " + mCurrentPage + " fail, host = null");
                    }
                } else {
                    if (Media3D.DEBUG) {
                        Log.v(TAG, "Entering new page: " + mCurrentPage);
                    }
                    mState = STATE_ENTERING_NEW_PAGE;
                    mOldPage = page;
                    mCurrentPage.enter(mLeavingTransitionType);
                }
            } else {
                throw new IllegalStateException("onPageLeft called in wrong state: " + mState);
            }
        }

        public void onPageEntered(Page page) {
            if (Media3D.DEBUG) {
                Log.v(TAG, "onPageEntered: " + page);
            }

            if (mState == STATE_ENTERING_NEW_PAGE) {
                assert (mCurrentPage.equals(page));
                if (Media3D.DEBUG) {
                    Log.v(TAG, "Back to idle state");
                }
                mState = STATE_IDLE;
            } else {
                throw new IllegalStateException("onPageEntered called in wrong state: " + mState);
            }
        }

        public VideoPlayer setVideoBackground(int resId, HashMap<Integer,Pair<Integer, Integer>> seekTable) {
            if (mVideoPlayer == null) {
                mVideoPlayer = new VideoPlayer(resId);
            }
            mVideoPlayer.setupSeekTable(seekTable);
            return mVideoPlayer;
        }

        public void enterPage(String pageName) {
            Media3DView.this.enterPage(getPageFromName(pageName));
        }

        public Actor getThumbnailActor(String pageName) {
            if (pageName.equals(PageHost.PORTAL)) {
                throw new IllegalArgumentException("Unknown page name: " + pageName);
            }
            return getPageFromName(pageName).getThumbnailActor();
        }

        public Page getOldPage() {
            return mOldPage;
        }

        public Page getPage(String pageName) {
            return getPageFromName(pageName);
        }

        public int isPageEqual(Page page, String pageName) {
            if (Media3D.DEBUG) {
                Log.v(TAG, "isPageEqual, page = " + page + " , name = " + pageName);
            }
            Page basePage = getPageFromName(pageName);
            return comparePage(page, basePage);
        }
    };

    public void onFling(int direction) {
        if (!mCurrentPage.isOptionsSupported(Page.SUPPORT_FLING)) {
            if (Media3D.DEBUG) {
                Log.v(TAG, "onFling not respond when in Portal Page");
            }
            return;
        }

        if (mBarState != BAR_STATE_HIDDEN) {
            if (Media3D.DEBUG) {
                Log.v(TAG, "onFling not respond when bar is in");
            }
            return;
        }

        if (direction == FlingEvent.UP || direction == FlingEvent.DOWN) {
            if (mState == STATE_IDLE) {
                mCurrentPage.onFling(direction);
            } else {
                if (Media3D.DEBUG) {
                    Log.v(TAG, "onFling up and down when not in STATE_IDLE");
                }
            }
        } else {
            if (direction == FlingEvent.LEFT) {
                enterPage(getNextPage(), Page.TransitionType.INNER_TO_LEFT);
            } else if (direction == FlingEvent.RIGHT) {
                enterPage(getPrevPage(), Page.TransitionType.INNER_TO_RIGHT);
            }
        }
    }

    public void addPageAsync(Page page) {
        mPages.add(page);

        final Page finalPage = page;
        final PageHost finalPageHost = mPageHost;
        new Thread(new Runnable() {
            public void run() {
                Log.v(TAG, "add sync start: " + finalPage);
                finalPage.onAdded(finalPageHost);
                Log.v(TAG, "add sync done: " + finalPage);
            }
        }).start();
    }

    public void addPage(Page page) {
        mPages.add(page);
        page.onAdded(mPageHost);
    }

    public void loadPage(Page page) {
        page.onLoad();
    }

    private static class PageLoader extends AsyncTask<Page, Void, Integer> {

        private void loadPage(Page... pages) {
            final int count = pages.length;
            for (int i = 0; i < count; i++) {
                pages[i].onLoad();
            }
        }

        protected Integer doInBackground(Page... pages) {
            if (pages.length < 0 || pages[0] == null) {
                Log.e(TAG, "pages terminated before loading");
                return 0;
            }

            int oldPriority;
            oldPriority = Process.getThreadPriority(Process.myTid());
            Log.v(TAG, "priority of bg task is: " + oldPriority);
            Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);

            loadPage(pages);

            Process.setThreadPriority(oldPriority);
            return 1;
        }
    }

    private PageLoader mPageLoader;

    public void loadPageAsync(Page... pages) {
        if (mPageLoader != null) {
            mPageLoader.cancel(true);
        }
        mPageLoader = new PageLoader();
        mPageLoader.execute(pages);
    }

    public void removePage(Page page) {
        if (mPageLoader != null) {
            mPageLoader.cancel(true);
            mPageLoader = null;
        }

        if (mPages.remove(page)) {
            page.onRemoved();
        }

        // Clean up all queued runnable and messages
        // Since application is ready to destroy.
        mHandler.removeCallbacksAndMessages(null);
    }

    private int comparePage(Page targetPage, Page basePage) {
        int targetIndex = mPages.indexOf(targetPage);
        int baseIndex = mPages.indexOf(basePage);
        return (targetIndex - baseIndex);
    }

    private int comparePage(Page targetPage) {
        return comparePage(targetPage, mCurrentPage);
    }

    public void enterPage(Page newPage) {
        enterPage(newPage, getTransitionType(newPage));
    }

    private int getTransitionType(Page newPage) {
        int transitionType = Page.TransitionType.NO_TRANSITION;

        if (newPage != null && !newPage.equals(mCurrentPage)) {
            if (mCurrentPage == null && newPage.getPageType() == Page.PageType.PORTAL) {
                transitionType = Page.TransitionType.LAUNCH_PORTAL;
            } else if (mCurrentPage != null) {
                if (mCurrentPage.getPageType() == Page.PageType.PORTAL || newPage.getPageType() == Page.PageType.PORTAL) {
                    transitionType = Page.TransitionType.PORTAL_INNER;
                } else if (comparePage(newPage) >= 0) {
                    transitionType = Page.TransitionType.INNER_TO_LEFT;
                } else {
                    transitionType = Page.TransitionType.INNER_TO_RIGHT;
                }
            }
        }
        if (Media3D.DEBUG) {
            Log.v(TAG, "getTransitionType = " + transitionType);
        }
        return transitionType;
    }

    PropertyAnimation mStereoFadeIn = new PropertyAnimation(getStage(), "color", OPAQUE_BLACK, TRANSPARENT_COLOR);
    public void enableStereo3DMode(boolean enable) {
        mStereoFadeIn.setDuration(TRANSITION_DURATION).setMode(Mode.LINEAR);
        mStereoFadeIn.addListener(mAnimationCompletedHandler);
        mStereoFadeIn.setTag(sStereoTag);

        mStereo3DMode = enable;
        if (mCurrentPage instanceof WeatherPage) {
            ((WeatherPage) mCurrentPage).stopBackgroundVideo();
        }
        mStage.setColor(OPAQUE_BLACK);

        if (enable) {
            postDelayed(new Runnable() {
                public void run() {
                    setStereo3D(mCurrentPage);
                    mStereoFadeIn.start();
                }
            }, TRANSITION_DURATION);
        } else {
            postDelayed(new Runnable() {
                public void run() {
                    Stereo3DWrapper.set3DLayout(Media3DView.this, Stereo3DWrapper.LAYOUT3D_DISABLED);
                    enableStereoscopic3D(false);
                    mStereoFadeIn.start();
                }
            }, TRANSITION_DURATION);
        }
    }

    private void setStereo3D(Page page) {
        if (page instanceof PortalPage) {
            Stereo3DWrapper.set3DLayout(this, Stereo3DWrapper.LAYOUT3D_SIDE_BY_SIDE);
            enableStereoscopic3D(true, 1800);
        } else if (page instanceof WeatherPage) {
            Stereo3DWrapper.set3DLayout(this, Stereo3DWrapper.LAYOUT3D_SIDE_BY_SIDE);
            enableStereoscopic3D(true, 1800);
        } else if (page instanceof PhotoPage) {
            Stereo3DWrapper.set3DLayout(this, Stereo3DWrapper.LAYOUT3D_SIDE_BY_SIDE);
            enableStereoscopic3D(true, 1800);
        } else if (page instanceof VideoPage) {
            Stereo3DWrapper.set3DLayout(this, Stereo3DWrapper.LAYOUT3D_SIDE_BY_SIDE);
            enableStereoscopic3D(true, 1800);
        }
    }

    private void enterPage(Page newPage, int transitionType) {
        if (Media3D.DEBUG) {
            Log.v(TAG, "enterPage: " + newPage + ", transitionType = " + transitionType);
        }

        if (mStereo3DMode) {
            setStereo3D(newPage);
        }

        if (newPage == null || newPage.equals(mCurrentPage) || transitionType == Page.TransitionType.NO_TRANSITION) {
            return;
        }

        hideBarImmediately();

        switch (mState) {
        case STATE_IDLE:
            if (mCurrentPage == null) {
                if (newPage.getHost() == null) {
                    mState = STATE_IDLE;
                    mOldPage = mCurrentPage;
                    mCurrentPage = newPage;
                    if (Media3D.DEBUG) {
                        Log.v(TAG, "Entering page: " + newPage + " fail, host = null");
                    }
                } else {
                    if (Media3D.DEBUG) {
                        Log.v(TAG, "Entering page: " + newPage);
                    }
                    mState = STATE_ENTERING_NEW_PAGE;
                    mOldPage = mCurrentPage;
                    mCurrentPage = newPage;
                    mCurrentPage.enter(transitionType);
                }
            } else {
                Page oldPage = mCurrentPage;
                if (Media3D.DEBUG) {
                    Log.v(TAG, "Leaving page: " + oldPage);
                }
                mState = STATE_LEAVING_OLD_PAGE;
                mCurrentPage = newPage;
                mLeavingTransitionType = transitionType;
                oldPage.leave(transitionType);
            }
            break;

        case STATE_ENTERING_NEW_PAGE:
            if (transitionType == mLeavingTransitionType) {
                Log.w(TAG, "Should enter in opposite direction while the old page is entering");
                // throw new IllegalStateException("Should enter in opposite direction while the old page is entering");
            }

            mState = STATE_STOPPING_PAGE;
            Page oldPage = mCurrentPage;
            if (Media3D.DEBUG) {
                Log.v(TAG, "Stopping page: " + oldPage);
            }
            mCurrentPage = newPage;
            mLeavingTransitionType = transitionType;
            oldPage.leave(transitionType);
            break;

        case STATE_STOPPING_PAGE:
        case STATE_LEAVING_OLD_PAGE:
            if (transitionType != mLeavingTransitionType) {
                Log.w(TAG, "Cannot change direction while old page is leaving");
                // throw new IllegalStateException("Cannot change direction while old page is leaving");
            }

            if (Media3D.DEBUG) {
                Log.v(TAG, "Change new page to: " + newPage);
            }
            mCurrentPage = newPage; // just update the current page
            break;

        default:
            if (Media3D.DEBUG) {
                Log.v(TAG, "Ignore enterPage in state: " + mState);
            }
            break;
        }
    }

    private Page getNextPage() {
        if (mCurrentPage.isOptionsSupported(Page.SUPPORT_FLING)) {
            final int n = mPages.size();
            for (int i = 0, j = mPages.indexOf(mCurrentPage) + 1; i < n; i++, j++) {
                if (j >= n) {
                    j -= n;
                }
                if (mPages.get(j).getPageType() == Page.PageType.CIRCULAR) {
                    return mPages.get(j);
                }
            }
        }
        return null;
    }

    private Page getPrevPage() {
        if (mCurrentPage.isOptionsSupported(Page.SUPPORT_FLING)) {
            final int n = mPages.size();
            for (int i = 0, j = mPages.indexOf(mCurrentPage) - 1; i < n; i++, j--) {
                if (j < 0) {
                    j += n;
                }
                if (mPages.get(j).getPageType() == Page.PageType.CIRCULAR) {
                    return mPages.get(j);
                }
            }
        }
        return null;
    }

    public Page getBackPage() {
        Page page = null;
        if (mCurrentPage.getPageType() == Page.PageType.CIRCULAR) {
            for (Page p : mPages) {
                if (p.getPageType() == Page.PageType.PORTAL) {
                    page = p;
                }
            }
        }
        if (Media3D.DEBUG) {
            Log.v(TAG, "getBackPage(), page = " + page);
        }
        return page;
    }

    private Page getPageFromName(String pageName) {
        if (pageName.equals(PageHost.PORTAL)) {
            return mPages.get(0);
        } else if (pageName.equals(PageHost.WEATHER)) {
            return mPages.get(1);
        } else if (pageName.equals(PageHost.PHOTO)) {
            return mPages.get(2);
        } else if (pageName.equals(PageHost.VIDEO)) {
            return mPages.get(3);
        } else {
            throw new IllegalArgumentException("Unknown page name: " + pageName);
        }
    }

    private String getNameFromPage(Page page) {
        if (page == mPages.get(0)) {
            return PageHost.PORTAL;
        } else if (page == mPages.get(1)) {
            return PageHost.WEATHER;
        } else if (page == mPages.get(2)) {
            return PageHost.PHOTO;
        } else if (page == mPages.get(3)) {
            return PageHost.VIDEO;
        }
        return PageHost.PORTAL;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        mCurrentPage.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        super.onSurfaceChanged(gl, width, height);
        // While resuming from screenlock unlock, onSurfaceChanged() will be called twice.
        // First, the surface from WindowManager is portrait size
        // and then changes to landscape size surface in second time.
        // Because media3d is designed for landscape mode,
        // the actors position will be so wrong on portrait surface.
        // Thus, reveal stage only if landscape mode.surface is given.
        mStage.setVisible(width > height);
        adjustLayout(width, height);
    }

    public void adjustLayout(int width, int height) {
        mToolBar.adjustLayout(width);
    }

    public boolean onTouchEvent(MotionEvent event) {
        // if there is bar, then bar handle touch event first.
        if (mBarState == BAR_STATE_ENTERED) {
            if (mNavigationBar.onTouchEvent(event) || mToolBar.onTouchEvent(event)) {
                return true;
            }
        }

        if (Main.ON_DRAG_MODE) {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                // PageDragHelper handles ACTION_UP event first.
                // Dispatch it to others when direction is NONE only.
                // Other direction is handled by PageDragHelper.handleActionUp.
                PageDragHelper.Direction dir = mPageDragHelper.handleActionUp(mCurrentPage, this);
                if (dir != PageDragHelper.Direction.NONE) {
                    return true;
                }
            }
        }

        return mCurrentPage.onTouchEvent(event);
    }

    public boolean onSingleTapConfirmed(MotionEvent event) {
        if (Media3D.DEBUG) {
            Log.v(TAG, "onSingleTapConfirmed, action = " + event.getAction());
        }

        boolean handled = false;
        if (mBarState == BAR_STATE_HIDDEN) {
            handled = mCurrentPage.onSingleTapConfirmed(event);
        }
        if (!handled && mState == STATE_IDLE) {
            // could only show Bar when the page state is idle
            handled = onBarShowHide();
        }

        return handled;
    }

    public boolean onScroll(float disX, float disY) {
        if (mBarState != BAR_STATE_HIDDEN) {
            if (Media3D.DEBUG) {
                Log.v(TAG, "onScroll not respond when bar is in");
            }
            return true;
        }

        if (mState == STATE_IDLE) {
            return mPageDragHelper.onScroll(disX, disY, mCurrentPage, this);
        }
        return false;
    }

    public boolean onBarShowHide() {
        if (mCurrentPage != getPageFromName(PageHost.PORTAL)) {
            switch (mBarState) {
            case BAR_STATE_HIDDEN:
                showBars();
                return true;

            case BAR_STATE_ENTERED:
                mHandler.removeCallbacks(mHideBarsRunnable);
                hideBars();
                return true;

            case BAR_STATE_ENTERING:
                showBarsReverse();
                return true;

            case BAR_STATE_ABOUT_TO_HIDE:
            default:
                break;
            }
        }
        return false;
    }

    private final Handler mHandler = new Handler();
    private final Runnable mHideBarsRunnable = new Runnable() {
        public void run() {
            hideBars();
        }
    };

    private final Runnable mSetBarHiddenRunnable = new Runnable() {
        public void run() {
            mBarState = BAR_STATE_HIDDEN;
        }
    };

    private void showBars() {
        if (Media3D.DEBUG) {
            Log.v(TAG, "showBars");
        }
        mToolBar.getNavigationMenu().clear();
        mCurrentPage.onCreateBarMenu(mToolBar.getNavigationMenu());
        mToolBar.fillMenuIcon();

        mNavigationBar.setVisible(true);
        mToolBar.setVisible(true);
        Animation ani = mAnimations.get("bar_in");
        if (ani != null) {
            ani.setDirection(Animation.FORWARD);
            ani.start();
        }

        mBarState = BAR_STATE_ENTERING;
        if (Media3D.DEBUG) {
            Log.v(TAG, "barState = " + mBarState);
        }
    }

    private void showBarsReverse() {
        if (Media3D.DEBUG) {
            Log.v(TAG, "showBarsReverse");
        }
        Animation ani = mAnimations.get("bar_in");
        if (ani != null) {
             ani.reverse();
        }
    }

    private void hideBars() {
        if (Media3D.DEBUG) {
            Log.v(TAG, "hideBars");
        }
        if (mToolBar.getVisible()) {
            Animation ani = mAnimations.get("bar_in");
            if (ani != null) {
                ani.setDirection(Animation.BACKWARD);
                ani.start();
            }
            mBarState = BAR_STATE_ENTERING;
            if (Media3D.DEBUG) {
                Log.v(TAG, "barState = " + mBarState);
            }
        }
    }

    public void hideBarImmediately() {
        if (Media3D.DEBUG) {
            Log.v(TAG, "hideBarImmediately");
        }
        if (mToolBar.getVisible()) {
            mHandler.removeCallbacks(mHideBarsRunnable);
            mToolBar.setVisible(false);
            mNavigationBar.setVisible(false);
            mBarState = BAR_STATE_ABOUT_TO_HIDE;

            mHandler.removeCallbacks(mSetBarHiddenRunnable);
            mHandler.postDelayed(mSetBarHiddenRunnable, DELAY_ABOUT_TO_HIDE_BAR);

            if (Media3D.DEBUG) {
                Log.v(TAG, "barState = " + mBarState);
            }
        }
    }

    private final Animation.Listener mAnimationListener = new Animation.Listener() {
        public void onPaused(final Animation animation) {
            if (animation == mAnimations.get("bar_in")) {
                ((Activity) mContext).runOnUiThread(new Runnable() {
                    public void run() {
                        if (animation.getDirection() == Animation.FORWARD) {
                            if (Media3D.DEBUG) {
                                Log.v(TAG, "onPaused: page = " + Media3DView.this + " , ani = bar_in, FORWARD");
                            }
                            if (mToolBar.getVisible() && mBarState == BAR_STATE_ENTERING) {
                                mHandler.removeCallbacks(mHideBarsRunnable);
                                mHandler.postDelayed(mHideBarsRunnable, DELAY_HIDE_BAR);

                                mBarState = BAR_STATE_ENTERED;
                                if (Media3D.DEBUG) {
                                    Log.v(TAG, "barState = " + mBarState);
                                }
                            }
                        } else {
                            if (Media3D.DEBUG) {
                                Log.v(TAG, "onPaused: page = " + Media3DView.this + " , ani = bar_in, BACKWARD");
                            }
                            if (mToolBar.getVisible()) {
                                mNavigationBar.setVisible(false);
                                mToolBar.setVisible(false);
                                mBarState = BAR_STATE_HIDDEN;
                                if (Media3D.DEBUG) {
                                    Log.v(TAG, "barState = " + mBarState);
                                }
                            }
                        }
                    }
                });
            }
        }
    };

    public int getBarState() {
        return mBarState;
    }

    private AnimationGroup addAnimationList(String name, int... resIdList) {
        AnimationGroup animationGroup = new AnimationGroup();
        for (int id : resIdList) {
            Animation animation = AnimationLoader.loadAnimation(mContext, id);
            if (animation != null) {
                animation.setTag(id);
                animationGroup.add(animation);
            }
        }
        animationGroup.addListener(mAnimationListener);
        mAnimations.put(name, animationGroup);

        return animationGroup;
    }

    @Override
    public void onPause() {
        if (mCurrentPage != null && mState == STATE_IDLE) {
            LogUtil.v(TAG, "Page :" + mCurrentPage);
            mCurrentPage.onPause();
        }

        super.onPause();
    }

    private final static String cacheFile = "/.media3d";
    private final static String lastPage = "LastPage";

    public void saveStatus() {
        try {
            FileWriter writer = new FileWriter(mContext.getCacheDir().getAbsolutePath() + cacheFile);
            writer.write(getNameFromPage(mCurrentPage));
            writer.close();
            Log.v(TAG, "Write cache : " + getNameFromPage(mCurrentPage));
        } catch (IOException e) {
            Log.v(TAG, "Can't write cache : " + e);
        }
    }

    private String reloadStatus() {
        File file = new File(mContext.getCacheDir().getAbsolutePath() + cacheFile);
        String page = PageHost.PORTAL;
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String readPage = br.readLine();
            if (readPage != null &&
               (readPage.equals(PageHost.WEATHER) ||
                readPage.equals(PageHost.PHOTO) ||
                readPage.equals(PageHost.VIDEO))) {
                page = readPage;
            }
            br.close();
            Log.v(TAG, "Read cache : " + readPage);
        } catch (IOException e) {
            Log.v(TAG, "Can't read cache : " + e);
        }
        return page;
    }

    public void clearStatus() {
        File file = new File(mContext.getCacheDir().getAbsolutePath() + cacheFile);
        boolean result = file.delete();
        Log.v(TAG, "Clear cache : " + result);
    }

    public void onSaveInstanceState(Bundle outState) {
        outState.putCharSequence(lastPage, getNameFromPage(mCurrentPage));
        for (int i = 1; i < mPages.size(); ++i) {
            mPages.get(i).onSaveInstanceState(outState);
        }
    }

    public void reload(Bundle bundle) {
        String page = PageHost.PORTAL;
        // From onSaveInstanceState()
        if (bundle != null) {
            String last = bundle.getString(lastPage);
            if (last != null) {
                page = last;
            }
        } else {
            // From saveStatus()
            page = reloadStatus();
            // Clean up cache, once it has been applied.
            clearStatus();
        }
        prepareEnterPage(page);
    }

    private void prepareEnterPage(String pageName) {
        if (pageName.equals(PageHost.PORTAL)) {
            enterPage(getPageFromName(pageName));
            return;
        }
        // Prepare environment for other pages
        mCurrentPage = getPageFromName(PageHost.PORTAL);
        mCurrentPage.onPageEntering(Page.TransitionType.LAUNCH_PORTAL);
        Page target = getPageFromName(pageName);
        target.initialize();
        enterPage(target);
    }

    private static int sStereoTag = 2;
    @Override
    public void onResume() {
        super.onResume();
        if (mCurrentPage != null) {
            mCurrentPage.onResume();
        }
        // Remove dragging status
        mPageDragHelper.handleActionUp(mCurrentPage, this);
    }

    public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
    }

    private final Animation.Listener mAnimationCompletedHandler = new Animation.Listener() {
        public void onCompleted(Animation animation) {
            if (animation.getTag() == sStereoTag) {
                mHandler.post(new Runnable() {
                    public void run() {
                        if (mCurrentPage instanceof WeatherPage) {
                            ((WeatherPage) mCurrentPage).initBackgroundVideo();
                            ((WeatherPage) mCurrentPage).startBackgroundVideo();
                        }
                    }
                });
            }
        }
    };

    private static final float MAX_DRAG_RANGE = 800.0f;
    private static final float DRAG_TO_NEXT_THRESHOLD = 0.05f;

    Page mNewPage;
    int mTransition;
    AnimationGroup mDraggingAnimation = new AnimationGroup();

    private void confirmDragging() {
        enterPage(mNewPage, mTransition);
    }

    private void revertDragging() {
        mCurrentPage.revertDragLeaving();
    }

    boolean mIsDragging;
    public boolean onDrag(PageDragHelper.State state, float disX) {
        if (getCurrentPage() == getPageFromName(PageHost.PORTAL)) {
            return false;
        }

        switch (state) {
            case INITIAL:
                if (mDraggingAnimation.isStarted()) {
                    mDraggingAnimation.complete();
                    mDraggingAnimation.stop();
                }
                break;
            case START:
                mIsDragging = true;
                mDraggingAnimation.clear();
                if (disX < 0) { // drag right
                    mNewPage = getPrevPage();
                    mTransition = Page.TransitionType.INNER_TO_RIGHT;
                } else { // drag left
                    mNewPage = getNextPage();
                    mTransition = Page.TransitionType.INNER_TO_LEFT;
                }
                mCurrentPage.prepareDragLeaving();
                mDraggingAnimation.add(mCurrentPage.prepareDragLeavingAnimation(mTransition));
                mDraggingAnimation.startDragging();
                break;
            case DRAGGING:
                mDraggingAnimation.setProgress(Math.abs(disX) / MAX_DRAG_RANGE);
                break;
            case FINISH:
                if (mIsDragging) {
                    mIsDragging = false;
                    mDraggingAnimation.stopDragging();
                    if (mDraggingAnimation.getProgress() < DRAG_TO_NEXT_THRESHOLD) {
                        revertDragging();
                        mDraggingAnimation.reverse();
                    } else {
                        confirmDragging();
                    }
                }
                break;
            default:
                break;
        }
        return true;
    }
}
