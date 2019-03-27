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

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.IBinder;
import android.os.RemoteException;

import com.mediatek.weather.IWeatherService;
import com.mediatek.weather.IWeatherServiceCallback;
import com.mediatek.weather.Weather;
import com.mediatek.weather.WeatherUpdateResult;

import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WeatherBureau {

    private static final String TAG = "W3D/WeatherBureau";
    private ContentResolver mContentResolver;
    private IWeatherService mWeatherService;
    private Context mContext;
    private int mState = STATE_NOT_INIT;

    public LinkedList<LocationWeather> mLocations = new LinkedList<LocationWeather>();
    private final HashMap<Integer, Integer> mCityIdIndexMap = new HashMap<Integer, Integer>();

    private static final LocationWeather INVALID_LOCATION = new LocationWeather(-1);
    private boolean mIsAlarmSet;

    private int mNeedInitCount = 0;
    private int mInitCount = 0;
    private int mInitedCount = 0;

    private static final int REFRESH_TYPE_INIT = 0;

    // press refresh button, refreshWeatherByLocationId
    private static final int REFRESH_TYPE_REFRESH = 1;

    // happen under these two conditions:
    // (1) when the original result is not SUCCESS, and then the network available, try to get weather
    // (2) scroll, the weather result of next city is not SUCCESS, and the network is available, tyr to get weather
    private static final int REFRESH_TYPE_REFRESH_LIGHT = 2;

    private static final int REFRESH_TYPE_ADD_CITY = 3;

    // server update, onWeatherUpdate, refresh_single in UpdateService, refreshWeatherByCityId
    private static final int REFRESH_TYPE_ON_UPDATE = 4;

    public static final int NOTIFY_INITED = 1;
    public static final int NOTIFY_REFRESH_FINISH = 2;
    public static final int NOTIFY_ON_CITY_LIST_CHANGE_FINISH = 3;
    public static final int NOTIFY_ON_WEATHER_UPDATE_FINISH = 4;
    public static final int NOTIFY_INIT_FAIL = 5;

    private static final int STATE_NOT_INIT = 0;
    private static final int STATE_INITING = 1;
    private static final int STATE_INTIED = 2;

    public void init(final Context context) {
        LogUtil.v(TAG, "init - mState = " + mState);
        if (mState == STATE_NOT_INIT) {
            mContext = context;
            if (Weather3D.isDemoMode()) {
                LogUtil.v(TAG, "init - isDemoMode - true");
                initDemoData();
            } else {
                LogUtil.v(TAG, "init - isDemoMode - false");
                bindToWeatherService();
                // initWeatherData will do in onServiceConnected
            }
        }
    }

    private void initDemoData() {
        LogUtil.v(TAG, "initDemoData");
        // Sunny
        ForecastData[] forecastData1 = {new ForecastData(1, 29, 25,
                LocationWeather.getWeather(Weather.WeatherCondition.Sunny)),
                new ForecastData(2, 30, 24, LocationWeather.getWeather(Weather.WeatherCondition.Windy)),
                new ForecastData(3, 28, 25, LocationWeather.getWeather(Weather.WeatherCondition.Cloudy))};
        mLocations.add(new LocationWeather(0, "Shenzhen", "GMT+8", Weather.WeatherCondition.Sunny,
                28, 24, 32, forecastData1));

        ForecastData[] forecastData2 = {new ForecastData(1, 31, 25,
                LocationWeather.getWeather(Weather.WeatherCondition.Hurricane)),
                new ForecastData(2, 30, 25, LocationWeather.getWeather(Weather.WeatherCondition.Sunny)),
                new ForecastData(3, 30, 25, LocationWeather.getWeather(Weather.WeatherCondition.Windy))};
        mLocations.add(new LocationWeather(1, "Japanese Village One", "GMT-4", Weather.WeatherCondition.Sunny,
                26, 25, 31, forecastData2));

        // Windy
        ForecastData[] forecastData3 = {new ForecastData(1, 17, 4,
                LocationWeather.getWeather(Weather.WeatherCondition.Sunny)),
                new ForecastData(2, 18, 4, LocationWeather.getWeather(Weather.WeatherCondition.Windy)),
                new ForecastData(3, 21, 6, LocationWeather.getWeather(Weather.WeatherCondition.Hurricane))};
        mLocations.add(new LocationWeather(2, "San Francisco", "GMT-8", Weather.WeatherCondition.Windy,
                6, 7, 17, forecastData3));

        ForecastData[] forecastData4 = {new ForecastData(1, 25, 14,
                LocationWeather.getWeather(Weather.WeatherCondition.Hurricane)),
                new ForecastData(2, 25, 14, LocationWeather.getWeather(Weather.WeatherCondition.Windy)),
                new ForecastData(3, 24, 14, LocationWeather.getWeather(Weather.WeatherCondition.Sunny))};
        mLocations.add(new LocationWeather(3, "Dubai", "GMT+4", Weather.WeatherCondition.Windy,
                23, 14, 27, forecastData4));

        // Blustery (Strong Wind)
        ForecastData[] forecastData5 = {new ForecastData(1, 17, 4,
                LocationWeather.getWeather(Weather.WeatherCondition.Tornado)),
                new ForecastData(2, 18, 4, LocationWeather.getWeather(Weather.WeatherCondition.Hurricane)),
                new ForecastData(3, 21, 6, LocationWeather.getWeather(Weather.WeatherCondition.Cloudy))};
        mLocations.add(new LocationWeather(4, "Los Angeles", "GMT-8", Weather.WeatherCondition.Hurricane,
                6, 7, 17, forecastData5));

        ForecastData[] forecastData6 = {new ForecastData(1, 25, 14,
                LocationWeather.getWeather(Weather.WeatherCondition.Cloudy)),
                new ForecastData(2, 25, 14, LocationWeather.getWeather(Weather.WeatherCondition.Hurricane)),
                new ForecastData(3, 24, 14, LocationWeather.getWeather(Weather.WeatherCondition.Tornado))};
        mLocations.add(new LocationWeather(5, "Moscow", "GMT+4", Weather.WeatherCondition.Hurricane,
                23, 14, 27, forecastData6));

        // Cloudy
        ForecastData[] forecastData7 = {new ForecastData(1, 1, -5,
                LocationWeather.getWeather(Weather.WeatherCondition.Overcast)),
                new ForecastData(2, 2, -3, LocationWeather.getWeather(Weather.WeatherCondition.Cloudy)),
                new ForecastData(3, 1, -10, LocationWeather.getWeather(Weather.WeatherCondition.Drizzle))};
        mLocations.add(new LocationWeather(6, "Dhaka", "GMT+6", Weather.WeatherCondition.Cloudy,
                0, -5, 2, forecastData7));

        ForecastData[] forecastData8 = {new ForecastData(1, 3, -3,
                LocationWeather.getWeather(Weather.WeatherCondition.Drizzle)),
                new ForecastData(2, 2, -4, LocationWeather.getWeather(Weather.WeatherCondition.Cloudy)),
                new ForecastData(3, 0, -5, LocationWeather.getWeather(Weather.WeatherCondition.Overcast))};
        mLocations.add(new LocationWeather(7, "Houston", "GMT-6", Weather.WeatherCondition.Cloudy,
                1, -10, 3, forecastData8));

        // Rainy
        ForecastData[] forecastData9 = {new ForecastData(1, 29, 14,
                LocationWeather.getWeather(Weather.WeatherCondition.Drizzle)),
                new ForecastData(2, 29, 16, LocationWeather.getWeather(Weather.WeatherCondition.Shower)),
                new ForecastData(3, 30, 16, LocationWeather.getWeather(Weather.WeatherCondition.Rain))};
        mLocations.add(new LocationWeather(8, "Santiago", "GMT-4", Weather.WeatherCondition.Shower,
            22, 14, 27, forecastData9));

        ForecastData[] forecastData10 = {new ForecastData(1, 15, 12,
                LocationWeather.getWeather(Weather.WeatherCondition.Rain)),
                new ForecastData(2, 15, 12, LocationWeather.getWeather(Weather.WeatherCondition.Shower)),
                new ForecastData(3, 14, 11, LocationWeather.getWeather(Weather.WeatherCondition.Drizzle))};
        mLocations.add(new LocationWeather(9, "Taipei", "GMT+8", Weather.WeatherCondition.Shower,
            18, 14, 22, forecastData10));

        // Heavy Rain
        ForecastData[] forecastData11 = {new ForecastData(1, 15, 12,
                LocationWeather.getWeather(Weather.WeatherCondition.Downpour)),
                new ForecastData(2, 15, 12, LocationWeather.getWeather(Weather.WeatherCondition.FreezingRain)),
                new ForecastData(3, 14, 11, LocationWeather.getWeather(Weather.WeatherCondition.Hail))};
        mLocations.add(new LocationWeather(10, "Barcelona", "GMT+1", Weather.WeatherCondition.Downpour,
                18, 14, 22, forecastData11));

        ForecastData[] forecastData12 = {new ForecastData(1, 18, 12,
                LocationWeather.getWeather(Weather.WeatherCondition.Hail)),
                new ForecastData(2, 20, 13, LocationWeather.getWeather(Weather.WeatherCondition.FreezingRain)),
                new ForecastData(3, 20, 15, LocationWeather.getWeather(Weather.WeatherCondition.Downpour))};
        mLocations.add(new LocationWeather(11, "Wellington", "GMT+13", Weather.WeatherCondition.Downpour,
                11, 11, 19, forecastData12));

        // Thunder
        ForecastData[] forecastData13 = {new ForecastData(1, 12, 9,
                LocationWeather.getWeather(Weather.WeatherCondition.ThunderstormHail)),
                new ForecastData(2, 13, 3, LocationWeather.getWeather(Weather.WeatherCondition.ThunderyShower)),
                new ForecastData(3, 9, 5, LocationWeather.getWeather(Weather.WeatherCondition.Snow))};
        mLocations.add(new LocationWeather(12, "Lisbon", "GMT", Weather.WeatherCondition.ThunderyShower,
                8, 7, 14, forecastData13));

        ForecastData[] forecastData14 = {new ForecastData(1, 18, 12,
                LocationWeather.getWeather(Weather.WeatherCondition.Snow)),
                new ForecastData(2, 20, 13, LocationWeather.getWeather(Weather.WeatherCondition.ThunderyShower)),
                new ForecastData(3, 20, 15, LocationWeather.getWeather(Weather.WeatherCondition.ThunderstormHail))};
        mLocations.add(new LocationWeather(13, "Suva", "GMT+12", Weather.WeatherCondition.ThunderyShower,
                11, 11, 19, forecastData14));

        // Snow
        ForecastData[] forecastData15 = {new ForecastData(1, 1, -5,
                LocationWeather.getWeather(Weather.WeatherCondition.SnowShowers)),
                new ForecastData(2, 2, -3, LocationWeather.getWeather(Weather.WeatherCondition.Snow)),
                new ForecastData(3, 1, -10, LocationWeather.getWeather(Weather.WeatherCondition.Flurries))};
        mLocations.add(new LocationWeather(14, "St. Petersburg", "GMT+6", Weather.WeatherCondition.Snow,
                0, -5, 2, forecastData15));

        ForecastData[] forecastData16 = {new ForecastData(1, 3, -3,
                LocationWeather.getWeather(Weather.WeatherCondition.Flurries)),
                new ForecastData(2, 2, -4, LocationWeather.getWeather(Weather.WeatherCondition.Snow)),
                new ForecastData(3, 0, -5, LocationWeather.getWeather(Weather.WeatherCondition.SnowShowers))};
        mLocations.add(new LocationWeather(15, "Chicago", "GMT-6", Weather.WeatherCondition.Snow,
                1, -10, 3, forecastData16));

        // Heavy Snow
        ForecastData[] forecastData17 = {new ForecastData(1, 1, -5,
                LocationWeather.getWeather(Weather.WeatherCondition.Snow)),
                new ForecastData(2, 2, -3, LocationWeather.getWeather(Weather.WeatherCondition.HeavySnow)),
                new ForecastData(3, 1, -10, LocationWeather.getWeather(Weather.WeatherCondition.Sleet))};
        mLocations.add(new LocationWeather(16, "Kiev", "GMT+6", Weather.WeatherCondition.HeavySnow,
                0, -5, 2, forecastData17));

        ForecastData[] forecastData18 = {new ForecastData(1, 3, -3,
                LocationWeather.getWeather(Weather.WeatherCondition.Sleet)),
                new ForecastData(2, 2, -4, LocationWeather.getWeather(Weather.WeatherCondition.Blizzard)),
                new ForecastData(3, 0, -5, LocationWeather.getWeather(Weather.WeatherCondition.Snow))};
        mLocations.add(new LocationWeather(17, "Winnipeg", "GMT-6", Weather.WeatherCondition.HeavySnow,
                1, -10, 3, forecastData18));

        // Snow Rain
        ForecastData[] forecastData19 = {new ForecastData(1, 1, -5,
                LocationWeather.getWeather(Weather.WeatherCondition.HeavySnow)),
                new ForecastData(2, 2, -3, LocationWeather.getWeather(Weather.WeatherCondition.Sleet)),
                new ForecastData(3, 1, -10, LocationWeather.getWeather(Weather.WeatherCondition.Snow))};
        mLocations.add(new LocationWeather(18, "Tokyo", "GMT+9", Weather.WeatherCondition.Sleet,
                0, -5, 2, forecastData19));

        ForecastData[] forecastData20 = {new ForecastData(1, 3, -3,
                LocationWeather.getWeather(Weather.WeatherCondition.Snow)),
                new ForecastData(2, 2, -4, LocationWeather.getWeather(Weather.WeatherCondition.Sleet)),
                new ForecastData(3, 0, -5, LocationWeather.getWeather(Weather.WeatherCondition.HeavySnow))};
        mLocations.add(new LocationWeather(19, "Greenland", "GMT-3", Weather.WeatherCondition.Sleet,
                1, -10, 3, forecastData20));

        // Fog
        ForecastData[] forecastData21 = {new ForecastData(1, 1, -5,
                LocationWeather.getWeather(Weather.WeatherCondition.Sunny)),
                new ForecastData(2, 2, -3, LocationWeather.getWeather(Weather.WeatherCondition.Fog)),
                new ForecastData(3, 1, -10, LocationWeather.getWeather(Weather.WeatherCondition.Snow))};
        mLocations.add(new LocationWeather(20, "London", "GMT", Weather.WeatherCondition.Fog,
                0, -5, 2, forecastData21));

        ForecastData[] forecastData22 = {new ForecastData(1, 3, -3,
                LocationWeather.getWeather(Weather.WeatherCondition.Snow)),
                new ForecastData(2, 2, -4, LocationWeather.getWeather(Weather.WeatherCondition.Fog)),
                new ForecastData(3, 0, -5, LocationWeather.getWeather(Weather.WeatherCondition.Sunny))};
        mLocations.add(new LocationWeather(21, "Sydney", "GMT+12", Weather.WeatherCondition.Fog,
                1, -10, 3, forecastData22));

        // Sand
        ForecastData[] forecastData23 = {new ForecastData(1, 1, -5,
                LocationWeather.getWeather(Weather.WeatherCondition.Dust)),
                new ForecastData(2, 2, -3, LocationWeather.getWeather(Weather.WeatherCondition.Sand)),
                new ForecastData(3, 1, -10, LocationWeather.getWeather(Weather.WeatherCondition.SandStorm))};
        mLocations.add(new LocationWeather(22, "Roseau", "GMT-4", Weather.WeatherCondition.Sand,
                0, -5, 2, forecastData23));

        ForecastData[] forecastData24 = {new ForecastData(1, 30, 19,
                LocationWeather.getWeather(Weather.WeatherCondition.SandStorm)),
                new ForecastData(2, 29, 15, LocationWeather.getWeather(Weather.WeatherCondition.Sand)),
                new ForecastData(3, 25, 14, LocationWeather.getWeather(Weather.WeatherCondition.Dust))};
        mLocations.add(new LocationWeather(23, "Ulan Bator", "GMT+8", Weather.WeatherCondition.Sand,
                16, 29, 14, forecastData24));

        mState = STATE_INTIED;
        sendOutNotifyIntent(NOTIFY_INITED);
    }

    private void initWeatherData() {
        LogUtil.v(TAG, "init - ServerMode");
        mState = STATE_INITING;

        mContentResolver = mContext.getContentResolver();
        queryLocations(mContext);
    }

    private void queryLocations(Context context) {
        LogUtil.v(TAG, "queryLocations");
        if (mWeatherService == null) {
            LogUtil.v(TAG, "queryLocations - mService - null");
            return;
        }
        final Cursor cc = context.getContentResolver().query(Weather.City.CONTENT_URI, null, null, null, null);
        if (cc == null) {
            return;
        }

        mLocations.clear();
        mNeedInitCount = cc.getCount();
        mInitCount = 0;
        mInitedCount = 0;
        if (cc.getCount() != 0 && cc.moveToFirst()) {
            int locationIndex = 0;
            do {
                int cityId = cc.getInt(cc.getColumnIndex(Weather.City.ID));
                LogUtil.v(TAG, "weather_in_db, index = " + locationIndex + ", cityId = " + cityId);
                LocationWeather lw = new LocationWeather(locationIndex, cityId);
                // get CityName first
                lw.queryCityName(cc);
                mCityIdIndexMap.put(cityId, locationIndex);
                mLocations.add(lw);
                // get Weather information from non-UI thread
                mInitCount++;
                refreshWeather(REFRESH_TYPE_INIT, cityId, locationIndex, lw);
                locationIndex++;
            } while (cc.moveToNext());
        } else {
            mState = STATE_INTIED;
            sendOutNotifyIntent(NOTIFY_INITED);
        }
        cc.close();
    }

    public void deinit() {
        LogUtil.v(TAG, "deinit");

        if (!Weather3D.isDemoMode()) {
            LogUtil.i(TAG, "onDestroy call unbindService");
            if (mContext != null) {
                mContext.unbindService(mConnection);
            }
            // break cyclic garbage - WeatherBureau -> ContentResolver -> Main
            mContentResolver = null;
            mWeatherService = null;
        }
        if (mLocations != null) {
            mLocations.clear();
        }
        if (mCityIdIndexMap != null) {
            mCityIdIndexMap.clear();
        }
        mState = STATE_NOT_INIT;
    }

    private LinkedList<LocationWeather> getCurrentLocations() {
        return mLocations;
    }

    public int getLocationCount() {
        return getCurrentLocations().size();
    }

    public LocationWeather getLocationByIndex(int index) {
        LinkedList<LocationWeather> locations = getCurrentLocations();

        if (index < 0 || index >= locations.size()) {
            return INVALID_LOCATION;
        }
        return locations.get(index);
    }

    public LocationWeather getLocationByCityId(int cityId) {
        LinkedList<LocationWeather> locations = getCurrentLocations();

        for (int i = 0; i < locations.size(); i++) {
            if (locations.get(i).mCityId == cityId) {
                return locations.get(i);
            }
        }
        return INVALID_LOCATION;
    }

    public void refresh(Context context) {
        // refresh all weather info
        LogUtil.v(TAG, "refresh");
        queryLocations(context);
    }

    public void refreshWeatherByCityId(int cityId) {
        int locationIndex = mCityIdIndexMap.get(cityId);
        LogUtil.v(TAG, "refreshWeatherByCityId, cityID = " + cityId + ", locationIndex = " + locationIndex);
        LocationWeather lw = mLocations.get(locationIndex);
        if (lw != null) {
            refreshWeather(REFRESH_TYPE_ON_UPDATE, cityId, locationIndex, lw);
        }
    }

    public void refreshWeatherByLocationId(int locationIndex, boolean isLightRefresh) {
        LocationWeather lw = mLocations.get(locationIndex);
        if (lw == null) {
            LogUtil.v(TAG, "refreshWeatherByLocationId - no such location index");
        } else {
            int cityId = lw.getCityId();
            LogUtil.v(TAG, "refreshWeatherByLocationId, cityID = " + cityId + ", locationIndex = " + locationIndex);

            int refreshType = REFRESH_TYPE_REFRESH;
            if (isLightRefresh) {
                refreshType = REFRESH_TYPE_REFRESH_LIGHT;
            }
            refreshWeather(refreshType, cityId, locationIndex, lw);
        }
    }

    public void refreshWeatherForCityListChange() {
        LogUtil.v(TAG, "refreshWeatherForCityListChange");
        final Cursor cc = mContentResolver.query(Weather.City.CONTENT_URI, null, null, null, null);
        if (cc == null) {
            return;
        }

        int newCityCount = cc.getCount();
        int oldCityCount = getLocationCount();

        HashMap<Integer, Integer> cityHashMap = new HashMap<Integer, Integer>();
        int[] cityArray = new int[newCityCount];

        if (newCityCount != 0 && cc.moveToFirst()) {
            int index = 0;
            do {
                int cityId = cc.getInt(cc.getColumnIndex(Weather.City.ID));
                cityHashMap.put(cityId, index);
                cityArray[index] = cityId;
                index++;
            } while (cc.moveToNext());
        }

        cc.close();

        LogUtil.v(TAG, "refreshWeatherForCityListChange - (new, old) = (" + newCityCount + ", " + oldCityCount + ")");

        int deltaCityCount = newCityCount - oldCityCount;
        int workingCityId = 0;
        int workingLocationId = 0;
        LocationWeather workingLW;

        switch (deltaCityCount) {
            case -1:
                // delete one city
                for (int cityId : mCityIdIndexMap.keySet()) {
                    if (!cityHashMap.containsKey(cityId)) {
                        workingCityId = cityId;
                        break;
                    }
                }
                // find the deleted location id by cityId
                workingLocationId = mCityIdIndexMap.get(workingCityId);

                mCityIdIndexMap.clear();

                for (int i = mLocations.size() - 1; i >= 0; i--) {
                    workingLW = mLocations.get(i);
                    if (i == workingLocationId) {
                        mLocations.remove(workingLocationId);
                    } else {
                        if (i > workingLocationId) {
                            workingLW.mLocationIndex--;
                        }
                        mCityIdIndexMap.put(workingLW.getCityId(), workingLW.getLocationIndex());
                    }
                }
                callbackForCityListChanged(-1);
                LogUtil.v(TAG, "refreshWeatherForCityListChange - delete one city done");
                break;

            case 0:
                // change city order
                LocationWeather[] mTempLocations = new LocationWeather[newCityCount];
                int i = 0;

                for (i = 0; i < newCityCount; i++) {
                    mTempLocations[i] = mLocations.get(mCityIdIndexMap.get(cityArray[i]));
                    mTempLocations[i].mLocationIndex = i;
                }
                mLocations.clear();
                mCityIdIndexMap.clear();
                for (i = 0; i < newCityCount; i++) {
                    mLocations.add(mTempLocations[i]);
                    mCityIdIndexMap.put(mTempLocations[i].getCityId(), mTempLocations[i].getLocationIndex());
                }
                callbackForCityListChanged(0);
                LogUtil.v(TAG, "refreshWeatherForCityListChange - change city order done");
                break;

            case 1:
                // add one new city
                for (int cityID : cityHashMap.keySet()) {
                    if (!mCityIdIndexMap.containsKey(cityID)) {
                        workingCityId = cityID;
                        break;
                    }
                }

                // because new city will be added to the last position, so id = original size
                workingLocationId = mLocations.size();
                workingLW = new LocationWeather(workingLocationId, workingCityId);
                mLocations.add(workingLW);

                mCityIdIndexMap.put(workingCityId, workingLocationId);
                refreshWeather(REFRESH_TYPE_ADD_CITY, workingCityId, workingLocationId, workingLW);
                LogUtil.v(TAG, "refreshWeatherForCityListChange - lw = " + workingLW);
                break;

            default:
                break;
        }
    }

    private void callbackForCityListChanged(int type) {
        // type = -1: delete a city
        // type = 0: city order change
        // type = 1: add a city
        Intent intent = new Intent(mContext, UpdateService.class);
        intent.setAction(WeatherWidgetAction.ACTION_WEATHER_BUREAU_NOTIFY);
        intent.putExtra(WeatherWidgetAction.NOTIFY_TYPE, NOTIFY_ON_CITY_LIST_CHANGE_FINISH);
        mContext.startService(intent);
    }

    public boolean isCityIdExist(int cityId) {
        return mCityIdIndexMap.containsKey(cityId);
    }

    public int getLocationIdByCityId(int cityId) {
        return mCityIdIndexMap.get(cityId);
    }

    public boolean isInited() {
        LogUtil.v("mState = " + mState);
        if (mState == STATE_INTIED) {
            LogUtil.v("true; mState = " + mState);
            return true;
        } else {
            LogUtil.v("false; mState = " + mState);
            return false;
        }
    }

    public void setNextAlarm(Context context) {
        int count = getLocationCount();
        if (count != 0) {
            TreeMap<Integer, String> timeUp = new TreeMap<Integer, String>();
            Calendar c;
            LocationWeather lw;
            int hour; // the hour of city
            int min;  // the min of city
            int diffMin; // min difference of next alarm

            for (int i = 0; i < count; i++) {
                lw = mLocations.get(i);
                LogUtil.v(TAG, "ith = " + i + "; " + lw);
                if (lw.getResult() == WeatherUpdateResult.SUCCESS) {
                    // get correct timezone, when WeatherUpdateResult = SUCCESS and ERROR_NETWORK_NOT_AVAILABLE;
                    // and get null timezone when WeatherUpdateResult = ERROR_UPDATE_WEATHER_FAILED;
                    // and we just need to care about day night update when the result = SUCCESS.
                    c = Util.getTime(lw.getTimezone());
                    hour = c.get(Calendar.HOUR_OF_DAY);
                    min = c.get(Calendar.MINUTE);

                    if (hour >= 6 && hour < 18) {
                        // calculate how many minutes from now to 18:00
                        // current time is 6:00 - 17:59
                        diffMin = (60 - min) + (17 - hour) * 60;
                    } else if (hour >= 18) {
                        // calculate how many minutes from now to 6:00
                        // current time >= 18:00, but not yet pass 24:00
                        diffMin = (60 - min) + (24 + 5 - hour) * 60;
                    } else {
                        // calculate how many minutes from now to 6:00
                        // current time pass 24:00, but not yet 06:00
                        diffMin = (60 - min) + (5 - hour) * 60;
                    }
                    LogUtil.v(TAG, "ith = " + i + "; diffMin = " + diffMin);
                    timeUp.put(diffMin, lw.getTimezone());
                }
            }
            LogUtil.v(TAG, "setNextAlarm - sortMap = " + timeUp);

            if (!timeUp.isEmpty()) {
                Alarm.setAlarm(context, timeUp.get(timeUp.firstKey()));
                mIsAlarmSet = true;
            }
        }
    }

    public void cancelAlarm(Context context) {
        LogUtil.v(TAG, "cancelAlarm");
        if (mIsAlarmSet) {
            Alarm.stopAlarm(context);
            mIsAlarmSet = false;
        }
    }

    public String getUpdateTimeZone() {
        int count = getLocationCount();
        if (count == 0) {
            return null;
        }

        int hour;
        int min;
        int diffMin;
        Calendar c;
        LocationWeather lw;

        for (int i = 0; i < count; i++) {
            lw = mLocations.get(i);
            LogUtil.v(TAG, "ith = " + i + "; " + lw);
            if (lw.getResult() == WeatherUpdateResult.SUCCESS) {
                c = Util.getTime(lw.getTimezone());
                hour = c.get(Calendar.HOUR_OF_DAY);
                min = c.get(Calendar.MINUTE);

                if (hour >= 6 && hour < 18) {
                    // calculate how many minutes passed 06:00
                    // current time is 6:00 - 17:59
                    diffMin = (hour - 6) * 60 + min;
                } else if (hour >= 18) {
                    // calculate how many minutes passed 18:00
                    // current time >= 18:00, but not yet pass 24:00
                    diffMin = (hour - 18) * 60 + min;
                } else {
                    // calculate how many minutes passed 18:00
                    // current time >= 18:00, and passed 24:00
                    diffMin = (hour + 24 - 18) * 60 + min;
                }

                if (diffMin < 1) {
                    return lw.getTimezone();
                }
            }
        }
        return null;
    }

    private ExecutorService mExecutorService;
    private class WeatherLoader implements Runnable {
        private final int mLocationIndex;
        private final int mCityId;
        private final int mRefreshType;
        private final LocationWeather mLocationWeather;

        public WeatherLoader(int refreshType, int cityId, int locationIndex, LocationWeather lw) {
            mRefreshType = refreshType;
            mCityId = cityId;
            mLocationIndex = locationIndex;
            mLocationWeather = lw;
        }

        public void run() {
            int result = WeatherUpdateResult.INIT_VALUE;
            long currentMillis = System.currentTimeMillis();

            try {
                result = mWeatherService.updateWeather(mCityId, currentMillis).mResult;
            } catch (RemoteException e) {
                LogUtil.v(TAG, "got exception, " + e.getMessage());
            }
            LogUtil.v(TAG, "run result = " + result);

            mLocationWeather.mResult = result;
            mLocationWeather.queryGeoInformation(mContentResolver);
            // no matter result is 0 or not, should set Time zone information here.
            // when weather bureau inited, will set alarm, it will need timezone to set.
            // if not set, then null pointer exception happens

            if (result == WeatherUpdateResult.SUCCESS) {
                mLocationWeather.queryWeather(mContentResolver);
                LogUtil.v(TAG, "WeatherLoader done = " + mLocationWeather);
            } else {
                mLocationWeather.mLastUpdated = currentMillis;
                LogUtil.v(TAG, "get weather fail");
            }

            if (mRefreshType == REFRESH_TYPE_INIT) {
                LogUtil.v(TAG, "WeatherLoader - REFRESH_TYPE_INIT");
                if (!isInited()) {
                    mInitedCount++;
                    setInitFlag();
                }
                if (isInited()) {
                    sendOutNotifyIntent(NOTIFY_INITED);
                }
            } else if (mRefreshType == REFRESH_TYPE_REFRESH || mRefreshType == REFRESH_TYPE_REFRESH_LIGHT) {
                LogUtil.v(TAG, "WeatherLoader - REFRESH_TYPE_REFRESH");
                Intent intent = new Intent(mContext, UpdateService.class);
                intent.setAction(WeatherWidgetAction.ACTION_WEATHER_BUREAU_NOTIFY);
                intent.putExtra(WeatherWidgetAction.NOTIFY_TYPE, NOTIFY_REFRESH_FINISH);
                intent.putExtra(WeatherWidgetAction.LOCATION_ID, mLocationWeather.getLocationIndex());
                intent.putExtra(WeatherWidgetAction.CITY_ID, mLocationWeather.getCityId());
                mContext.startService(intent);
            } else if (mRefreshType == REFRESH_TYPE_ON_UPDATE) {
                LogUtil.v(TAG, "WeatherLoader - REFRESH_TYPE_ON_UPDATE - id = " + mLocationWeather.getLocationIndex());
                Intent intent = new Intent(mContext, UpdateService.class);
                intent.setAction(WeatherWidgetAction.ACTION_WEATHER_BUREAU_NOTIFY);
                intent.putExtra(WeatherWidgetAction.NOTIFY_TYPE, NOTIFY_ON_WEATHER_UPDATE_FINISH);
                intent.putExtra(WeatherWidgetAction.LOCATION_ID, mLocationWeather.getLocationIndex());
                intent.putExtra(WeatherWidgetAction.CITY_ID, mLocationWeather.getCityId());
                mContext.startService(intent);
            } else if (mRefreshType == REFRESH_TYPE_ADD_CITY) {
                LogUtil.v(TAG, "WeatherLoader - REFRESH_TYPE_ADD_CITY");
                callbackForCityListChanged(1);
            }
        }
    }

    private void refreshWeather(int refreshType, int cityId, int locationIndex, LocationWeather lw) {
        WeatherLoader wl = new WeatherLoader(refreshType, cityId, locationIndex, lw);
        if (mExecutorService == null) {
            // Weather Provider allows one thread query
            mExecutorService = Executors.newSingleThreadExecutor();
        }
        mExecutorService.submit(wl);
    }

    private void setInitFlag() {
        LogUtil.v(TAG, "(NeedInitCount, InitCount, InitedCount) = (" +
                mNeedInitCount + ", " + mInitCount + ", " + mInitedCount + ")");
        if (mNeedInitCount == mInitCount && mNeedInitCount == mInitedCount) {
            mNeedInitCount = 0;
            mInitCount = 0;
            mInitedCount = 0;
            mState = STATE_INTIED;
            LogUtil.v(TAG, "WeatherBureau Inited");
        }
    }

    // below is WeatherService related code
    private boolean bindToWeatherService() {
        boolean result = mContext.bindService(
                new Intent(IWeatherService.class.getName()), mConnection, Context.BIND_AUTO_CREATE);
        if (result) {
            LogUtil.i(TAG, "bindToWeatherService - success");
        } else {
            LogUtil.i(TAG, "bindToWeatherService - fail");
        }
        return result;
    }

    /**
     * Class for interacting with the main interface of the service.
     */
    private final ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            LogUtil.i(TAG, "weather service connected");
            mWeatherService = IWeatherService.Stub.asInterface(service);
            initWeatherData();
        }

        public void onServiceDisconnected(ComponentName className) {
            LogUtil.i(TAG,"weather service disconnected");
            mWeatherService = null;
            if (mState == STATE_INITING) {
                sendOutNotifyIntent(NOTIFY_INIT_FAIL);
            }
        }
    };

    private void sendOutNotifyIntent(int notifyType) {
        Intent intent = new Intent(mContext, UpdateService.class);
        intent.setAction(WeatherWidgetAction.ACTION_WEATHER_BUREAU_NOTIFY);
        intent.putExtra(WeatherWidgetAction.NOTIFY_TYPE, notifyType);
        mContext.startService(intent);
    }

    public boolean isNeedInit() {
        if (mState == STATE_NOT_INIT) {
            LogUtil.v("true; mState = " + mState);
            return true;
        } else {
            LogUtil.v("true; mState = " + mState);
            return false;
        }
    }
}
