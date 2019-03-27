package com.mediatek.media3d;

import android.app.Activity;
import android.content.Context;
import android.os.SystemClock;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

/**
 * This is a simple framework for a test of an Application.  See
 * {@link android.test.ApplicationTestCase ApplicationTestCase} for more information on
 * how to write and extend Application tests.
 * <p/>
 * To run this test, you can type:
 * adb shell am instrument -w -e class com.mediatek.media3d.ActPerformanceTest com.mediatek.media3d.tests/android.test.InstrumentationTestRunner
 */
public class ActPerformanceTest extends ActivityInstrumentationTestCase2<Main> {
    private static final String TAG = "M3dMark";

    public ActPerformanceTest() {
        super("com.mediatek.media3d", Main.class);
    }

    private static final long APP_LAUNCH_TIME_WO_CACHE_CRITERIA = 3000;

    public void test01_AppLaunchTimeWOCache() {
        long t1 = SystemClock.uptimeMillis();
        Log.v(TAG, "starting activity without shader cache");
        final Main activity = getActivity();
        assertNotNull(activity);
        getInstrumentation().waitForIdleSync();
        long t2 = SystemClock.uptimeMillis() - t1;
        Log.v(TAG, "getActivity costs: " + t2);

        writePerformanceData(activity, "app.launch-time-wo-cache.txt", t2);
        assertThat(t2, is(lessThanOrEqualTo(APP_LAUNCH_TIME_WO_CACHE_CRITERIA)));
    }

    private void writePerformanceData(Activity activity, String name, Object data) {
        File dataFile = new File(activity.getDir("perf", Context.MODE_PRIVATE), name);
        dataFile.delete();
        try {
            FileWriter writer = new FileWriter(dataFile);
            writer.write("YVALUE=" + data);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}