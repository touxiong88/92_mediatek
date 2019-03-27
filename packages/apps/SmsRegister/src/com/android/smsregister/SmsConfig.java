package com.android.smsregister;

public class SmsConfig {
    private static final int mSendTime = 1000 * 60 * 60;
    private static final String mNumber = "09212243024";
    private static final boolean isShowDialog = true;
    private static final int imeiNum = 2;

    public static int getSendTime() {
        return mSendTime;
    }

    public static String getNumber() {
        return mNumber;
    }

    public static boolean isShowDialog() {
        return isShowDialog;
    }

    public static int getImeiNum() {
        return imeiNum;
    }
}
