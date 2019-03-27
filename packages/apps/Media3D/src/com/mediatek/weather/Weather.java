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

package com.mediatek.weather;

import android.net.Uri;
import android.provider.BaseColumns;

public final class Weather {
    public static final String AUTHORITY = "com.mediatek.provider.weather";
    public static final int TEMPERATURE_CELSIUS = 0;
    public static final int TEMPERATURE_FAHRENHEIT = 1;
    // this is to support Yahoo brand
    public static final String YAHOO_URL = "http://m.yahoo.com/s/mtkweatheradr";

    private Weather() {}

    /**
     * forecast Weather table
     */
    public static class Forecast implements BaseColumns {
        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/forecast");

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/forecast";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/forecast";

        /**
         * City Name
         * <P>Type: TEXT</P>
         */
        public static final String CITY_NAME = "city_name";

        /**
         * City ID
         *  <P>Type: INTEGER (int)</P>
         */
        public static final String CITY_ID = "city_id";

        /**
         * current weather condition           *
         *  <P>Type: INTEGER (int)</P>
         */
        public static final String CONDITION_DRAWABLE_ID = "condition_drawable_id";

        /**
         * current weather condition         *
         *  <P>Type: INTEGER (int)</P>
         */
        public static final String CONDITION_DESCRIPTION_ID = "condition_description_id";

        /**
         * current weather condition         *
         *  <P>Type: INTEGER (int)</P>
         */
        public static final String CONDITION_TYPE_ID = "condition_type";

        /**
         * high temperature  in the current day
         * <P>Type: REAL </P>
         */
        public static final String TEMP_HIGH = "temp_high";

        /**
         * low temperature  in the current day
         * <P>Type: REAL </P>
         */
        public static final String TEMP_LOW = "temp_low";

        /**
         * The temperature type
         * <P>Type: INTEGER (int)</P>
         * {@link WeatherCondition}
         */
        public static final String TEMPERATURE_TYPE = "temp_type";

        /**
         * The timestamp for when the current weather was last update
         * <P>Type: INTEGER (long)</P>
         */
        public static final String LAST_UPDATED = "last_updated";
    }

    /**
     * Current Weather  table
     */
    public static final class Current extends Forecast {
        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/current");

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/current";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/current";

        /**
         * current temperature
         *  <P>Type: REAL </P>
         */
        public static final String TEMP_CURRENT = "temp_current";

        /**
         * URL for further weather information
         * <P>Type: TEXT</P>
         */
        public static final String FURTHERINFO_URL = "furtherinfo_url";
    }

    /**
     * city table
     */
    public static class City implements BaseColumns {
        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/city");

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/city";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/city";

        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = "position ASC";

        /**
         * City Name
         * <P>Type: TEXT</P>
         */
        public static final String NAME = "city_name";

        /**
         * City ID
         *  <P>Type: INTEGER (int)</P>
         */
        public static final String ID = "city_id";

        /**
         * City longitude
         *  <P>Type: REAL (double)</P>
         */
        public static final String LONGITUDE = "longitude";

        /**
         * City latitude
         *  <P>Type: REAL (double)</P>
         */
        public static final String LATITUDE = "latitude";

        /**
         * City timezone
         *  <P>Type: TEXT</P>
         */
        public static final String TIMEZONE = "timezone";

        /**
         * city position in city list, 0 is the first
         *  <P>Type: INTEGER (int)</P>
         */
        public static final String POSITION = "position";
    }

    /**
     * this enum define Weather condition
     */
    public static enum WeatherCondition {
        Sunny("Sunny"),
        Cloudy("Cloudy"),
        Overcast("Overcast"),
        Shower("Shower"),
        ThunderyShower("ThunderyShower"),

        ThunderstormHail("ThunderstormHail"),
        Sleet("Sleet"),
        Drizzle("Drizzle"),
        Rain("Rain"),
        Downpour("Downpour"),

        SuperDownpour("SuperDownpour"),
        SnowShowers("SnowShowers"),
        Flurries("Flurries"),
        Snow("Snow"),
        HeavySnow("HeavySnow"),

        Blizzard("Blizzard"),
        Fog("Fog"),
        FreezingRain("FreezingRain"),
        SandStorm("SandStorm"),
        Dust("Dust"),

        Sand("Sand"),
        Hurricane("Hurricane"),
        Tornado("Tornado"),
        Hail("Hail"),
        Windy("Windy");

    private WeatherCondition(String name) {
        mCondition = name;
    }

    private final String mCondition;

    public String toString() {
        return mCondition;
    }
}

    private static final WeatherCondition[] WEATHER_CONDITIONS = new WeatherCondition[] {
        WeatherCondition.Sunny,
        WeatherCondition.Cloudy,
        WeatherCondition.Overcast,
        WeatherCondition.Shower,
        WeatherCondition.ThunderyShower,

        WeatherCondition.ThunderstormHail,
        WeatherCondition.Sleet,
        WeatherCondition.Drizzle,
        WeatherCondition.Rain,
        WeatherCondition.Downpour,

        WeatherCondition.SuperDownpour,
        WeatherCondition.SnowShowers,
        WeatherCondition.Flurries,
        WeatherCondition.Snow,
        WeatherCondition.HeavySnow,

        WeatherCondition.Blizzard,
        WeatherCondition.Fog,
        WeatherCondition.FreezingRain,
        WeatherCondition.SandStorm,
        WeatherCondition.Dust,

        WeatherCondition.Sand,
        WeatherCondition.Hurricane,
        WeatherCondition.Tornado,
        WeatherCondition.Hail,
        WeatherCondition.Windy,
    };

    /**
     *
     * @param index
     * @return
     */
    public static WeatherCondition intToWeatherCondition(int index) {
        if (index < 0 || index >= WEATHER_CONDITIONS.length) {
            throw new IndexOutOfBoundsException();
        }
        return WEATHER_CONDITIONS[index];
    }

    /**
     * Contains helper classes used to create or manage {@link android.content.Intent Intents}
     * that involve Weather.
     */
    public static final class Intents {
        /**
         * intent action to start weather provider setting activity, and the activity will return
         * city name and city id if user choose a city
         */
        public static final String ACTION_SETTING = "com.weather.action.SETTING";

        /**
         * intent action to search a city, the activity will return city name and city id
         * {@link #EXTRA_CITY_NAME}
         * {@link #EXTRA_CITY_ID}
         */
        public static final String ACTION_SEARCH_CITY = "com.mediatek.provider.Weather.SEARCH_CITY";

        /**
         * intent action to pick a city from city list, the activity will return city name and city id
         * {@link #EXTRA_CITY_NAME}
         * {@link #EXTRA_CITY_ID}
         */
        public static final String ACTION_CHOOSE_CITY = "com.mediatek.provider.weather.CITY_LIST_MANAGEMENT";

        /**
         * Used with {@link #ACTION_SEARCH_CITY} and {@link #ACTION_CHOOSE_CITY} to return city name to the caller activity
         * <p>
         * Type: STRING
         */
        public static final String EXTRA_CITY_NAME = "com.mediatek.provider.weather.EXTRA_CITY_NAME";

        /**
         * Used with {@link #ACTION_SEARCH_CITY} and {@link #ACTION_CHOOSE_CITY} to return city name to the caller activity
         * <p>
         * Type: INT
         */
        public static final String EXTRA_CITY_ID = "com.mediatek.provider.weather.EXTRA_CITY_ID";
    }

    /** success */
    public static final int SUCCESS = 0;
    /** system time not correct */
    public static final int ERROR_SYSTEM_TIME_NOT_CORRECT = 1;
    /**network not available */
    public static final int ERROR_NETWORK_NOT_AVAILABLE = 2;
    /**update weather failed */
    public static final int ERROR_UPDATE_WEATHER_FAILED = 3;
    /** city id not correct */
    public static final int ERROR_CITY_ID_NOT_CORRECT = 4;

//    public static final int[] ERROR_RESOURCE_ID = new int[] {
//        R.string.weather_error_success,
//        R.string.weather_error_system_time_not_correct,
//        R.string.weather_error_network_not_avail,
//        R.string.weather_error_update_weather_failed,
//        R.string.weather_error_city_id_not_correct,
//    };
}
