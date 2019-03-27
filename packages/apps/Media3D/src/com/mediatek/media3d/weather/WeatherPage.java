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

package com.mediatek.media3d.weather;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.os.Handler;
import android.os.Message;
import android.util.Pair;

import com.mediatek.media3d.LayoutManager;
import com.mediatek.media3d.LogUtil;
import com.mediatek.media3d.NavigationBarMenu;
import com.mediatek.media3d.NavigationBarMenuItem;
import com.mediatek.media3d.Page;
import com.mediatek.media3d.PageDragHelper;
import com.mediatek.media3d.PageHost;
import com.mediatek.media3d.R;
import com.mediatek.media3d.VideoBackground;

import com.mediatek.ngin3d.Actor;
import com.mediatek.ngin3d.Container;
import com.mediatek.ngin3d.Image;
import com.mediatek.ngin3d.Point;
import com.mediatek.ngin3d.Rotation;
import com.mediatek.ngin3d.Scale;
import com.mediatek.ngin3d.Sphere;
import com.mediatek.ngin3d.Stage;
import com.mediatek.ngin3d.Text;
import com.mediatek.ngin3d.Transaction;
import com.mediatek.ngin3d.animation.Animation;
import com.mediatek.ngin3d.animation.AnimationGroup;
import com.mediatek.ngin3d.animation.AnimationLoader;
import com.mediatek.weather.Weather;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Locale;

public class WeatherPage extends Page {
    private static final String TAG = "Media3D.WeatherPage";

    private final Stage mStage;
    private Container mContainer = new Container();
    private Sphere mEarth;
    private LocationWeather mCity;
    private VideoBackground mBackgroundVideo;

    private final WeatherBureau mBureau = new WeatherBureau();
    private WeatherPanel mWeather;
    private WeatherPanel mWeatherBuffer;

    private static final int SWITCH_PREVIOUS = 0;
    private static final int SWITCH_NEXT = 1;
    private static final int GO_LAST_CITY = 0;
    private static final int GO_NEXT_CITY = 1;

    private final HashMap<Integer, AnimationGroup> mAnimationLists = new HashMap<Integer, AnimationGroup>();
    private final AnimationGroup mAnimationGoNextAlias = new AnimationGroup();
    private final AnimationGroup mAnimationGoLastAlias = new AnimationGroup();
    private AnimationGroup mRunningAnimation;

    public WeatherPage(Stage stage) {
        super(Page.SUPPORT_FLING);
        LogUtil.v(TAG);
        mStage = stage;
    }

    @Override
    public void onAdded(PageHost host) {
        LogUtil.v(TAG);
        mStage.add(mContainer);
        super.onAdded(host);
    }

    @Override
    public void onLoad() {
        LogUtil.v(TAG);
        super.onLoad();
    }

    private float mDragMax;
    private float mDragThreshold;

    public void initialize() {
        if (mInitialized) {
            return;
        }
        mDragMax = getLayoutManager().getFloat("weather_drag_maximum");
        mDragThreshold = getLayoutManager().getFloat("weather_drag_threshold");
        mBureau.init(getActivity());
        refreshWeatherSource(true);
        initWeatherPanels();
        initEarth();
        loadAnimations();
        super.initialize();
    }

    @Override
    public void onDetach() {
        LogUtil.v(TAG);
        detachHandler();
        stopAnimations();
        stopBackgroundVideo();
        mBureau.deInit();
        super.onDetach();
    }

    @Override
    protected void onPageEntering(int transition) {
        LogUtil.v(TAG, "Transition :" + transition);
        initialize();
        attachHandler();
        startAnimation(getTransition(transition, "enter"));
        rotateEarth(mCity);
        mBureau.startRequestLocationUpdate(getActivity());
        if (!isNoCity()) {
            initBackgroundVideo();
            startBackgroundVideo();
        }
        super.onPageEntering(transition);
        setState(IDLE); // Enable response for user input immediately.
    }

    protected void prepareDragLeaving() {
        stopAnimations();
        stopBackgroundVideo();
        mBureau.stopRequestLocationUpdate();
        mWeather.setVisible(false);
        mEarth.setVisible(false);
        super.prepareDragLeaving();
    }

    protected Animation prepareDragLeavingAnimation(int transition) {
        return bindAnimations(mWeather, getTransition(transition, "leave"));
    }

    protected void revertDragLeaving() {
        if (!isNoCity()) {
            initBackgroundVideo();
            startBackgroundVideo();
        }

        // Take back temperature and city name actors to normal position.
        startAnimation(R.string.weather_homing);
        mWeather.setVisible(true);
        mEarth.setVisible(true);
        super.revertDragLeaving();
    }

    @Override
    protected void onPageLeaving(int transition) {
        LogUtil.v(TAG, "transition :" + transition);
        if (isDragLeaving()) {
            setState(IDLE);
            return;
        }
        stopAnimations();
        stopBackgroundVideo();
        mBureau.stopRequestLocationUpdate();
        mWeather.setVisible(false);
        mEarth.setVisible(false);
        startAnimation(getTransition(transition, "leave"));
    }

    @Override
    protected void onPageLeft() {
        LogUtil.v(TAG);
        super.onPageLeft();
    }

    @Override
    protected Container getContainer() {
        return mContainer;
    }

    public void onRemoved() {
        LogUtil.v(TAG);
        super.onRemoved();
    }

    boolean mPauseVideo;
    public void onPause() {
        mPauseVideo = true;
        stopBackgroundVideo();
        super.onPause();
    }

    public void onResume() {
        super.onResume();
        initBackgroundVideo();
        startBackgroundVideo();
        mPauseVideo = false;
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

    private AnimationGroup bindAnimations(final WeatherPanel target, final int id) {
        return target.bindAnimations(getAnimations(id));
    }

    private void startAnimation(final int animation) {
        bindAnimations(mWeather, animation).start();
    }

    private void stopAnimations() {
        LogUtil.v(TAG);
        stopAndHideEffect();

        if (mWeather != null) {
            final Animation animation = mWeather.getBoundAnimation(true);
            if (animation != null) {
                LogUtil.v(TAG, "Stop first animation id:" + animation.getTag());
                animation.stop();
            }
        }

        if (mWeatherBuffer != null) {
            final Animation animation = mWeatherBuffer.getBoundAnimation(true);
            if (animation != null) {
                LogUtil.v(TAG, "Stop second animation id:" + animation.getTag());
                animation.stop();
            }
        }

        if (mRunningAnimation != null) {
            LogUtil.v(TAG, "Stop group animation id:" + mRunningAnimation.getTag());
            // Finish the animation to prevent half-played actors show after animation stop.
            mRunningAnimation.complete();
            mRunningAnimation.stop();
            mRunningAnimation.setDirection(Animation.FORWARD);
            mRunningAnimation = null;
        }
    }

    private void loadAnimations() {
        LogUtil.v(TAG);
        // scroll to next city
        loadAnimationGroup(R.string.next_enter, R.array.weather_next_enter_id, R.array.weather_next_enter_strings);
        loadAnimationGroup(R.string.next_exit, R.array.weather_next_exit_id, R.array.weather_next_exit_strings)
                .enableOptions(Animation.HIDE_TARGET_ON_COMPLETED);
        // scroll to previous city
        loadAnimationGroup(R.string.last_enter, R.array.weather_last_enter_id, R.array.weather_last_enter_strings);
        loadAnimationGroup(R.string.last_exit, R.array.weather_last_exit_id, R.array.weather_last_exit_strings)
                .enableOptions(Animation.HIDE_TARGET_ON_COMPLETED);
        // switch to left page
        loadAnimationGroup(R.string.left_enter, R.array.weather_swap_enter_left_id,
                R.array.weather_swap_enter_left_strings);
        loadAnimationGroup(R.string.left_exit, R.array.weather_swap_exit_left_id, R.array.weather_swap_exit_left_strings)
                .enableOptions(Animation.HIDE_TARGET_ON_COMPLETED);
        // switch to right page
        loadAnimationGroup(R.string.right_enter, R.array.weather_swap_enter_right_id,
                R.array.weather_swap_enter_right_strings);
        loadAnimationGroup(R.string.right_exit, R.array.weather_swap_exit_right_id,
                R.array.weather_swap_exit_right_strings)
                .enableOptions(Animation.HIDE_TARGET_ON_COMPLETED);

        // Take back temperature and city name actors to normal position.
        loadAnimationGroup(R.string.weather_homing, R.array.weather_swap_homing_id,
                R.array.weather_swap_homing_strings);

        // Setup Listener
        mAnimationGoNextAlias.addListener(mAnimationCompletedHandler);
        mAnimationGoLastAlias.addListener(mAnimationCompletedHandler);
    }

    private void addAnimation(final int id, final AnimationGroup animations) {
        mAnimationLists.put(id, animations);
    }

    private AnimationGroup getAnimations(final int id) {
        return mAnimationLists.get(id);
    }

    private AnimationGroup loadAnimationGroup(int nameResId, int animArrayResId, int cacheNameArrayResId) {
        LogUtil.v(TAG, "animation :" + getString(nameResId));
        final AnimationGroup animationGroup = new AnimationGroup();
        final TypedArray animResIds = getActivity().getResources().obtainTypedArray(animArrayResId);
        final String[] cacheNameResIds = getActivity().getResources().getStringArray(cacheNameArrayResId);
        final int length = animResIds.length();
        for (int i = 0; i < length; i++) {
            final int id = animResIds.getResourceId(i, 0);
            if (id != 0) {
                final Animation ani = AnimationLoader.loadAnimation(getActivity(), id, cacheNameResIds[i]);
                ani.setTag(i);
                animationGroup.add(ani);
            }
        }
        animationGroup.setTag(nameResId);
        animationGroup.addListener(mAnimationCompletedHandler);
        addAnimation(nameResId, animationGroup);
        return animationGroup;
    }

    private final Animation.Listener mAnimationCompletedHandler = new Animation.Listener() {
        public void onPaused(Animation animation) {
            final int tag = animation.getTag();
            final Activity parent = getActivity();
            LogUtil.v(TAG, "animation tag :" + tag + ", activity :" + parent);
            if (getAnimations(tag) != null && parent != null) {
                parent.runOnUiThread(new Runnable() {
                    public void run() {
                        setState(IDLE);
                    }
                });
            }
        }

        public void onCompleted(Animation animation) {
            final int tag = animation.getTag();
            final Activity parent = getActivity();
            LogUtil.v(TAG, "animation tag :" + tag + ", activity :" + parent);
            if (parent == null) {
                return;
            }

            if (tag == R.string.right_enter || tag == R.string.left_enter ||
                tag == R.string.next_enter || tag == R.string.last_enter) {
                parent.runOnUiThread(new Runnable() {
                    public void run() {
                        playEffectAnimation();
                    }
                });
            } else if (tag == R.string.last_exit || tag == R.string.next_exit) {
                parent.runOnUiThread(new Runnable() {
                    public void run() {
                        mWeatherBuffer.setVisible(false);
                    }
                });
            }

            parent.runOnUiThread(new Runnable() {
                public void run() {
                    if (mRunningAnimation != null &&
                            mRunningAnimation.getTag() == tag &&
                            !mIsDragging) {
                        mRunningAnimation.setDirection(Animation.FORWARD);
                        mRunningAnimation = null;
                    }
                }
            });
        }
    };

    private void refreshWeatherSource(final boolean reset) {
        mBureau.updateLocations(getActivity());
        if (reset || mCity == null) {
            mCity = mBureau.getLocationByIndex(0);
        } else {
            mCity = mBureau.getLocationByCityId(mCity.getCityId());
        }
    }

    private void initEarth() {
        mEarth = Sphere.createFromResource(getActivity().getResources(), R.raw.bigearth_new);
        mEarth.setPosition(getLayoutManager().getPoint("weather_earth_pos"));
        mEarth.setScale(getLayoutManager().getScale("weather_earth_scale"));
        mEarth.setVisible(false);
        mContainer.add(mEarth);
    }

    private void initWeatherPanels() {
        LogUtil.v(TAG);
        loadEffectAnimations();
        mWeather = new WeatherPanel(getActivity(), mCity, getLayoutManager());
        mWeather.setCity(mCity, false);
        mWeather.addToContainer(mContainer);

        mWeatherBuffer = new WeatherPanel(getActivity(), mCity, getLayoutManager());
        mWeatherBuffer.addToContainer(mContainer);
    }

    private final HashMap<Integer, Animation> mEffectAnimations =
            new HashMap<Integer, Animation>();
    private Animation mEffectAnimation;

    public void loadEffectAnimations() {
        mEffectAnimations.clear();
        final Context context = getActivity();
        Animation animation = AnimationLoader.loadAnimation(context,
                R.raw.weather_effect_sunny_ani,
                context.getString(R.string.weather_effect_sunny_ani));
        mEffectAnimations.put(LocationWeather.SUNNY, animation);

        animation = AnimationLoader.loadAnimation(context,
                R.raw.weather_effect_cloudy_ani,
                context.getString(R.string.weather_effect_cloudy_ani));
        mEffectAnimations.put(LocationWeather.CLOUDY, animation);

        animation = AnimationLoader.loadAnimation(context,
                R.raw.weather_effect_raining_ani,
                context.getString(R.string.weather_effect_raining_ani));
        mEffectAnimations.put(LocationWeather.RAINY, animation);
    }

    public void playEffectAnimation() {
        mEffectAnimation = mEffectAnimations.get(mCity.getWeather());
        if (mEffectAnimation != null) {
            mWeather.bindEffectAnimation(mEffectAnimation).start();
        }
    }

    private final HashMap<Integer,Pair<Integer, Integer>> mSeekTable = new HashMap<Integer, Pair<Integer, Integer>>();
    private HashMap<Integer,Pair<Integer, Integer>> prepareSeekTable() {
        if (mSeekTable.isEmpty()) {
            // According to the weather_video.mp4 information.
            final int sunnyStartTime = 0;
            final int sunnyEndTime = 9956;
            final int cloudyStartTime = 10050;
            final int cloudyEndTime = 19920;
            final int rainyStartTime = 20050;
            final int rainyEndTime = 28500;
            Pair<Integer, Integer> sunnyPair = new Pair<Integer, Integer>(sunnyStartTime, sunnyEndTime);
            mSeekTable.put(LocationWeather.SUNNY, sunnyPair);
            Pair<Integer, Integer> cloudyPair = new Pair<Integer, Integer>(cloudyStartTime, cloudyEndTime);
            mSeekTable.put(LocationWeather.CLOUDY, cloudyPair);
            Pair<Integer, Integer> rainyPair = new Pair<Integer, Integer>(rainyStartTime, rainyEndTime);
            mSeekTable.put(LocationWeather.RAINY, rainyPair);
        }
        return mSeekTable;
    }

    public void initBackgroundVideo() {
        PageHost host = getHost();
        mBackgroundVideo = (host == null) ? null :
                host.setVideoBackground(R.raw.weather_video, prepareSeekTable());
    }

    public void pauseBackgroundVideo() {
        if (mBackgroundVideo != null) {
            mBackgroundVideo.pause();
        }
    }

    public void startBackgroundVideo() {
        if (mBackgroundVideo != null && mCity.isUpdated()) {
            mBackgroundVideo.play(mCity.getWeather());
        }
    }

    public void stopBackgroundVideo() {
        if (mBackgroundVideo != null) {
            mBackgroundVideo.stop();
        }
    }

    private void swapWeather() {
        final WeatherPanel weatherSwap = mWeather;
        mWeather = mWeatherBuffer;
        mWeatherBuffer = weatherSwap;
    }

    private void setupTransitionAnimation(final int direction) {
        if (direction == SWITCH_PREVIOUS) {
            mAnimationGoLastAlias.clear();
            mAnimationGoLastAlias.add(bindAnimations(mWeather, R.string.last_exit));
            mAnimationGoLastAlias.add(bindAnimations(mWeatherBuffer, R.string.last_enter));
            mAnimationGoLastAlias.setTag(GO_LAST_CITY);
            mRunningAnimation = mAnimationGoLastAlias;
        } else {
            mAnimationGoNextAlias.clear();
            mAnimationGoNextAlias.add(bindAnimations(mWeather, R.string.next_exit));
            mAnimationGoNextAlias.add(bindAnimations(mWeatherBuffer, R.string.next_enter));
            mAnimationGoNextAlias.setTag(GO_NEXT_CITY);
            mRunningAnimation = mAnimationGoNextAlias;
        }
    }

    public void switchToNextCity(final int direction, final boolean setupAnimation) {
        if (isLessOneCity()) {
            return;
        }

        final int place = mCity.getLocationIndex();
        final int newPlace = (direction ==  SWITCH_PREVIOUS) ?
                mBureau.getPrev(place) : mBureau.getNext(place);
        mCity = mBureau.getLocationByIndex(newPlace);

        mWeather.hideNameAndTemperature();
        mWeatherBuffer.setCity(mCity, false);
        rotateEarth(mCity);

        if (setupAnimation) {
            setupTransitionAnimation(direction);
        }

        swapWeather();
    }

    public void revert(final int direction) {
        switchToNextCity(reverseDirection(direction), false);
        mWeather.setVisible(true);
    }

    private static final float EARTH_X_PAD = 45f;

    private void rotateEarth(final LocationWeather destination) {
        final float longitude = destination.getLongitude();
        final float latitude = destination.getLatitude();

        Transaction.beginImplicitAnimation();
        mEarth.setRotation(new Rotation(-latitude + EARTH_X_PAD, longitude, 0));
        Transaction.commit();
    }

    private void stopAndHideEffect() {
        if (mWeather != null) {
            mWeather.stopAndHideEffect();
        }
    }

    private boolean isLessOneCity() {
        return (mBureau.getLocationCount() <= 1);
    }

    private boolean isNoCity() {
        return (mBureau.getLocationCount() == 0);
    }

    private int getDirection(final int deltaY) {
        return (deltaY > 0) ? SWITCH_PREVIOUS : SWITCH_NEXT;
    }

    private int reverseDirection(final int direction) {
        return (direction == SWITCH_PREVIOUS) ? SWITCH_NEXT : SWITCH_PREVIOUS;
    }

    private boolean mIsDragging;
    public boolean onDrag(PageDragHelper.State state, float disY) {
        if (isLessOneCity()) {
            return true;
        }

        final int direction = getDirection((int)disY);
        switch (state) {
            case INITIAL:
                if (mRunningAnimation != null && mRunningAnimation.isStarted()) {
                    mRunningAnimation.complete();
                    mRunningAnimation.stop();
                    mRunningAnimation.setDirection(Animation.FORWARD);
                    mRunningAnimation = null;
                }
                break;
            case START:
                mIsDragging = true;
                pauseBackgroundVideo();
                stopAndHideEffect();
                switchToNextCity(direction, true);
                mRunningAnimation.startDragging();
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
                    if (mRunningAnimation.getProgress() < mDragThreshold) {
                        mRunningAnimation.reverse();
                        revert(direction);
                    }
                    startBackgroundVideo();
                }
                break;
            default:
                break;
        }
        return true;
    }

    private static final String[] METHODS = { "updateWeather", "updateClock" };
    private static final Class[] ARGS = new Class[] { Message.class };

    private static Object call(final Object ownerObject, final String methodName, final Message msg) {
        Object returnObject = null;
        try {
            returnObject = ownerObject.getClass().getMethod(methodName, ARGS).invoke(ownerObject, msg);
        } catch (NoSuchMethodException e) {
            LogUtil.v(TAG, e.toString());
        } catch (IllegalAccessException e) {
            LogUtil.v(TAG, e.toString());
        } catch (InvocationTargetException e) {
            LogUtil.v(TAG, e.toString());
        }
        return returnObject;
    }

    private static final int CLOCK_UPDATE_INTERVAL = 5000;
    public void updateClock(Message msg) {
        mWeather.updateClock();
        mHandler.sendEmptyMessageDelayed(MSG_UPDATE_CLOCK, CLOCK_UPDATE_INTERVAL);
    }

    public void updateWeather(Message msg) {
        final int cityId = msg.arg1;
        if (cityId == mCity.getCityId()) {
            // current city is updated, re-get the instance to make sure data is synchronized.
            mCity = mBureau.getLocationByCityId(cityId);
            mWeather.setCity(mCity, true);
            stopAndHideEffect();

            if (!mPauseVideo) {
                initBackgroundVideo();
                startBackgroundVideo();
            }
        }
    }

    public static final int MSG_UPDATE_WEATHER = 0;
    public static final int MSG_UPDATE_CLOCK = 1;

    private void attachHandler() {
        LocationWeather.setUpdateHandler(mHandler);
        mHandler.sendEmptyMessageDelayed(MSG_UPDATE_CLOCK, CLOCK_UPDATE_INTERVAL);
    }

    private void detachHandler() {
        LocationWeather.setUpdateHandler(null);
        mHandler.removeMessages(MSG_UPDATE_WEATHER);
        mHandler.removeMessages(MSG_UPDATE_CLOCK);
    }

    public final Handler mHandler = new UIHandler();
    private final class UIHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            call(WeatherPage.this, METHODS[msg.what], msg);
        }
    }

    private static final int CMD_REFRESH = 1;
    private static final int CMD_SETTING = 2;

    @Override
    public boolean onCreateBarMenu(NavigationBarMenu menu) {
        super.onCreateBarMenu(menu);
        menu.add(CMD_REFRESH).setIcon(R.drawable.top_weaher_refresh);
        menu.add(CMD_SETTING).setIcon(R.drawable.top_weaher_setting);
        return true;
    }

    @Override
    public boolean onBarMenuItemSelected(NavigationBarMenuItem item) {
        final int id = item.getItemId();
        LogUtil.v(TAG, "id :" + id);
        if (id == CMD_REFRESH) {
            // Invalid locationWeather (mCity) with invalid city id is given by default if there is no selected city.
            // To avoid querying with invalid city id, check it in advance.
            if (mCity.isValidCityId()) {
                mBureau.refreshWeather(mCity.getCityId());
                mWeather.updateWeather();
                startBackgroundVideo();
            }
        } else if (id == CMD_SETTING) {
            final Intent intent = new Intent(Weather.Intents.ACTION_SETTING);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            startActivityForResult(intent, CMD_SETTING);
        } else {
            return super.onBarMenuItemSelected(item);
        }
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        LogUtil.v(TAG, "request code :" + requestCode + ", result code :" + resultCode);
        if (requestCode == CMD_SETTING) {
            refreshWeatherSource(true);
            mWeather.setCity(mCity, true);
            rotateEarth(mCity);
        }
    }

    private String getString(final int resId) {
        return (resId == 0) ? null : getActivity().getString(resId);
    }

    @Override
    public Actor getThumbnailActor() {
        LogUtil.v(TAG);
        final Image background = Image.createFromResource(getActivity().getResources(),
            R.drawable.mainmenuitems_weather_blank);
        background.setPosition(new Point(0, 0, +0.08f));
        background.setReactive(false);
        final Container thumbnail = new Container();
        thumbnail.add(background);

        if (mCity == null) {
            mBureau.init(getActivity());
            refreshWeatherSource(true);
        }

        if (mCity != null && mCity.isUpdated()) {
            LogUtil.v(TAG, "city :" + mCity.getCityId());
            final Text temperature = mCity.getTemperatureText(getActivity());
            temperature.setPosition(new Point(25, 40, -45f));
            temperature.setScale(new Scale(0.7f, 0.7f, 0.7f));
            temperature.setReactive(false);
            thumbnail.add(temperature);

            final Image weather = Image.createFromResource(getActivity().getResources(), WeatherPanel.getWeatherIcon(mCity));
            weather.setPosition(new Point(-65, -25, -10f));
            weather.setScale(new Scale(0.5f, 0.5f, 0.5f));
            weather.setReactive(false);
            thumbnail.add(weather);
        }

        return thumbnail;
    }

    public void updateLocale(Locale locale) {
        if (mWeather != null) {
            mWeather.updateLocale(locale, getActivity());
        }
        if (mWeatherBuffer != null) {
            mWeatherBuffer.updateLocale(locale, getActivity());
        }
    }
}
