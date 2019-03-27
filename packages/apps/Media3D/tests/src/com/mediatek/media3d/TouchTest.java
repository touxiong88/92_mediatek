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

package com.mediatek.media3d;

import android.test.ActivityInstrumentationTestCase2;

/**
 * This is a simple framework for a test of an Application.  See
 * {@link android.test.ApplicationTestCase ApplicationTestCase} for more information on
 * how to write and extend Application tests.
 * <p/>
 * To run this test, you can type:
 * adb shell am instrument -w -e class com.mediatek.media3d.TouchTest com.mediatek.media3d.tests/android.test.InstrumentationTestRunner
 */

public class TouchTest extends ActivityInstrumentationTestCase2<Main> {
    private Main mActivity;
    private Media3DView mMedia3DView;

    public TouchTest() {
        super("com.mediatek.media3d", Main.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
        if (null != mActivity) {
            mMedia3DView = mActivity.getMedia3DView();
        }
    }

    @Override
    public void tearDown() throws Exception {
        mMedia3DView = null;
        mActivity = null;
        super.tearDown();
    }

    private void enterPortalPage(Main activity) {
        CommonTestUtil.waitPageForIdleSync(getInstrumentation(), activity.getMedia3DView(), activity.getPortalPage(), CommonTestUtil.DEFAULT_PAGE_SWITCH_TIMEOUT_IN_MS);

        CommonTestUtil.waitLoadForIdleSync(getInstrumentation(), activity.getWeatherPage(), CommonTestUtil.DEFAULT_PAGE_SWITCH_TIMEOUT_IN_MS);
        CommonTestUtil.waitLoadForIdleSync(getInstrumentation(), activity.getPhotoPage(), CommonTestUtil.DEFAULT_PAGE_SWITCH_TIMEOUT_IN_MS);
        CommonTestUtil.waitLoadForIdleSync(getInstrumentation(), activity.getVideoPage(), CommonTestUtil.DEFAULT_PAGE_SWITCH_TIMEOUT_IN_MS);

        assertTrue(activity.getMedia3DView().getCurrentPage() == activity.getPortalPage());
    }

    public void test01WeatherPageTouch() {
        enterPortalPage(mActivity);

        CommonTestUtil.sendSingleTapConfirmedEventOnUiThread(mActivity, mMedia3DView, CommonTestUtil.WEATHER_ICON_IN_PORTALPAGE);
        CommonTestUtil.sleep(5000);

        assertTrue(mMedia3DView.getCurrentPage() == mActivity.getWeatherPage());
        CommonTestUtil.sleep(3000);

        // wait time out back
        //test if the bar is out
        CommonTestUtil.sendSingleTapConfirmedEventOnUiThread(mActivity, mMedia3DView, CommonTestUtil.NON_ACTOR_POINT);
        CommonTestUtil.sleep(2000);
        assertEquals(Media3DView.BAR_STATE_ENTERED, mMedia3DView.getBarState());
        // test if the bar is back
        CommonTestUtil.sleep(4000);
        assertEquals(Media3DView.BAR_STATE_HIDDEN, mMedia3DView.getBarState());

        // tap back on bar
        // test tap bar out/in
        CommonTestUtil.sendSingleTapConfirmedEventOnUiThread(mActivity, mMedia3DView, CommonTestUtil.NON_ACTOR_POINT);
        CommonTestUtil.sleep(2000);
        assertEquals(Media3DView.BAR_STATE_ENTERED, mMedia3DView.getBarState());
        CommonTestUtil.sendSingleTapConfirmedEventOnUiThread(mActivity, mMedia3DView, CommonTestUtil.NON_ACTOR_POINT);
        CommonTestUtil.sleep(2000);
        assertEquals(Media3DView.BAR_STATE_HIDDEN, mMedia3DView.getBarState());

        // tap back not on bar
        // test tap bar out/in
        CommonTestUtil.sendSingleTapConfirmedEventOnUiThread(mActivity, mMedia3DView, CommonTestUtil.NON_ACTOR_POINT);
        CommonTestUtil.sleep(1500);
        assertEquals(Media3DView.BAR_STATE_ENTERED, mMedia3DView.getBarState());
        CommonTestUtil.sendSingleTapConfirmedEventOnUiThread(mActivity, mMedia3DView, CommonTestUtil.CENTER);
        CommonTestUtil.sleep(1500);
        assertEquals(Media3DView.BAR_STATE_HIDDEN, mMedia3DView.getBarState());

        // tap home button on tool bar
        CommonTestUtil.sendSingleTapConfirmedEventOnUiThread(mActivity, mMedia3DView, CommonTestUtil.NON_ACTOR_POINT);
        CommonTestUtil.sleep(1500);
        assertEquals(Media3DView.BAR_STATE_ENTERED, mMedia3DView.getBarState());

        CommonTestUtil.sendTouchEventsOnUiThread(mActivity, mMedia3DView, CommonTestUtil.BACK_ICON_IN_TOP_MENU);
        CommonTestUtil.sleep(1500);
        assertTrue(mMedia3DView.getCurrentPage() == mActivity.getPortalPage());
    }

    public void test02PhotoPageTouch() {
        enterPortalPage(mActivity);

        CommonTestUtil.sendSingleTapConfirmedEventOnUiThread(mActivity, mMedia3DView, CommonTestUtil.PHOTO_ICON_IN_PORTALPAGE);
        CommonTestUtil.sleep(5000);
        assertTrue(mMedia3DView.getCurrentPage() == mActivity.getPhotoPage());
        CommonTestUtil.sleep(3000);

        //test if the bar is out
        CommonTestUtil.sendSingleTapConfirmedEventOnUiThread(mActivity, mMedia3DView, CommonTestUtil.NON_ACTOR_POINT);
        CommonTestUtil.sleep(2000);
        assertEquals(Media3DView.BAR_STATE_ENTERED, mMedia3DView.getBarState());

        // test if the bar is back
        CommonTestUtil.sleep(4000);
        assertEquals(Media3DView.BAR_STATE_HIDDEN, mMedia3DView.getBarState());

        // test tap bar out/in
        CommonTestUtil.sendSingleTapConfirmedEventOnUiThread(mActivity, mMedia3DView, CommonTestUtil.NON_ACTOR_POINT);
        CommonTestUtil.sleep(2000);
        assertEquals(Media3DView.BAR_STATE_ENTERED, mMedia3DView.getBarState());
        CommonTestUtil.sendSingleTapConfirmedEventOnUiThread(mActivity, mMedia3DView, CommonTestUtil.NON_ACTOR_POINT);
        CommonTestUtil.sleep(2000);
        assertEquals(Media3DView.BAR_STATE_HIDDEN, mMedia3DView.getBarState());


        // test tap bar out/in
        CommonTestUtil.sendSingleTapConfirmedEventOnUiThread(mActivity, mMedia3DView, CommonTestUtil.NON_ACTOR_POINT);
        CommonTestUtil.sleep(1500);
        assertEquals(Media3DView.BAR_STATE_ENTERED, mMedia3DView.getBarState());
        CommonTestUtil.sendSingleTapConfirmedEventOnUiThread(mActivity, mMedia3DView, CommonTestUtil.CENTER);
        CommonTestUtil.sleep(1500);
        assertEquals(Media3DView.BAR_STATE_HIDDEN, mMedia3DView.getBarState());

        // tap home button on tool bar
        CommonTestUtil.sendSingleTapConfirmedEventOnUiThread(mActivity, mMedia3DView, CommonTestUtil.NON_ACTOR_POINT);
        CommonTestUtil.sleep(1500);
        assertEquals(Media3DView.BAR_STATE_ENTERED, mMedia3DView.getBarState());

        CommonTestUtil.sendTouchEventsOnUiThread(mActivity, mMedia3DView, CommonTestUtil.BACK_ICON_IN_TOP_MENU);
        CommonTestUtil.sleep(1500);
        assertTrue(mMedia3DView.getCurrentPage() == mActivity.getPortalPage());
    }

    public void test03VideoPageTouch() {
        enterPortalPage(mActivity);

        CommonTestUtil.sendSingleTapConfirmedEventOnUiThread(mActivity, mMedia3DView, CommonTestUtil.VIDEO_ICON_IN_PORTALPAGE);
        CommonTestUtil.sleep(5000);
        assertTrue(mMedia3DView.getCurrentPage() == mActivity.getVideoPage());
        CommonTestUtil.sleep(3000);

        //test if the bar is out
        CommonTestUtil.sendSingleTapConfirmedEventOnUiThread(mActivity, mMedia3DView, CommonTestUtil.NON_ACTOR_POINT);
        CommonTestUtil.sleep(2000);
        assertEquals(Media3DView.BAR_STATE_ENTERED, mMedia3DView.getBarState());

        // test if the bar is back
        CommonTestUtil.sleep(4000);
        assertEquals(Media3DView.BAR_STATE_HIDDEN, mMedia3DView.getBarState());

        // test tap bar out/in
        CommonTestUtil.sendSingleTapConfirmedEventOnUiThread(mActivity, mMedia3DView, CommonTestUtil.NON_ACTOR_POINT);
        CommonTestUtil.sleep(2000);
        assertEquals(Media3DView.BAR_STATE_ENTERED, mMedia3DView.getBarState());
        CommonTestUtil.sendSingleTapConfirmedEventOnUiThread(mActivity, mMedia3DView, CommonTestUtil.NON_ACTOR_POINT);
        CommonTestUtil.sleep(2000);
        assertEquals(Media3DView.BAR_STATE_HIDDEN, mMedia3DView.getBarState());

        // test tap bar out/in
        CommonTestUtil.sendSingleTapConfirmedEventOnUiThread(mActivity, mMedia3DView, CommonTestUtil.NON_ACTOR_POINT);
        CommonTestUtil.sleep(1500);
        assertEquals(Media3DView.BAR_STATE_ENTERED, mMedia3DView.getBarState());
        CommonTestUtil.sendSingleTapConfirmedEventOnUiThread(mActivity, mMedia3DView, CommonTestUtil.CENTER);
        CommonTestUtil.sleep(1500);
        assertEquals(Media3DView.BAR_STATE_HIDDEN, mMedia3DView.getBarState());

        // tap home button on tool bar
        CommonTestUtil.sendSingleTapConfirmedEventOnUiThread(mActivity, mMedia3DView, CommonTestUtil.NON_ACTOR_POINT);
        CommonTestUtil.sleep(1500);
        assertEquals(Media3DView.BAR_STATE_ENTERED, mMedia3DView.getBarState());

        CommonTestUtil.sendTouchEventsOnUiThread(mActivity, mMedia3DView, CommonTestUtil.BACK_ICON_IN_TOP_MENU);
        CommonTestUtil.sleep(1500);
        assertTrue(mMedia3DView.getCurrentPage() == mActivity.getPortalPage());
    }

    public void test04NavigationBar() {
        int PAGE_SWITCH_DELAY_SLEEP = 5000;
        int BAR_DELAY_SLEEP = 3000;

        enterPortalPage(mActivity);

        CommonTestUtil.sendSingleTapConfirmedEventOnUiThread(mActivity, mMedia3DView, CommonTestUtil.WEATHER_ICON_IN_PORTALPAGE);
        CommonTestUtil.sleep(PAGE_SWITCH_DELAY_SLEEP);
        assertTrue(mMedia3DView.getCurrentPage() == mActivity.getWeatherPage());
        CommonTestUtil.sleep(PAGE_SWITCH_DELAY_SLEEP);

        // test if the bar is out
        CommonTestUtil.sendSingleTapConfirmedEventOnUiThread(mActivity, mMedia3DView, CommonTestUtil.NON_ACTOR_POINT);
        CommonTestUtil.sleep(BAR_DELAY_SLEEP);
        assertEquals(Media3DView.BAR_STATE_ENTERED, mMedia3DView.getBarState());

        // weather -> weather
        CommonTestUtil.sendTouchEventsOnUiThread(mActivity, mMedia3DView, CommonTestUtil.WEATHER_ICON_IN_NAVI_BAR);
        CommonTestUtil.sleep(PAGE_SWITCH_DELAY_SLEEP);
        assertTrue(mMedia3DView.getCurrentPage() == mActivity.getWeatherPage());

        // test if the bar is out
        CommonTestUtil.sendSingleTapConfirmedEventOnUiThread(mActivity, mMedia3DView, CommonTestUtil.NON_ACTOR_POINT);
        CommonTestUtil.sleep(BAR_DELAY_SLEEP);
        assertEquals(Media3DView.BAR_STATE_ENTERED, mMedia3DView.getBarState());

        // weather -> photo
        CommonTestUtil.sendTouchEventsOnUiThread(mActivity, mMedia3DView, CommonTestUtil.PHOTO_ICON_IN_NAVI_BAR);
        CommonTestUtil.sleep(PAGE_SWITCH_DELAY_SLEEP);
        assertTrue(mMedia3DView.getCurrentPage() == mActivity.getPhotoPage());

        // test if the bar is out
        CommonTestUtil.sendSingleTapConfirmedEventOnUiThread(mActivity, mMedia3DView, CommonTestUtil.NON_ACTOR_POINT);
        CommonTestUtil.sleep(BAR_DELAY_SLEEP);
        assertEquals(Media3DView.BAR_STATE_ENTERED, mMedia3DView.getBarState());

        // photo -> photo
        CommonTestUtil.sendTouchEventsOnUiThread(mActivity, mMedia3DView, CommonTestUtil.PHOTO_ICON_IN_NAVI_BAR);
        CommonTestUtil.sleep(PAGE_SWITCH_DELAY_SLEEP);
        assertTrue(mMedia3DView.getCurrentPage() == mActivity.getPhotoPage());

        // test if the bar is out
        CommonTestUtil.sendSingleTapConfirmedEventOnUiThread(mActivity, mMedia3DView, CommonTestUtil.NON_ACTOR_POINT);
        CommonTestUtil.sleep(BAR_DELAY_SLEEP);
        assertEquals(Media3DView.BAR_STATE_ENTERED, mMedia3DView.getBarState());

        // photo -> video
        CommonTestUtil.sendTouchEventsOnUiThread(mActivity, mMedia3DView, CommonTestUtil.VIDEO_ICON_IN_NAVI_BAR);
        CommonTestUtil.sleep(PAGE_SWITCH_DELAY_SLEEP);
        assertTrue(mMedia3DView.getCurrentPage() == mActivity.getVideoPage());

        // test if the bar is out
        CommonTestUtil.sendSingleTapConfirmedEventOnUiThread(mActivity, mMedia3DView, CommonTestUtil.NON_ACTOR_POINT);
        CommonTestUtil.sleep(BAR_DELAY_SLEEP);
        assertEquals(Media3DView.BAR_STATE_ENTERED, mMedia3DView.getBarState());

        // video -> video
        CommonTestUtil.sendTouchEventsOnUiThread(mActivity, mMedia3DView, CommonTestUtil.VIDEO_ICON_IN_NAVI_BAR);
        CommonTestUtil.sleep(PAGE_SWITCH_DELAY_SLEEP);
        assertTrue(mMedia3DView.getCurrentPage() == mActivity.getVideoPage());

        // test if the bar is out
        CommonTestUtil.sendSingleTapConfirmedEventOnUiThread(mActivity, mMedia3DView, CommonTestUtil.NON_ACTOR_POINT);
        CommonTestUtil.sleep(BAR_DELAY_SLEEP);
        assertEquals(Media3DView.BAR_STATE_ENTERED, mMedia3DView.getBarState());

        // video -> weather
        CommonTestUtil.sendTouchEventsOnUiThread(mActivity, mMedia3DView, CommonTestUtil.WEATHER_ICON_IN_NAVI_BAR);
        CommonTestUtil.sleep(PAGE_SWITCH_DELAY_SLEEP);
        assertTrue(mMedia3DView.getCurrentPage() == mActivity.getWeatherPage());

        // test if the bar is out
        CommonTestUtil.sendSingleTapConfirmedEventOnUiThread(mActivity, mMedia3DView, CommonTestUtil.NON_ACTOR_POINT);
        CommonTestUtil.sleep(BAR_DELAY_SLEEP);
        assertEquals(Media3DView.BAR_STATE_ENTERED, mMedia3DView.getBarState());

        // weather -> video
        CommonTestUtil.sendTouchEventsOnUiThread(mActivity, mMedia3DView, CommonTestUtil.VIDEO_ICON_IN_NAVI_BAR);
        CommonTestUtil.sleep(PAGE_SWITCH_DELAY_SLEEP);
        assertTrue(mMedia3DView.getCurrentPage() == mActivity.getVideoPage());

        // test if the bar is out
        CommonTestUtil.sendSingleTapConfirmedEventOnUiThread(mActivity, mMedia3DView, CommonTestUtil.NON_ACTOR_POINT);
        CommonTestUtil.sleep(BAR_DELAY_SLEEP);
        assertEquals(Media3DView.BAR_STATE_ENTERED, mMedia3DView.getBarState());

        // video -> photo
        CommonTestUtil.sendTouchEventsOnUiThread(mActivity, mMedia3DView, CommonTestUtil.PHOTO_ICON_IN_NAVI_BAR);
        CommonTestUtil.sleep(PAGE_SWITCH_DELAY_SLEEP);
        assertTrue(mMedia3DView.getCurrentPage() == mActivity.getPhotoPage());

        // test if the bar is out
        CommonTestUtil.sendSingleTapConfirmedEventOnUiThread(mActivity, mMedia3DView, CommonTestUtil.NON_ACTOR_POINT);
        CommonTestUtil.sleep(BAR_DELAY_SLEEP);
        assertEquals(Media3DView.BAR_STATE_ENTERED, mMedia3DView.getBarState());

        // photo -> weather
        CommonTestUtil.sendTouchEventsOnUiThread(mActivity, mMedia3DView, CommonTestUtil.WEATHER_ICON_IN_NAVI_BAR);
        CommonTestUtil.sleep(PAGE_SWITCH_DELAY_SLEEP);
        assertTrue(mMedia3DView.getCurrentPage() == mActivity.getWeatherPage());
    }
}