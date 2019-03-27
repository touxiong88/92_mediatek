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

import android.app.Instrumentation;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import com.mediatek.media3d.weather.TimeZoneTransition;

/**
 * This is a simple framework for a test of an Application.  See
 * {@link android.test.ApplicationTestCase ApplicationTestCase} for more information on
 * how to write and extend Application tests.
 * <p/>
 * To run this test, you can type:
 * adb shell am instrument -w -e class com.mediatek.media3d.WeatherPageTest com.mediatek.media3d.tests/android.test.InstrumentationTestRunner
 */
public class WeatherPageTest extends ActivityInstrumentationTestCase2<Main> {

    private Instrumentation mInstrumentation;
    private Main mActivity;
    private Media3DView mMedia3DView;

    public WeatherPageTest() {
        super("com.mediatek.media3d", Main.class);
    }

    public void validateNoNullMember() {
        if (null == mActivity || null == mMedia3DView || null == mInstrumentation) {
            throw new NullPointerException(
                    "There is at least one null-pointer data member.");
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mInstrumentation = getInstrumentation();
        mActivity = getActivity();
        if (null != mActivity) {
            mMedia3DView = mActivity.getMedia3DView();
        }
    }

    @Override
    protected void tearDown() throws Exception {
        mInstrumentation = null;
        mMedia3DView = null;
        mActivity = null;
        super.tearDown();
    }

    public void enterWeatherPageWaitForIdle() {
        validateNoNullMember();
        CommonTestUtil.waitPageForIdleSync(mInstrumentation, mMedia3DView, mActivity.getPortalPage(), CommonTestUtil.DEFAULT_PAGE_SWITCH_TIMEOUT_IN_MS);
        CommonTestUtil.waitLoadForIdleSync(mInstrumentation, mActivity.getWeatherPage(), CommonTestUtil.DEFAULT_PAGE_SWITCH_TIMEOUT_IN_MS);

        CommonTestUtil.sendSingleTapConfirmedEventOnUiThread(mActivity, mMedia3DView, CommonTestUtil.WEATHER_ICON_IN_PORTALPAGE);
        CommonTestUtil.waitPageForIdleSync(mInstrumentation, mMedia3DView, mActivity.getWeatherPage(), CommonTestUtil.DEFAULT_PAGE_SWITCH_TIMEOUT_IN_MS);

        assertTrue(mMedia3DView.getCurrentPage() == mActivity.getWeatherPage());
    }

    // Testing Case #1
    public void testFastFlingUp() {
        enterWeatherPageWaitForIdle();

        final int FLING_COUNT =10;
        for (int i = 0; i < FLING_COUNT; ++i) {
            CommonTestUtil.sendDragSync(mInstrumentation, mActivity, CommonTestUtil.DragDirection.DOWN);
        }
        CommonTestUtil.wait(mInstrumentation, CommonTestUtil.WAIT_FOR_INNER_PAGE_SWITCH_TIME_IN_SECS);

        for (int i = 0; i < FLING_COUNT; ++i) {
            CommonTestUtil.sendDragSync(mInstrumentation, mActivity, CommonTestUtil.DragDirection.UP);
        }
        CommonTestUtil.wait(mInstrumentation, CommonTestUtil.WAIT_FOR_INNER_PAGE_SWITCH_TIME_IN_SECS);
        assertTrue(mMedia3DView.getCurrentPage() == mActivity.getWeatherPage());
    }

    // Testing Case #2
    public void testSettingOnMenuBar() {
        enterWeatherPageWaitForIdle();

        CommonTestUtil.sendSingleTapConfirmedEventOnUiThread(mActivity, mMedia3DView, CommonTestUtil.NON_ACTOR_POINT);
        CommonTestUtil.waitMenuBarForActionSync(mInstrumentation, mMedia3DView, CommonTestUtil.DEFAULT_MENU_BAR_TIMEOUT_IN_MS);

        // click setting button
        CommonTestUtil.sendTouchEventsOnUiThread(mActivity, mMedia3DView, CommonTestUtil.SETTING_ICON_IN_TOP_MENU);
        CommonTestUtil.wait(mInstrumentation, CommonTestUtil.WAIT_FOR_PAGE_SWITCH_TIME_IN_SECS);

        final int CMD_SETTING = 2; // refer to WeatherPage
        CommonTestUtil.finishLaunchedActivity(mActivity, CMD_SETTING);
        CommonTestUtil.waitPageForIdleSync(mInstrumentation, mMedia3DView, mActivity.getWeatherPage(), CommonTestUtil.DEFAULT_PAGE_SWITCH_TIMEOUT_IN_MS);
        assertTrue(mMedia3DView.getCurrentPage() == mActivity.getWeatherPage());
    }

    // Testing Case #3
    public void testBackOnMenuBar() {
        enterWeatherPageWaitForIdle();

        CommonTestUtil.sendSingleTapConfirmedEventOnUiThread(mActivity, mMedia3DView, CommonTestUtil.NON_ACTOR_POINT);
        CommonTestUtil.waitMenuBarForActionSync(mInstrumentation, mMedia3DView, CommonTestUtil.DEFAULT_MENU_BAR_TIMEOUT_IN_MS);

        // click back button
        CommonTestUtil.sendTouchEventsOnUiThread(mActivity, mMedia3DView, CommonTestUtil.BACK_ICON_IN_TOP_MENU);
        CommonTestUtil.waitPageForIdleSync(mInstrumentation, mMedia3DView, mActivity.getPortalPage(), CommonTestUtil.DEFAULT_PAGE_SWITCH_TIMEOUT_IN_MS);
        assertTrue(mMedia3DView.getCurrentPage() == mActivity.getPortalPage());
    }

    // Testing Case #4
    public void testPauseResume() {
        enterWeatherPageWaitForIdle();

        CommonTestUtil.callOnPauseOnUiThread(mActivity, mInstrumentation);
        CommonTestUtil.wait(mInstrumentation, CommonTestUtil.WAIT_FOR_PAGE_SWITCH_TIME_IN_SECS);

        CommonTestUtil.callOnResumeOnUiThread(mActivity, mInstrumentation);
        CommonTestUtil.waitPageForIdleSync(mInstrumentation, mMedia3DView, mActivity.getWeatherPage(), CommonTestUtil.DEFAULT_PAGE_SWITCH_TIMEOUT_IN_MS);
        assertTrue(mMedia3DView.getCurrentPage() == mActivity.getWeatherPage());
    }

    // Testing Case #5
    public void testOrphanFunctions() {
        String GmtTz = TimeZoneTransition.getGmtTz("IDLW"); // Transition
        assertNotNull(GmtTz);
        assertTrue(GmtTz.equals("GMT-12:00"));
        assertTrue(TimeZoneTransition.TzTransition.valueOf("IDLW") == TimeZoneTransition.TzTransition.IDLW);
    }
}