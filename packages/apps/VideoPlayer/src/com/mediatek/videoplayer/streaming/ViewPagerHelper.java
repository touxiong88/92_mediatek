/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2013. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.mediatek.videoplayer.streaming;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

//import com.mediatek.videoplayer.LetvApplication;
import com.mediatek.videoplayer.MovieListActivity;
import com.mediatek.videoplayer.MtkLog;
import com.mediatek.videoplayer.streaming.SettingsParser.VideoPlayerSetting;

import android.app.Activity;
import android.app.LocalActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Debug;
import android.util.Log;
import android.view.View;

public class ViewPagerHelper {
    private static final String TAG = "ViewPagerHelper";
    private LocalActivityManager mActivityManager = null;
    private Context mContext = null;
    private ArrayList<VideoPlayerSetting> mSettings = null;
    private static final String LETV_ID = "letv_id";

    public ViewPagerHelper(Context context, LocalActivityManager activityManager) {
        mContext = context;
        mActivityManager = activityManager;
        mSettings = new SettingsParser(context).getSettings();
        MtkLog.d(TAG, "ViewPagerHelper() mSettings is " + mSettings);
    }

    public int getTabCount() {
        return mSettings.size();
    }
    
    public String getTabId(int index) {
        return mSettings.get(index).getId();
    }

    public int getTabIndex(String tabId) {
        int index = -1;
        for (VideoPlayerSetting setting : mSettings) {
            if (tabId != null && tabId.equals(setting.getId())) {
                return setting.getIndex();
            }
        }
        return index;
    }
    
    public String getTabName(int index) {
        return mSettings.get(index).getTabName();
    }

    public View getView(int index) {
        initVendorSdk(index);
        String action = mSettings.get(index).getAction();
        Intent intent = new Intent(action);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        View view = mActivityManager.startActivity(mSettings.get(index).getId(), intent).getDecorView();
        return view;
    }

    private void initVendorSdk(int index) throws RuntimeException {
        if (LETV_ID.equals(mSettings.get(index).getId())) {
            initLetvApplication();
        }
    }

    private void initLetvApplication() throws RuntimeException {
        Class myClass;
        try {
            myClass = Class.forName("com.mediatek.videoplayer.LetvApplication");
            Object obj = myClass.newInstance();
            Method method = obj.getClass().getDeclaredMethod("onCreate", Context.class);
            method.invoke(obj, mContext.getApplicationContext());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException("ClassNotFoundException");
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            throw new RuntimeException("NoSuchMethodException");
        } catch (InstantiationException e) {
            e.printStackTrace();
            throw new RuntimeException("InstantiationException");
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            throw new RuntimeException("IllegalAccessException");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            throw new RuntimeException("IllegalArgumentException");
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            throw new RuntimeException("InvocationTargetException");
        }
    }
    
    public void notifyActivities(int position) {
        String tabId = getTabId(position);
        for (VideoPlayerSetting setting : mSettings) {
            Activity activity = mActivityManager.getActivity(setting.getId());
            if (activity == null) {// mActivityManager.startActivity is async, so this may get null
                Log.e(TAG, "activity is null. Tab id is  " + setting.getId());
                return;
            }
            boolean isFocus = setting.getId().equals(tabId);
            String method = setting.getCallBackFocus();
            try {
                long start = System.currentTimeMillis();
                reflectCallOnActivityFocus(activity, method, isFocus);
                long end = System.currentTimeMillis();
                MtkLog.d(TAG, "Call onActivityFocus(" + isFocus + ") "+ setting.getId() + ", cost:" + (end - start));
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("IllegalArgumentException e:" + e);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
                throw new RuntimeException("NoSuchMethodException e:" + e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("IllegalAccessException e:" + e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException("InvocationTargetException e:" + e);
            }
        }
    }
    
    private void reflectCallOnActivityFocus(Activity activity, String methodName, boolean isFocus) throws NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        Method method = activity.getClass().getDeclaredMethod(methodName, boolean.class);
        method.invoke(activity, isFocus);
    }
}
