/* Build
 * ./mk mt6589_phone_qhdv2 mm ./mediatek/frameworks/base/tests/JavaLibraryJPETest/
 * Execute
 * adb shell am instrument -w -e class com.mediatek.javalibrary.jpe.test.JPETest com.mediatek.javalibrary.jpe.test/android.test.InstrumentationTestRunner
 */

package com.mediatek.javalibrary.jpe.test;

import android.app.Instrumentation;
import android.test.AndroidTestCase;
import android.util.Log;
import com.mediatek.common.jpe.a;
import java.lang.reflect.Method;
import junit.framework.Assert;

//Target Package PluginManager
import com.mediatek.pluginmanager.PluginManager;
import android.content.pm.Signature;

public class JPETest extends AndroidTestCase {
    private static final String TAG = "JavaLibraryJPETest";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        com.mediatek.common.jpe.a.b = false;
        Log.i(TAG, " Setup JPE Test Case ");
        Log.i(TAG, " Setup NativeCheck = " + com.mediatek.common.jpe.a.b);

    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        com.mediatek.common.jpe.a.b = false;
        Log.i(TAG, " TearDown JPE Test Case ");
        Log.i(TAG, " TearDown NativeCheck = " + com.mediatek.common.jpe.a.b);
    }

    public void testPluginManagerJPE() throws Throwable {

        Log.i(TAG, " testPluginManagerJPE ");

        Signature [] mSignature = null;

        PluginManager<String> checkClass = PluginManager.create(getContext(),"testPluginManagerJPE", mSignature);

        Log.i(TAG, "PluginManager NativeCheck = " + com.mediatek.common.jpe.a.b);
        assertTrue("PluginManager is not JPE checked",(com.mediatek.common.jpe.a.b));

    }

}