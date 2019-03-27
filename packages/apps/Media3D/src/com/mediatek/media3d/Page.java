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
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;

import com.mediatek.ngin3d.Actor;
import com.mediatek.ngin3d.Container;
import com.mediatek.ngin3d.Transaction;
import com.mediatek.ngin3d.animation.Animation;

/**
 * Base page that will be hosted in a page container.
 */
public abstract class Page {

    private static final String TAG = "Page";

    private Activity mActivity;
    private PageHost mHost;
    protected boolean mIsDragLeaving;

    public static final int EMPTY = 0;
    public static final int IDLE = 1;
    public static final int ENTERING = 2;
    public static final int LEAVING = 3;
    private static final String STATE_NAMES[] = {
        "EMPTY", "IDLE", "ENTERING", "LEAVING"
    };

    public static class PageType {
        public static final int PORTAL = 1;
        public static final int CIRCULAR = 2;
    }

    public static class TransitionType {
        public static final int NO_TRANSITION = 0;
        public static final int LAUNCH_PORTAL = 1;
        public static final int PORTAL_INNER = 2;
        public static final int INNER_TO_LEFT = 3;
        public static final int INNER_TO_RIGHT = 4;

        /**
         * LAUNCHER_PORTAL: (enter or leave) (null <-> portal)
         * PORTAL_INNER: (enter or leave) (portal <-> inner)
         * INNER_TO_LEFT: (enter or leave) (inner <-> inner)
         * INNER_TO_RIGHT: (enter or leave) (inner <-> inner)
         */
    }

    private static final String TRANSITION_NAMES[] = {
        "NO", "LAUNCH_PORTAL", "PORTAL_INNER", "INNER_TO_LEFT", "INNER_TO_RIGHT"
    };

    public static final int SUPPORT_FLING = 1 << 0;
    private int mOptions;
    private int mState;

    public Page(int options) {
        mOptions |= options;
    }

    public int getState() {
        return mState;
    }

    public void setState(int newState) {
        if (newState == mState) {
            return;
        }

        int oldState = mState;
        mState = newState;
        Log.v(TAG, "Enter state " + STATE_NAMES[mState] + " (" + this + ")");

        Transaction.beginPropertiesModification();

        if (newState == IDLE) {
            if (oldState == ENTERING) {
                if (mHost != null) {
                    Log.v(TAG, "onPageEntered (" + this + ")");
                    onPageEntered();
                    mHost.onPageEntered(this);
                }
            } else if (oldState == LEAVING) {
                if (mHost != null) {
                    Log.v(TAG, "onPageLeft (" + this + ")");
                    Long t1 = System.currentTimeMillis();
                    onPageLeft();
                    Long t2 = System.currentTimeMillis();
                    mHost.onPageLeft(this);
                    Long t3 = System.currentTimeMillis();
                    Log.v(TAG, "onPageLeft = " + (t2 - t1) + " , mHost.onPageLeft = " + (t3 - t2));
                }
            }
        } else if (newState == LEAVING) {
            Long t1 = System.currentTimeMillis();
            if (oldState == ENTERING) {
                Log.v(TAG, "onAbortEntering (" + this + ")");
                onAbortEntering();
            } else if (oldState == IDLE) {
                Log.v(TAG, "onPageLeaving (" + this + ")");
                onPageLeaving(mPageTransitionType);
            }
            Long t2 = System.currentTimeMillis();
            Log.v(TAG, "Page Leaving time = " + (t2 - t1));
        } else if (newState == ENTERING) {
            if (oldState == LEAVING) {
                Log.v(TAG, "onAbortLeaving (" + this + ")");
                onAbortLeaving();
            } else if (oldState == IDLE) {
                Log.v(TAG, "OnPageEntering (" + this + ")");
                onPageEntering(mPageTransitionType);
            }
        }

        Transaction.commit();
    }

    protected void onPageEntering(int transitionType) {
        getContainer().setVisible(true);
    }

    protected void onAbortEntering() {
        getContainer().stopAnimations();
    }

    protected void prepareDragLeaving() {
        mIsDragLeaving = true;
    }

    @SuppressWarnings("PMD.EmptyMethodInAbstractClassShouldBeAbstract")
    protected Animation prepareDragLeavingAnimation(int transitionType) {
        return null;
    }

    protected boolean isDragLeaving() {
        return mIsDragLeaving;
    }

    protected void revertDragLeaving() {
        mIsDragLeaving = false;
    }

    @SuppressWarnings("PMD.EmptyMethodInAbstractClassShouldBeAbstract")
    protected void onPageEntered() {
        // onPageEntered callback function when page is completely entered
    }

    @SuppressWarnings("PMD.EmptyMethodInAbstractClassShouldBeAbstract")
    protected void onPageLeaving(int transitionType) {
        // onPageLeaving callback function when page is starting leaving
    }

    protected void onAbortLeaving() {
        getContainer().stopAnimations();
    }

    protected void onPageLeft() {
        getContainer().setVisible(false);
        mIsDragLeaving = false;
    }

    public PageHost getHost() {
        return mHost;
    }

    public Activity getActivity() {
        return mActivity;
    }

    /**
     * Called when a page is first attached to its activity. onCreate(Bundle) will be called after this.
     *
     * @param activity containing activity
     */
    public void onAttach(Activity activity) {
        mActivity = activity;
    }

    /**
     * Called to do initial creation of a page. his is called after onAttach(Activity).
     *
     * @param savedInstanceState f the page is being re-created from a previous saved state, this is the state.
     */
    @SuppressWarnings({"PMD.EmptyMethodInAbstractClassShouldBeAbstract","PMD.CallSuperFirst"})
    public void onCreate(Bundle savedInstanceState) {
        // onCreate callback function when page is created
    }

    protected abstract Container getContainer();

    /**
     * Notify being added to a host.
     *
     * @param host page host
     */
    public void onAdded(PageHost host) {
        Log.v(TAG, "onAdded (" + this + "), Host = " + host);
        mHost = host;
    }

    /**
     *  Notify the page is being initialized, page can put time comsumption initialization
     *  code here.
     *
     */
    public void onLoad() {
        setState(IDLE);
    }

    public boolean isLoaded() {
        return mState != EMPTY;
    }

    protected int mPageTransitionType;

    /**
     * Enter a page.
     *
     * @param transitionType transition type
     * @see Page.TransitionType
     */
    public void enter(int transitionType) {
        Log.v(TAG, "enter (" + this + "), transitionType = " + TRANSITION_NAMES[transitionType]);
        mPageTransitionType = transitionType;
        setState(ENTERING);
    }

    /**
     * Leave a page.
     *
     * @param transitionType transition type
     * @see Page.TransitionType
     */
    public void leave(int transitionType) {
        Log.v(TAG, "leave (" + this + "), transitionType = " + TRANSITION_NAMES[transitionType]);
        mPageTransitionType = transitionType;
        setState(LEAVING);
    }

    /**
     * Notify flinging a page.
     *
     * @param direction one of TO_LEFT, TO_RIGHT, TO_UP, TO_DOWN
     */
    @SuppressWarnings("PMD.EmptyMethodInAbstractClassShouldBeAbstract")
    public void onFling(int direction) {
        // onFling callback function, when page is fling left, right, up, down.
    }

    /**
     * Get page type. By default, it is circular.
     *
     * @return page type.
     */
    public int getPageType() {
        return PageType.CIRCULAR;
    }

    /**
     * Called when a page is removed from its host.
     */
    public void onRemoved() {
        Log.v(TAG, "onRemoved (" + this + ")");
        if (isLoaded()) {
            getContainer().stopAnimations();
        }
        mHost = null;
    }

    /**
     * Called to retrieve per-instance state from an page before being killed so that
     * the state can be restored in onCreate(Bundle).
     *
     * @param outState Bundle in which to place your saved state.
     */
    @SuppressWarnings("PMD.EmptyMethodInAbstractClassShouldBeAbstract")
    protected void onSaveInstanceState(Bundle outState) {
        // onSaveInstanceState callback function.
    }

    /**
     * Called when the page is no longer attached to its activity.
     */
    public void onDetach() {
        mActivity = null;
    }

    /**
     * Called when Media3DView is onPause.
     */
    @SuppressWarnings({"PMD.EmptyMethodInAbstractClassShouldBeAbstract","PMD.CallSuperFirst"})
    public void onPause() {
        // onPause callback function when Media3DView is onPause.
    }

    /**
     * Called when Media3dView is onResume.
     */
    @SuppressWarnings({"PMD.EmptyMethodInAbstractClassShouldBeAbstract","PMD.CallSuperFirst"})
    public void onResume() {
        // onResume callback function when Media3DView is onResume.
    }

    /**
     * Called when the page is no longer in use.
     */
    @SuppressWarnings("PMD.EmptyMethodInAbstractClassShouldBeAbstract")
    public void onDestroy() {
        // onDestroy callback function when page is destroyed.
    }

    /**
     * Call startActivity(Intent) on the page's containing Activity.
     *
     * @param intent The intent to start.
     */
    protected void startActivity(Intent intent) {
        try {
            mActivity.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, e.toString());
        }
    }

    /**
     * Call startActivityForResult(Intent, int) on the page's containing Activity.
     *
     * @param intent      The intent to start.
     * @param requestCode If >= 0, this code will be returned in onActivityResult() when the activity exits.
     */
    protected void startActivityForResult(Intent intent, int requestCode) {
        try {
            mActivity.startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, e.toString());
        }
    }

    /**
     * Receive the result from a previous call to startActivityForResult(Intent, int).
     *
     * @param requestCode
     * The integer request code originally supplied to startActivityForResult(),
     *      allowing you to identify who this result came from.
     *
     * @param resultCode
     * The integer result code returned by the child activity through its setResult().
     *
     * @param data
     * An Intent, which can return result data to the caller (various data can be attached to Intent "extras").
     */
    @SuppressWarnings("PMD.EmptyMethodInAbstractClassShouldBeAbstract")
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // onActivityResult callback function
    }

    @SuppressWarnings("PMD.EmptyMethodInAbstractClassShouldBeAbstract")
    protected boolean onTouchEvent(MotionEvent event) {
        return false;
    }

    @SuppressWarnings("PMD.EmptyMethodInAbstractClassShouldBeAbstract")
    protected boolean onSingleTapConfirmed(MotionEvent event) {
        return false;
    }

    @SuppressWarnings("PMD.EmptyMethodInAbstractClassShouldBeAbstract")
    protected boolean onDrag(PageDragHelper.State state, float disY) {
        return false;
    }

    @SuppressWarnings("PMD.EmptyMethodInAbstractClassShouldBeAbstract")
    protected boolean onCreateBarMenu(NavigationBarMenu menu) {
        return false;
    }

    @SuppressWarnings("PMD.EmptyMethodInAbstractClassShouldBeAbstract")
    protected boolean onBarMenuItemSelected(NavigationBarMenuItem item) {
        return false;
    }

    @SuppressWarnings("PMD.EmptyMethodInAbstractClassShouldBeAbstract")
    protected Actor getThumbnailActor() {
        return null;
    }

    protected void enableOptions(int options) {
        mOptions |= options;
    }

    protected void disableOptions(int options) {
        mOptions &= ~options;
    }

    protected boolean isOptionsSupported(int options) {
        return ((mOptions & options) == options);
    }

    public abstract void updateLocale(java.util.Locale locale);


    protected boolean mInitialized;
    public void initialize() {
        mInitialized = true;
    }

    public LayoutManager getLayoutManager() {
        if (mActivity == null ||
         !(mActivity instanceof Main)) {
          return null;
        }

        return ((Main)mActivity).getLayoutManager();
    }
}