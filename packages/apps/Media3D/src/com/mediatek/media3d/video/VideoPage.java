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

package com.mediatek.media3d.video;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MotionEvent;

import com.mediatek.media3d.LayoutManager;
import com.mediatek.media3d.Main;
import com.mediatek.media3d.Media3D;
import com.mediatek.media3d.MediaItem;
import com.mediatek.media3d.MediaItemSet;
import com.mediatek.media3d.MediaUtils;
import com.mediatek.media3d.NavigationBarMenu;
import com.mediatek.media3d.NavigationBarMenuItem;
import com.mediatek.media3d.Page;
import com.mediatek.media3d.PageDragHelper;
import com.mediatek.media3d.PageHost;
import com.mediatek.media3d.PageTransitionDetector;
import com.mediatek.media3d.R;
import com.mediatek.media3d.ResourceItemSet;
import com.mediatek.media3d.VideoPlayer;
import com.mediatek.ngin3d.Actor;
import com.mediatek.ngin3d.Color;
import com.mediatek.ngin3d.Container;
import com.mediatek.ngin3d.Dimension;
import com.mediatek.ngin3d.Image;
import com.mediatek.ngin3d.Point;
import com.mediatek.ngin3d.Rotation;
import com.mediatek.ngin3d.Stage;
import com.mediatek.ngin3d.Text;
import com.mediatek.ngin3d.android.StageView;
import com.mediatek.ngin3d.animation.Animation;
import com.mediatek.ngin3d.animation.AnimationGroup;
import com.mediatek.ngin3d.animation.AnimationLoader;
import com.mediatek.ngin3d.animation.Mode;
import com.mediatek.ngin3d.animation.PropertyAnimation;
import com.mediatek.ngin3d.animation.Timeline;
import com.mediatek.ngin3d.presentation.BitmapGenerator;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VideoPage extends Page implements PageTransitionDetector.ActionListener {
    private static final String TAG = "VideoPage";
    private static final int VIDEO_PER_PAGE = 4;

    private static final int STYLE_ENTER_RIGHT = 0;
    private static final int STYLE_EXIT_RIGHT = 1;
    private static final int STYLE_ENTER_LEFT = 2;
    private static final int STYLE_EXIT_LEFT = 3;
    private static final int STYLE_NEXT_ENTER = 4;
    private static final int STYLE_NEXT_EXIT = 5;
    private static final int STYLE_LAST_ENTER = 6;
    private static final int STYLE_LAST_EXIT = 7;
    private static final int STYLE_DRIFTING = 8;

    // an alias to group to common animation together.
    private static final int STYLE_GO_NEXT = 9;  // STYLE_NEXT_ENTER + STYLE_NEXT_EXIT;
    private static final int STYLE_GO_LAST = 10;  // STYLE_LAST_ENTER + STYLE_LAST_EXIT;
    private static final int STYLE_FADE_IN = 11;
    private static final int STYLE_FADE_OUT = 12;

    private Actor[] mActorBefore = new Actor[VIDEO_PER_PAGE];
    private Actor[] mActorNow = new Actor[VIDEO_PER_PAGE];
    private Actor[] mActorNext = new Actor[VIDEO_PER_PAGE];
    private Actor[] mActors;

    // This value effect how many actors will be decode if not exist related to current index
    // if it is already decoded and caches, it will not be re-decode before it is freed
    private static final int THUMB_PRELOAD_RANGE = VIDEO_PER_PAGE * 1;

    // If an actor is out of the preserve range of current index, it will be freed.
    // One could change the size of kept decoded actors by adjusting this value.
    // Enlarging the value could prevent frequent removal or decoding of actors, and
    // costs more memory.
    private static final int ACTOR_PRESERVE_RANGE = VIDEO_PER_PAGE * 3;

    private static final int ACTOR_TAG_BASE = 100;

    private int mIndex;     // current start index.
    private int mTotalPage = -1;     // total pages  (mCount + (VDIEO_PER_PAGE - 1)) / VIDEO_PER_PAGE
    private final Stage mStage;

    private String mBucketId;
    private MediaItemSet mVideoItemSet;

    private final AnimationGroup mAnimations = new AnimationGroup();
    private Animation mAnimBackgroundEnter;
    private Animation mAnimBackgroundExit;

    // alias for starting / stopping animation together
    private final AnimationGroup mAnimationGoNextAlias = new AnimationGroup();
    private final AnimationGroup mAnimationGoLastAlias = new AnimationGroup();

    private PropertyAnimation mFadeIn;
    private PropertyAnimation mFadeOut;
    private static final int FADE_DURATION = 240;

    private SharedPreferences mPrefs;
    private static final String KEY_VIDEO_BUCKET_ID = "video_bucket_id";

    private Actor mIconLast;
    private Actor mIconNext;
    private final Container mPageRoot = new Container();

    private final PageTransitionDetector mScrollDetector = new PageTransitionDetector(this);

    private AnimationGroup mRunningAnimation;

    private static final float ANIM_SPEED_NORMAL = 1.0f;
    private static final float ANIM_SPEED_FASTER = 1.2f;

    private int mLeavingStyle = STYLE_LAST_EXIT;

    private Text mPageIndexText;

    private static final Bitmap BLANK_BITMAP = Bitmap.createBitmap(2, 2, Bitmap.Config.RGB_565);

    private boolean mIsMediaItemSetModified;

    /**
     * Matches code in MediaProvider.computeBucketValues. Should be a common
     * function.
     */
    public static String getBucketId(String path) {
        return String.valueOf(path.toLowerCase(Locale.ENGLISH).hashCode());
    }

    private void setBucketId(String bucketId) {
        mBucketId = (bucketId == null) ? MediaUtils.CAMERA_IMAGE_BUCKET_ID : bucketId;
    }

    public VideoPage(Stage stage) {
        super(Page.SUPPORT_FLING);
        mStage = stage;
        // do most of the job in initialize.
    }

    private VideoDbItemSet createVideoDbItemSet() {
        return new VideoDbItemSet(getActivity().getContentResolver(), mBucketId);
    }

    private VideoDbItemSet createNullVideoDbItemSet() {
        return new VideoDbItemSet(getActivity().getContentResolver(), null);
    }

    private MediaItemSet getVideoItemSet() {
        if (mVideoItemSet == null) {
            if (Media3D.isDemoMode()) {
                final int demoDrawable[] = new int[] {
                        R.drawable.gg_hyoyeon, R.drawable.gg_jessica,
                        R.drawable.gg_seohyun, R.drawable.gg_sunny,
                        R.drawable.gg_taeyeon, R.drawable.gg_tiffany,
                        R.drawable.gg_yoona, R.drawable.gg_yuri };
                final int demoRaw[] = new int[] {
                        R.raw.gg_hyoyeon, R.raw.gg_jessica,
                        R.raw.gg_seohyun, R.raw.gg_sunny,
                        R.raw.gg_taeyeon, R.raw.gg_tiffany,
                        R.raw.gg_yoona, R.raw.gg_yuri };
                mVideoItemSet = new ResourceItemSet(getActivity().getResources(),
                        demoDrawable, demoRaw);
            } else {
                mVideoItemSet = createVideoDbItemSet();
            }
        }
        return mVideoItemSet;
    }

    private void destroyVideoItemSet() {
        if (mVideoItemSet != null) {
            mVideoItemSet.close();
            mVideoItemSet = null;
        }
    }

    /**
     *  This api is for or test code to obtain current video position on page.
     *
     *  @return current video index 1st video on page
     */
    public int getIndex() {
        return mIndex;
    }

    /**
     *  This api is for or test code to obtain total video count in selected folder
     *
     *  @return total video count in volder
     */
    public int getTotalVideoCount() {
        return getVideoItemSet().getItemCount();
    }

    private Actor [] getActorArray() {
        if (mActors == null) {
            int c = getTotalVideoCount();
            if (c == 0) {
                return null;
            } else {
                mActors = new Actor [c];
            }
        }
        return mActors;
    }

    private static class ThumbLoader implements Runnable {
        private final ArrayList<Actor> mActors = new ArrayList<Actor>();
        private final ArrayList<MediaItem> mVideoItems = new ArrayList<MediaItem>();
        private final Context mContext;
        private volatile boolean mIsCancelled;
        private int mOwner = -1;
        private Dimension mThumbDim;

        ThumbLoader(Context c, Dimension dim) {
            mContext = c;
            mThumbDim = dim;
        }

        public void setOwner(int ownerPage) {
            mOwner = ownerPage;
        }

        public int getOwner() {
            return mOwner;
        }

        public boolean hasOwner() {
            return (mOwner != -1);
        }


        public void add(Actor a, MediaItem mi) {
            if (a == null || mi == null) {
                throw new IllegalArgumentException();
            }
            mActors.add(a);
            mVideoItems.add(mi);
        }

        public int size() {
            return mActors.size();
        }

        public void clear() {
            mActors.clear();
            mVideoItems.clear();
            mOwner = -1;
            mIsCancelled = false;
        }

        public void cancel() {
            mIsCancelled = true;
        }

        public void run() {
            final int count = mActors.size();
            Log.v(TAG, "loader thread start, total job todo: " + count);

            final Paint p = new Paint();
            p.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
            p.setTextSize(StageView.dpToPixel(mContext, VideoThumbnailActor.getFontSize()));
            p.setAntiAlias(true);

            for (int i = 0; i < count; i++) {
                if (mIsCancelled) {
                    break;
                }

                final MediaItem mi = mVideoItems.get(i);
                BitmapGenerator generator = new BitmapGenerator() {
                    public Bitmap generate() {
                        Bitmap bmp = getThumbnail(mi, (int)mThumbDim.width, (int)mThumbDim.height);
                        VideoThumbnailActor.drawDuration(bmp, mi.getDuration(), p);
                        return bmp;
                    }
                };
                /**
                 * Set default bitmap for generator and if it generates null thumbnail,
                 * it will return default one we provided.
                 */
                generator.setDefaultBitmap(BLANK_BITMAP);
                generator.cacheBitmap();
                ((VideoThumbnailActor) mActors.get(i)).setThumbImage(generator);
                Thread.yield();
            }
            clear();
        }
    }

    private Actor getActor(int i, MediaItem mi) {
        if (mi == null) {
            return null;
        }

        Actor [] actors = getActorArray();
        if (actors == null) {
            return null;
        }
        // we don't really get the thumbnail here, just create actor.
        actors[i] = new VideoThumbnailActor(getActivity(), mi.getDuration());
        actors[i].setTag(i + ACTOR_TAG_BASE);
        actors[i].setVisible(false);
        mPageRoot.add(actors[i]);

        return actors[i];
    }

    private boolean isValidIndex(int index) {
        return (index >= 0) && (index < getTotalVideoCount());
    }

    private int remapIndex(int index) {
        final int pageAlignIndex = getTotalVideoPage() * VIDEO_PER_PAGE;
        if (index < 0) {
            return (index + pageAlignIndex);
        }
        if (index >= getTotalVideoCount()) {
            return (index - pageAlignIndex);
        }
        return index;
    }

    private static final int OUTSIDE_WINDOW_RANGE = VIDEO_PER_PAGE + THUMB_PRELOAD_RANGE;
    private static class IndexWindow {
        public static final int END_INDEX_SYMBOL = -1;
        private int mEndIndex = 0;
        private int mCurrentIndex = 0;
        private final int mIndexWindow [] = new int[ACTOR_PRESERVE_RANGE + 1];

        public void head() {
            mCurrentIndex = 0;
        }

        public boolean hasNext() {
            return (mCurrentIndex < mEndIndex);
        }

        public int next() {
            if (hasNext()) {
                final int value = mIndexWindow[mCurrentIndex];
                mCurrentIndex++;
                return value;
            }
            return END_INDEX_SYMBOL;
        }

        public void add(final int value) {
            if (contain(value)) {
                return;
            }
            if (isValidRange(mEndIndex)) {
                mIndexWindow[mEndIndex] = value;
                mEndIndex++;
            }
        }

        public void clear() {
            mEndIndex = 0;
            mCurrentIndex = 0;
            mIndexWindow[mEndIndex] = END_INDEX_SYMBOL;
        }

        private boolean isValidRange(final int index) {
            return (index >= 0) && (index < ACTOR_PRESERVE_RANGE);
        }

        private boolean contain(final int value) {
            for (int i = 0; i < mEndIndex; ++i) {
                if (value == mIndexWindow[i]) {
                    return true;
                }
            }
            return false;
        }
    }

    private final IndexWindow mIndexWindow = new IndexWindow();
    private void getOutsideWindow(int videoStartIndex) {
        mIndexWindow.clear();
        if (getTotalVideoCount() > ACTOR_PRESERVE_RANGE) {
            int index = 0;
            final int rightWindowStartIndex = remapIndex(videoStartIndex + OUTSIDE_WINDOW_RANGE);
            for (int i = 0; i < VIDEO_PER_PAGE; ++i) {
                index = rightWindowStartIndex + i;
                if (isValidIndex(index)) {
                    mIndexWindow.add(index);
                }
            }
            final int leftWindowStartIndex = remapIndex(videoStartIndex - OUTSIDE_WINDOW_RANGE);
            for (int i = 0; i < VIDEO_PER_PAGE; ++i) {
                index = leftWindowStartIndex + i;
                if (isValidIndex(index)) {
                    mIndexWindow.add(index);
                }
            }
        }
    }

    private void removeActorsOutsideWindow(int videoStartIndex) {
        getOutsideWindow(videoStartIndex);
        mIndexWindow.head();
        while (mIndexWindow.hasNext()) {
            int index = mIndexWindow.next();
            if (mActors[index] != null) {
                mPageRoot.remove(mActors[index]);
                mActors[index] = null;
            }
        }
    }

    private void getInsideWindow(int videoStartIndex) {
        mIndexWindow.clear();
        if (!isEmpty()) {
            int index = 0;
            int insideWindowStartIndex = videoStartIndex - THUMB_PRELOAD_RANGE;
            for (int i = 0; i < ACTOR_PRESERVE_RANGE; ++i) {
                index = remapIndex(insideWindowStartIndex + i);
                if (isValidIndex(index)) {
                    mIndexWindow.add(index);
                }
            }
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
        mThumbLoaders.clear();
        mThumbLoaders.addAll(mRunningLoaders);
        mRunningLoaders.clear();
    }

    private ThumbLoader getFreeLoader(int ownerPage) {
        cancelRunningLoader(ownerPage);

        for (ThumbLoader loader : mThumbLoaders) {
            if (!loader.hasOwner()) {
                loader.setOwner(ownerPage);
                return loader;
            }
        }

        ThumbLoader newLoader = new ThumbLoader(getActivity(), mThumbDim);
        newLoader.setOwner(ownerPage);
        mThumbLoaders.add(newLoader);
        return newLoader;
    }

    private void cancelRunningLoader(int ownerPage) {
        for (ThumbLoader loader : mThumbLoaders) {
            if (loader.getOwner() == ownerPage) {
                loader.cancel();
                return;
            }
        }
    }

    private static final int MAX_THREADS_NUM = 8;
    private void loadActorsInsideWindow(int videoStartIndex, MediaItemSet mis) {
        getInsideWindow(videoStartIndex);
        mIndexWindow.head();

        ThumbLoader newLoader = null;
        while (mIndexWindow.hasNext()) {
            int index = mIndexWindow.next();
            if (mActors[index] == null) {
                MediaItem mi = mis.getItem(index);
                Actor a = getActor(index, mi);
                if (newLoader == null) {
                    newLoader = getFreeLoader(videoStartIndex);
                }
                newLoader.add(a, mi);
            }
        }

        if (newLoader != null && newLoader.size() > 0) {
            if (mExecutorService == null) {
                mExecutorService = Executors.newFixedThreadPool(MAX_THREADS_NUM);
            }
            mExecutorService.submit(newLoader);
        }
    }

    private void prepareActors(int videoStartIndex) {
        Log.v(TAG, "prepareActors() from #" + videoStartIndex);
        if (getTotalVideoCount() == 0) {
            Log.v(TAG, "Ignore empty video list");
            return;
        }

        Actor actors[] = mActorNow;
        for (int i = 0; i < VIDEO_PER_PAGE; i++) {
            MediaItem mi = getVideoItemSet().getItem(videoStartIndex + i);
            actors[i] = (mi == null) ? null : mActors[videoStartIndex + i];
            if (actors[i] != null) {
                actors[i].setVisible(false);
            }
        }
    }

    private void prepareDriftingAnimations() {
        TypedArray anims =
            getActivity().getResources().obtainTypedArray(R.array.video_page_floating_anims);
        // the hash string resource id array. will invoke getString() later for this.
        String [] hashNames = getActivity().getResources().getStringArray(
                                  R.array.video_page_floating_anims_cache_name);

        AnimationGroup driftingGroup = new AnimationGroup();
        for (int i = 0; i < VIDEO_PER_PAGE; i++) {
            Animation a = AnimationLoader.loadAnimation(getActivity(),
                          anims.getResourceId(i, 0), hashNames[i]);
            if (a != null) {
                a.setTag(i);
                a.disableOptions(Animation.CAN_START_WITHOUT_TARGET);
                driftingGroup.add(a);
            }
        }
        driftingGroup.setLoop(true).setAutoReverse(true);
        driftingGroup.setTag(STYLE_DRIFTING);
        mAnimations.add(driftingGroup);
    }

    static final int FLAG_VIDEO_ENTER = Animation.SHOW_TARGET_ON_STARTED
                                        | Animation.ACTIVATE_TARGET_ON_COMPLETED;
    static final int FLAG_VIDEO_EXIT = Animation.HIDE_TARGET_ON_COMPLETED
                                       | Animation.DEACTIVATE_TARGET_ON_STARTED;

    private void prepareAnimations() {
        int [] styles = new int[] { STYLE_ENTER_RIGHT, STYLE_EXIT_RIGHT, STYLE_ENTER_LEFT,
                                    STYLE_EXIT_LEFT, STYLE_NEXT_ENTER, STYLE_NEXT_EXIT,
                                    STYLE_LAST_ENTER, STYLE_LAST_EXIT
                                  };

        TypedArray animations =
            getActivity().getResources().obtainTypedArray(R.array.video_page_animations);
        // the hash string resource id array. will invoke getString() later for this.
        String [] animHashNames = getActivity().getResources().getStringArray(
                                      R.array.video_page_animations_cache_name);

        // use a 2D for loop to load animation of each video object with different styles
        for (int i = 0; i < styles.length; i++) {       // per style
            AnimationGroup ag = new AnimationGroup();
            final int offset = i * VIDEO_PER_PAGE;
            for (int j = 0; j < VIDEO_PER_PAGE; j++) {  // per video
                Animation a = AnimationLoader.loadAnimation(
                                  getActivity(), animations.getResourceId(offset + j, 0),
                                  animHashNames[offset + j]);
                a.setTag(j);
                a.disableOptions(Animation.CAN_START_WITHOUT_TARGET);
                ag.add(a);
            }
            ag.setTag(styles[i]);
            ag.enableOptions(((i % 2) == 0) ? FLAG_VIDEO_ENTER : FLAG_VIDEO_EXIT);
            if (isEnterOrExitPageAnimation(styles[i])) {
                ag.addListener(mAnimationCompletedHandler);
            }
            mAnimations.add(ag);
        }
        prepareDriftingAnimations();

        // create an alias for next /last, for better reversing()
        mAnimationGoLastAlias.add(mAnimations.getAnimationByTag(STYLE_LAST_ENTER));
        mAnimationGoLastAlias.add(mAnimations.getAnimationByTag(STYLE_LAST_EXIT));
        mAnimationGoLastAlias.addListener(mAnimationCompletedHandler);
        mAnimationGoLastAlias.setTag(STYLE_GO_LAST);

        mAnimationGoNextAlias.add(mAnimations.getAnimationByTag(STYLE_NEXT_ENTER));
        mAnimationGoNextAlias.add(mAnimations.getAnimationByTag(STYLE_NEXT_EXIT));
        mAnimationGoNextAlias.addListener(mAnimationCompletedHandler);
        mAnimationGoNextAlias.setTag(STYLE_GO_NEXT);

        mAnimBackgroundEnter = AnimationLoader.loadAnimation(getActivity(),
                               R.raw.video_swap_enter_right_video_background,
                               getActivity().getResources().getString(
                                   R.string.video_swap_enter_right_video_background));
        if (mAnimBackgroundEnter != null) {
            mAnimBackgroundEnter.enableOptions(Animation.SHOW_TARGET_ON_STARTED);
            mAnimBackgroundEnter.setName("bgEnter");
        }
        mAnimBackgroundExit = AnimationLoader.loadAnimation(getActivity(),
                              R.raw.video_swap_exit_right_video_background,
                              getActivity().getString(
                                  R.string.video_swap_exit_right_video_background));
        if (mAnimBackgroundExit != null) {
            mAnimBackgroundExit.setTag(R.string.video_swap_exit_right_video_background);
            mAnimBackgroundExit.addListener(mAnimationCompletedHandler);
            mAnimBackgroundEnter.setName("bgExit");
        }

        Color invisDark = new Color(0, 0, 0, 0);
        Color fullShow = new Color(255, 255, 255, 255);

        mFadeIn = new PropertyAnimation(mPageRoot, "color", invisDark, fullShow);
        mFadeIn.setDuration(FADE_DURATION).setMode(Mode.LINEAR)
        .enableOptions(Animation.SHOW_TARGET_ON_STARTED);
        mFadeIn.setTag(STYLE_FADE_IN);
        mFadeIn.addListener(mAnimationCompletedHandler);

        mFadeOut = new PropertyAnimation(mPageRoot, "color", fullShow, invisDark);
        mFadeOut.setDuration(FADE_DURATION).setMode(Mode.LINEAR)
        .enableOptions(Animation.HIDE_TARGET_ON_COMPLETED);
        mFadeOut.setTag(STYLE_FADE_OUT);
        mFadeOut.addListener(mAnimationCompletedHandler);
    }

    private boolean isEnterOrExitPageAnimation(int tag) {
        return (tag == STYLE_ENTER_RIGHT || tag == STYLE_ENTER_LEFT
                || tag == STYLE_EXIT_RIGHT || tag == STYLE_EXIT_LEFT || tag == STYLE_NEXT_ENTER
                || tag == STYLE_LAST_EXIT || tag == STYLE_FADE_OUT || tag == STYLE_FADE_IN
                || (isEmpty() && tag == R.string.video_swap_exit_right_video_background));

        // (isEmpty() && tag ==R.string.video_swap_exit_right_video_background)
        // It is a special case, original design is to hide background, then
        // hide the actor, however there is no actor to hide when no video exists
        // so we consider in this case this is an exit animation.
        // I can't check only the tag directly otherwise
        // it will set idle before actor is hidden.
    }

    private boolean isEnterAnimation(int tag) {
        // STYLE_NEXT_ENTER is added here because it is using for entering from portal.
        return (tag == STYLE_GO_LAST || tag == STYLE_GO_NEXT || tag == STYLE_ENTER_LEFT
                || tag == STYLE_ENTER_RIGHT || tag == STYLE_NEXT_ENTER);
    }

    private void prepareIdleScene(final int tag) {
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                if (mRunningAnimation != null &&
                    mRunningAnimation.getTag() == tag &&
                    !mIsDragging) {
                    Log.v(TAG, "removing: " + tag);
                    mRunningAnimation = null;
                }
            }
        });

        if (tag == STYLE_GO_LAST || tag == STYLE_GO_NEXT) {
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    removeActorsOutsideWindow(getIndex());
                    loadActorsInsideWindow(getIndex(), getVideoItemSet());
                }
            });
        }
    }

    private final Animation.Listener mAnimationCompletedHandler = new Animation.Listener() {
        public void onPaused(Animation animation) {
            final int tag = animation.getTag();
            if (isEnterOrExitPageAnimation(tag)) {
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        setState(IDLE);
                    }
                });
            }

            if (Main.ON_DRAG_MODE) {
                prepareIdleScene(tag);
            }
        }

        public void onCompleted(Animation animation) {
            final int tag = animation.getTag();
            if (Media3D.DEBUG) {
                Log.v(TAG, "onCompleted(): " + tag);
            }

            // moving video actors in/out after background fade in/out is done
            if (tag == STYLE_FADE_IN) {
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        updateIndicators();
                        updatePageIndexText();
                    }
                });
                return;
            } else if (tag == R.string.video_swap_exit_right_video_background) {
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        if (!isEmpty()) {
                            Animation ani = getAnimationAndBindActor(mActorNow, mLeavingStyle);
                            if (ani != null) {
                                ani.start();
                            }
                        }
                    }
                });
                return;
            }
            if (!Main.ON_DRAG_MODE) {
                prepareIdleScene(tag);
            }

            if (isEnterAnimation(tag)) {
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        if (!mIsDragging) {
                            startAnimation(mActorNow, STYLE_DRIFTING);
                        }
                    }
                });
            }
        }
    };

    private Dimension mThumbDim;
    private Dimension mPortalThumbDim;
    private float mDragMax;
    private float mDragThreshold;

    public void initialize() {
        Log.v(TAG, "VideoPage.initialize()");
        if (mInitialized) {
            return;
        }

        mThumbDim = getLayoutManager().getDimension("video_thumb_size");
        mDragThreshold = getLayoutManager().getFloat("video_drag_threshold");
        mDragMax = getLayoutManager().getFloat("video_drag_maximum");

        getBackground().setVisible(false);
        loadSettings();
        setVideoIndex(0);

        getActorArray();
        if (getTotalVideoCount() > 0) {
            loadActorsInsideWindow(getIndex(), getVideoItemSet());
        }

        prepareActors(getIndex());
        prepareAnimations();
        mTotalPage = (-1);

        getIconLast();
        getIconNext();

        mPageIndexText = new Text();
        mPageIndexText.setPosition(getLayoutManager().getPoint("video_page_number_pos"));
        mPageIndexText.setScale(getLayoutManager().getScale("photo_page_number_scale"));
        mPageRoot.add(mPageIndexText);

        mStage.add(mPageRoot);
        mPageRoot.setVisible(false);
        super.initialize();
        Log.v(TAG, "VideoPage.initialize() done");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected Container getContainer() {
        return mPageRoot;
    }

    @Override
    public void onAdded(PageHost host) {
        super.onAdded(host);
    }

    @Override
    public void onLoad() {
        super.onLoad();
    }

    // will be the factor of VIDEO_PER_PAGE.
    private void setVideoIndex(int i) {
        int c = getTotalVideoCount();
        if (c == 0) {
            mIndex = 0;
        } else {
            mIndex = (i >= 0) ? i % c : (c + (i + 3 % c));
            mIndex = mIndex / VIDEO_PER_PAGE * VIDEO_PER_PAGE;
        }
        Log.v(TAG, "video index/total: " + mIndex + " / " + c);
    }

    // range: [0, mTotalPage)
    private boolean setVideoPage(int i) {
        if (i < 0 || i >= getTotalVideoPage()) {
            return false;
        }
        setVideoIndex(i * VIDEO_PER_PAGE);
        return true;
    }

    public int getVideoPage() {
        return (mIndex + VIDEO_PER_PAGE - 1) / VIDEO_PER_PAGE;
    }

    public int getTotalVideoPage() {
        if (mTotalPage == -1) {
            mTotalPage = (getTotalVideoCount() + VIDEO_PER_PAGE - 1) / VIDEO_PER_PAGE;
        }
        return mTotalPage;
    }

    public boolean haveNextPage() {
        return getVideoPage() + 1 < getTotalVideoPage();
    }

    public boolean haveLastPage() {
        return getVideoPage() > 0;
    }

    private boolean gotoNextPage() {
        int nextPageIndex = haveNextPage() ? (getVideoPage() + 1) : 0;
        return setVideoPage(nextPageIndex);
    }

    private boolean gotoPreviousPage() {
        int previousPageIndex = haveLastPage() ? (getVideoPage() - 1) : (getTotalVideoPage() - 1);
        return setVideoPage(previousPageIndex);
    }

    Text mNoVideoAlert = null;

    private Actor getNoVideoText() {
        if (mNoVideoAlert == null) {
            mNoVideoAlert = new Text(getActivity().getResources().getString(R.string.no_video_text));
            mNoVideoAlert.setTextColor(Color.BLACK);
            mNoVideoAlert.setPosition(getLayoutManager().getPoint("video_no_content_alert_pos"));
            mNoVideoAlert.setScale(getLayoutManager().getScale("video_no_content_alert_scale"));
            mPageRoot.add(mNoVideoAlert);
        }
        return mNoVideoAlert;
    }

    private AnimationGroup getAnimationAndBindActor(Actor[] actors, int style) {
        AnimationGroup ag = (AnimationGroup)mAnimations.getAnimationByTag(style);
        if (ag == null) {
            Log.e(TAG, "no animation found, hide actors!");
            for (int i = 0; i < actors.length; i++) {
                if (actors[i] != null) {
                    actors[i].setVisible(false);
                }
            }
            return null;
        }
        ag.setDirection(Timeline.FORWARD);
        ag.setTimeScale(ANIM_SPEED_NORMAL);
        Animation a;
        Actor actor;
        for (int i = 0; i < VIDEO_PER_PAGE; ++i) {
            actor = actors[i];
            a = ag.getAnimationByTag(i);
            if (actor != null) {
                actor.stopAnimations();
            }
            if (a != null) {
                a.setTarget(actor);
            }
        }
        return ag;
    }

    private void stopAnimation(int style) {
        AnimationGroup ag = (AnimationGroup)mAnimations.getAnimationByTag(style);
        if (ag != null) {
            ag.stop();
        }
    }

    private AnimationGroup startAnimation(Actor[] actors, int style) {
        AnimationGroup ag = getAnimationAndBindActor(actors, style);
        if (ag == null) {
            return null;
        }
        if (ag.isStarted()) {
            Log.e(TAG, "animation already started");
        } else {
            Log.v(TAG, "startAnimation: #" + ag.getTag());
            ag.start();
        }
        return ag;
    }

    private void showNoVideoText() {
        getNoVideoText().setVisible(true);
    }

    private void updatePageIndexText() {
        if (isEmpty()) {
            mPageIndexText.setVisible(false);
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(getVideoPage() + 1);
        sb.append("/");
        sb.append(getTotalVideoPage());
        mPageIndexText.setText(sb.toString());
        mPageIndexText.setVisible(true);
    }

    private boolean isEmpty() {
        return (getTotalVideoCount() == 0);
    }

    private boolean isLessOrEqualOnePage() {
        return (getTotalVideoPage() <= 1);
    }

    private void updateIndicators() {
        final boolean isArrowVisible = !isLessOrEqualOnePage();
        getIconLast().setVisible(isArrowVisible);
        getIconNext().setVisible(isArrowVisible);
    }

    private void showBackground() {
        mAnimBackgroundEnter.setTarget(getBackground()).start();
    }

    private void hideBackground() {
        mAnimBackgroundExit.setTarget(getBackground()).start();
    }

    private int getTransition(final int transitionType, final String action) {
        final boolean isEnter = action.equalsIgnoreCase("enter");
        switch (transitionType) {
            case TransitionType.INNER_TO_LEFT:
                return isEnter ? STYLE_ENTER_RIGHT : STYLE_EXIT_LEFT;
            case TransitionType.INNER_TO_RIGHT:
                return isEnter ? STYLE_ENTER_LEFT : STYLE_EXIT_RIGHT;
            case TransitionType.PORTAL_INNER:
                return isEnter ? STYLE_NEXT_ENTER : STYLE_LAST_EXIT;
            default:
                return STYLE_LAST_EXIT;
        }
    }

    private void enterPage(int type) {
        Log.v(TAG, "enterPage: " + type);
        initialize();
        getActorArray();            // in case mActor is null before but available now.
        prepareActors(getIndex());
        showBackground();
        if (isEmpty()) {
            showNoVideoText();
        }
        mFadeIn.start();
        Animation ani = getAnimationAndBindActor(mActorNow, getTransition(type, "enter"));
        if (ani != null) {
            ani.start();
        }

        if (mNoVideoAlert != null) {
            setState(IDLE);
        }
    }

    @Override
    protected void onPageEntering(int transitionType) {
        Log.v(TAG, String.format("onPageEntering(): transitionType: %d", transitionType));
        enterPage(transitionType);
        super.onPageEntering(transitionType);
        mScrollDetector.reset();
    }

    protected void updateStandIns(boolean visible) {
        for (Actor actor : mActorBefore) {
            if (actor != null) {
                actor.setVisible(visible);
            }
        }
        for (Actor actor : mActorNext) {
            if (actor != null) {
                actor.setVisible(visible);
            }
        }
    }

    protected void prepareDragLeaving() {
        stopAllAnimations();
        updateStandIns(false);
        getIconLast().setVisible(false);
        getIconNext().setVisible(false);
        if (mNoVideoAlert != null) {
            mNoVideoAlert.setVisible(false);
        }
        if (mPageIndexText != null) {
            mPageIndexText.setVisible(false);
        }
        super.prepareDragLeaving();
    }

    protected Animation prepareDragLeavingAnimation(int transitionType) {
        mLeavingStyle = getTransition(transitionType, "leave");
        if (mLeavingStyle == STYLE_LAST_EXIT) { // back to portal, hide everything together
            return mFadeOut;
        }

        return isEmpty() ? mAnimBackgroundExit.setTarget(getBackground()) :
            getAnimationAndBindActor(mActorNow, mLeavingStyle);
    }

    protected void revertDragLeaving() {
        updateStandIns(true);
        updateIndicators();
        if (mNoVideoAlert != null) {
            mNoVideoAlert.setVisible(true);
        }
        if (mPageIndexText != null) {
            mPageIndexText.setVisible(true);
        }
        startAnimation(mActorNow, STYLE_DRIFTING);
        super.revertDragLeaving();
    }

    @Override
    protected void onPageLeaving(int transitionType) {
        Log.v(TAG, "onPageLeaving: " + transitionType);
        if (isDragLeaving()) {
            setState(IDLE);
            return;
        }

        stopAllAnimations();
        updateStandIns(false);

        mLeavingStyle = getTransition(transitionType, "leave");
        if (mLeavingStyle == STYLE_LAST_EXIT) {
            mFadeOut.start();       // back to portal, hide everything together,
        } else {
            hideBackground();
        }

        getIconLast().setVisible(false);
        getIconNext().setVisible(false);

        if (mNoVideoAlert != null) {
            mNoVideoAlert.setVisible(false);
        }
        mScrollDetector.reset();
        releaseLoaders(false);
    }

    private void shiftActors(boolean next) {
        Actor[] actorTmp = next ? mActorBefore : mActorNext;

        if (next) {
            mActorBefore = mActorNow;
            mActorNow = mActorNext;
            mActorNext = actorTmp;
        } else {
            mActorNext = mActorNow;
            mActorNow = mActorBefore;
            mActorBefore = actorTmp;
        }
    }

    @Override
    public void onFling(int type) {
        Log.v(TAG, "onFling: " + type);

        if (getTotalVideoPage() <= 1) {
            return;     // ignore fling event if no item on screen, or only 1 page.
        }

        mScrollDetector.onFling(type);
    }

    public void cleanUpActors() {
        if (mActors != null) {
            for (int i = 0; i < mActors.length; ++i) {
                if (mActors[i] != null) {
                    mPageRoot.remove(mActors[i]);
                    mActors[i] = null;
                }
            }
            mActors = null;
        }
    }

    public void stopAllAnimations() {
        mAnimations.stop();
        mAnimationGoNextAlias.stop();
        mAnimationGoLastAlias.stop();
        if (mAnimBackgroundEnter != null) {
            mAnimBackgroundEnter.stop();
        }
        if (mAnimBackgroundExit != null) {
            mAnimBackgroundExit.stop();
        }
        if (mRunningAnimation != null) {
            mRunningAnimation.stop();
        }
        if (mFadeIn != null) {
            mFadeIn.stop();
        }
        if (mFadeOut != null) {
            mFadeOut.stop();
        }
    }

    @Override
    public void onRemoved() {
        stopAllAnimations();
        destroyVideoItemSet();
        cleanUpActors();
        super.onRemoved();
    }

    @Override
    public void onResume() {
        if (mIsMediaItemSetModified) {
            destroyVideoItemSet();
            getVideoItemSet();
            resetPageIndex();
            rebuildActors();
            enterPage(TransitionType.PORTAL_INNER);
            mIsMediaItemSetModified = false;
        }
        super.onResume();
    }

    private final int mRequestIDBase = this.hashCode();
    private static final int REQUEST_PICK_FOLDER = 0;

    private void fireFolderSelectActivity() {
        try {
            Intent i = new Intent("com.mediatek.action.PICK_VIDEO_FOLDER");
            i.setType("video/*");
            getActivity().startActivityForResult(i, mRequestIDBase + REQUEST_PICK_FOLDER);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, e.toString());
            Intent i = new Intent("android.appwidget.action.GET_PARAMS");
            i.setPackage("com.cooliris.media"); // uses gallery3D only.
            i.setType("video/*");
            startActivityForResult(i, mRequestIDBase + REQUEST_PICK_FOLDER);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.v(TAG, "onActivityResult(): " + data);
        if (requestCode == mRequestIDBase + REQUEST_PICK_FOLDER
                && resultCode == Activity.RESULT_OK) {
            String bid = data.getStringExtra("bucketId");
            int mediaTypes = data.getIntExtra("mediaTypes", 0);
            Log.v(TAG, String.format("onActivityResult(), result=%d, bucketId=%s, mediaTypes=%d",
                                     resultCode, bid, mediaTypes));
            setBucketId(bid);
            saveSettings();
            destroyVideoItemSet();
            resetPageIndex();
            rebuildActors();
            enterPage(TransitionType.PORTAL_INNER);
        }
    }

    private void fireVideoPlayer(Uri uri) {
        Log.v(TAG, "fireVideoPlayer(): " + uri);
        Intent intent;
        if (uri.getScheme().equals(ContentResolver.SCHEME_ANDROID_RESOURCE)) {
            intent = new Intent(getActivity(), VideoPlayer.class);
            intent.setData(uri);
        } else {
            intent = new Intent(Intent.ACTION_VIEW, uri);
        }
        startActivity(intent);
    }

    /* preferences related */
    private void loadSettings() {
        if (mPrefs == null) {
            mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        }
        mBucketId = mPrefs.getString(KEY_VIDEO_BUCKET_ID, null);
        Log.v(TAG, "shared preference loaded, id: " + mBucketId);
        if (mBucketId == null) {
            mBucketId = MediaUtils.getDefaultBucketId();
            saveSettings();
        }
    }

    private void saveSettings() {
        Log.v(TAG, "saveSettings()");
        SharedPreferences.Editor edit = mPrefs.edit();
        edit.putString(KEY_VIDEO_BUCKET_ID, mBucketId);
        edit.commit();
    }

    // check if an on screen actor is hit
    private Actor hitTest(MotionEvent e) {
        Point pos = new Point(e.getX(), e.getY());
        Actor a = mPageRoot.hitTest(pos);
        if (a != null) {
            int tag = a.getTag() - ACTOR_TAG_BASE;
            // check if the hitted actor is on screen.
            if (tag >= getIndex() && tag < getIndex() + VIDEO_PER_PAGE) {
                return a;
            }
        }
        return null;
    }

    @Override
    protected boolean onSingleTapConfirmed(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_DOWN) {
            return false;
        }

        // check if an actor on screen is hitted.
        Actor a = hitTest(event);
        if (a != null) {
            fireVideoPlayer(getVideoItemSet().getItem(a.getTag() - ACTOR_TAG_BASE).getUri());
            return true;
        }
        return false;
    }

    @Override
    protected boolean onTouchEvent(MotionEvent event) {
        return false;
    }

    private Image mBackground;

    private Actor getBackground() {
        if (mBackground == null) {
            mBackground = Image.createFromResource(getActivity().getResources(), R.drawable.video_background);
            mBackground.setScale(getLayoutManager().getScale("video_bg_scale"));
            mPageRoot.add(mBackground);
        }
        return mBackground;
    }

    private Actor getIconLast() {
        if (mIconLast == null) {
            mIconLast = Image.createFromResource(getActivity().getResources(), R.drawable.ic_arrow_up);
            mIconLast.setPosition(getLayoutManager().getPoint("video_prev_arrow_pos"));
            mIconLast.setVisible(false);
            mPageRoot.add(mIconLast);
        }
        return mIconLast;
    }

    private Actor getIconNext() {
        if (mIconNext == null) {
            mIconNext = Image.createFromResource(getActivity().getResources(), R.drawable.ic_arrow_up);
            mIconNext.setPosition(getLayoutManager().getPoint("video_next_arrow_pos"));
            mIconNext.setRotation(new Rotation(0, 0, 180));
            mIconNext.setVisible(false);
            mPageRoot.add(mIconNext);
        }
        return mIconNext;
    }

    private static final int CMD_CHOOSE_FOLDER = 1;

    @Override
    public boolean onCreateBarMenu(NavigationBarMenu menu) {
        super.onCreateBarMenu(menu);
        menu.add(CMD_CHOOSE_FOLDER).setIcon(R.drawable.top_photo_folder);
        return true;
    }

    @Override
    public boolean onBarMenuItemSelected(NavigationBarMenuItem item) {
        if (item.getItemId() == CMD_CHOOSE_FOLDER) {
            fireFolderSelectActivity();
            return true;
        }
        return super.onBarMenuItemSelected(item);
    }

    public void movingActor(boolean next) {
        if (next) {
            gotoNextPage();
        } else {
            gotoPreviousPage();
        }
        shiftActors(next);
        updateIndicators();
        updatePageIndexText();
    }

    private void startAnimation(AnimationGroup ag, boolean accelerated) {
        mRunningAnimation = ag;
        mRunningAnimation.setDirection(Timeline.FORWARD);
        mRunningAnimation.setTimeScale(accelerated ? ANIM_SPEED_FASTER : ANIM_SPEED_NORMAL);
        mRunningAnimation.start();
        Log.v(TAG, "Start animation: #" + mRunningAnimation.getTag());
    }

    private void preparePreviousScene() {
        stopAnimation(STYLE_DRIFTING);
        getAnimationAndBindActor(mActorNow, STYLE_NEXT_EXIT);
        shiftActors(false);
        if (!isLessOrEqualOnePage()) {
            prepareActors(getIndex());
        }
        getAnimationAndBindActor(mActorNow, STYLE_NEXT_ENTER);
        updateIndicators();
    }

    private void prepareNextScene() {
        stopAnimation(STYLE_DRIFTING);
        getAnimationAndBindActor(mActorNow, STYLE_LAST_EXIT);
        shiftActors(true);
        if (!isLessOrEqualOnePage()) {
            prepareActors(getIndex());
        }
        getAnimationAndBindActor(mActorNow, STYLE_LAST_ENTER);
        updateIndicators();
    }

    private void rollbackActor() {
        if (mRunningAnimation.getTag() == STYLE_GO_NEXT) {
            movingActor(mRunningAnimation.getDirection() != Timeline.FORWARD);
        } else {
            movingActor(mRunningAnimation.getDirection() == Timeline.FORWARD);
        }
    }

    private boolean mIsDragging;
    public boolean onDrag(PageDragHelper.State state, float disY) {
        switch (state) {
        case INITIAL:
            if (mRunningAnimation != null && mRunningAnimation.isStarted()) {
                mRunningAnimation.complete();
                mRunningAnimation.stop();
            }
            break;
        case START:
            if (disY < 0) {
                if (gotoPreviousPage()) {
                    mIsDragging = true;
                    preparePreviousScene();
                    mAnimationGoNextAlias.startDragging();
                    mRunningAnimation = mAnimationGoNextAlias;
                }
            } else {
                if (gotoNextPage()) {
                    mIsDragging = true;
                    prepareNextScene();
                    mAnimationGoLastAlias.startDragging();
                    mRunningAnimation = mAnimationGoLastAlias;
                }
            }
            break;
        case DRAGGING:
            if (mRunningAnimation != null) {
                mRunningAnimation.setProgress(Math.abs(disY) / mDragMax);
            }
            break;
        case FINISH:
            if (mRunningAnimation != null && mIsDragging) {
                mIsDragging = false;
                mRunningAnimation.stopDragging();
                if (mRunningAnimation.getProgress() < mDragThreshold ||
                    isLessOrEqualOnePage()) {
                    mRunningAnimation.reverse();
                    rollbackActor();
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
        Log.v(TAG, "onAction(): " + action);
        switch (action) {
        case DOWN:
            if (gotoPreviousPage()) {
                preparePreviousScene();

                startAnimation(mAnimationGoNextAlias, accelerated);
                return true;
            } else {                    // TODO: bounding animation
                Log.v(TAG, "last page");
            }
            break;

        case UP:
            if (gotoNextPage()) {
                prepareNextScene();

                startAnimation(mAnimationGoLastAlias, accelerated);
                return true;
            } else {                    // TODO: bounding animation
                Log.v(TAG, "1st page");
            }
            break;

        case REVERSE:
            // TODO: change
            Log.v(TAG, "animation reverse: #" + mRunningAnimation.getTag());
            mRunningAnimation.reverse();
            mRunningAnimation.start();
            rollbackActor();
            return true;

        case ACCELERATE:
            mRunningAnimation.setTimeScale(ANIM_SPEED_FASTER);
            return true;

        default:
            break;
        }
        return false;
    }

    @Override
    public Actor getThumbnailActor() {
        Image iOverlay = Image.createFromResource(getActivity().getResources(),
                R.drawable.portal_video_none);
        iOverlay.setPosition(new Point(0, 0, -0.6f));
        iOverlay.setReactive(false);

        if (mBucketId == null) {
            loadSettings();
        }

        Image iThumb;
        if (isEmpty()) {
            iThumb = Image.createFromResource(getActivity().getResources(),
                    R.drawable.mainmenu_video_empty);
        } else {
            mPortalThumbDim = getLayoutManager().getDimension("video_thumb_for_portal_size");
            final MediaItem mi = getVideoItemSet().getItem(0);
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
            iThumb = Image.createFromBitmapGenerator(generator);
        }
        iThumb.setRotation(new Rotation(0, 0, 15));
        iThumb.setPosition(new Point(0, 0, +0.1f));
        iThumb.setReactive(false);

        Container thumbContainer = new Container();
        thumbContainer.add(iThumb, iOverlay);
        return thumbContainer;
    }

    private static Bitmap getThumbnail(MediaItem mi, int desiredWidth, int desiredHeight) {
        return mi.getThumbnail(desiredWidth, desiredHeight);
    }

    private void resetPageIndex() {
        setVideoIndex(0);
        mTotalPage = (-1);
    }

    private void rebuildActors() {
        cleanUpActors();
        getActorArray();
        loadActorsInsideWindow(getIndex(), getVideoItemSet());
        if (getTotalVideoCount() > 0 && (mNoVideoAlert != null)) {
            mNoVideoAlert.setVisible(false);
            mNoVideoAlert = null;
        }
    }

    public void updateLocale(Locale locale) {
        if (mNoVideoAlert != null) {
            mNoVideoAlert.setText(getActivity().getResources().getString(R.string.no_video_text));
        }
    }
}
