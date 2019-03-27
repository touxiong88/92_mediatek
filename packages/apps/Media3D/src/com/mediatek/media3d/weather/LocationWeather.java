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

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.text.format.Time;

import com.mediatek.location.LocationEx;
import com.mediatek.media3d.LogUtil;
import com.mediatek.media3d.R;
import com.mediatek.ngin3d.Text;
import com.mediatek.weather.IWeatherService;
import com.mediatek.weather.Weather;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.ArrayBlockingQueue;

public class LocationWeather {
    private static final String TAG = "Media3D.LocationWeather";
    private static IWeatherService sWeatherService;
    private static ContentResolver sResolver;
    private static Handler sUpdateHandler;
    private static final Object LOCK = new Object();

    private int mLocationIndex;
    private int mCityId = -1;
    private String mLocationName;
    private float mLongitude;
    private float mLatitude;
    private String mTimezone;

    // following data members are queried from IWeatherService asynchronously.
    private int mTempType;
    private double mCurrentTemp;
    private double mTempHigh;
    private double mTempLow;
    private long mLastUpdated;
    private Weather.WeatherCondition mCondition;
    private final ArrayBlockingQueue<QueryWeatherTask> mQueryQueue = new ArrayBlockingQueue<QueryWeatherTask>(10);

    public static void setWeatherService(final IWeatherService service) {
        sWeatherService = service;
    }

    public static void setContentResolver(final ContentResolver resolver) {
        synchronized (LOCK) {
            sResolver = resolver;
        }
    }

    public static void setUpdateHandler(final Handler handler) {
        synchronized (LOCK) {
            sUpdateHandler = handler;
        }
    }

    public LocationWeather(final int locationIndex) {
        mLocationIndex = locationIndex;
        mLocationName = "";
        mTimezone = Time.getCurrentTimezone();
        mCondition = Weather.WeatherCondition.Sunny;
    }

    public LocationWeather(final int locationIndex, final int cityId) {
        this(locationIndex);
        mCityId = cityId;
    }

    public LocationWeather(int locationIndex, String name, String timezone, Weather.WeatherCondition condition,
            float latitude, float longitude, double currentTemp, double tempLow, double tempHigh, long lastUpdated) {
        this(locationIndex);
        mLocationName = name;
        mTimezone = timezone;
        mCondition = condition;
        mLatitude = latitude;
        mLongitude = longitude;
        mCurrentTemp = currentTemp;
        mTempLow = tempLow;
        mTempHigh = tempHigh;
        mLastUpdated = lastUpdated;
    }

    public boolean isValid() {
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

    public float getLongitude() {
        return mLongitude;
    }

    public float getLatitude() {
        return mLatitude;
    }

    public double getTempHigh() {
        return mTempHigh;
    }

    public long getLastUpdated() {
        return mLastUpdated;
    }

    public void queryWeather() {
        Cursor c;

        synchronized (LOCK) {
            if (sResolver == null) {
                return;
            }

            c = sResolver.query(
                    ContentUris.withAppendedId(Weather.Current.CONTENT_URI, mCityId),
                    null, null, null, null);
        }

        if (c != null && c.getCount() != 0) {
            if (mLocationIndex == -1) {
                mLocationIndex = 0;
            }
            if (c.moveToFirst()) {
                mTempType = c.getInt(c.getColumnIndex(Weather.Current.TEMPERATURE_TYPE));
                mCurrentTemp = c.getDouble(c.getColumnIndex(Weather.Current.TEMP_CURRENT));
                mTempHigh = c.getDouble(c.getColumnIndex(Weather.Current.TEMP_HIGH));
                mTempLow = c.getDouble(c.getColumnIndex(Weather.Current.TEMP_LOW));
                mLastUpdated = c.getLong(c.getColumnIndex(Weather.Current.LAST_UPDATED));
                mCondition = Weather.intToWeatherCondition(c.getInt(c.getColumnIndex(
                        Weather.Current.CONDITION_TYPE_ID)));
            }
        }
        if (c != null) {
            c.close();
        }

        Cursor c2;
        // sResolver may be set as null while application exits
        // and this background thread is still under query.
        // To prevent NullPointerException, check sResolver again with atomic operation.
        synchronized (LOCK) {
            if (sResolver == null) {
                return;
            }

            // Request it asynchronously again to prevent invalid value.
            c2 = sResolver.query(
                    ContentUris.withAppendedId(Weather.City.CONTENT_URI, mCityId),
                    null, null, null, null);
        }

        if (c2 != null && c2.getCount() != 0 && c2.moveToFirst()) {
            mTimezone = TimeZoneTransition.getGmtTz(c2.getString(c2.getColumnIndex(Weather.City.TIMEZONE)));
        }

        if (c2 != null) {
            c2.close();
        }
    }

    public void notifyHandler() {
        synchronized (LOCK) {
            if (sUpdateHandler != null) {
                final Message msg = Message.obtain();
                msg.what = WeatherPage.MSG_UPDATE_WEATHER;
                msg.arg1 = mCityId;
                sUpdateHandler.sendMessage(msg);
            }
        }
    }

    private void fetchWeatherFromProvider(boolean isRequestUpdateFirst) {
        try {
            if (sWeatherService != null) {
                sWeatherService.updateWeather(getCityId(),  isRequestUpdateFirst ? 0 : -1);
            }
            queryWeather();
            if (isUpdated()) {
                notifyHandler();
            }
        } catch (RemoteException e) {
            LogUtil.v(TAG, "Failed to fetch weather from service. Exception : " + e.getMessage());
        }
    }

    private class QueryWeatherTask extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... none) {
            fetchWeatherFromProvider(true);
            return null;
        }

        protected void onPostExecute(Void result) {
            mQueryQueue.remove();
            if (isUpdated()) {
                mQueryQueue.clear();
                return;
            }

            QueryWeatherTask nextTask = mQueryQueue.peek();
            if (nextTask != null) {
                nextTask.execute();
            }
        }
    }

    private void queryWeatherAsync() {
        if (sWeatherService == null) {
            LogUtil.v(TAG, "Weather Service isn't ready." + ", city id :" + getCityId());
            return;
        }

        // remove previous querying, only one querying task is allowed.
        final QueryWeatherTask task = new QueryWeatherTask();
        try {
            mQueryQueue.add(task);
        } catch (IllegalStateException e) {
            LogUtil.v(TAG, "Too many weather queries, exception :" + e.getMessage());
        }

        QueryWeatherTask nextTask = mQueryQueue.peek();
        if (nextTask != null) {
            nextTask.execute();
        }
    }

    public void query(final Cursor c, boolean isSynchronousTry) {
        mLocationName = c.getString(c.getColumnIndex(Weather.City.NAME));
        mLongitude = Float.valueOf(c.getString(c.getColumnIndex(Weather.City.LONGITUDE)));
        mLatitude = Float.valueOf(c.getString(c.getColumnIndex(Weather.City.LATITUDE)));

        queryCurrentWeatherStatus(isSynchronousTry);
    }

    public void queryCurrentWeatherStatus(boolean isSynchronousTry) {
        if (isSynchronousTry) {
            fetchWeatherFromProvider(false);
        }

        if (getLastUpdated() == 0) {
            queryWeatherAsync();
        }
    }

    public void fetch(final LocationEx location) {
        mCityId = (int)location.getWoeid();
        mLocationName = location.getCity();
        mLongitude = (float)location.getLongitude();
        mLatitude = (float)location.getLatitude();
    }

    public Text getNameText() {
        return new Text(getLocationName());
    }

    public Text getTempHighLowText(final Context c) {
        return new Text(getTempHighLowString(c));
    }

    public String getTempHighLowString(final Context c) {
        String highLowStr = "";
        if (isUpdated()) {
            highLowStr = String.format("%.1f", mTempLow) + "~" + String.format("%.1f", mTempHigh);
            if (mTempType == Weather.TEMPERATURE_CELSIUS) {
                highLowStr += c.getString(R.string.celsius);
            } else {
                highLowStr += c.getString(R.string.fahrenheit);
            }
        }
        return highLowStr;
    }

    public Text getDateText() {
        return new Text(getDateString());
    }

    public String getDateString() {
        final Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(getTimeZone()));
        final SimpleDateFormat sdf = (SimpleDateFormat)DateFormat.getDateInstance(DateFormat.MEDIUM);
        return sdf.format(cal.getTime());
    }

    public String getTimeZone() {
        if (isValid() && mTimezone != null) {
            return mTimezone;
        }
        return Time.getCurrentTimezone();
    }

    public Text getTemperatureText(final Context c) {
        Text text;
        if (isUpdated()) {
            if (mTempType == Weather.TEMPERATURE_CELSIUS) {
                text = new Text(String.format("%.1f", mCurrentTemp) + c.getString(R.string.celsius));
            } else {
                text = new Text(String.format("%.1f", mCurrentTemp) + c.getString(R.string.fahrenheit));
            }
        } else {
            text = new Text(c.getString(R.string.no_weather_text));
        }
        return text;
    }

    public String getTemperatureString(final Context c) {
        String text;
        if (isUpdated()) {
            if (mTempType == Weather.TEMPERATURE_CELSIUS) {
                text = String.format("%.1f", mCurrentTemp) + c.getString(R.string.celsius);
            } else {
                text = String.format("%.1f", mCurrentTemp) + c.getString(R.string.fahrenheit);
            }
        } else {
            text = c.getString(R.string.no_weather_text);
        }
        return text;
    }

    public static final int CLOUDY = 0;
    public static final int SUNNY = 1;
    public static final int RAINY = 2;

    public int getWeather() {
        int weather = SUNNY;
        if (isUpdated() && mCondition != null) {
            switch (mCondition) {
                case Sunny:
                case Hurricane:
                case Tornado:
                case Hail:
                case Windy:
                    weather = SUNNY;
                    break;
                case Overcast:
                case Cloudy:
                case Fog:
                case FreezingRain:
                case SandStorm:
                case Dust:
                case Sand:
                    weather = CLOUDY;
                    break;

                case Shower:
                case ThunderyShower:
                case ThunderstormHail:
                case Sleet:
                case Drizzle:
                case Downpour:
                case SuperDownpour:
                case SnowShowers:
                case Flurries:
                case Snow:
                case HeavySnow:
                case Blizzard:
                    weather = RAINY;
                    break;
                default:
                    weather = SUNNY;
                    break;
            }
        }
        return weather;
    }

    public boolean isValidCityId() {
        return (mCityId >= 0);
    }

    public boolean isUpdated() {
        return (mLastUpdated > 0);
    }
}