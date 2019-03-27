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

package com.mediatek.location;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.AsyncTask;
import android.os.Bundle;

import com.mediatek.media3d.LogUtil;

public class LocationUpdater implements android.location.LocationListener {
    private static final String TAG = "LocationUpdater";

    public static final int MSG_LOCATION_NOT_FOUND_YET = 0;
    public static final int MSG_CANCELLED_BY_CLIENT = 1;
    public static final int MSG_PROVIDER_ENABLED = 2;
    public static final int MSG_PROVIDER_DISABLED = 3;
    public static final int MSG_PROVIDER_OUT_OF_SERVICE = 4;
    public static final int MSG_PROVIDER_UNAVAILABLE = 5;
    public static final int MSG_PROVIDER_AVALIABLE = 6;

    private LocationManager mLocationManager;
    private LocationListener mListener;
    private Context mContext;
    private int mUpdateInterval;

    private final GeoCoder mGeoCoder = new GeoCoder();

    public LocationUpdater(Context ctx) {
        mContext = ctx;
    }

    public interface LocationListener {
        void onLocationChanged(LocationEx location);
        void onStatusChanged(int msg);
    }

    public Location getLastKnownLocation() {
        try {
            if (null != mLocationManager) {
                Location lastKnownLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (lastKnownLocation == null) {
                    lastKnownLocation = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
                return lastKnownLocation; 
            }
        } catch (SecurityException ex) {
            LogUtil.v(TAG, "Exception :" + ex);
        } catch (IllegalArgumentException ex) {
            LogUtil.v(TAG, "Exception :" + ex);
        }
        return null;
    }

    public void registerLocationListener(LocationListener listener) {
        mListener = listener;
    }

    public void unregisterLocationListener(LocationListener listener) {
        if (mListener.equals(listener)) {
            mLocationManager.removeUpdates(this);
            mIsRequestLocationUpdate = false;
            mContext = null;
            mListener = null;
        }
    }

    private boolean mIsRequestLocationUpdate;
    private static final long MINUTE = 60000l;

    public void requestLocationUpdates(int interval) {
        mUpdateInterval = interval;
        mIsRequestLocationUpdate = true;
        
        if (null == mLocationManager) {
            mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
            LogUtil.v(TAG, "location manager : " + mLocationManager);
        }

        if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            mLocationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, MINUTE * interval,
                10, this);
        } else {
            LogUtil.v(TAG, "Gps provider is disabled.");
        }

        if (mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            mLocationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER, MINUTE * interval,
                0, this);
        } else {
            LogUtil.v(TAG, "Network provider is disabled.");
        }

        Location lastKnownLocation = getLastKnownLocation();
        LogUtil.v(TAG, "last known location :" + lastKnownLocation);
        if (lastKnownLocation != null) {
            new ReverseGeocodeWoeidTask().execute(lastKnownLocation);
        }
    }

    private class ReverseGeocodeWoeidTask extends AsyncTask<Location, Void, LocationEx> {
        @Override
        protected LocationEx doInBackground(Location... params) {
            return mGeoCoder.getCityFromGeoCode(params[0]);
        }

        @Override
        protected void onPostExecute(LocationEx location) {
            if (isCancelled()) {
                notifyStatusChanged(MSG_CANCELLED_BY_CLIENT);
                return;
            }

            if (location == null) {
                notifyStatusChanged(MSG_LOCATION_NOT_FOUND_YET);
            } else {
                LogUtil.v(TAG, "locationEx : " + location);
                notifyLocationChanged(location);
            }
        }
    }

    private void notifyLocationChanged(LocationEx location) {
        if (mListener != null) {
            LogUtil.v(TAG, "Notify Location Changed : " + location);
            mListener.onLocationChanged(location);
        }
    }

    private void notifyStatusChanged(int msg) {
        if (mListener != null) {
            LogUtil.v(TAG, "Notify Status Changed : " + msg);
            mListener.onStatusChanged(msg);
        }
    }

    // android.location.LocationListener
    public void onLocationChanged(Location location) {
        LogUtil.v(TAG, "Location : " + location);
        new ReverseGeocodeWoeidTask().execute(location);
    }

    public void onProviderDisabled(String provider) {
        if (mIsRequestLocationUpdate) {
            notifyStatusChanged(MSG_PROVIDER_DISABLED);
        }
    }

    public void onProviderEnabled(String provider) {
        if (mIsRequestLocationUpdate) {
            requestLocationUpdates(mUpdateInterval);
            notifyStatusChanged(MSG_PROVIDER_ENABLED);
        }
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
        if (mIsRequestLocationUpdate) {
            int result = MSG_PROVIDER_AVALIABLE;
            switch (status) {
            case LocationProvider.OUT_OF_SERVICE :
                result = MSG_PROVIDER_OUT_OF_SERVICE;
                break;
            case LocationProvider.TEMPORARILY_UNAVAILABLE :
                result = MSG_PROVIDER_UNAVAILABLE;
                break;
            case LocationProvider.AVAILABLE :
                result = MSG_PROVIDER_AVALIABLE;
                break;
            default:
                break;
            }
            notifyStatusChanged(result);
        }
    }
}