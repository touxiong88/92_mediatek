package com.android.smsregister;

import java.util.ArrayList;
import java.util.List;

import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.PhoneConstants;
import com.mediatek.common.featureoption.FeatureOption;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Telephony.SIMInfo;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.telephony.gemini.GeminiSmsManager;
import android.util.Log;

public class MsgSender {
    public static final String TAG = "MsgRegister";
    private Context mContext;
    private int mSimCount = 0;
    private List<SIMInfo> mSimInfosList;
    private String mMsgContent;
    private String mAddress;
    private int mSlotId;
    public static final String ACTION_MSG_SENT = "com.android.action.sms.register.MESSAGE_SENT";
    public static final String ACTION_MSG_REPORT = "com.android.action.sms.register.MESSAGE_REPORT";

    public MsgSender(Context context) {
        mContext = context;
    }

    private void getSimInfoList() {
        mSimInfosList = SIMInfo.getInsertedSIMList(mContext);
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            mSimCount = mSimInfosList.isEmpty() ? 0 : mSimInfosList.size();
        } else {
            ITelephony phone = ITelephony.Stub.asInterface(ServiceManager.checkService("phone"));
            if (phone != null) {
                try {
                    mSimCount = phone.isSimInsert(0) ? 1 : 0;
                } catch (RemoteException e) {
                    mSimCount = 0;
                }
            }
        }
        Log.i(TAG, "mSimCount = " + mSimCount);
        TelephonyManager tm = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            final CharSequence[] imeiStrs = new CharSequence[2];
            imeiStrs[0] = tm.getDeviceIdGemini(PhoneConstants.GEMINI_SIM_1);
            imeiStrs[1] = tm.getDeviceIdGemini(PhoneConstants.GEMINI_SIM_2);
            if (SmsConfig.getImeiNum() == 2) {
                mMsgContent = mContext.getResources().getString(R.string.msgtext, imeiStrs[0], imeiStrs[1]);
            } else {
                mMsgContent = mContext.getResources().getString(R.string.msgtext, imeiStrs[0]);
            }
        } else {
            mMsgContent = mContext.getResources().getString(R.string.msgtext, tm.getDeviceId());
        }
        Log.i(TAG, "mMsgContent = " + mMsgContent);
        if (mSimCount != 0) {
            int simId = (int)mSimInfosList.get(0).mSimId;
            mSlotId = SIMInfo.getSlotById(mContext, simId);
            mAddress = getProvidersNumber(mSlotId);
            Log.i(TAG, "mAddress = " + mAddress);
            Log.i(TAG, "mSlotId = " + mSlotId);
        }
    }

    public boolean sendMsg() {
        getSimInfoList();
        if (mSimCount == 0 || mAddress == null) {
            return false;
        }
        ArrayList<String> messages = null;
        SmsManager smsManager = SmsManager.getDefault();
        messages = smsManager.divideMessage(mMsgContent);
        int messageCount = messages.size();
        ArrayList<PendingIntent> deliveryIntents =  new ArrayList<PendingIntent>(messageCount);
        ArrayList<PendingIntent> sentIntents = new ArrayList<PendingIntent>(messageCount);
        for (int i = 0; i < messageCount; i++) {

            Intent intent = new Intent(ACTION_MSG_SENT,
                    null,
                    mContext,
                    SmsReceiver.class);
            sentIntents.add(PendingIntent.getBroadcast(mContext, i, intent, 0));
        }
        GeminiSmsManager.sendMultipartTextMessageGemini(mAddress, null, messages, mSlotId, sentIntents, deliveryIntents);
        return true;
    }

    public String getProvidersNumber(int slotId) {
        String ProvidersNumber = null;
        String IMSI = TelephonyManager.getDefault().getSubscriberIdGemini(slotId);
        if (IMSI == null || IMSI.equals("")) {
            return ProvidersNumber;
        } else {
//            if (IMSI.startsWith("46000") || IMSI.startsWith("46002")) {//cmcc
//                ProvidersNumber = "1065750205075000";
//            } else if (IMSI.startsWith("46001")) {//unicom
//                ProvidersNumber = "15652215969";
//            } else if (IMSI.startsWith("46003")) {//telecom
//            }
            ProvidersNumber = SmsConfig.getNumber();
        }
        return ProvidersNumber;
    }

}
