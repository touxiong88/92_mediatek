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

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.LocalActivityManager;
import android.app.TabActivity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.TabHost.OnTabChangeListener;

import com.mediatek.videoplayer.MtkLog;
import com.mediatek.videoplayer.R;


public class StreamingVideoTabActivity extends TabActivity implements OnTabChangeListener,
        ViewPager.OnPageChangeListener {
    private static final String TAG = "StreamingVideoTabActivity";
    private static final String SAVE_TAB = "activitetab";
    private static final int DEFAULT_INDEX = 0;
    private TabHost mTabHost = null;
    private ViewPager mViewPager = null;
    private LocalActivityManager mActivityManager = null;
    private List<View> mScreenViews = new ArrayList<View>();
    private ViewPagerHelper mViewPagerHelper = null;
    private LayoutInflater mLayoutInflater = null;
    private int mCurrentIndex = DEFAULT_INDEX;
    private int mViewPagerState = ViewPager.SCROLL_STATE_IDLE;
    private boolean mPageChanged = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.stream_video_tab_activity);
        
        mActivityManager = new LocalActivityManager(this, false);
        mActivityManager.dispatchCreate(savedInstanceState);
        mLayoutInflater = LayoutInflater.from(this);

        mViewPagerHelper = new ViewPagerHelper(this, mActivityManager);
        mTabHost = getTabHost();
        initTab();
        mCurrentIndex = StreamingUtils.getIntPref(this, SAVE_TAB, DEFAULT_INDEX);
        MtkLog.d(TAG, "onCreate() mCurrentIndex is " + mCurrentIndex);
        mTabHost.setCurrentTab(mCurrentIndex);
        mTabHost.setOnTabChangedListener(this);

        initPager();
        mViewPager = (ViewPager) findViewById(R.id.viewpage);
        mViewPager.setAdapter(new ViewPagerAdapter());
        mViewPager.setOnPageChangeListener(this);
        mViewPager.setCurrentItem(mCurrentIndex);
    }
    
    /**
     * initial view pager
     */
    private void initPager() {
        mScreenViews.clear();
        for (int i = 0; i < mViewPagerHelper.getTabCount(); i++) {
            mScreenViews.add(getView(i)); // Avoid activity null pointer, because we will call onActivityFocus()
        }
    }
    
    /**
     * get current view
     * 
     * @param index
     * @return View
     */
    private View getView(int index) {
        return mViewPagerHelper.getView(index);
    }
    
    class ViewPagerAdapter extends PagerAdapter {
        // Check whether the first nofity activity called
        private boolean mFirstLaunchNotified = false;
        
        @Override
        public void destroyItem(View container, int position, Object object) {
            ViewPager viewPager = ((ViewPager) container);
            viewPager.removeView(mScreenViews.get(position));
            
        }

        @Override
        public void finishUpdate(View arg0) {
        }

        @Override
        public int getCount() {
            return mScreenViews.size();
        }

        @Override
        public Object instantiateItem(View container, int position) {
            ViewPager viewPager = ((ViewPager) container);
            MtkLog.d(TAG, "instantiateItem() position is " + position);
            View view = mScreenViews.get(position);
            if (view == null) {
                view = getView(position);
                mScreenViews.remove(position);
                mScreenViews.add(position, view);
                mActivityManager.dispatchResume();
            }
            viewPager.addView(view);
            // Notify the activity for the first launch time
            if (!mFirstLaunchNotified) {
                mFirstLaunchNotified = true;
                mViewPagerHelper.notifyActivities(mCurrentIndex); // Notify activity on focus or not
            }
            return mScreenViews.get(position);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == null ? false : view.equals(object);
        }

        @Override
        public void restoreState(Parcelable arg0, ClassLoader arg1) {
        }

        @Override
        public Parcelable saveState() {
            return null;
        }

        @Override
        public void startUpdate(View arg0) {
        }
    }
    
    private void initTab() {
        for (int i = 0; i < mViewPagerHelper.getTabCount(); i++) {
            String tabId = mViewPagerHelper.getTabId(i);
            String tabName = mViewPagerHelper.getTabName(i);
            TextView tabView;
            tabView = (TextView) mLayoutInflater.inflate(R.layout.tab_widget_indicator, mTabHost.getTabWidget(), false);
            tabView.setText(tabName);
            tabView.setContentDescription(tabId);
            // For long string
            //tabView.setSingleLine(true);
            //tabView.setEllipsize(TextUtils.TruncateAt.valueOf("MARQUEE"));
            //tabView.setMarqueeRepeatLimit(2);
            
            mTabHost.addTab(mTabHost.newTabSpec(tabId).setIndicator(tabView).setContent(android.R.id.tabcontent));
            
            // Reset the width of TextView to 1/3 of the ScreenWidthDp
            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            ViewGroup.LayoutParams params = tabView.getLayoutParams();
            params.width =  metrics.widthPixels/3;
            tabView.setLayoutParams(params);
            MtkLog.d(TAG, "initTab() widthPixels is " + metrics.widthPixels + ", width is " + params.width);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        mActivityManager.dispatchResume();
    }
    
    @Override
    protected void onPause() {
        StreamingUtils.setIntPref(this, SAVE_TAB, mCurrentIndex);
        mActivityManager.dispatchPause(false);
        super.onPause();
    }
    
    @Override
    protected void onStop() {
        mActivityManager.dispatchStop();
        super.onStop();
    }
    
    @Override
    protected void onDestroy() {
        mActivityManager.dispatchDestroy(false);
        super.onDestroy();
    }

    @Override
    public void onTabChanged(String tabId) {
        int tabIndex = mViewPagerHelper.getTabIndex(tabId);
        MtkLog.d(TAG, "onTabChanged() tabId" + tabId  + ", tabIndex: " + tabId);
        mViewPager.setCurrentItem(tabIndex);
        mCurrentIndex = tabIndex;
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        MtkLog.d(TAG, "onPageScrollStateChanged() state is " + state);
        mViewPagerState = state;
        if ((state == ViewPager.SCROLL_STATE_IDLE) && (mPageChanged == true)) {
            MtkLog.d(TAG, "onPageScrollStateChanged() notify activity focus" + mCurrentIndex);
            mViewPagerHelper.notifyActivities(mCurrentIndex); // Notify activity on focus or not
            mPageChanged = false;
        }
    }

    @Override
    public void onPageScrolled(int arg0, float arg1, int arg2) {
    }

    @Override
    public void onPageSelected(final int position) {
        MtkLog.d(TAG, "onPageSelected " + position);
        mPageChanged = true;
        mTabHost.setCurrentTab(position);
    }
}