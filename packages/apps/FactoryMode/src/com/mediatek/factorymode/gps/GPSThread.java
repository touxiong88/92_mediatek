
package com.mediatek.factorymode.gps;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

public class GPSThread {

    private boolean mIsSuccess = false;

    private Context mContext;

    private GpsUtil mGpsUtil;

    public GPSThread(Context context) {
        mContext = context;
        mGpsUtil = new GpsUtil(mContext);
    }

    public boolean isSuccess() {
        return mIsSuccess;
    }

    public void closeLocation() {
        mGpsUtil.closeLocation();
    }

    public void start() {
        getSatelliteInfo();
    }

    public int getSatelliteNum() {
        return mGpsUtil.getSatelliteNumber();
    }

    public String getSatelliteSignals() {
        return mGpsUtil.getSatelliteSignals();
    }

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 0:
                    getSatelliteInfo();
                    break;
            }
        }
    };

    private void getSatelliteInfo() {
        if ((mGpsUtil.getSatelliteNumber()) > 2) {
            mHandler.removeMessages(0);
            mIsSuccess = true;
            closeLocation();
        } else {
            mHandler.sendEmptyMessageDelayed(0, 2000);
        }
    }

}
