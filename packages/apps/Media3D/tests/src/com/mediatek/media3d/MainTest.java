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
import android.graphics.Bitmap;
import android.test.ActivityInstrumentationTestCase2;
import com.mediatek.media3d.portal.PortalPage;

/**
 * This is a simple framework for a test of an Application.  See
 * {@link android.test.ApplicationTestCase ApplicationTestCase} for more information on
 * how to write and extend Application tests.
 * <p/>
 * To run this test, you can type:
 * adb shell am instrument -w \
 * -e class com.mediatek.media3d.MainTest \
 * com.mediatek.media3d.tests/android.test.InstrumentationTestRunner
 */
public class MainTest extends ActivityInstrumentationTestCase2<Main> {
    private Instrumentation mInstrumentation;
    private Main mActivity;
    private Media3DView mMedia3DView;

    public MainTest() {
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

    public void enterPortalPageWaitForIdle() {
        validateNoNullMember();
        CommonTestUtil.waitPageForIdleSync(mInstrumentation, mMedia3DView, mActivity.getPortalPage(), CommonTestUtil.DEFAULT_PAGE_SWITCH_TIMEOUT_IN_MS);

        CommonTestUtil.waitLoadForIdleSync(mInstrumentation, mActivity.getWeatherPage(), CommonTestUtil.DEFAULT_PAGE_SWITCH_TIMEOUT_IN_MS);
        CommonTestUtil.waitLoadForIdleSync(mInstrumentation, mActivity.getPhotoPage(), CommonTestUtil.DEFAULT_PAGE_SWITCH_TIMEOUT_IN_MS);
        CommonTestUtil.waitLoadForIdleSync(mInstrumentation, mActivity.getVideoPage(), CommonTestUtil.DEFAULT_PAGE_SWITCH_TIMEOUT_IN_MS);

        assertTrue(mMedia3DView.getCurrentPage() == mActivity.getPortalPage());
    }

    // Testing Case #1
    public void testPageSwitching() {
        enterPortalPageWaitForIdle();

        // Launch Weather Page
        CommonTestUtil.sendSingleTapConfirmedEventOnUiThread(mActivity, mMedia3DView, CommonTestUtil.WEATHER_ICON_IN_PORTALPAGE);
        CommonTestUtil.waitPageForIdleSync(mInstrumentation, mMedia3DView, mActivity.getWeatherPage(), CommonTestUtil.DEFAULT_PAGE_SWITCH_TIMEOUT_IN_MS);

        CommonTestUtil.sendDragSync(mInstrumentation, mActivity, CommonTestUtil.DragDirection.LEFT);
        CommonTestUtil.waitPageForIdleSync(mInstrumentation, mMedia3DView, mActivity.getPhotoPage(), CommonTestUtil.DEFAULT_PAGE_SWITCH_TIMEOUT_IN_MS);
        assertTrue(mMedia3DView.getCurrentPage() == mActivity.getPhotoPage());

        CommonTestUtil.sendDragSync(mInstrumentation, mActivity, CommonTestUtil.DragDirection.LEFT);
        CommonTestUtil.waitPageForIdleSync(mInstrumentation, mMedia3DView, mActivity.getVideoPage(), CommonTestUtil.DEFAULT_PAGE_SWITCH_TIMEOUT_IN_MS);
        assertTrue(mMedia3DView.getCurrentPage() == mActivity.getVideoPage());

        CommonTestUtil.sendDragSync(mInstrumentation, mActivity, CommonTestUtil.DragDirection.LEFT);
        CommonTestUtil.waitPageForIdleSync(mInstrumentation, mMedia3DView, mActivity.getWeatherPage(), CommonTestUtil.DEFAULT_PAGE_SWITCH_TIMEOUT_IN_MS);
        assertTrue(mMedia3DView.getCurrentPage() == mActivity.getWeatherPage());

        CommonTestUtil.sendDragSync(mInstrumentation, mActivity, CommonTestUtil.DragDirection.RIGHT);
        CommonTestUtil.waitPageForIdleSync(mInstrumentation, mMedia3DView, mActivity.getVideoPage(), CommonTestUtil.DEFAULT_PAGE_SWITCH_TIMEOUT_IN_MS);
        assertTrue(mMedia3DView.getCurrentPage() == mActivity.getVideoPage());

        CommonTestUtil.sendDragSync(mInstrumentation, mActivity, CommonTestUtil.DragDirection.RIGHT);
        CommonTestUtil.waitPageForIdleSync(mInstrumentation, mMedia3DView, mActivity.getPhotoPage(), CommonTestUtil.DEFAULT_PAGE_SWITCH_TIMEOUT_IN_MS);
        assertTrue(mMedia3DView.getCurrentPage() == mActivity.getPhotoPage());

        CommonTestUtil.sendDragSync(mInstrumentation, mActivity, CommonTestUtil.DragDirection.RIGHT);
        CommonTestUtil.waitPageForIdleSync(mInstrumentation, mMedia3DView, mActivity.getWeatherPage(), CommonTestUtil.DEFAULT_PAGE_SWITCH_TIMEOUT_IN_MS);
        assertTrue(mMedia3DView.getCurrentPage() == mActivity.getWeatherPage());
    }

    // Testing Case #2
    public void testPortalPage() {
        enterPortalPageWaitForIdle();

        CommonTestUtil.sendDragSync(mInstrumentation, mActivity, CommonTestUtil.DragDirection.UP);
        CommonTestUtil.sendDragSync(mInstrumentation, mActivity, CommonTestUtil.DragDirection.UP);
        CommonTestUtil.sendDragSync(mInstrumentation, mActivity, CommonTestUtil.DragDirection.DOWN);
        CommonTestUtil.sendDragSync(mInstrumentation, mActivity, CommonTestUtil.DragDirection.DOWN);

        CommonTestUtil.waitPageForIdleSync(mInstrumentation, mMedia3DView, mActivity.getPortalPage(), CommonTestUtil.DEFAULT_PAGE_SWITCH_TIMEOUT_IN_MS);
        assertTrue(mMedia3DView.getCurrentPage() == mActivity.getPortalPage());
    }

    // Testing Case #3
    public void testNavigationBar() {
        enterPortalPageWaitForIdle();

        // Launch Weather Page
        CommonTestUtil.sendSingleTapConfirmedEventOnUiThread(mActivity, mMedia3DView, CommonTestUtil.WEATHER_ICON_IN_PORTALPAGE);
        CommonTestUtil.waitPageForIdleSync(mInstrumentation, mMedia3DView, mActivity.getWeatherPage(), CommonTestUtil.DEFAULT_PAGE_SWITCH_TIMEOUT_IN_MS);
        assertTrue(mMedia3DView.getCurrentPage() == mActivity.getWeatherPage());

        // Trigger menu bar
        CommonTestUtil.sendSingleTapConfirmedEventOnUiThread(mActivity, mMedia3DView, CommonTestUtil.NON_ACTOR_POINT);
        CommonTestUtil.waitMenuBarForActionSync(mInstrumentation, mMedia3DView, CommonTestUtil.DEFAULT_MENU_BAR_TIMEOUT_IN_MS);

        CommonTestUtil.sendTouchEventsOnUiThread(mActivity, mMedia3DView, CommonTestUtil.WEATHER_ICON_IN_NAVI_BAR);
        CommonTestUtil.waitPageForIdleSync(mInstrumentation, mMedia3DView, mActivity.getWeatherPage(), CommonTestUtil.DEFAULT_PAGE_SWITCH_TIMEOUT_IN_MS);
        assertTrue(mMedia3DView.getCurrentPage() == mActivity.getWeatherPage());

        // Trigger menu bar
        CommonTestUtil.sendSingleTapConfirmedEventOnUiThread(mActivity, mMedia3DView, CommonTestUtil.NON_ACTOR_POINT);
        CommonTestUtil.waitMenuBarForActionSync(mInstrumentation, mMedia3DView, CommonTestUtil.DEFAULT_MENU_BAR_TIMEOUT_IN_MS);

        CommonTestUtil.sendTouchEventsOnUiThread(mActivity, mMedia3DView, CommonTestUtil.PHOTO_ICON_IN_NAVI_BAR);
        CommonTestUtil.waitPageForIdleSync(mInstrumentation, mMedia3DView, mActivity.getPhotoPage(), CommonTestUtil.DEFAULT_PAGE_SWITCH_TIMEOUT_IN_MS);
        assertTrue(mMedia3DView.getCurrentPage() == mActivity.getPhotoPage());

        // Trigger menu bar
        CommonTestUtil.sendSingleTapConfirmedEventOnUiThread(mActivity, mMedia3DView, CommonTestUtil.NON_ACTOR_POINT);
        CommonTestUtil.waitMenuBarForActionSync(mInstrumentation, mMedia3DView, CommonTestUtil.DEFAULT_MENU_BAR_TIMEOUT_IN_MS);

        CommonTestUtil.sendTouchEventsOnUiThread(mActivity, mMedia3DView, CommonTestUtil.PHOTO_ICON_IN_NAVI_BAR);
        CommonTestUtil.waitPageForIdleSync(mInstrumentation, mMedia3DView, mActivity.getPhotoPage(), CommonTestUtil.DEFAULT_PAGE_SWITCH_TIMEOUT_IN_MS);
        assertTrue(mMedia3DView.getCurrentPage() == mActivity.getPhotoPage());

        // Trigger menu bar
        CommonTestUtil.sendSingleTapConfirmedEventOnUiThread(mActivity, mMedia3DView, CommonTestUtil.NON_ACTOR_POINT);
        CommonTestUtil.waitMenuBarForActionSync(mInstrumentation, mMedia3DView, CommonTestUtil.DEFAULT_MENU_BAR_TIMEOUT_IN_MS);

        CommonTestUtil.sendTouchEventsOnUiThread(mActivity, mMedia3DView, CommonTestUtil.VIDEO_ICON_IN_NAVI_BAR);
        CommonTestUtil.waitPageForIdleSync(mInstrumentation, mMedia3DView, mActivity.getVideoPage(), CommonTestUtil.DEFAULT_PAGE_SWITCH_TIMEOUT_IN_MS);
        assertTrue(mMedia3DView.getCurrentPage() == mActivity.getVideoPage());

        // Trigger menu bar
        CommonTestUtil.sendSingleTapConfirmedEventOnUiThread(mActivity, mMedia3DView, CommonTestUtil.NON_ACTOR_POINT);
        CommonTestUtil.waitMenuBarForActionSync(mInstrumentation, mMedia3DView, CommonTestUtil.DEFAULT_MENU_BAR_TIMEOUT_IN_MS);

        CommonTestUtil.sendTouchEventsOnUiThread(mActivity, mMedia3DView, CommonTestUtil.VIDEO_ICON_IN_NAVI_BAR);
        CommonTestUtil.waitPageForIdleSync(mInstrumentation, mMedia3DView, mActivity.getVideoPage(), CommonTestUtil.DEFAULT_PAGE_SWITCH_TIMEOUT_IN_MS);
        assertTrue(mMedia3DView.getCurrentPage() == mActivity.getVideoPage());

        // Trigger menu bar
        CommonTestUtil.sendSingleTapConfirmedEventOnUiThread(mActivity, mMedia3DView, CommonTestUtil.NON_ACTOR_POINT);
        CommonTestUtil.waitMenuBarForActionSync(mInstrumentation, mMedia3DView, CommonTestUtil.DEFAULT_MENU_BAR_TIMEOUT_IN_MS);

        CommonTestUtil.sendTouchEventsOnUiThread(mActivity, mMedia3DView, CommonTestUtil.WEATHER_ICON_IN_NAVI_BAR);
        CommonTestUtil.waitPageForIdleSync(mInstrumentation, mMedia3DView, mActivity.getWeatherPage(), CommonTestUtil.DEFAULT_PAGE_SWITCH_TIMEOUT_IN_MS);
        assertTrue(mMedia3DView.getCurrentPage() == mActivity.getWeatherPage());
    }

    // Testing Case #4
    public void testBitmapUtil() {
        Bitmap bmp = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        assertNotNull(bmp);

        Bitmap newCroppedBmp = BitmapUtil.autoCropBitmapBySize(bmp, 50, 50);
        assertNotNull(newCroppedBmp);

        Bitmap newCroppedBlurBmp = BitmapUtil.autoCropBitmapBySizeBlur(bmp, 50, 50);
        assertNotNull(newCroppedBlurBmp);
    }

    // Testing Case #5
    public void testDemoApp() {
        Demo demo = new Demo();
        assertNotNull(demo);
        demo.finish();
    }

    // Testing Case #6
    public void testVideoPlayer() {
        VideoPlayer vp = new VideoPlayer();
        assertNotNull(vp);
        vp = null;
    }

    // testing case #7
    public void testResourceItemSet() {
        assertNotNull(mActivity);
        ResourceItemSet set = new ResourceItemSet(
                mActivity.getResources(),
                new int[] {
                    R.drawable.gg_hyoyeon,
                    R.drawable.gg_jessica,
                    R.drawable.gg_seohyun,
                    R.drawable.gg_sunny,
                    R.drawable.gg_taeyeon,
                    R.drawable.gg_tiffany,
                    R.drawable.gg_yoona,
                    R.drawable.gg_yuri,
                },
                new int[] {
                    R.drawable.gg_hyoyeon,
                    R.drawable.gg_jessica,
                    R.drawable.gg_seohyun,
                    R.drawable.gg_sunny,
                    R.drawable.gg_taeyeon,
                    R.drawable.gg_tiffany,
                    R.drawable.gg_yoona,
                    R.drawable.gg_yuri,
                });
        assertNotNull(set);
        assertTrue(set.getItemCount() == 8);
        MediaItem item = set.getItem(0);
        assertNotNull(item);
        assertTrue(item.getDuration() == 1000);    // refer to the ResourceItemSet.
        assertTrue(item.getFilePath().equals("")); // refer to the ResourceItemSet.
        set.close();

        // test second constructor
        set = new ResourceItemSet(mActivity.getAssets(), "");
        set.close();
    }

    // Testing Case #8
    public void testPauseResume() {
        enterPortalPageWaitForIdle();

        mInstrumentation.callActivityOnPause(mActivity);
        CommonTestUtil.wait(mInstrumentation, CommonTestUtil.WAIT_FOR_PAGE_SWITCH_TIME_IN_SECS);

        mInstrumentation.callActivityOnResume(mActivity);
        CommonTestUtil.waitPageForIdleSync(mInstrumentation, mMedia3DView, mActivity.getPortalPage(), CommonTestUtil.DEFAULT_PAGE_SWITCH_TIMEOUT_IN_MS);
        assertTrue(mMedia3DView.getCurrentPage() == mActivity.getPortalPage());
    }

    // Testing Case #9
    public void testSingleTap() {
        enterPortalPageWaitForIdle();

        CommonTestUtil.sendSingleTapConfirmedEventOnUiThread(mActivity, mMedia3DView, CommonTestUtil.NON_ACTOR_POINT);
        CommonTestUtil.waitPageForIdleSync(mInstrumentation, mMedia3DView, mActivity.getPortalPage(), CommonTestUtil.DEFAULT_PAGE_SWITCH_TIMEOUT_IN_MS);
        assertTrue(mMedia3DView.getCurrentPage() == mActivity.getPortalPage());
    }

    // Testing Case #10
    public void testOrphanFunctions() {
        PortalPage p = mActivity.getPortalPage();
        assertTrue(p.getPageType() == Page.PageType.PORTAL);

        Page.PageType pageType = new Page.PageType();
        assertNotNull(pageType);

        Page.TransitionType transitionType = new Page.TransitionType();
        assertNotNull(transitionType);

        FlingEvent flingEvent = new FlingEvent();
        assertNotNull(flingEvent);
    }
}