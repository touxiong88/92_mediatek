package com.mediatek.media3d;

import android.app.Instrumentation;
import android.test.ActivityInstrumentationTestCase2;

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
public class DemoTest extends ActivityInstrumentationTestCase2<Demo> {

    private Instrumentation mInstrumentation;
    private Main mActivity;
    private Media3DView mMedia3DView;

    public DemoTest() {
        super("com.mediatek.media3d", Demo.class);
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
        CommonTestUtil.waitPageForIdleSync(mInstrumentation, mMedia3DView,
                mActivity.getPortalPage(), CommonTestUtil.DEFAULT_PAGE_SWITCH_TIMEOUT_IN_MS);

        CommonTestUtil.waitLoadForIdleSync(mInstrumentation,
                mActivity.getWeatherPage(), CommonTestUtil.DEFAULT_PAGE_SWITCH_TIMEOUT_IN_MS);
        CommonTestUtil.waitLoadForIdleSync(mInstrumentation,
                mActivity.getPhotoPage(), CommonTestUtil.DEFAULT_PAGE_SWITCH_TIMEOUT_IN_MS);
        CommonTestUtil.waitLoadForIdleSync(mInstrumentation,
                mActivity.getVideoPage(), CommonTestUtil.DEFAULT_PAGE_SWITCH_TIMEOUT_IN_MS);

        assertTrue(mMedia3DView.getCurrentPage() == mActivity.getPortalPage());
    }

    public void enterWeatherPageWaitForIdle() {
        validateNoNullMember();
        CommonTestUtil.waitPageForIdleSync(mInstrumentation, mMedia3DView,
                mActivity.getPortalPage(), CommonTestUtil.DEFAULT_PAGE_SWITCH_TIMEOUT_IN_MS);
        CommonTestUtil.waitLoadForIdleSync(mInstrumentation,
                mActivity.getWeatherPage(), CommonTestUtil.DEFAULT_PAGE_SWITCH_TIMEOUT_IN_MS);

        CommonTestUtil.sendSingleTapConfirmedEventOnUiThread(mActivity,
                mMedia3DView, CommonTestUtil.WEATHER_ICON_IN_PORTALPAGE);
        CommonTestUtil.waitPageForIdleSync(mInstrumentation, mMedia3DView,
                mActivity.getWeatherPage(), CommonTestUtil.DEFAULT_PAGE_SWITCH_TIMEOUT_IN_MS);

        assertTrue(mMedia3DView.getCurrentPage() == mActivity.getWeatherPage());
    }

    public void enterPhotoPageWaitForIdle() {
        validateNoNullMember();
        CommonTestUtil.waitPageForIdleSync(mInstrumentation, mMedia3DView,
                mActivity.getPortalPage(), CommonTestUtil.DEFAULT_PAGE_SWITCH_TIMEOUT_IN_MS);
        CommonTestUtil.waitLoadForIdleSync(mInstrumentation,
                mActivity.getPhotoPage(), CommonTestUtil.DEFAULT_PAGE_SWITCH_TIMEOUT_IN_MS);

        CommonTestUtil.sendSingleTapConfirmedEventOnUiThread(mActivity,
                mMedia3DView, CommonTestUtil.PHOTO_ICON_IN_PORTALPAGE);
        CommonTestUtil.waitPageForIdleSync(mInstrumentation, mMedia3DView,
                mActivity.getPhotoPage(), CommonTestUtil.DEFAULT_PAGE_SWITCH_TIMEOUT_IN_MS);

        assertTrue(mMedia3DView.getCurrentPage() == mActivity.getPhotoPage());
    }

    public void enterVideoPageWaitForIdle() {
        validateNoNullMember();
        CommonTestUtil.waitPageForIdleSync(mInstrumentation, mMedia3DView,
                mActivity.getPortalPage(), CommonTestUtil.DEFAULT_PAGE_SWITCH_TIMEOUT_IN_MS);
        CommonTestUtil.waitLoadForIdleSync(mInstrumentation,
                mActivity.getVideoPage(), CommonTestUtil.DEFAULT_PAGE_SWITCH_TIMEOUT_IN_MS);

        CommonTestUtil.sendSingleTapConfirmedEventOnUiThread(mActivity,
                mMedia3DView, CommonTestUtil.VIDEO_ICON_IN_PORTALPAGE);
        CommonTestUtil.waitPageForIdleSync(mInstrumentation, mMedia3DView,
                mActivity.getVideoPage(), CommonTestUtil.DEFAULT_PAGE_SWITCH_TIMEOUT_IN_MS);

        assertTrue(mMedia3DView.getCurrentPage() == mActivity.getVideoPage());
    }

    public void fastFlingUp() {
        final int FLING_COUNT =10;
        for (int i = 0; i < FLING_COUNT; ++i) {
            CommonTestUtil.sendDragSync(mInstrumentation,
                    mActivity, CommonTestUtil.DragDirection.DOWN);
        }
        CommonTestUtil.wait(mInstrumentation,
                CommonTestUtil.WAIT_FOR_INNER_PAGE_SWITCH_TIME_IN_SECS);

        for (int i = 0; i < FLING_COUNT; ++i) {
            CommonTestUtil.sendDragSync(mInstrumentation,
                    mActivity, CommonTestUtil.DragDirection.UP);
        }
        CommonTestUtil.wait(mInstrumentation,
                CommonTestUtil.WAIT_FOR_INNER_PAGE_SWITCH_TIME_IN_SECS);
    }

    public void testEnableStereo3D() {
        enterPortalPageWaitForIdle();

        if (mMedia3DView != null) {
            mMedia3DView.enableStereo3DMode(true);
            mMedia3DView.enableStereo3DMode(false);
        }
    }

    public void testEnterDemoWeather() {
        enterWeatherPageWaitForIdle();
        fastFlingUp();
        assertTrue(mMedia3DView.getCurrentPage() == mActivity.getWeatherPage());
    }

    public void testEnterDemoPhoto() {
        enterPhotoPageWaitForIdle();
        fastFlingUp();
        assertTrue(mMedia3DView.getCurrentPage() == mActivity.getPhotoPage());
    }

    public void testEnterDemoVideo() {
        enterVideoPageWaitForIdle();
        fastFlingUp();
        assertTrue(mMedia3DView.getCurrentPage() == mActivity.getVideoPage());
    }
}
