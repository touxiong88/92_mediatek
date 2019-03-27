package com.mediatek.weather;

import com.mediatek.weather.IWeatherServiceCallback;
import com.mediatek.weather.WeatherUpdateResult;

interface IWeatherService {
    /**
     * update weather information directly
     * @param cityId  to update which city
     * @param timeAfter  if timeAfter is greater than last update, it will return value in database, else it will fetch 
     *  weather information from network,
     *  special value:  timeAfter == -1  : if there is any weather data in database ,just return it, else fetch it from network     
     * return the result of weather update
     *   0 : success
     *   1 : system time not correct
     *   2 : network not available
     *   3 : general update weather fail
     *   4 : city id not correct
     */
    WeatherUpdateResult updateWeather(int cityId,long timeAfter);
    
    /**
     * registering a callback interface with weather service
     */
    void registerCallback(IWeatherServiceCallback cb);
    
     /**
     * Remove a previously registered callback interface.
     */
    void unregisterCallback(IWeatherServiceCallback cb);    
}
