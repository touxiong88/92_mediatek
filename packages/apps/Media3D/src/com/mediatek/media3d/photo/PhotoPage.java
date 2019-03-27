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

package com.mediatek.media3d.photo;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MotionEvent;

import com.mediatek.media3d.FadeInOutAnimation;
import com.mediatek.media3d.ImageViewer;
import com.mediatek.media3d.LayoutManager;
import com.mediatek.media3d.LogUtil;
import com.mediatek.media3d.Main;
import com.mediatek.media3d.Media3D;
import com.mediatek.media3d.MediaItem;
import com.mediatek.media3d.MediaItemSet;
import com.mediatek.media3d.MediaDbItemSet;
import com.mediatek.media3d.MediaUtils;
import com.mediatek.media3d.NavigationBarMenu;
import com.mediatek.media3d.NavigationBarMenuItem;
import com.mediatek.media3d.Page;
import com.mediatek.media3d.PageDragHelper;
import com.mediatek.media3d.PageHost;
import com.mediatek.media3d.PageTransitionDetector;
import com.mediatek.media3d.R;
import com.mediatek.media3d.ResourceItemSet;

import com.mediatek.ngin3d.Actor;
import com.mediatek.ngin3d.Color;
import com.mediatek.ngin3d.Container;
import com.mediatek.ngin3d.Dimension;
import com.mediatek.ngin3d.Image;
import com.mediatek.ngin3d.Point;
import com.mediatek.ngin3d.Rotation;
import com.mediatek.ngin3d.Stage;
import com.mediatek.ngin3d.Text;
import com.mediatek.ngin3d.animation.Animation;
import com.mediatek.ngin3d.animation.AnimationGroup;
import com.mediatek.ngin3d.animation.AnimationLoader;
import com.mediatek.ngin3d.animation.Mode;
import com.mediatek.ngin3d.animation.PropertyAnimation;
import com.mediatek.ngin3d.animation.Timeline;
import com.mediatek.ngin3d.presentation.BitmapGenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class PhotoPage extends Page implements PageTransitionDetector.ActionListener {
    private static final String TAG = "PhotoPage";
    private static final int PHOTO_PER_PAGE = 4;
    private final Stage mStage;
    private Container mContainer = new Container();

    private Image mBackground;
    private Text mNoPhotoText;
    private Text mPageIndexText;

    private int mPhotoPageIndex;
    private int mPhotoOffsetIndex;

    private String mBucketId;
    private MediaItemSet mMediaItemSet;

    private final PageTransitionDetector mScrollDetector = new PageTransitionDetector(this);
    private final HashMap<Integer, AnimationGroup> mAnimationLists = new HashMap<Integer, AnimationGroup>();
    private AnimationGroup mRunningAnimation;
    private int mRunningAniId;

    private static final float ANIM_SPEED_NORMAL = 1.0f;
    private static final float ANIM_SPEED_FASTER = 1.4f;

    // Reference to logical (before, now, next).
    // In normal three pages or above case,
    //  (mActorBefore, mActorNow, mActorNext)
    //  -> (mActorBeforeContent, mActorNowContent, mActorNextContent)
    // However, in two pages case, in order to accelerate page swap speed.
    // While dragging up/down, only operate on reference but not actor content.
    // Therefore, the reference to content mapping may be
    //  (mActorBefore, mActorNow, mActorNext)
    //  -> (mActorBeforeContent, mActorNowContent, mActorBeforeContent)
    private Actor[] mActorBefore;
    private Actor[] mActorNow;
    private Actor[] mActorNext;

    // Actually allocated actors array.
    private Actor[] mActorBeforeContent = new Container[PHOTO_PER_PAGE];
    private Actor[] mActorNowContent = new Container[PHOTO_PER_PAGE];
    private Actor[] mActorNextContent = new Container[PHOTO_PER_PAGE];
    private static final int ACTORS_HOLDER_SIZE = 3; // mActorBefore, mActorNow, mActorNext

    private Actor mIconLast;
    private Actor mIconNext;

    private boolean mIsMediaItemSetModified;

    private static final Bitmap BLANK_BITMAP = Bitmap.createBitmap(2, 2, Bitmap.Config.RGB_565);

    private static final int PHOTO_TAG = 1;
    private static final String KEY_PHOTO_BUCKET_ID = "photo_bucket_id";

    public PhotoPage(Stage stage) {
        super(Page.SUPPORT_FLING);
        mStage = stage;
    }

    @Override
    public void onAdded(PageHost host) {
        super.onAdded(host);
    }

    @Override
    public void onLoad() {
        if (Media3D.DEBUG) {
            Log.v(TAG, "onLoad");
        }
        super.onLoad();
    }

    public void initialize() {
        if (mInitialized) {
            return;
        }

        initPhoto();
        mBucketId = getBucketId();
        prepareMediaItemSet(mBucketId);
        mPhotoPageIndex = 0;
        mPhotoOffsetIndex = 0;
        initPhotoActorContent();
        super.initialize();
    }

    private int getTransition(final int transitionType, final String action) {
        final boolean isEnter = action.equalsIgnoreCase("enter");
        switch (transitionType) {
            case TransitionType.INNER_TO_LEFT:
                return isEnter ? R.string.right_enter : R.string.left_exit;
            case TransitionType.INNER_TO_RIGHT:
                return isEnter ? R.string.left_enter : R.string.right_exit;
            case TransitionType.PORTAL_INNER:
                return isEnter ? R.string.next_enter : R.string.next_exit;
            default:
                return R.string.next_enter;
        }
    }

    @Override
    protected void onPageEntering(int transitionType) {
        initialize();
        if (isNoPhoto()) {
            mNoPhotoText.setVisible(true);
            setState(IDLE);
        } else {
            mFadeInAni.start();
            showPhoto(getTransition(transitionType, "enter"));
        }
        super.onPageEntering(transitionType);
        mScrollDetector.reset();
    }

    protected void prepareDragLeaving() {
        setActorVisibility(mActorBefore, false);
        setActorVisibility(mActorNext, false);
        stopAllAnimations();
        updateBackgroundText();
        mPageIndexText.setVisible(false);
        getIconLast().setVisible(false);
        getIconNext().setVisible(false);
        releaseLoaders(false);
        super.prepareDragLeaving();
    }

    protected Animation prepareDragLeavingAnimation(int transitionType) {
        if (transitionType == TransitionType.PORTAL_INNER) {
            return mAnimationLists.get(R.string.fade_out);
        }

        AnimationGroup photoLeaving = preparePhotoAnimation(getTransition(transitionType, "leave"));
        if (mMediaItemSet.getItemCount() != 0 && photoLeaving != null) {
            AnimationGroup backgroundLeaving = mAnimationLists.get(R.string.swap_out);
            if (backgroundLeaving != null && backgroundLeaving.getAnimation(0) != null) {
                backgroundLeaving.getAnimation(0).setTarget(mBackground);
                return photoLeaving.add(backgroundLeaving);
            }
        }
        return photoLeaving;
    }

    protected void revertDragLeaving() {
        startPhotoAnimation(R.string.floating);
        showBackground(R.string.swap_in);
        updateBackgroundText();
        updateIndicators();
        updatePageIndexText();
        super.revertDragLeaving();
    }

    @Override
    protected void onPageLeaving(int transitionType) {
        if (isDragLeaving()) {
            setState(IDLE);
            return;
        }
        setActorVisibility(mActorBefore, false);
        setActorVisibility(mActorNext, false);
        stopAllAnimations();

        if (isNoPhoto()) {
            mNoPhotoText.setVisible(false);
            setState(IDLE);
        } else {
            if (transitionType == TransitionType.PORTAL_INNER) {
                Animation fadeOut = mAnimationLists.get(R.string.fade_out);
                if (fadeOut != null) {
                    fadeOut.start();
                }
            } else {
                hidePhoto(getTransition(transitionType, "leave"));
            }
        }
        releaseLoaders(false);
        mScrollDetector.reset();
    }

    @Override
    public void onRemoved() {
        Log.v(TAG, "onRemoved (" + this + ")");
        destroyMediaItemSet();
        super.onRemoved();
    }

    @Override
    protected Container getContainer() {
        return mContainer;
    }

    @Override
    public void onFling(int direction) {
        if (Media3D.DEBUG) {
            Log.v(TAG, "onFling: " + direction);
        }
        if (getTotalPhotoPage() <= 1) {
            return;     // ignore fling event if no item on screen, or only 1 page.
        }

        mScrollDetector.onFling(direction);
    }

    FadeInOutAnimation mFadeInAni;

    private void preparePhotoAnimations() {
        mFadeInAni = new FadeInOutAnimation(getContainer(), FadeInOutAnimation.FadeType.IN);
        FadeInOutAnimation fadeOutAni = new FadeInOutAnimation(getContainer(), FadeInOutAnimation.FadeType.OUT);

        final int enterMarkerDuration = 240;
        // left enter
        addAnimationList(R.string.left_enter,
            R.array.photo_left_enter_id, R.array.photo_left_enter_strings)
            .addMarkerAtTime("enter", enterMarkerDuration);

        // left exit
        addAnimationList(R.string.left_exit,
            R.array.photo_left_exit_id, R.array.photo_left_exit_strings)
            .enableOptions(Animation.HIDE_TARGET_ON_COMPLETED);

        // right enter
        addAnimationList(R.string.right_enter,
            R.array.photo_right_enter_id, R.array.photo_right_enter_strings)
            .addMarkerAtTime("enter", enterMarkerDuration);

        // right exit
        addAnimationList(R.string.right_exit,
            R.array.photo_right_exit_id, R.array.photo_right_exit_strings)
            .enableOptions(Animation.HIDE_TARGET_ON_COMPLETED);

        // next enter
        addAnimationList(R.string.next_enter,
            R.array.photo_next_enter_id, R.array.photo_next_enter_strings);

        // next exit
        addAnimationList(R.string.next_exit,
            R.array.photo_next_exit_id, R.array.photo_next_exit_strings)
            .add(fadeOutAni)
            .enableOptions(Animation.HIDE_TARGET_ON_COMPLETED);

        // last enter
        addAnimationList(R.string.last_enter,
            R.array.photo_last_enter_id, R.array.photo_last_enter_strings);

        // last exit
        addAnimationList(R.string.last_exit,
            R.array.photo_last_exit_id, R.array.photo_last_exit_strings)
            .add(fadeOutAni)
            .enableOptions(Animation.HIDE_TARGET_ON_COMPLETED);

        // swap in
        AnimationGroup swapIn = addAnimationList(R.string.swap_in,
            R.array.photo_swap_enter_id, R.array.photo_swap_enter_strings);

        int startValue = (int)(255 * 0.4f);
        Color c1 = new Color(startValue, startValue, startValue, 255);
        Color c2 = new Color(255, 255, 255, 255);
        PropertyAnimation bgFadeInAni = new PropertyAnimation(mBackground, "color", c1, c2);
        bgFadeInAni.setDuration(swapIn.getDuration()).setMode(Mode.LINEAR).enableOptions(Animation.SHOW_TARGET_ON_STARTED);
        swapIn.add(bgFadeInAni);

        // swap out
        addAnimationList(R.string.swap_out,
            R.array.photo_swap_exit_id, R.array.photo_swap_exit_strings);

        // floating
        addAnimationList(R.string.floating,
            R.array.photo_floating_id, R.array.photo_floating_strings)
            .setLoop(true).setAutoReverse(true);

        // next exit - enter
        addAnimationList(R.string.next_exit_enter,
            R.array.photo_next_exit_enter_id, R.array.photo_next_exit_enter_strings);

        // last exit - enter
        addAnimationList(R.string.last_exit_enter,
            R.array.photo_last_exit_enter_id, R.array.photo_last_exit_enter_strings);

        AnimationGroup group = new AnimationGroup();
        group.setName(getString(R.string.fade_out));
        group.add(fadeOutAni);
        group.addListener(mAnimationCompletedHandler);
        mAnimationLists.put(R.string.fade_out, group);
    }

    public final int getPageIndex() {
        return mPhotoPageIndex;
    }

    public final int getNextPageIndex() {
        if (isLessOrEqualOnePage()) {
            return 0;
        }
        return (getPageIndex() + 1) < getTotalPhotoPage() ? (getPageIndex() + 1) : 0;
    }

    public final int getPrevPageIndex() {
        if (isLessOrEqualOnePage()) {
            return 0;
        }
        return (getPageIndex() > 0) ? (getPageIndex() - 1) : (getTotalPhotoPage() - 1);
    }

    // Setup actor reference to actual allocated actor array
    private void initPhotoActorsReference() {
        mActorBefore = mActorBeforeContent;
        mActorNow = mActorNowContent;
        mActorNext = mActorNextContent;
    }

    private Dimension mBgDim;
    private Dimension mThumbDim;
    private Dimension mTempThumbDim;
    private Dimension mPortalThumbDim;
    private float mDragMax;
    private float mDragThreshold;

    private void initPhoto() {
        mContainer.setVisible(false);
        mBgDim = getLayoutManager().getDimension("photo_bg_size");
        mThumbDim = getLayoutManager().getDimension("photo_thumb_size");
        mTempThumbDim = getLayoutManager().getDimension("photo_temp_size");
        mDragThreshold = getLayoutManager().getFloat("photo_drag_threshold");
        mDragMax = getLayoutManager().getFloat("photo_drag_maximum");

        initPhotoActorsReference();
        int index = 1;
        initPhotoActor(mActorBefore, index);
        index += mActorBefore.length;
        initPhotoActor(mActorNow, index);
        index += mActorNow.length;
        initPhotoActor(mActorNext, index);

        mBackground = Image.createEmptyImage();
        mBackground.setVisible(false);

        mContainer.add(mBackground);

        mNoPhotoText = new Text(getActivity().getResources().getString(R.string.no_photo_text));
        mNoPhotoText.setPosition(getLayoutManager().getPoint("photo_no_content_alert_pos"));
        mNoPhotoText.setScale(getLayoutManager().getScale("photo_no_content_alert_scale"));
        mNoPhotoText.setVisible(false);
        mContainer.add(mNoPhotoText);

        mPageIndexText = new Text();
        mPageIndexText.setPosition(getLayoutManager().getPoint("photo_page_number_pos"));
        mPageIndexText.setScale(getLayoutManager().getScale("photo_page_number_scale"));
        mPageIndexText.setVisible(false);
        mContainer.add(mPageIndexText);

        mStage.add(mContainer);
        preparePhotoAnimations();
    }

    private void initPhotoActor(Actor[] actors, int index) {
        int idx = index;
        for (int i = 0; i < actors.length; i++) {
            actors[i] = new Container();

            Image frame = Image.createFromResource(
                    getActivity().getResources(), R.drawable.photo_frame);
            frame.setReactive(false);
            frame.setPosition(new Point(0, 0, -0.02f));
            ((Container) actors[i]).add(frame);

            Image defaultThumbnail = Image.createFromResource(
                    getActivity().getResources(), R.drawable.photo_frame_background);
            defaultThumbnail.setReactive(false);
            defaultThumbnail.setPosition(new Point(0, 0, +0.1f));
            ((Container) actors[i]).add(defaultThumbnail);

            actors[i].setVisible(false);
            actors[i].setTag(idx++);
            getContainer().add(actors[i]);
        }
    }

    private void removeChildByTag(Container container, int tag) {
        if (container != null) {
            final Actor child = container.findChildByTag(tag);
            if (child != null) {
                container.remove(child);
            }
        }
    }

    private void resetActor(int pageIndex, Actor[] actors) {
        int totalCount = mMediaItemSet.getItemCount();
        for (int i = 0; i < actors.length; i++) {
            Container c = (Container)actors[i];
            removeChildByTag(c, PHOTO_TAG);
            if (pageIndex * PHOTO_PER_PAGE + i < totalCount) {
                Container image = new Container();
                image.setReactive(false);
                image.setPosition(new Point(0, 0, +0.08f));
                image.setTag(PHOTO_TAG);
                c.add(image);
            }
        }
    }

    private void removeActorContent(Actor[] actors) {
        for (Actor actor : actors) {
            removeChildByTag((Container)actor, PHOTO_TAG);
        }
    }

    private void initPhotoActorContent() {
        if (getTotalPhotoPage() > 0) {
            resetActor(getPageIndex(), mActorNow);
            setPhotoActorContent(getPageIndex(), mActorNow);

            if (isLessOrEqualOnePage()) {
                removeActorContent(mActorBefore);
                removeActorContent(mActorNext);
            } else {
                resetActor(getPrevPageIndex(), mActorBefore);
                setPhotoActorContent(getPrevPageIndex(), mActorBefore);
                resetActor(getNextPageIndex(), mActorNext);
                setPhotoActorContent(getNextPageIndex(), mActorNext);
            }
        } else {
            removeActorContent(mActorNow);
        }
    }

    private AnimationGroup preparePhotoAnimation(int animationName) {
        AnimationGroup group = mAnimationLists.get(animationName);
        if (group == null) {
            return null;
        }
        for (int i = 0; i < mActorNow.length; i++) {
            Animation  ani = group.getAnimation(i);
            if (ani == null) {
                continue;
            }
            if (((Container) mActorNow[i]).findChildByTag(PHOTO_TAG) == null) {
                ani.setTarget(null);
            } else {
                ani.setTarget(mActorNow[i]);
            }
        }
        return group;
    }

    private void startPhotoAnimation(int name) {
        Animation ani = preparePhotoAnimation(name);
        if (ani != null) {
            ani.start();
        }
    }

    private void startPhotoBackgroundAnimation(int animation) {
        AnimationGroup group = mAnimationLists.get(animation);
        if (group != null &&
                (animation == R.string.swap_in || animation == R.string.swap_out)) {
            Animation ani = group.getAnimation(0);
            if (ani != null) {
                ani.setTarget(mBackground);
            }
            group.start();
        }
    }

    private void stopPhotoAnimation(int name) {
        Animation ani = mAnimationLists.get(name);
        if (ani != null) {
            ani.stop();
        }
    }

    private void stopAllAnimations() {
        for (Animation ani : mAnimationLists.values()) {
             ani.stop();
        }
    }

    private void showPhoto(int transitionName) {
        LogUtil.v(TAG, "trans : " + transitionName);
        startPhotoAnimation(transitionName);
        updateIndicators();
        updatePageIndexText();
    }

    private void hidePhoto(int transitionName) {
        leavePage(transitionName);
        leaveBackground(R.string.swap_out);
    }

    private AnimationGroup addAnimationList(int nameResId, int aniArrayResId, int cacheNameArrayResId) {
        AnimationGroup group = new AnimationGroup();
        group.setName(getString(nameResId));
        TypedArray aniResIds = getActivity().getResources().obtainTypedArray(aniArrayResId);
        String[] cacheNameResIds =
            getActivity().getResources().getStringArray(cacheNameArrayResId);

        final int length = aniResIds.length();
        for (int i = 0; i < length; i++) {
            Animation ani = AnimationLoader.loadAnimation(
                    getActivity(), aniResIds.getResourceId(i, 0), cacheNameResIds[i]).
                    disableOptions(Animation.CAN_START_WITHOUT_TARGET);
            if (ani != null) {
                group.add(ani);
            }
        }
        mAnimationLists.put(nameResId, group);
        group.addListener(mAnimationCompletedHandler);
        return group;
    }

    private void setActorVisibility(Actor[] actors, boolean visible) {
        for (Actor actor : actors) {
            actor.setVisible(visible);
        }
    }

    public int getTotalPhotoPage() {
        return (mMediaItemSet.getItemCount() + PHOTO_PER_PAGE - 1) / PHOTO_PER_PAGE;
    }

    private boolean isEnterAnimation(String name) {
        return name.equals(getString(R.string.left_enter))
            || name.equals(getString(R.string.right_enter))
            || name.equals(getString(R.string.next_enter));
    }

    private boolean isExitAnimation(String name) {
        return name.equals(getString(R.string.left_exit))
            || name.equals(getString(R.string.right_exit))
            || name.equals(getString(R.string.fade_out));
    }

    private boolean isEnterExitAnimation(String name) {
        return isEnterAnimation(name) || isExitAnimation(name);
    }

    private final Animation.Listener mAnimationCompletedHandler = new Animation.Listener() {
        public void onMarkerReached(Animation animation, int direction, String marker) {
            if ("enter".equals(marker)) {
                setState(IDLE);
            }
        }

        private void prepareIdleScene(final String name) {
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    if (name.equals(getString(R.string.next_exit_enter))) {
                        setActorVisibility(mActorNext, false);
                    } else {
                        setActorVisibility(mActorBefore, false);
                    }
                    updateIndicators();
                    updatePageIndexText();
                }
            });
        }

        public void onPaused(Animation animation) {
            if (getActivity() == null) {
                return;
            }
            final String name = animation.getName();
            if (Media3D.DEBUG) {
                Log.v(TAG, "ani - onPaused: " + animation.getName() + " ," + animation + ", state = " + getState());
            }

            if (Main.ON_DRAG_MODE) {
                if (mRunningAnimation != null && mRunningAnimation.getName().equals(name)) {
                    getActivity().runOnUiThread(new Runnable() {
                        public void run() {
                            if (Media3D.DEBUG) {
                                Log.v(TAG, "removing: " + name);
                            }
                            mRunningAnimation = null;
                        }
                    });
                }
                if (name.equals(getString(R.string.next_exit_enter)) || name.equals(getString(R.string.last_exit_enter))) {
                    prepareIdleScene(name);
                }
            }
        }

        public void onCompleted(Animation animation) {
            if (getActivity() == null) {
                return;
            }
            final String name = animation.getName();
            if (Media3D.DEBUG) {
                Log.v(TAG, "ani - onCompleted: " + animation.getName() + " ," + animation);
            }

            if (isEnterExitAnimation(name) && getState() != IDLE) {
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        setState(IDLE);
                    }
                });
            }

            if (isEnterAnimation(name)) {
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        Animation ani = mAnimationLists.get(R.string.floating);
                        if (ani != null) {
                            ani.setDirection(Timeline.FORWARD);
                        }
                        startPhotoAnimation(R.string.floating);
                        showBackground(R.string.swap_in);
                    }
                });
            } else if (name.equals(getString(R.string.swap_in)) && getState() == IDLE) {
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        mPhotoOffsetIndex = getNextOffsetIndex();
                        showBackground(R.string.swap_in);
                    }
                });
            } else if (name.equals(getString(R.string.next_exit_enter)) ||
                    name.equals(getString(R.string.last_exit_enter))) {
                if (!Main.ON_DRAG_MODE) {
                    prepareIdleScene(name);
                }
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        if (!Main.ON_DRAG_MODE) {
                            mScrollDetector.onAnimationFinish();
                        }
                        Animation ani = mAnimationLists.get(R.string.floating);
                        if (ani != null) {
                            ani.setDirection(Timeline.FORWARD);
                        }
                        startPhotoAnimation(R.string.floating);
                        mPhotoOffsetIndex = 0;
                        showBackground(R.string.swap_in);
                    }
                });
            }

        }
    };

    private static final int REQUEST_GET_PHOTO = 2;

    private void choosePhotoFolder() {
        stopAllAnimations();
        setActorVisibility(mActorNow, false);
        mBackground.setEmptyImage();
        mBackground.setVisible(false);

        try {
            Intent intent = new Intent("com.mediatek.action.PICK_IMAGE_FOLDER");
            intent.setType("image/*");
            getActivity().startActivityForResult(intent, REQUEST_GET_PHOTO);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, e.toString());
            Intent intent = new Intent("android.appwidget.action.GET_PARAMS");
            intent.setType("image/*");
            intent.putExtra("fromAppWidgetConfigure", true);
            startActivityForResult(intent, REQUEST_GET_PHOTO);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (Media3D.DEBUG) {
            Log.v(TAG, "onActivityResult(): " + data);
        }
        if (requestCode == REQUEST_GET_PHOTO && resultCode == Activity.RESULT_OK) {
            String bid = data.getStringExtra("bucketId");
            int mediaTypes = data.getIntExtra("mediaTypes", 0);
            if (Media3D.DEBUG) {
                Log.v(TAG, String.format(
                        "onActivityResult(), result=%d, bucketId=%s, mediaTypes=%d",
                        resultCode, bid, mediaTypes));
            }

            if (bid != null && bid.length() > 0) {
                mBucketId = bid;
                setBucketId(mBucketId);
                prepareMediaItemSet(mBucketId);
                adjustPageIndex(true);
                // Reset actor references to original status.
                initPhotoActorsReference();
                initPhotoActorContent();
                updateBackgroundText();
            }
        }
        if (!isNoPhoto()) {
            showPhoto(R.string.next_enter);
        }

    }

    private ImageDbItemSet createImageDbItemSet(String bucketId) {
        return new ImageDbItemSet(getActivity().getContentResolver(), bucketId);
    }

    MediaContentObserver mObserver;
    private void prepareMediaItemSet(String bucketId) {
        if (mMediaItemSet != null) {
            mMediaItemSet.close();
        }

        if (Media3D.isDemoMode()) {
            final int [] demoImage = new int[] {
                    R.drawable.gg_hyoyeon, R.drawable.gg_jessica,
                    R.drawable.gg_seohyun, R.drawable.gg_sunny,
                    R.drawable.gg_taeyeon, R.drawable.gg_tiffany,
                    R.drawable.gg_yoona, R.drawable.gg_yuri };
            mMediaItemSet = new ResourceItemSet(
                getActivity().getResources(), demoImage, demoImage);
        } else {
            mMediaItemSet = createImageDbItemSet(bucketId);
            mObserver = new MediaContentObserver(mHandler);
            ((MediaDbItemSet)mMediaItemSet).registerObserver(mObserver);
        }
    }

    private void destroyMediaItemSet() {
        if (mMediaItemSet != null) {
            mMediaItemSet.close();
            mMediaItemSet = null;
        }
    }

    private void onSlideShowClicked() {
        if (mMediaItemSet.getItemCount() == 0) {
            return;
        }

        Uri targetUri = Uri.parse("content://media/external/images/media");
        if (mBucketId != null) {
            targetUri = targetUri.buildUpon().appendQueryParameter("bucketId", mBucketId).build();
        }

        if (Media3D.DEBUG) {
            Log.v(TAG, "onSlideShowClicked, targetUri = " + targetUri);
        }

        Intent intent = new Intent(Intent.ACTION_VIEW, targetUri);
        intent.putExtra("slideshow", true);
        intent.putExtra("repeat", false);
        startActivity(intent);
    }

    private void onViewClicked(int offsetIndex) {
        int index = getPageIndex() * PHOTO_PER_PAGE + offsetIndex;
        Uri targetUri = mMediaItemSet.getItem(index).getUri();
        if (Media3D.isDemoMode()) {
            Intent intent = new Intent(getActivity(), ImageViewer.class);
            intent.setData(targetUri);
            startActivity(intent);
        } else {
            Intent intent = new Intent(Intent.ACTION_VIEW, targetUri);
            startActivity(intent);
        }
    }

    private static Bitmap getThumbnail(MediaItem mi, int desiredWidth, int desiredHeight) {
        return mi.getThumbnail(desiredWidth, desiredHeight);
    }

    private static class ThumbLoader implements Runnable {
        private final ArrayList<Actor> mActors = new ArrayList<Actor>();
        private final ArrayList<MediaItem> mMediaItems = new ArrayList<MediaItem>();
        private Actor[] mOwner;
        private volatile boolean mIsCancelled;
        private Dimension mThumbDim;

        public void setThumbDim(Dimension dim) {
            mThumbDim = dim;
        }

        public void setOwner(Actor[] owner) {
            mOwner = owner;
        }

        public Actor[] getOwner() {
            return mOwner;
        }

        public boolean hasOwner() {
            return (mOwner != null);
        }

        public void add(Actor a, MediaItem mi) {
            if (a == null || mi == null) {
                throw new IllegalArgumentException();
            }
            mActors.add(a);
            mMediaItems.add(mi);
        }

        public int size() {
            return mActors.size();
        }

        public void clear() {
            mActors.clear();
            mMediaItems.clear();
            mOwner = null;
            mIsCancelled = false;
        }

        public void cancel() {
            mIsCancelled = true;
        }

        public void run() {
            final int count = mActors.size();
            LogUtil.v(TAG, "Loader start, job count: " + count);

            for (int i = 0; i < count; i++) {
                if (mIsCancelled) {
                    LogUtil.v(TAG, "Cancelled, at count: " + i);
                    break;
                }
                final MediaItem mi = mMediaItems.get(i);
                BitmapGenerator generator = new BitmapGenerator() {
                    public Bitmap generate() {
                        return getThumbnail(mi, (int)mThumbDim.width, (int)mThumbDim.height);
                    }
                };
                /**
                 * Set default bitmap for generator and if it generates null thumbnail,
                 * it will return default one we provided.
                 */
                generator.setDefaultBitmap(BLANK_BITMAP);
                generator.cacheBitmap();
                Image imageActor = Image.createFromBitmapGenerator(generator);
                ((Container) mActors.get(i)).add(imageActor);
                Thread.yield();
            }
            clear();
        }
    }

    private ExecutorService mExecutorService;
    private final ArrayList<ThumbLoader> mThumbLoaders = new ArrayList<ThumbLoader>();
    private final ArrayList<ThumbLoader> mRunningLoaders = new ArrayList<ThumbLoader>();
    private void releaseLoaders(boolean isForce) {
        for (ThumbLoader loader : mThumbLoaders) {
            if (loader.hasOwner()) {
                if (isForce) {
                    loader.cancel();
                } else {
                    mRunningLoaders.add(loader);
                }
            }
        }
        LogUtil.v(TAG, "Running loader count :" + mRunningLoaders.size());
        mThumbLoaders.clear();
        mThumbLoaders.addAll(mRunningLoaders);
        mRunningLoaders.clear();
    }

    private ThumbLoader getFreeLoader(Actor[] actors) {
        for (int i = 0 ; i < mThumbLoaders.size(); ++i) {
            ThumbLoader loader = mThumbLoaders.get(i);
            if (loader.getOwner() == actors) {
                loader.cancel();
                continue;
            }
            if (!loader.hasOwner()) {
                loader.setOwner(actors);
                loader.setThumbDim(mThumbDim);
                return loader;
            }
        }

        ThumbLoader newLoader = new ThumbLoader();
        newLoader.setOwner(actors);
        newLoader.setThumbDim(mThumbDim);
        mThumbLoaders.add(newLoader);
        LogUtil.v(TAG, "Total Loader :" + mThumbLoaders.size());
        return newLoader;
    }

    private static final int MAX_THREADS_NUM = 8;
    private void setPhotoActorContent(int pageIndex, Actor[] photoActor) {
        if (photoActor.length != PHOTO_PER_PAGE) {
            return;
        }

        ThumbLoader newLoader = null;
        int totalCount = mMediaItemSet.getItemCount();
        for (int i = 0; i < photoActor.length; i++) {
            int index = pageIndex * PHOTO_PER_PAGE + i;
            if (index >= totalCount) {
                break;
            }
            Container imageContainer = (Container) (((Container) (photoActor[i])).findChildByTag(PHOTO_TAG));
            MediaItem mi = mMediaItemSet.getItem(index);
            if (imageContainer != null && mi != null) {
                if (newLoader == null) {
                    newLoader = getFreeLoader(photoActor);
                }
                newLoader.add(imageContainer, mi);
            }
        }

        if (newLoader != null && newLoader.size() > 0) {
            if (mExecutorService == null) {
                mExecutorService = Executors.newFixedThreadPool(MAX_THREADS_NUM);
            }
            mExecutorService.submit(newLoader);
        }
    }

    private ExecutorService mBackgroundExecutorService;
    private Future<?> mBackgroundFuture;

    private static class BackgroundLoader implements Runnable {
        private final Image mActor;
        private final MediaItem mMediaItem;
        private final AnimationGroup mAniGrp;
        private final Dimension mBgDim;
        private final Dimension mTempDim;

        public BackgroundLoader(Image a, MediaItem mi, AnimationGroup aniGrp, Dimension tempDim, Dimension bgDim) {
            if (a == null || mi == null || aniGrp == null || bgDim == null) {
                throw new IllegalArgumentException();
            }
            mActor = a;
            mMediaItem = mi;
            mAniGrp = aniGrp;
            mTempDim = tempDim;
            mBgDim = bgDim;
        }

        public void run() {
            BitmapGenerator generator = new BitmapGenerator() {
                public Bitmap generate() {
                    return getThumbnail(mMediaItem, (int)mTempDim.width, (int)mTempDim.height);
                }
            };
            /**
             * Set default bitmap for generator and if it generates null thumbnail,
             * it will return default one we provided.
             */
            generator.setDefaultBitmap(BLANK_BITMAP);
            generator.cacheBitmap();
            mActor.setImageFromBitmapGenerator(generator);

            // If aspect ratio of image is 1.333, vertical line artifact occurred when applying simple blur effect.
            // Change aspect ratio and fit the image size with screen resolution.
            mActor.setSize(mBgDim);
            mAniGrp.stop();
            mAniGrp.getAnimation(0).setTarget(mActor);
            mAniGrp.start();
        }
    }

    private void setBackgroundPhotoActorContent(int animation) {
        int index = getPageIndex() * PHOTO_PER_PAGE + mPhotoOffsetIndex;
        if (index >= mMediaItemSet.getItemCount()) {
            throw new IllegalArgumentException("index > totalCount");
        } else {
            AnimationGroup aniGrp;
            if (animation == R.string.swap_in || animation == R.string.swap_out) {
                aniGrp = mAnimationLists.get(animation);
                BackgroundLoader bl = new BackgroundLoader(
                        mBackground, mMediaItemSet.getItem(index), aniGrp, mTempThumbDim, mBgDim);
                if (mBackgroundExecutorService == null) {
                    mBackgroundExecutorService = Executors.newSingleThreadExecutor();
                }

                if (mBackgroundFuture != null) {
                    mBackgroundFuture.cancel(false);
                }
                mBackgroundFuture = mBackgroundExecutorService.submit(bl);
            }
        }
    }

    private void leavePage(int name) {
        startPhotoAnimation(name);
    }

    private int getNextOffsetIndex() {
        if (mPhotoOffsetIndex == PHOTO_PER_PAGE - 1 ||
                getPageIndex() * PHOTO_PER_PAGE + mPhotoOffsetIndex ==
                        mMediaItemSet.getItemCount() - 1) {
            return 0;
        } else {
            return mPhotoOffsetIndex + 1;
        }
    }

    private void leaveBackground(int name) {
        if (mMediaItemSet.getItemCount() != 0) {
            startPhotoBackgroundAnimation(name);
        }
    }

    private void showBackground(int name) {
        if (mMediaItemSet.getItemCount() != 0) {
            setBackgroundPhotoActorContent(name);
        } else {
            mBackground.setVisible(false);
        }
    }

    @Override
    protected boolean onSingleTapConfirmed(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            Point pos = new Point(event.getX(), event.getY());
            if (Media3D.DEBUG) {
                Log.v(TAG, "onSingleTapConfirmed - down, Point = " + pos);
            }
            return hitTest(pos);
        }
        return false;
    }

    @Override
    protected boolean onTouchEvent(MotionEvent event) {
        return false;
    }

    private boolean hitTest(Point pos) {
        Actor hitActor = mStage.hitTest(pos);
        if (hitActor == null || mActorNow == null) {
            return false;
        }

        for (int i = 0; i < PHOTO_PER_PAGE; i++) {
            if (hitActor == mActorNow[i]) {
                onViewClicked(i);
                return true;
            }
        }

        return false;
    }

    private static final int CMD_CHOOSE_FOLDER = 1;
    private static final int CMD_ENTER_SLIDESHOW = 2;

    @Override
    public boolean onCreateBarMenu(NavigationBarMenu menu) {
        super.onCreateBarMenu(menu);
        menu.add(CMD_CHOOSE_FOLDER).setIcon(R.drawable.top_photo_folder);
        menu.add(CMD_ENTER_SLIDESHOW).setIcon(R.drawable.top_photo_slideshow);
        return true;
    }

    @Override
    public boolean onBarMenuItemSelected(NavigationBarMenuItem item) {
        int itemId = item.getItemId();
        if (itemId == CMD_CHOOSE_FOLDER) {
            choosePhotoFolder();
        } else if (itemId == CMD_ENTER_SLIDESHOW) {
            onSlideShowClicked();
        } else {
            return super.onBarMenuItemSelected(item);
        }
        return true;
    }

    @Override
    public Actor getThumbnailActor() {
        Container thumbContainer = new Container();
        Image thumbImage;

        // get mediaitemset first
        if (mMediaItemSet == null) {
            prepareMediaItemSet(getBucketId());
        }

        if (mMediaItemSet.getItemCount() == 0) {
            thumbImage = Image.createFromResource(getActivity().getResources(), R.drawable.mainmenu_photo_empty);
        } else {
            mPortalThumbDim = getLayoutManager().getDimension("photo_thumb_for_portal_size");
            final MediaItem mi = mMediaItemSet.getItem(0);
            BitmapGenerator generator = new BitmapGenerator() {
                public Bitmap generate() {
                    return getThumbnail(mi, (int)mPortalThumbDim.width, (int)mPortalThumbDim.height);
                }
            };
            /**
             * Set default bitmap for generator and if it generates null thumbnail,
             * it will return default one we provided.
             */
            generator.setDefaultBitmap(BLANK_BITMAP);
            generator.cacheBitmap();
            thumbImage = Image.createFromBitmapGenerator(generator);
        }
        thumbImage.setPosition(new Point(2, 0, +0.08f));
        thumbImage.setRotation(new Rotation(0, 0, -12));
        thumbImage.setReactive(false);
        thumbContainer.add(thumbImage);

        Image emptyImage = Image.createFromResource(getActivity().getResources(), R.drawable.portal_photo_none);
        emptyImage.setPosition(new Point(0, 0, -0.02f));
        emptyImage.setReactive(false);
        thumbContainer.add(emptyImage);
        return thumbContainer;
    }

    private String getString(int resId) {
        return getActivity().getString(resId);
    }

    private boolean switchPageIndex(boolean isNext) {
        if (isNoPhoto()) {
            return false;
        }

        if (isNext) {
            mPhotoPageIndex = getNextPageIndex();
            return true;

        } else {
            mPhotoPageIndex = getPrevPageIndex();
            return true;
        }
    }

    private void shiftActors(boolean isNext) {
        Actor[] actorTmp = isNext ? mActorBefore : mActorNext;
        // If page number is two, next item is current actors before shift.
        // For instance, (A, B, A) shit to (B, A, B)
        // Before shift, (ActorBefore, ActorNow, ActorNext) = (A, B, A)
        // no matter forward, or backward shift, both will reduce to
        // (ActorBefore, ActorNow, ActorNext) = (B, A, B)
        // Which means actorTmp should be B (which is ActorNow before shift)
        if (getTotalPhotoPage() == 2) {
            actorTmp = mActorNow;
        }
        if (isNext) {
            mActorBefore = mActorNow;
            mActorNow = mActorNext;
            mActorNext = actorTmp;
        } else {
            mActorNext = mActorNow;
            mActorNow = mActorBefore;
            mActorBefore = actorTmp;
        }
    }

    void prepareNewActorsIfNeeded(boolean isNext) {
        // While page number is less than or equal to the number of holders,
        // all actors have been hold by mActorBefore, mActorNow and mActorNext
        // there is no need to prepare new actors.
        if (getTotalPhotoPage() <= ACTORS_HOLDER_SIZE) {
            return;
        }

        if (isNext) {
            // prepare new ActorNext
            resetActor(getNextPageIndex(), mActorNext);
            mHandler.post(new Runnable() {
                public void run() {
                    setPhotoActorContent(getNextPageIndex(), mActorNext);
                }
            });
        } else {
            // prepare new ActorBefore
            resetActor(getPrevPageIndex(), mActorBefore);
            mHandler.post(new Runnable() {
                public void run() {
                    setPhotoActorContent(getPrevPageIndex(), mActorBefore);
                }
            });
        }
    }

    private Actor getIconLast() {
        if (mIconLast == null) {
            mIconLast = Image.createFromResource(getActivity().getResources(), R.drawable.ic_arrow_up);
            mIconLast.setPosition(getLayoutManager().getPoint("photo_prev_arrow_pos"));
            mIconLast.setVisible(false);
            getContainer().add(mIconLast);
        }
        return mIconLast;
    }

    private Actor getIconNext() {
        if (mIconNext == null) {
            mIconNext = Image.createFromResource(getActivity().getResources(), R.drawable.ic_arrow_up);
            mIconNext.setPosition(getLayoutManager().getPoint("photo_next_arrow_pos"));
            mIconNext.setRotation(new Rotation(0, 0, 180));
            mIconNext.setVisible(false);
            getContainer().add(mIconNext);
        }
        return mIconNext;
    }

    private boolean isNoPhoto() {
        return (mMediaItemSet.getItemCount() == 0);
    }

    private boolean isLessOrEqualOnePage() {
        return (getTotalPhotoPage() <= 1);
    }

    private void updateBackgroundText() {
        mNoPhotoText.setVisible(isNoPhoto());
    }

    private void updateIndicators() {
        final boolean isArrowVisible = !isLessOrEqualOnePage();
        getIconLast().setVisible(isArrowVisible);
        getIconNext().setVisible(isArrowVisible);
    }

    private void updatePageIndexText() {
        if (isNoPhoto()) {
            mPageIndexText.setVisible(false);
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(getPageIndex() + 1);
        sb.append("/");
        sb.append(getTotalPhotoPage());
        mPageIndexText.setText(sb.toString());
        mPageIndexText.setVisible(true);
    }

    public void movingActor(boolean isNext) {
        switchPageIndex(isNext);
        shiftActors(isNext);
        prepareNewActorsIfNeeded(isNext);
        updateIndicators();
        updatePageIndexText();
    }

    void startTransitionAnimation(int name, Actor[] actor1, Actor[] actor2, boolean accelerated) {
        AnimationGroup group = mAnimationLists.get(name);
        if (group == null) {
            return;
        }
        int index = 0;

        for (int i = 0; i < actor1.length; ++i) {
            Actor actor = actor1[i];
            if (((Container) actor).findChildByTag(PHOTO_TAG) == null) {
                group.getAnimation(index).setTarget(null);
            } else {
                actor.stopAnimations();
                group.getAnimation(index).setTarget(actor);
            }
            index++;
        }

        for (int i = 0; i < actor2.length; ++i) {
            Actor actor = actor2[i];
            if (((Container) actor).findChildByTag(PHOTO_TAG) == null) {
                group.getAnimation(index).setTarget(null);
            } else {
                actor.stopAnimations();
                group.getAnimation(index).setTarget(actor);
            }
            index++;
        }

        mRunningAniId = name;
        group.setDirection(Timeline.FORWARD);
        group.setTimeScale(accelerated ? ANIM_SPEED_FASTER : ANIM_SPEED_NORMAL);
        if (Main.ON_DRAG_MODE) {
            mRunningAnimation = group;
        } else {
            group.start();
        }
    }

    private void preparePreviousScene(boolean accelerated) {
        // next exit and next enter
        stopAllAnimations();
        shiftActors(false);
        prepareNewActorsIfNeeded(false);
        startTransitionAnimation(R.string.next_exit_enter, mActorNext, mActorNow, accelerated);
    }

    private void prepareNextScene(boolean accelerated) {
        // last exit and last enter
        stopAllAnimations();
        shiftActors(true);
        prepareNewActorsIfNeeded(true);
        startTransitionAnimation(R.string.last_exit_enter, mActorBefore, mActorNow, accelerated);
    }

    public boolean onDrag(PageDragHelper.State state, float disY) {
        if (Media3D.DEBUG) {
            Log.v(TAG, "onDrag(): " + state);
        }

        switch (state) {
            case INITIAL:
                if (mRunningAnimation != null && mRunningAnimation.isStarted()) {
                    mRunningAnimation.complete();
                    mRunningAnimation.stop();
                }
                break;
            case START:
                if (disY < 0) { // drag up
                    if (switchPageIndex(false)) {
                        preparePreviousScene(false);
                        mRunningAnimation.startDragging();
                    }
                } else {  // drag down
                    if (switchPageIndex(true)) {
                        prepareNextScene(false);
                        mRunningAnimation.startDragging();
                    }
                }
                break;
            case DRAGGING:
                if (mRunningAnimation != null) {
                    mRunningAnimation.setProgress(Math.abs(disY) / mDragMax);
                }
                break;
            case FINISH:
                if (mRunningAnimation != null) {
                    mRunningAnimation.stopDragging();
                    if (mRunningAnimation.getProgress() < mDragThreshold ||
                        isLessOrEqualOnePage()) {
                        mRunningAnimation.reverse();
                        if (mRunningAnimation.getName().equals(getString(R.string.last_exit_enter))) {
                            movingActor(mRunningAnimation.getDirection() == Timeline.FORWARD);
                        } else {
                            movingActor(mRunningAnimation.getDirection() != Timeline.FORWARD);
                        }
                    } else {
                        updatePageIndexText();
                    }
                }
                break;
            default:
                break;
        }
        return true;
    }

    public boolean onAction(PageTransitionDetector.Action action, boolean accelerated) {
        if (Media3D.DEBUG) {
            Log.v(TAG, "onAction(): " + action);
        }

        switch (action) {
            case DOWN:
                    if (switchPageIndex(false)) {
                        preparePreviousScene(accelerated);
                        return true;
                    } else {                    // TODO: bounding animation
                        if (Media3D.DEBUG) {
                            Log.v(TAG, "1st page");
                        }
                    }
                break;

            case UP:
                    if (switchPageIndex(true)) {
                        prepareNextScene(accelerated);
                        return true;
                    } else {                    // TODO: bounding animation
                        if (Media3D.DEBUG) {
                            Log.v(TAG, "1st page");
                        }
                    }
                break;

            case REVERSE:
                AnimationGroup group = mAnimationLists.get(mRunningAniId);
                if (group != null) {
                    group.reverse();
                    if (mRunningAniId == R.string.last_exit_enter) {
                        movingActor(group.getDirection() == Timeline.FORWARD);
                    } else {
                        movingActor(group.getDirection() != Timeline.FORWARD);
                    }
                }
                return true;

            case ACCELERATE:
                Animation ani = mAnimationLists.get(mRunningAniId);
                if (ani != null) {
                    ani.setTimeScale(ANIM_SPEED_FASTER);
                }
                return true;

            default:
                break;
        }
        return false;
    }

    private String getBucketId() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String bid = pref.getString(KEY_PHOTO_BUCKET_ID, MediaUtils.getDefaultBucketId());

        if (Media3D.DEBUG) {
            Log.v(TAG, "getBucketId - id = " + bid);
        }
        return bid;
    }

    private void setBucketId(String newBucketId) {
        if (newBucketId != null && newBucketId.length() > 0) {
            mBucketId = newBucketId;
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
            SharedPreferences.Editor editor = pref.edit();
            editor.putString(KEY_PHOTO_BUCKET_ID, newBucketId);
            editor.commit();
        }
    }

    @Override
    public void onResume() {
        if (mIsMediaItemSetModified) {
            stopPhotoAnimation(R.string.floating);
            prepareMediaItemSet(mBucketId);
            setActorVisibility(mActorNow, false);
            adjustPageIndex(false);
            initPhotoActorsReference();
            initPhotoActorContent();
            updateBackgroundText();
            showPhoto(R.string.next_enter);
            mIsMediaItemSetModified = false;
        }
        super.onResume();
    }

    private void adjustPageIndex(boolean reset) {
        if (reset) {
            mPhotoPageIndex = 0;
        } else {
            int lastPageIndex = (getTotalPhotoPage() > 0) ? (getTotalPhotoPage() - 1) : 0;
            if (lastPageIndex < mPhotoPageIndex) {
                mPhotoPageIndex = lastPageIndex;
            }
        }
        mPhotoOffsetIndex = 0;
    }

    private final Handler mHandler = new Handler();

    public void updateLocale(Locale locale) {
        if (mNoPhotoText != null) {
            mNoPhotoText.setText(getActivity().getResources().getString(R.string.no_photo_text));
        }
    }

    private class MediaContentObserver extends ContentObserver {
        public MediaContentObserver(Handler handler) {
            super(handler);
        }

        public void onChange(boolean selfChange, Uri uri) {
            mIsMediaItemSetModified = true;
        }
    }
}