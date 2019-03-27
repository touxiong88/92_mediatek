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
import android.os.SystemClock;
import android.test.ActivityInstrumentationTestCase2;
import android.view.KeyEvent;
import com.mediatek.media3d.video.Util;
import com.mediatek.media3d.video.VideoPage;
import com.mediatek.media3d.video.VideoThumbnailActor;
import java.io.File;

/**
 * This is a simple framework for a test of an Application.  See
 * {@link android.test.ApplicationTestCase ApplicationTestCase} for more information on
 * how to write and extend Application tests.
 * <p/>
 * To run this test, you can type:
 * adb shell am instrument -w -e class com.mediatek.media3d.VideoPageTest com.mediatek.media3d.tests/android.test.InstrumentationTestRunner
 */
public class VideoPageTest extends ActivityInstrumentationTestCase2<Main> {

    private Instrumentation mInstrumentation;
    private Main mActivity;
    private Media3DView mMedia3DView;

    public VideoPageTest() {
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

    public void enterVideoPageWaitForIdle() {
        validateNoNullMember();
        CommonTestUtil.waitPageForIdleSync(mInstrumentation, mMedia3DView, mActivity.getPortalPage(), CommonTestUtil.DEFAULT_PAGE_SWITCH_TIMEOUT_IN_MS);
        CommonTestUtil.waitLoadForIdleSync(mInstrumentation, mActivity.getVideoPage(), CommonTestUtil.DEFAULT_PAGE_SWITCH_TIMEOUT_IN_MS);

        CommonTestUtil.sendSingleTapConfirmedEventOnUiThread(mActivity, mMedia3DView, CommonTestUtil.VIDEO_ICON_IN_PORTALPAGE);
        CommonTestUtil.waitPageForIdleSync(mInstrumentation, mMedia3DView, mActivity.getVideoPage(), CommonTestUtil.DEFAULT_PAGE_SWITCH_TIMEOUT_IN_MS);

        assertTrue(mMedia3DView.getCurrentPage() == mActivity.getVideoPage());
    }

    // Testing Case #1
    public void testLeaveVideoPageByBack() {
        enterVideoPageWaitForIdle();

        sendKeys(KeyEvent.KEYCODE_BACK);
        CommonTestUtil.waitPageForIdleSync(mInstrumentation, mMedia3DView, mActivity.getPortalPage(), CommonTestUtil.DEFAULT_PAGE_SWITCH_TIMEOUT_IN_MS);
        assertTrue(mMedia3DView.getCurrentPage() == mActivity.getPortalPage());
    }

    // Testing Case #2
    public void testLeaveVideoPageBySlide() {
        enterVideoPageWaitForIdle();

        CommonTestUtil.sendDragSync(mInstrumentation, mActivity, CommonTestUtil.DragDirection.RIGHT);
        assertTrue(mMedia3DView.getCurrentPage() != mActivity.getVideoPage());

        CommonTestUtil.sendDragSync(mInstrumentation, mActivity, CommonTestUtil.DragDirection.LEFT);
        assertTrue(mMedia3DView.getCurrentPage() == mActivity.getVideoPage());
    }

    // Testing Case #3
    public void testBasicDrag() {
        enterVideoPageWaitForIdle();

        VideoPage v = mActivity.getVideoPage();
        if (0 == v.getTotalVideoPage()) {
            return; // no video, skip this test
        }

        // sub-case #1
        int currentPageIndex = v.getVideoPage();
        CommonTestUtil.sendDragSync(mInstrumentation, mActivity, CommonTestUtil.DragDirection.UP);
        CommonTestUtil.wait(mInstrumentation, CommonTestUtil.WAIT_FOR_INNER_PAGE_SWITCH_TIME_IN_SECS);

        int pageAfterFlingUpIndex = v.getVideoPage();
        if (v.getTotalVideoPage() > 1) {
            assertTrue(currentPageIndex != pageAfterFlingUpIndex);
        } else {
            assertTrue(currentPageIndex == pageAfterFlingUpIndex); // at the last page
        }

        // sub-case #2
        currentPageIndex = v.getVideoPage();
        CommonTestUtil.sendDragSync(mInstrumentation, mActivity, CommonTestUtil.DragDirection.DOWN);
        CommonTestUtil.wait(mInstrumentation, CommonTestUtil.WAIT_FOR_INNER_PAGE_SWITCH_TIME_IN_SECS);
        int pageAfterFlingDownIndex = v.getVideoPage();
        if (currentPageIndex == 0) {
            assertTrue(currentPageIndex == pageAfterFlingDownIndex);
        } else {
            assertTrue(currentPageIndex != pageAfterFlingDownIndex);
        }
    }

    // Testing Case #4
    public void testPauseResume() {
        enterVideoPageWaitForIdle();

        mInstrumentation.callActivityOnPause(mActivity);
        CommonTestUtil.wait(mInstrumentation, CommonTestUtil.WAIT_FOR_PAGE_SWITCH_TIME_IN_SECS);

        mInstrumentation.callActivityOnResume(mActivity);
        CommonTestUtil.waitPageForIdleSync(mInstrumentation, mMedia3DView, mActivity.getVideoPage(), CommonTestUtil.DEFAULT_PAGE_SWITCH_TIMEOUT_IN_MS);
        assertTrue(mMedia3DView.getCurrentPage() == mActivity.getVideoPage());
    }

    // Testing Case #5
    public void testVideoPageCyclicScrolling() {
        enterVideoPageWaitForIdle();

        long startTime = SystemClock.elapsedRealtime();
        VideoPage p = mActivity.getVideoPage();
        final int pageCount = p.getTotalVideoPage();
        final int beginIndex = p.getIndex();

        for (int i = 0; i < pageCount; ++i) {
            CommonTestUtil.sendDragSync(mInstrumentation, mActivity, CommonTestUtil.DragDirection.UP);
        }
        CommonTestUtil.wait(mInstrumentation, CommonTestUtil.WAIT_FOR_INNER_PAGE_SWITCH_TIME_IN_SECS);

        assertTrue(mMedia3DView.getCurrentPage() == p);
        final int afterUpScrollingIndex = p.getIndex();
        assertEquals(beginIndex, afterUpScrollingIndex);

        for (int i = 0; i < pageCount; ++i) {
            CommonTestUtil.sendDragSync(mInstrumentation, mActivity, CommonTestUtil.DragDirection.DOWN);
        }
        CommonTestUtil.wait(mInstrumentation, CommonTestUtil.WAIT_FOR_INNER_PAGE_SWITCH_TIME_IN_SECS);

        assertTrue(mMedia3DView.getCurrentPage() == p);
        final int afterDownScrollingIndex = p.getIndex();
        assertEquals(beginIndex, afterDownScrollingIndex);
    }

    // Testing Case #6
    public void testFolderOnMenuBar() {
        enterVideoPageWaitForIdle();

        // trigger menu bar
        CommonTestUtil.sendSingleTapConfirmedEventOnUiThread(mActivity, mMedia3DView, CommonTestUtil.NON_ACTOR_POINT);
        CommonTestUtil.waitMenuBarForActionSync(mInstrumentation, mMedia3DView, CommonTestUtil.DEFAULT_MENU_BAR_TIMEOUT_IN_MS);

        // click folder icon on menu bar
        CommonTestUtil.sendTouchEventsOnUiThread(mActivity, mMedia3DView, CommonTestUtil.FOLDER_ICON_IN_TOP_MENU);
        CommonTestUtil.waitPageForIdleSync(mInstrumentation, mMedia3DView, mActivity.getVideoPage(), CommonTestUtil.DEFAULT_PAGE_SWITCH_TIMEOUT_IN_MS);

        final int REQUEST_PICK_FOLDER = 0; // refer to VideoPage
        int requestCode = mActivity.getVideoPage().hashCode() + REQUEST_PICK_FOLDER; // refer to VideoPage
        CommonTestUtil.finishLaunchedActivity(mActivity, requestCode);
        CommonTestUtil.wait(mInstrumentation, CommonTestUtil.WAIT_FOR_PAGE_SWITCH_TIME_IN_SECS);

        assertTrue(mMedia3DView.getCurrentPage() == mActivity.getVideoPage());
    }

    // Testing Case #7
    public void testBackOnMenuBar() {
        enterVideoPageWaitForIdle();
        // trigger menu bar
        CommonTestUtil.sendSingleTapConfirmedEventOnUiThread(mActivity, mMedia3DView, CommonTestUtil.NON_ACTOR_POINT);
        CommonTestUtil.waitMenuBarForActionSync(mInstrumentation, mMedia3DView, CommonTestUtil.DEFAULT_MENU_BAR_TIMEOUT_IN_MS);

        // click back icon on menu bar
        CommonTestUtil.sendTouchEventsOnUiThread(mActivity, mMedia3DView, CommonTestUtil.BACK_ICON_IN_TOP_MENU);
        CommonTestUtil.waitPageForIdleSync(mInstrumentation, mMedia3DView, mActivity.getPortalPage(), CommonTestUtil.DEFAULT_PAGE_SWITCH_TIMEOUT_IN_MS);
        assertTrue(mMedia3DView.getCurrentPage() == mActivity.getPortalPage());
    }

    // Testing Case #8
    public void testOrphanFunctions() {
        final Bitmap bmp = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        assertNotNull(bmp);

        final VideoThumbnailActor vActor = new VideoThumbnailActor(mActivity, 100);
        assertNotNull(vActor);

        final File externalFilesDir = mActivity.getExternalFilesDir(null);
        if (externalFilesDir != null) {
            final String bmpSavedName = externalFilesDir.toString() + "videoOrphanFunctionTest.bmp";
            final String filename = Util.dumpBitmap(bmp, bmpSavedName, null);
            assertTrue(filename.equals(bmpSavedName));
        }
    }
}
