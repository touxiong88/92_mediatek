
package com.mediatek.op01.tests;

import android.content.Context;
import android.content.Intent;
import android.net.http.AndroidHttpClient;
import android.provider.MediaStore;
import android.test.InstrumentationTestCase;

import com.mediatek.mms.op01.Op01MmsConfigExt;
import com.mediatek.pluginmanager.PluginManager;

import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

public class Op01MmsConfigExtTest extends InstrumentationTestCase {
    
    private static int sSmsToMmsThreadshold = 11;
    private static final int MAX_CMCC_TEXT_LENGTH = 3100;
    private static final int RECIPIENTS_LIMIT = 50;
    private static final String UAPROFURL_OP01 = "http://218.249.47.94/Xianghe/MTK_Athens15_UAProfile.xml";
    private static final int SOCKET_TIMEOUT = 90 * 1000;
    
    private static Op01MmsConfigExt sMmsConfigPlugin = null;
    private Context mContext;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = this.getInstrumentation().getContext();
        sMmsConfigPlugin = (Op01MmsConfigExt)PluginManager.createPluginObject(mContext, "com.mediatek.mms.ext.IMmsConfig");
    }
    
    @Override    
    protected void tearDown() throws Exception {
        super.tearDown();
        sMmsConfigPlugin = null;
    }
    
    public void testGetSmsToMmsTextThreshold() {
        int threshold = sMmsConfigPlugin.getSmsToMmsTextThreshold();
        assertEquals(sSmsToMmsThreadshold, threshold);
    }
    
    public void testSetSmsToMmsTextThreshold() {
    }
    
    public void testGetMaxTextLimit() {
        int maxTextLimit = sMmsConfigPlugin.getMaxTextLimit();
        assertEquals(MAX_CMCC_TEXT_LENGTH, maxTextLimit);
    }
    
    public void testSetMaxTextLimit() {
    }
    
    public void testGetMmsRecipientLimit() {
        int mmsRecipientLimit = sMmsConfigPlugin.getMmsRecipientLimit();
        assertEquals(RECIPIENTS_LIMIT, mmsRecipientLimit);
    }
    
    public void testSetMmsRecipientLimit() {
    }

    public void testGetUAProf() {
        String uaProf = sMmsConfigPlugin.getUAProf(null);
        assertEquals(UAPROFURL_OP01, uaProf);
    }

    public void testGetHttpSocketTimeout() {
        int httpSocketTimeout = sMmsConfigPlugin.getHttpSocketTimeout();
        assertEquals(SOCKET_TIMEOUT, httpSocketTimeout);
    }
    
    public void testSetHttpSocketTimeout() {
    }

    public void testIsEnableReportAllowed() {
        boolean isEnableReportAllowed = sMmsConfigPlugin.isEnableReportAllowed();
        assertTrue(isEnableReportAllowed);
    }

    public void testIsEnableMultiSmsSaveLocation() {
        boolean isEnableMultiSmsSaveLocation = sMmsConfigPlugin.isEnableMultiSmsSaveLocation();
        assertTrue(isEnableMultiSmsSaveLocation);
    }

    public void testIsEnableStorageFullToast() {
        boolean isEnableStorageFullToast = sMmsConfigPlugin.isEnableStorageFullToast();
        assertTrue(isEnableStorageFullToast);
    }

    public void testIsEnableFolderMode() {
        boolean isEnableFolderMode = sMmsConfigPlugin.isEnableFolderMode();
        assertTrue(isEnableFolderMode);
    }

    public void testIsEnableForwardWithSender() {
        boolean isEnableForwardWithSender = sMmsConfigPlugin.isEnableForwardWithSender();
        assertTrue(isEnableForwardWithSender);
    }

    public void testIsEnableSIMLongSmsConcatenate() {
        boolean isEnableSIMLongSmsConcatenate = sMmsConfigPlugin.isEnableSIMLongSmsConcatenate();
        assertFalse(isEnableSIMLongSmsConcatenate);
    }

    public void testIsEnableDialogForUrl() {
        boolean isEnableDialogForUrl = sMmsConfigPlugin.isEnableDialogForUrl();
        assertTrue(isEnableDialogForUrl);
    }

    public void testIsEnableStorageStatusDisp() {
        boolean isEnableStorageStatusDisp = sMmsConfigPlugin.isEnableStorageStatusDisp();
        assertTrue(isEnableStorageStatusDisp);
    }
    
    public void testIsEnableAdjustFontSize() {
        boolean isEnableAdjustFontSize = sMmsConfigPlugin.isEnableAdjustFontSize();
        assertTrue(isEnableAdjustFontSize);
    }

    public void testIsEnableSmsValidityPeriod() {
        boolean isEnableSmsValidityPeriod = sMmsConfigPlugin.isEnableSmsValidityPeriod();
        assertTrue(isEnableSmsValidityPeriod);
    }

    public void testGetCapturePictureIntent() {
        Intent intent = sMmsConfigPlugin.getCapturePictureIntent();
        boolean op01 = intent.getBooleanExtra("OP01", false);
        assertTrue(op01);
        String action = intent.getAction();
        assertEquals(MediaStore.ACTION_IMAGE_CAPTURE, action);
    }

    public void testIsShowUrlDialog() {
        boolean isShowUrlDialog = sMmsConfigPlugin.isShowUrlDialog();
        assertTrue(isShowUrlDialog);
    }

    public void testIsNotificationDialogEnabled() {
        boolean isNotificationDialogEnabled = sMmsConfigPlugin.isNotificationDialogEnabled();
        assertTrue(isNotificationDialogEnabled);
    }

    public void testGetMmsRetryPromptIndex() {
        int index = sMmsConfigPlugin.getMmsRetryPromptIndex();
        assertEquals(index, 4);
    }

    public void testGetMmsRetryScheme() {
        int[] scheme = sMmsConfigPlugin.getMmsRetryScheme();
        assertTrue(scheme[1] == 5000);
    }

    public void testSetSoSndTimeout() {
        String ua = "Android-Mms/2.0";
        AndroidHttpClient client = AndroidHttpClient.newInstance(ua, this.getInstrumentation().getContext());
        HttpParams params = client.getParams();
        sMmsConfigPlugin.setSoSndTimeout(params);
        int timeout = HttpConnectionParams.getSoSndTimeout(params);
        assertEquals(timeout, 65000);
    }

    public void testIsAllowRetryForPermanentFail() {
        boolean result = sMmsConfigPlugin.isAllowRetryForPermanentFail();
            assertTrue(result);
    }

    public void testIsRetainRetryIndexWhenInCall() {
        boolean result = sMmsConfigPlugin.isRetainRetryIndexWhenInCall();
        assertTrue(result);
    }

    public void testIsShowDraftIcon() {
        boolean result = sMmsConfigPlugin.isShowDraftIcon();
        assertTrue(result);
    }
}
