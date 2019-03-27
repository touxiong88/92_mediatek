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

package com.mediatek.media3d.portal;

import android.content.res.TypedArray;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import com.mediatek.media3d.FadeInOutAnimation;
import com.mediatek.media3d.Media3D;
import com.mediatek.media3d.Page;
import com.mediatek.media3d.PageHost;
import com.mediatek.media3d.R;
import com.mediatek.media3d.photo.PhotoPage;
import com.mediatek.media3d.video.VideoPage;
import com.mediatek.media3d.weather.WeatherPage;
import com.mediatek.ngin3d.Actor;
import com.mediatek.ngin3d.Container;
import com.mediatek.ngin3d.Image;
import com.mediatek.ngin3d.Point;
import com.mediatek.ngin3d.Rotation;
import com.mediatek.ngin3d.Stage;
import com.mediatek.ngin3d.Text;
import com.mediatek.ngin3d.animation.Animation;
import com.mediatek.ngin3d.animation.AnimationGroup;
import com.mediatek.ngin3d.animation.AnimationLoader;
import com.mediatek.ngin3d.animation.PropertyAnimation;

import java.util.HashMap;
import java.util.Locale;

public class PortalPage extends Page {
    private static final String TAG = "PortalPage";
    private final Stage mStage;
    private Container mContainer;
    private final HashMap<Integer, Animation> mAnimations = new HashMap<Integer, Animation>();

    private Image mWaterSurface;
    private final Container mLoadingContainer = new Container();
    private final Handler mTimeHandler = new Handler();

    private String mNextPageName;
    private static final boolean ENABLE_LOADING_PAGE = true;
    private static final boolean ENABLE_PORTAL_THUMBNAIL = true;

    private static final int FRONT_ACTOR_TAG = 1;
    private Animation mEnteredAni;
    private boolean[] mPageInitialized = new boolean[3];

    private static final int[] DEFAULT_IMAGES = new int[] {
        R.drawable.portal_weather_demo,
        R.drawable.portal_photo_demo,
        R.drawable.portal_video_demo
    };

    private static final String[] CONTAINER_STRING = {
        "weather_container",
        "photo_container",
        "video_container"
    };

    public PortalPage(Stage stage) {
        super(0);
        mStage = stage;
    }

    @Override
    public int getPageType() {
        return PageType.PORTAL;
    }

    private void loadDefaultThumbnailContainer() {
        for (int i = 0; i < DEFAULT_IMAGES.length; i++) {
            Container c = new Container();
            c.setTag(DEFAULT_IMAGES[i]);

            if (ENABLE_PORTAL_THUMBNAIL) {
                Image frontActor = Image.createFromResource(getActivity().getResources(), DEFAULT_IMAGES[i]);
                frontActor.setReactive(false);
                frontActor.setPosition(new Point(0, 0, -0.02f));
                frontActor.setName("front");
                frontActor.setTag(FRONT_ACTOR_TAG);
                c.add(frontActor);
            }

            Image backActor = Image.createFromResource(getActivity().getResources(), R.drawable.mainmenu_blank2);
            backActor.setReactive(false);
            backActor.setPosition(new Point(0, 0, +2f));
            backActor.setRotation(new Rotation(0, 180, 0));
            backActor.setName("back");
            c.add(backActor);
            c.setName(CONTAINER_STRING[i]);
            mContainer.add(c);
        }
    }

    private Text[] mLables;
    private Text mLoadingText;
    private Text mAppTitleText;

    @Override
    public void onAdded(PageHost host) {
        super.onAdded(host);
        mContainer = new Container();
        loadDefaultThumbnailContainer();

        Image background = Image.createFromResource(getActivity().getResources(), R.drawable.portal_background);
        background.setTag(R.drawable.portal_background);
        background.setPosition(new Point(0.5f, 0.5f, 100, true));
        background.setScale(getLayoutManager().getScale("portal_bg_scale"));
        mContainer.add(background);

        mAppTitleText = new Text(getActivity().getResources().getString(R.string.app_name));
        mAppTitleText.setAnchorPoint(new Point(0, 0));
        mAppTitleText.setPosition(getLayoutManager().getPoint("portal_app_title_pos"));
        mAppTitleText.setScale(getLayoutManager().getScale("portal_app_title_scale"));
        mContainer.add(mAppTitleText);

        mWaterSurface = Image.createFromResource(getActivity().getResources(), R.drawable.perlin_noise);
        mContainer.add(mWaterSurface);
        mWaterSurface.setPosition(getLayoutManager().getPoint("portal_water_pos"));
        mWaterSurface.setScale(getLayoutManager().getScale("portal_water_scale"));
        mWaterSurface.setRotation(new Rotation(273, 0, 0));

        mLables = new Text[] {
            new Text(getActivity().getResources().getString(R.string.enter_weather)),
            new Text(getActivity().getResources().getString(R.string.enter_photo)),
            new Text(getActivity().getResources().getString(R.string.enter_video))
        };

        String[] lablePosIds = new String[] {
                "portal_weather_title_pos",
                "portal_photo_title_pos",
                "portal_video_title_pos"
        };

        for (int i = 0; i < mLables.length; i++) {
            mLables[i].setPosition(getLayoutManager().getPoint(lablePosIds[i]));
            mLables[i].setScale(getLayoutManager().getScale("portal_label_scale"));
            mLables[i].setMaxWidth(getLayoutManager().getInteger("portal_title_max_width"));
            mLables[i].setMaxLines(2);
            mLables[i].setEllipsizeStyle(Text.ELLIPSIZE_BY_3DOT);
            mContainer.add(mLables[i]);
        }

        mLoadingText = new Text(getActivity().getResources().getString(R.string.loading));
        mLoadingText.setPosition(getLayoutManager().getPoint("portal_loading_text_pos"));
        mLoadingText.setScale(getLayoutManager().getScale("portal_load_text_scale"));
        mLoadingContainer.add(mLoadingText);

        Image loadingImage = Image.createFromResource(getActivity().getResources(), R.drawable.spinner_black_48);
        loadingImage.setTag(R.drawable.spinner_black_48);
        loadingImage.setPosition(getLayoutManager().getPoint("portal_spinner_image_pos"));
        loadingImage.setScale(getLayoutManager().getScale("portal_load_image_scale"));
        mLoadingContainer.add(loadingImage);
        mLoadingContainer.setVisible(false);
        mContainer.add(mLoadingContainer);
        mStage.add(mContainer);

        /* enter */
        AnimationGroup group = addAnimationList(R.string.enter,
            R.array.portal_enter, R.array.portal_enter_strings);

        Animation ani = group.getAnimationByTag(R.raw.portal_top_weather_in_ani);
        if (ani != null) {
            ani.setTarget(mContainer.findChildByTag(R.drawable.portal_weather_demo));
        }
        ani = group.getAnimationByTag(R.raw.portal_top_photo_in_ani);
        if (ani != null) {
            ani.setTarget(mContainer.findChildByTag(R.drawable.portal_photo_demo));
        }

        ani = group.getAnimationByTag(R.raw.portal_top_video_in_ani);
        if (ani != null) {
            ani.setTarget(mContainer.findChildByTag(R.drawable.portal_video_demo));
        }

        FadeInOutAnimation fadeIn = new FadeInOutAnimation(mContainer, FadeInOutAnimation.FadeType.IN);
        fadeIn.setTag(R.string.fade_in);
        group.add(fadeIn);          // also add fade-in into animation group

        /* subpages popping animations */
        int outAnimationOptions = Animation.DEACTIVATE_TARGET_DURING_ANIMATION;
        AnimationGroup weatherOut = addAnimationList(R.string.weather_out,
            R.array.weather_out, R.array.weather_out_strings);

        ani = weatherOut.getAnimationByTag(R.raw.portal_top_weather_out_ani);
        if (ani != null) {
            ani.setTarget(mContainer.findChildByTag(R.drawable.portal_weather_demo));
            ani.enableOptions(outAnimationOptions);
        }

        AnimationGroup photoOut = addAnimationList(R.string.photo_out,
            R.array.photo_out, R.array.photo_out_strings);
        ani = photoOut.getAnimationByTag(R.raw.portal_top_photo_out_ani);
        if (ani != null) {
            ani.setTarget(mContainer.findChildByTag(R.drawable.portal_photo_demo));
            ani.enableOptions(outAnimationOptions);
        }

        AnimationGroup videoOut = addAnimationList(R.string.video_out,
            R.array.video_out, R.array.video_out_strings);
        ani = videoOut.getAnimationByTag(R.raw.portal_top_video_out_ani);
        if (ani != null) {
            ani.setTarget(mContainer.findChildByTag(R.drawable.portal_video_demo));
            ani.enableOptions(outAnimationOptions);
        }
    }

    private final Animation.Listener mAnimationListener = new Animation.Listener() {
        public void onCompleted(final Animation animation) {
            if (animation == mAnimations.get(R.string.enter)) {
                if (Media3D.DEBUG) {
                    Log.v(TAG, "onCompleted: page = " + PortalPage.this + " , ani = enter");
                }
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        setState(IDLE);
                    }
                });
            } else if (animation == mAnimations.get(R.string.weather_out)) {
                if (Media3D.DEBUG) {
                    Log.v(TAG, "onCompleted: page = " + PortalPage.this + " , ani = weather_out");
                }
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        if (animation.getDirection() == Animation.FORWARD) {
                            if (ENABLE_LOADING_PAGE && !mPageInitialized[0]) {
                                mNextPageName = PageHost.WEATHER;
                                showLoading();
                                Page nextPage = getHost().getPage(mNextPageName);
                                if (nextPage != null) {
                                    ((WeatherPage)nextPage).initialize();
                                }
                                mPageInitialized[0] = true;
                            } else {
                                getContainer().setVisible(false);
                                getHost().enterPage(PageHost.WEATHER);
                            }
                        } else {
                            setState(IDLE);
                        }
                    }
                });
            } else if (animation == mAnimations.get(R.string.photo_out)) {
                if (Media3D.DEBUG) {
                    Log.v(TAG, "onCompleted: page = " + PortalPage.this + " , ani = photo_out");
                }
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        if (animation.getDirection() == Animation.FORWARD) {
                            if (ENABLE_LOADING_PAGE && !mPageInitialized[1]) {
                                mNextPageName = PageHost.PHOTO;
                                showLoading();
                                Page nextPage = getHost().getPage(mNextPageName);
                                if (nextPage != null) {
                                    ((PhotoPage)nextPage).initialize();
                                }
                                mPageInitialized[1] = true;
                            } else {
                                getContainer().setVisible(false);
                                getHost().enterPage(PageHost.PHOTO);
                            }
                        } else {
                            setState(IDLE);
                        }
                    }
                });
            } else if (animation == mAnimations.get(R.string.video_out)) {
                if (Media3D.DEBUG) {
                    Log.v(TAG, "onCompleted: page = " + PortalPage.this + " , ani = video_out");
                }
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        if (animation.getDirection() == Animation.FORWARD) {
                            if (ENABLE_LOADING_PAGE && !mPageInitialized[2]) {
                                mNextPageName = PageHost.VIDEO;
                                showLoading();
                                Page nextPage = getHost().getPage(mNextPageName);
                                if (nextPage != null) {
                                    ((VideoPage)nextPage).initialize();
                                }
                                mPageInitialized[2] = true;
                            } else {
                                getContainer().setVisible(false);
                                getHost().enterPage(PageHost.VIDEO);
                            }
                        } else {
                            setState(IDLE);
                        }
                    }
                });
            }
        }
    };

    private final Runnable mTimerRun = new Runnable() {
        public void run() {
            Actor spinner = mLoadingContainer.findChildByTag(R.drawable.spinner_black_48);
            if (spinner != null) {
                spinner.stopAnimations();
            }
            mLoadingContainer.setVisible(false);

            String pageName = mNextPageName;
            mNextPageName = null;
            if (getHost() != null) {
                getHost().enterPage(pageName);
            }
        }
    };

    private void showLoading() {
        Rotation start = new Rotation(0, 0, 0);
        Rotation end = new Rotation(0, 0, 360);
        Actor spinner = mLoadingContainer.findChildByTag(R.drawable.spinner_black_48);
        PropertyAnimation ani = new PropertyAnimation(spinner, "rotation", start, end);
        ani.setDuration(800).setLoop(true).start();
        mLoadingContainer.setVisible(true);
        mTimeHandler.removeCallbacks(mTimerRun);
        mTimeHandler.postDelayed(mTimerRun, 1200);
    }

    public void cancelLoading() {
        mTimeHandler.removeCallbacks(mTimerRun);
        Animation anim = null;
        if (mNextPageName.equalsIgnoreCase(PageHost.WEATHER)) {
            anim = mAnimations.get(R.string.weather_out);
        } else if (mNextPageName.equalsIgnoreCase(PageHost.PHOTO)) {
            anim = mAnimations.get(R.string.photo_out);
        } else if (mNextPageName.equalsIgnoreCase(PageHost.VIDEO)) {
            anim = mAnimations.get(R.string.video_out);
        }
        if (anim != null) {
            anim.setDirection(Animation.BACKWARD);
            anim.start();
        }
        Actor spinner = mLoadingContainer.findChildByTag(R.drawable.spinner_black_48);
        if (spinner != null) {
            spinner.stopAnimations();
        }
        mLoadingContainer.setVisible(false);
        mNextPageName = null;
        mIsUnderPageTransition = false;
    }

    public boolean isShowLoading() {
        return (mNextPageName != null);
    }

    private AnimationGroup addAnimationList(int nameResId, int animArrayResId, int cacheNameArrayResId) {
        AnimationGroup group = new AnimationGroup();
        group.setName(getString(nameResId));
        TypedArray animResIds = getActivity().getResources().obtainTypedArray(animArrayResId);
        String[] cacheNameResIds =
            getActivity().getResources().getStringArray(cacheNameArrayResId);

        final int length = animResIds.length();
        for (int i = 0; i < length; i++) {
            int id = animResIds.getResourceId(i, 0);
            Animation animation =
                AnimationLoader.loadAnimation(getActivity(), id, cacheNameResIds[i]);
            if (animation != null) {
                animation.setTag(id);
                group.add(animation);
            }
        }
        mAnimations.put(nameResId, group);
        group.addListener(mAnimationListener);

        return group;
    }

    private void stopAllAnimations() {
        for (Animation ani : mAnimations.values()) {
            ani.stop();
        }
    }

    @Override
    protected void onAbortEntering() {
        getContainer().setVisible(false);
        if (mEnteredAni != null) {
            mEnteredAni.reset();
        }
        super.onAbortEntering();
    }

    @Override
    protected void onPageEntering(int transitionType) {
        if (Media3D.DEBUG) {
            Log.v(TAG, "onPageEntering");
        }

        if (ENABLE_PORTAL_THUMBNAIL) {
            Container thumbContainer;
            Actor thumbActor;
            Long t1;
            Long t2;
            if (Media3D.DEBUG) {
                t1 = System.currentTimeMillis();
            }
            for (int i = 0; i < DEFAULT_IMAGES.length; i++) {
                Long tStart;
                Long tEnd;
                if (i == 0) {
                    if (Media3D.DEBUG) {
                        tStart = System.currentTimeMillis();
                    }
                    thumbActor = getHost().getThumbnailActor(PageHost.WEATHER);
                    if (Media3D.DEBUG) {
                        tEnd = System.currentTimeMillis();
                        Log.v(TAG, "onPageEntering - get WEATHER thumbnail duration = " + (tEnd - tStart));
                    }
                    thumbContainer = ((Container) mContainer.findChildByTag(R.drawable.portal_weather_demo));
                    thumbActor.setName("weather_actor");
                } else if (i == 1) {
                    if (Media3D.DEBUG) {
                        tStart = System.currentTimeMillis();
                    }
                    thumbActor = getHost().getThumbnailActor(PageHost.PHOTO);
                    if (Media3D.DEBUG) {
                        tEnd = System.currentTimeMillis();
                        Log.v(TAG, "onPageEntering - get PHOTO thumbnail duration = " + (tEnd - tStart));
                    }
                    thumbContainer = ((Container) mContainer.findChildByTag(R.drawable.portal_photo_demo));
                    thumbActor.setName("photo_actor");
                } else {
                    if (Media3D.DEBUG) {
                        tStart = System.currentTimeMillis();
                    }
                    thumbActor = getHost().getThumbnailActor(PageHost.VIDEO);
                    if (Media3D.DEBUG) {
                        tEnd = System.currentTimeMillis();
                        Log.v(TAG, "onPageEntering - get VIDEO thumbnail duration = " + (tEnd - tStart));
                    }
                    thumbContainer = ((Container) mContainer.findChildByTag(R.drawable.portal_video_demo));
                    thumbActor.setName("video_actor");
                }

                if (thumbContainer != null) {
                    Actor oldThumb = thumbContainer.findChildByTag(FRONT_ACTOR_TAG);
                    if (oldThumb != null) {
                        thumbContainer.remove(oldThumb);
                    }

                    thumbActor.setReactive(false);
                    thumbActor.setPosition(new Point(0, 0, -0.02f));
                    thumbActor.setTag(FRONT_ACTOR_TAG);
                    thumbContainer.add(thumbActor);
                }

                // \todo Move back into onAdded() when material settings become
                // non-volatile.
                mWaterSurface.setMaterial("ngin3d#ripple.mat");
            }
            if (Media3D.DEBUG) {
                t2 = System.currentTimeMillis();
                Log.v(TAG, "onPageEntering - get thumbnail duration = " + (t2 - t1));
            }
        }

        if (transitionType == Page.TransitionType.LAUNCH_PORTAL) {
            Animation ani = mAnimations.get(R.string.enter);
            if (ani != null) {
                ani.start();
            }
        } else {
            Page oldPage = getHost().getOldPage();
            int resId = 0;
            if (getHost().isPageEqual(oldPage, PageHost.WEATHER) == 0) {
                resId = R.string.weather_out;
            } else if (getHost().isPageEqual(oldPage, PageHost.PHOTO) == 0) {
                resId = R.string.photo_out;
            } else if (getHost().isPageEqual(oldPage, PageHost.VIDEO) == 0) {
                resId = R.string.video_out;
            }

            if (resId != 0) {
                Animation ani = mAnimations.get(resId);
                if (ani != null) {
                    ani.setDirection(Animation.BACKWARD);
                    ani.start();
                }
            }
        }
        mIsUnderPageTransition = false;
        super.onPageEntering(transitionType);
    }

    @Override
    protected void onPageLeaving(int transitionType) {
        setState(IDLE);
        if (mEnteredAni != null) {
            mEnteredAni.reset();
            mIsUnderPageTransition = false;
        }
    }

    @Override
    public void onResume() {
        // It's a hack to restore actor material attribute from resume.
        // The material restoration should be done in ngin3d and a3m.
        mWaterSurface.setMaterial("ngin3d#ripple.mat");
        super.onResume();
    }

    @Override
    public void onRemoved() {
        Log.v(TAG, "onRemoved (" + this + ")");
        stopAllAnimations();
        super.onRemoved();
    }

    @Override
    protected Container getContainer() {
        return mContainer;
    }

    @Override
    protected boolean onTouchEvent(MotionEvent event) {
        return false;
    }


    public interface PageQueryCallback {
        Page queryWeatherPage();
        Page queryPhotoPage();
        Page queryVideoPage();
    }

    private PageQueryCallback mPageQueryCallback = null;

    public void setPageQueryCallback(PageQueryCallback pqc) {
        mPageQueryCallback = pqc;
    }

    private boolean mIsUnderPageTransition;
    @Override
    protected boolean onSingleTapConfirmed(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && !mIsUnderPageTransition) {
            Point pos = new Point(event.getX(), event.getY());
            if (Media3D.DEBUG) {
                Log.v(TAG, "onSingleTapConfirmed - down, Point = " + pos);
            }

            Actor hitActor = mContainer.hitTest(pos);
            if (Media3D.DEBUG) {
                Log.v(TAG, "hitActor = " + hitActor);
            }

            if (hitActor == null) {
                return false;
            }

            mEnteredAni = null;

            switch (hitActor.getTag()) {
            case R.drawable.portal_weather_demo:
                if (Media3D.DEBUG) {
                    Log.v(TAG, "Weather page touched");
                }
                if (mPageQueryCallback.queryWeatherPage().isLoaded()) {
                    mEnteredAni = mAnimations.get(R.string.weather_out);
                }
                break;
            case R.drawable.portal_photo_demo:
                if (Media3D.DEBUG) {
                    Log.v(TAG, "Photo page touched");
                }
                if (mPageQueryCallback.queryPhotoPage().isLoaded()) {
                    mEnteredAni = mAnimations.get(R.string.photo_out);
                }
                break;
            case R.drawable.portal_video_demo:
                if (Media3D.DEBUG) {
                    Log.v(TAG, "Video page touched");
                }
                if (mPageQueryCallback.queryVideoPage().isLoaded()) {
                    mEnteredAni = mAnimations.get(R.string.video_out);
                }
                break;
            default:
                if (Media3D.DEBUG) {
                    Log.v(TAG, "No page touched");
                }
                break;
            }

            if (mEnteredAni != null) {
                mEnteredAni.setDirection(Animation.FORWARD);
                mEnteredAni.start();
                mIsUnderPageTransition = true;
                return true;
            }
        }

        return false;
    }

    private String getString(int stringId) {
        return getActivity().getString(stringId);
    }

    public void updateLocale(Locale locale) {
        if (mLables != null) {
            mLables[0].setText(getActivity().getResources().getString(R.string.enter_weather));
            mLables[1].setText(getActivity().getResources().getString(R.string.enter_photo));
            mLables[2].setText(getActivity().getResources().getString(R.string.enter_video));
        }

        if (mAppTitleText != null) {
            mAppTitleText.setText(getActivity().getResources().getString(R.string.app_name));
        }

        if (mLoadingText != null) {
            mLoadingText.setText(getActivity().getResources().getString(R.string.loading));
        }
    }
}
