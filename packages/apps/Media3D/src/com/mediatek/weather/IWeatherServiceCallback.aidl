package com.mediatek.weather;

oneway interface IWeatherServiceCallback {
    /**
     * Called when the Weather Provider has new weather information available 
     * @param cityId  which city 's weather information is update
     * @param result  the weather update result 
        0 : success
        1 : system time not correct
        2 : network not available
        3 : general update weather fail
     */
    void onWeatherUpdate(int cityId,int result);
}
