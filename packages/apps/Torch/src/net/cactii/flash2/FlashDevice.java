
package net.cactii.flash2;

import android.os.Build;

import java.io.FileWriter;
import java.io.IOException;
import android.hardware.Camera;

public class FlashDevice {

/* Vanzo:huangchaojun on: Mon, 27 Jun 2011 10:19:44 +0800
 *
    private static final String DEVICE = "/sys/class/leds/flashlight/brightness";
    private static final String DEVICE_SHOLES = "/sys/class/leds/spotlight/brightness";
 */
    private static final String DEVICE = "/sys/bus/platform/drivers/kd_camera_flashlight/lighton";
    private static final String DEVICE_SHOLES = "/sys/bus/platform/drivers/kd_camera_flashlight/lighton";
// End of Vanzo:huangchaojun

    public static final int STROBE = -1;
    public static final int OFF = 0;
    public static final int ON = 1;
    public static final int DEATH_RAY = 3;
    public static final int HIGH = 128;
    public static final int ZEPP_ON = 100;
    public static final int ZEPP_DEATH_RAY = 255;

    private static FlashDevice instance;

    private static boolean useDeathRay = !Build.DEVICE.equals("supersonic")
            && !Build.DEVICE.equals("glacier");
    private static boolean useZeppDeathRay = Build.DEVICE.contains("zepp")
            || Build.DEVICE.equals("sholes");

    private FileWriter mWriter = null;

    private int mFlashMode = OFF;

    private Camera mCamera = null;
    private Camera.Parameters mParams;

    private FlashDevice() {
    }

    public static synchronized FlashDevice instance() {
        if (instance == null) {
            instance = new FlashDevice();
        }
        return instance;
    }

    public synchronized void setFlashMode(int mode) {
        try {
/* Vanzo:huangchaojun on: Mon, 27 Jun 2011 10:21:01 +0800
 *
                int value = mode;
        switch (mode) {
                case STROBE:
                    value = OFF;
                    break;
                case DEATH_RAY:
                    value = useDeathRay ? DEATH_RAY : HIGH;
                    value = (useZeppDeathRay && useDeathRay) ? ZEPP_DEATH_RAY : value;
                    break;
                case ON:
                    value = (Build.DEVICE.contains("zepp")) ? ZEPP_ON : value;
                    break;
                }
 */
            int value = mode;
            String cmdValue = "0 0 0";
            switch (mode) {
                case STROBE:
                    value = OFF;
                    break;
                case DEATH_RAY:
                    cmdValue = "1 32 0";
                    break;
                case ON:
                    cmdValue = "1 32 0";
                    break;
            }
// End of Vanzo:huangchaojun
/* Vanzo:wangyi on: Tue, 10 Jul 2012 19:20:39 +0800
 * bugfix #14807
            if (Build.DEVICE.contains("crespo")) {
 */
            if (true) {//change by bao | false
// End of Vanzo:wangyi
                if (mCamera == null) {
                    mCamera = Camera.open();
                }
                if (value == OFF) {
                    mParams = mCamera.getParameters();
                    mParams.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    mCamera.setParameters(mParams);
                    if (mode != STROBE) {
/* Vanzo:wangyi on: Mon, 20 Jan 2014 10:43:21 +0800
 * #62148
                        mCamera.stopPreview();
 */
// End of Vanzo: wangyi
                        mCamera.release();
                        mCamera = null;
                    }
                } else {
                    mParams = mCamera.getParameters();
                    mParams.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                    mCamera.setParameters(mParams);
/* Vanzo:wangyi on: Mon, 20 Jan 2014 10:43:40 +0800
 * #62148
                    if (mFlashMode != STROBE) {
                        mCamera.startPreview();
                    }
 */
// End of Vanzo: wangyi
                }
            } else {
                if (mWriter == null) {
                    if (Build.DEVICE.contains("sholes")) {
                        mWriter = new FileWriter(DEVICE_SHOLES);
                    } else {
                        mWriter = new FileWriter(DEVICE);
                    }
                }
/* Vanzo:huangchaojun on: Mon, 27 Jun 2011 10:37:14 +0800
 *
                    mWriter.write(String.valueOf(value));
 */
                mWriter.write(String.valueOf(cmdValue));
// End of Vanzo:huangchaojun
                mWriter.flush();
                if (mode == OFF) {
                    mWriter.close();
                    mWriter = null;
                }
            }
            mFlashMode = mode;
        } catch (IOException e) {
            throw new RuntimeException("Can't open flash device: " + DEVICE, e);
        }
    }

    public synchronized int getFlashMode() {
        return mFlashMode;
    }
}
