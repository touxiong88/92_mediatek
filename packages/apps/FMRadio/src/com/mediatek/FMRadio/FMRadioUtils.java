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

package com.mediatek.FMRadio;

import android.content.Context;
import android.os.Environment;
import android.os.storage.StorageManager;

import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.storage.StorageManagerEx;

import java.util.Locale;

/**
 * This class provider interface to compute station and frequency, get project string
 *
 */
public class FMRadioUtils {
    private static final String TAG = "FmRx/Utils";
    
    // default station frequency
    public static final int DEFAULT_STATION = FeatureOption.MTK_FM_50KHZ_SUPPORT ? 10000 : 1000; 
    // maximum station frequency
    public static final int HIGHEST_STATION = FeatureOption.MTK_FM_50KHZ_SUPPORT ? 10800 : 1080;
    // minimum station frequency
    public static final int LOWEST_STATION = FeatureOption.MTK_FM_50KHZ_SUPPORT ? 8750 : 875;
    // station step
    public static final int STEP = FeatureOption.MTK_FM_50KHZ_SUPPORT ? 5 : 1;
    // convert rate
    public static final int CONVERT_RATE = FeatureOption.MTK_FM_50KHZ_SUPPORT ? 100 : 10; 

/* Vanzo:wangyi on: Tue, 04 Dec 2012 16:37:17 +0800
 * add FM FactoryMode
 */
    private static final String HEADSET_STATE_PATH = "/sys/class/switch/h2w/state";
// End of Vanzo: wangyi

    public static final float DEFAULT_STATION_FLOAT 
        = FMRadioUtils.computeFrequency(FMRadioUtils.DEFAULT_STATION);

    /**
     * Whether the frequency is valid.
     * 
     * @param station the FM station
     * @return if the frequency is in the valid scale, return true, otherwise
     *         false
     */
    public static boolean isValidStation(int station) {
        boolean isValid = false; 
        final int checkNumber = 5;
        
        isValid = (station >= LOWEST_STATION && station <= HIGHEST_STATION);
        if (FeatureOption.MTK_FM_50KHZ_SUPPORT) {
            isValid = isValid && (station % checkNumber == 0);
        }
        LogUtils.v(TAG, "isValidStation: freq = " + station + ", valid = " + isValid);
        return isValid;
    }
    
    /**
     * compute increase station frequency
     * @param station station frequency
     * @return station frequency after increased
     */
    public static int computeIncreaseStation(int station) {
        int result = station + STEP;
        if (result > HIGHEST_STATION) {
            result = LOWEST_STATION;
        }
        return result;
    }
    
    /**
     * compute decrease station frequency
     * @param station station frequency
     * @return station frequency after decreased
     */
    public static int computeDecreaseStation(int station) {
        int result = station - STEP;
        if (result < FMRadioUtils.LOWEST_STATION) {
            result = FMRadioUtils.HIGHEST_STATION;
        }
        return result;
    }
    
    /**
     * compute station value with given frequency
     * @param frequency station frequency
     * @return station value
     */
    public static int computeStation(float frequency) {
        return (int) (frequency * CONVERT_RATE);
    }
    
    /**
     * compute frequency value with given station
     * @param station station value
     * @return station frequency
     */
    public static float computeFrequency(int station) {
        return (float) station / CONVERT_RATE;
    }
    
    /**
     * according station to get frequency string
     * @param station for 100KZ, range 875-1080, for 50khz 8750,1080
     * @return string like 87.5 or 87.50
     */
    public static String formatStation(int station) {
        float frequency = (float)station / CONVERT_RATE;
        String result = null;
        if (FeatureOption.MTK_FM_50KHZ_SUPPORT) {
            result = String.format(Locale.ENGLISH, "%.2f", Float.valueOf(frequency));
        } else {
            result = String.format(Locale.ENGLISH, "%.1f", Float.valueOf(frequency));
        }
        return result;
    }

/* Vanzo:wangyi on: Tue, 04 Dec 2012 16:38:16 +0800
 * add FM FactoryMode
 */
    static boolean isWiredHeadsetReallyOn() {
        try {
            char[] buffer = new char[8];
            java.io.FileReader file = new java.io.FileReader(HEADSET_STATE_PATH);
            int len = file.read(buffer, 0, 8);
            int state = Integer.valueOf((new String(buffer, 0, len)).trim());
            file.close();
            return (state == 1 || state == 2);
        } catch (java.io.FileNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
// End of Vanzo: wangyi
    
    /**
     * Get the phone storage path
     * @return the phone storage path
     */
    public static String getInternalStoragePath() {
        return StorageManagerEx.getInternalStoragePath();
    }
    
    /**
     * Get the phone storage state
     * @return the phone storage state
     */
    public static String getInternalStorageState(Context context) {
        ensureStorageManager(context);
        String state = sStorageManager.getVolumeState(getInternalStoragePath());
        LogUtils.d(TAG, "getInternalStorageState() return state:" + state);
        return state;
    }
    
    private static StorageManager sStorageManager = null;
    private static void ensureStorageManager(Context context) {
        if (sStorageManager == null) {
            sStorageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        }
    }
}
