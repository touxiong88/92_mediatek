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

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;

import com.mediatek.weather.Weather;
import com.mediatek.weather.WeatherUpdateResult;

public class LocationWeather {

    private static final String TAG = "W3D/LocationWeather";

    protected int mLocationIndex;
    protected int mCityId = -1;
    protected String mLocationName;
    protected String mTimezone;
    protected int mTempType;
    protected double mCurrentTemp;
    protected double mTempHigh;
    protected double mTempLow;
    protected long mLastUpdated;
    protected Weather.WeatherCondition mCondition;
    private static final int FORECAST_DAY = 3;
    protected ForecastData[] mForecastData;
    protected int mResult = WeatherUpdateResult.INIT_VALUE;

    public LocationWeather(int locationIndex) {
        LogUtil.v(TAG, "locationIndex = " + locationIndex);
        mLocationIndex = locationIndex;
        initForecastData();
    }

    public LocationWeather(final int locationIndex, final int cityId) {
        this(locationIndex);
        mCityId = cityId;
    }

    public LocationWeather(final int locationIndex, final String name, final String timezone,
                           final Weather.WeatherCondition condition, final double currentTemp, final double tempLow,
                           final double tempHigh, final ForecastData[] data) {
        // only for demo data, will always set result = 0, which means successfully
        this(locationIndex);
        mLocationName = name;
        mTimezone = timezone;
        mCondition = condition;
        mCurrentTemp = currentTemp;
        mTempLow = tempLow;
        mTempHigh = tempHigh;
        mLastUpdated = System.currentTimeMillis();
        System.arraycopy(data, 0, mForecastData, 0, data.length < FORECAST_DAY ? data.length : FORECAST_DAY);
        mResult = 0;
    }

    private void initForecastData() {
        mForecastData = new ForecastData[FORECAST_DAY];
        for (int i = 0; i < FORECAST_DAY; i++) {
            mForecastData[i] = new ForecastData(0, 0, 0, 0);
        }
    }

    private boolean isValid() {
        return (mLocationIndex >= 0);
    }

    public int getLocationIndex() {
        return mLocationIndex;
    }

    public int getCityId() {
        return mCityId;
    }

    public String getLocationName() {
        return mLocationName;
    }

    public String getTimezone() {
        return mTimezone;
    }

    public int getTempType() {
        return mTempType;
    }

    public double getCurrentTemp() {
        return mCurrentTemp;
    }

    public double getTempHigh() {
        return mTempHigh;
    }

    public double getTempLow() {
        return mTempLow;
    }

    public long getLastUpdated() {
        return mLastUpdated;
    }

    public int getResult() {
        return mResult;
    }

    public ForecastData[] getForecastData() {
        return mForecastData.clone();
    }

    public int getWeather() {
        int weather = WeatherType.Type.SUNNY;
        if (isValid() && mCondition != null) {
            weather = getWeather(mCondition);
        }
        return weather;
    }

    public void queryCityName(Cursor c) {
        mLocationName = c.getString(c.getColumnIndex(Weather.City.NAME));
        LogUtil.v(TAG, "LocationName = " + mLocationName);
    }

    public void queryGeoInformation(ContentResolver contentResolver) {
        Cursor c = contentResolver.query(ContentUris.withAppendedId(Weather.City.CONTENT_URI, mCityId), null, null,
                null, null);
        if (c == null) {
            return;
        }

        if (c.getCount() != 0) {
            if (mLocationIndex == -1) {
                mLocationIndex = 0;
            }
            if (c.moveToFirst()) {
                mLocationName = c.getString(c.getColumnIndex(Weather.City.NAME));
                mTimezone = c.getString(c.getColumnIndex(Weather.City.TIMEZONE));
            }
        }
        LogUtil.v(TAG, "LocationName = " + mLocationName + ", TimeZone = " + mTimezone);
        c.close();
    }

    public void queryWeather(ContentResolver contentResolver) {
        LogUtil.v(TAG);
        // reset mForecastData first
        for (int i = 0; i < FORECAST_DAY; i++) {
            mForecastData[i].resetForecastData();
        }

        // handle current weather information
        Cursor c = contentResolver.query(ContentUris.withAppendedId(Weather.Current.CONTENT_URI, mCityId), null, null,
            null, null);
        if (c == null) {
            return;
        }
        if (c.getCount() != 0) {
            if (mLocationIndex == -1) {
                mLocationIndex = 0;
            }
            if (c.moveToFirst()) {
                mTempType = c.getInt(c.getColumnIndex(Weather.Current.TEMPERATURE_TYPE));
                mCurrentTemp = c.getDouble(c.getColumnIndex(Weather.Current.TEMP_CURRENT));
                mTempHigh = c.getDouble(c.getColumnIndex(Weather.Current.TEMP_HIGH));
                mTempLow = c.getDouble(c.getColumnIndex(Weather.Current.TEMP_LOW));
                mLastUpdated = c.getLong(c.getColumnIndex(Weather.Current.LAST_UPDATED));
                mCondition = Weather
                        .intToWeatherCondition(c.getInt(c.getColumnIndex(Weather.Current.CONDITION_TYPE_ID)));
            }
        }
        c.close();

        // handle forecast weather information
        final Uri futureWeatherUri = ContentUris.withAppendedId(Weather.Forecast.CONTENT_URI, mCityId);
        LogUtil.i(TAG, "futureWeatherUri = " + futureWeatherUri);
        Cursor futureCursor = contentResolver.query(futureWeatherUri, null, null, null, null);
        if (futureCursor == null) {
            return;
        }
        if (futureCursor.getCount() >= FORECAST_DAY && futureCursor.moveToFirst()) {
            int conditionTypeIndex = futureCursor.getColumnIndex(Weather.Forecast.CONDITION_TYPE_ID);
            int tempHighIndex = futureCursor.getColumnIndex(Weather.Forecast.TEMP_HIGH);
            int tempLowIndex = futureCursor.getColumnIndex(Weather.Forecast.TEMP_LOW);
            int dayOfWeekIndex = futureCursor.getColumnIndex(Weather.Forecast.DAY_OF_WEEK);

            int i = 0;
            while (!futureCursor.isAfterLast() && i < FORECAST_DAY) {
                int condition = LocationWeather.getWeather(
                        Weather.intToWeatherCondition(futureCursor.getInt(conditionTypeIndex)));
                int dayOfWeek = futureCursor.getInt(dayOfWeekIndex);
                double highTemp = futureCursor.getDouble(tempHighIndex);
                double lowTemp = futureCursor.getDouble(tempLowIndex);
                mForecastData[i].setForecastData(dayOfWeek, highTemp, lowTemp, condition);
                LogUtil.v(TAG, "queryWeather = (" + i + ", " + dayOfWeek + ", " + highTemp + ", " + lowTemp + ", " +
                        condition + ")");
                futureCursor.moveToNext();
                i++;
            }
        }
        futureCursor.close();
    }

    public static int getWeather(Weather.WeatherCondition condition) {
        int weather = WeatherType.Type.SUNNY;

        if (condition != null) {
            switch (condition) {
                case Sunny:
                    weather = WeatherType.Type.SUNNY;
                    break;

                case Windy:
                    weather = WeatherType.Type.WINDY;
                    break;

                case Hurricane:
                    weather = WeatherType.Type.BLUSTERY;
                    break;

                case Tornado:
                    weather = WeatherType.Type.TORNADO;
                    break;

                case Cloudy:
                    weather = WeatherType.Type.CLOUDY;
                    break;

                case Overcast:
                    weather = WeatherType.Type.OVERCAST;
                    break;

                case Drizzle:
                case Shower:
                    weather = WeatherType.Type.SHOWER;
                    break;

                case Rain:
                    weather = WeatherType.Type.RAIN;
                    break;

                case Downpour:
                case FreezingRain:
                case SuperDownpour:
                    weather = WeatherType.Type.DOWNPOUR;
                    break;

                case Hail:
                    weather = WeatherType.Type.HAIL;
                    break;

                case ThunderyShower:
                    weather = WeatherType.Type.THUNDER_SHOWER;
                    break;

                case ThunderstormHail:
                    weather = WeatherType.Type.THUNDER_HAIL;
                    break;

                case SnowShowers:
                    weather = WeatherType.Type.SNOW_SHOWER;
                    break;

                case Flurries:
                    weather = WeatherType.Type.SNOW_LIGHT;
                    break;

                case Snow:
                    weather = WeatherType.Type.SNOW;
                    break;

                case Blizzard:
                case HeavySnow:
                    weather = WeatherType.Type.HEAVY_SNOW;
                    break;

                case Sleet:
                    weather = WeatherType.Type.SLEET;
                    break;

                case Fog:
                    weather = WeatherType.Type.FOG;
                    break;

                case Dust:
                    weather = WeatherType.Type.DUST;
                    break;

                case Sand:
                case SandStorm:
                    weather = WeatherType.Type.SAND;
                    break;

                default:
                    weather = WeatherType.Type.SUNNY;
                    break;
            }
        }
        return weather;
    }

    @Override
    public String toString() {
        return ("result = " + mResult + ", cityID = " + mCityId + ", Timezone = " + mTimezone + ", Temp = " +
                mCurrentTemp + ", Low = " + mTempLow + ", High = " + mTempHigh + ", lastUpdate = " + mLastUpdated +
                ", Day1: " + mForecastData[0] + ", Day2: " + mForecastData[1] + ", Day3: " + mForecastData[2]);
    }
}

