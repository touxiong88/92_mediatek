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

import android.content.Context;

import com.mediatek.media3d.FormatUtil;
import com.mediatek.media3d.LayoutManager;
import com.mediatek.media3d.LogUtil;
import com.mediatek.media3d.R;
import com.mediatek.ngin3d.Actor;
import com.mediatek.ngin3d.Container;
import com.mediatek.ngin3d.Image;
import com.mediatek.ngin3d.Scale;
import com.mediatek.ngin3d.Text;
import com.mediatek.ngin3d.animation.Animation;
import com.mediatek.ngin3d.animation.AnimationGroup;

import java.util.Locale;

public final class WeatherPanel {
    private static final String TAG = "Media3D.WeatherPanel";

    private static final int[] WEATHER_ICONS = new int[] {
            R.drawable.icon_moon, R.drawable.icon_sun,R.drawable.icon_raincloud};
    private static final int[] EFFECT_ICONS = new int[] {
            R.drawable.icon_nightcloud, R.drawable.icon_sunlight, R.drawable.icon_raindrops};

    private final Context mContext;
    private final Text mName;
    private final Image mWeather;
    private final Image mWeatherEffect;
    private final Container mWeatherContainer;
    private final DigitClock mClock;
    private final Text mTemperature;
    private final Actor[] mActorsMap; // index :index of animation script, value :weather sprite actor id.
    private LocationWeather mCity;

    public WeatherPanel(final Context context, final LocationWeather city, LayoutManager lm) {
        mCity = city;
        mContext = context;

        mName = mCity.getNameText();
        int fontSize = lm.getInteger("weather_title_wo_updated_size");
        FormatUtil.applyTextAttributes(mName, fontSize, false);
        float nameScale = getNameScale();
        mName.setScale(new Scale(nameScale, nameScale));
        mName.setPosition(lm.getPoint("weather_panel_city_pos"));

        mTemperature = mCity.getTemperatureText(context);
        FormatUtil.applyTextAttributes(mTemperature, fontSize, false);
        float cityScale = mCity.isUpdated() ? 2f : 1f;
        mTemperature.setScale(new Scale(cityScale, cityScale));
        mTemperature.setPosition(lm.getPoint("weather_panel_temperature_pos"));

        mClock = new DigitClock("weather_panel", context, mCity, lm);
        updateClock();

        mWeather = Image.createFromResource(context.getResources(), getWeatherIcon(mCity));
        mWeather.setPosition(lm.getPoint("weather_panel_icon_pos"));

        mWeatherEffect = Image.createFromResource(context.getResources(), EFFECT_ICONS[0]);
        mWeatherEffect.setVisible(false);

        mWeatherContainer = new Container();
        mWeatherContainer.add(mWeather, mWeatherEffect);
        mActorsMap = new Actor[] {mName, mTemperature, mClock, mWeatherContainer};
        setVisible(false);
    }

    public void addToContainer(final Container container) {
        LogUtil.v(TAG, "container :" + container);
        container.add(mWeatherContainer);
        container.add(mClock);
        container.add(mName);
        container.add(mTemperature);
    }

    public void setCity(final LocationWeather city, final boolean visibility) {
        mCity = city;

        mName.setText(mCity.getLocationName());
        float namescalesize = getNameScale();
        mName.setScale(new Scale(namescalesize, namescalesize));

        LogUtil.v(TAG, "city id:" + city.getCityId() + ", update :" + city.getLastUpdated());
        float scalesize = mCity.isUpdated() ? 2f : 1f;
        mTemperature.setScale(new Scale(scalesize, scalesize));
        mTemperature.setText(mCity.getTemperatureString(mContext));
        mWeather.setImageFromResource(mContext.getResources(), getWeatherIcon(mCity));
        mWeatherEffect.setImageFromResource(mContext.getResources(), getWeatherEffectIcon(mCity));
        setVisible(visibility);
        updateClock();
    }

    public void setVisible(final boolean visibility) {
        for (Actor actor : mActorsMap) {
            actor.setVisible(visibility);
        }
    }

    public void hideNameAndTemperature() {
        mName.setVisible(false);
        mTemperature.setVisible(false);
    }

    public void updateClock() {
        mClock.update(mCity, mContext);
    }

    private float getNameScale() {
        return (mCity.getLocationName().length() > 12) ? 1.0f : 2.0f;
    }

    public void updateWeather() {
        mWeather.setImageFromResource(mContext.getResources(), getWeatherIcon(mCity));
        final float scalesize = mCity.isUpdated() ? 2f : 1f;
        mTemperature.setScale(new Scale(scalesize, scalesize));
        mTemperature.setText(mCity.getTemperatureString(mContext));
        updateClock();
    }

    private Animation mCurrentAnimation;
    public AnimationGroup bindAnimations(final AnimationGroup group) {
        if (group == null) {
            return null;
        }
        for (int i = 0; i < group.getAnimationCount(); ++i) {
            final Animation animation = group.getAnimation(i);
            final int tag = animation.getTag();
            if (tag < mActorsMap.length && tag >= 0) {
                final Actor actor = mActorsMap[tag];
                if (actor != null) {
                    actor.stopAnimations();
                    animation.setTarget(actor);
                }
            }
        }
        mCurrentAnimation = group;
        return group;
    }

    public Animation getBoundAnimation(final boolean isRelease) {
        final Animation boundAnimation = mCurrentAnimation;
        if (isRelease) {
            mCurrentAnimation = null;
        }
        return boundAnimation;
    }

    private Animation mCurrentEffectAnimation;
    public Animation bindEffectAnimation(final Animation animation) {
        if (animation == null) {
            return null;
        }

        animation.setTarget(mWeatherEffect);
        animation.enableOptions(Animation.SHOW_TARGET_DURING_ANIMATION);
        mCurrentEffectAnimation = animation;
        return animation;
    }

    public static int getWeatherIcon(final LocationWeather city) {
        return WEATHER_ICONS[city.getWeather()];
    }

    public static int getWeatherEffectIcon(final LocationWeather city) {
        return EFFECT_ICONS[city.getWeather()];
    }

    public void updateLocale(Locale locale, Context context) {
        if (mTemperature != null && mCity != null) {
            mTemperature.setText(mCity.getTemperatureString(context));
        }
    }

    public void stopAndHideEffect() {
        if (mCurrentEffectAnimation != null) {
            mCurrentEffectAnimation.stop();
            mCurrentEffectAnimation.reset();
        }
        mWeatherEffect.setVisible(false);
    }
}