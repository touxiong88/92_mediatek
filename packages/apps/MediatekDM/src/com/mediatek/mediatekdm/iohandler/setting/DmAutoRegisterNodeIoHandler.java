
package com.mediatek.mediatekdm.iohandler.setting;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.mediatek.mediatekdm.DmConst;
import com.mediatek.mediatekdm.DmConst.TAG;
import com.mediatek.mediatekdm.DmService;
import com.mediatek.mediatekdm.mdm.MdmException;
import com.mediatek.mediatekdm.mdm.NodeIoHandler;
import com.mediatek.mediatekdm.xml.DmXMLParser;

import java.util.HashMap;
import java.util.Map;

public class DmAutoRegisterNodeIoHandler implements NodeIoHandler {

    private Context mContext = null;
    private Uri mUri = null;
    private String mRecordToWrite = null;
    private DmXMLParser mConfigReader = null;

    private String[] mItem = {
            "AutoRegSMSC", "AutoRegSMSport"
    };
    
    /* default projection value */
    private String[] mContentValue = {
            null, null
    };
    String[] mProjection = {
            "AutoRegSMSC", "AutoRegSMSport"
    };

    private static final String PREFS_NAME = "SmsRegSelf";

    private Map<String, String> mMap = new HashMap<String, String>();

    public DmAutoRegisterNodeIoHandler(Context ctx, Uri treeUri, String mccMnc) {
        mContext = ctx;
        mUri = treeUri;
        for (int i = 0; i < mItem.length; i++) {
            mMap.put(mItem[i], mProjection[i]);
        }
    }

    public int read(int arg0, byte[] arg1) throws MdmException {
        String recordToRead = null;
        String uriPath = mUri.getPath();
        Log.i(TAG.NODEIOHANDLER, "mUri: " + uriPath);
        Log.i(TAG.NODEIOHANDLER, "arg0: " + arg0);

        if (DmService.sCCStoredParams.containsKey(uriPath)) {
            recordToRead = DmService.sCCStoredParams.get(uriPath);
            Log.d(TAG.NODEIOHANDLER, "get valueToRead from mCCStoredParams, the value is "
                    + recordToRead);
        } else {
            recordToRead = new String();
            for (int i = 0; i < mItem.length; i++) {
                if (mUri.getPath().contains(mItem[i])) {
                    if ((String) mMap.get(mItem[i]) != null) {

                        // modify:added start
                        String defaultValue = "";
                        if (mItem[i].equals("AutoRegSMSC")) {
                            defaultValue = getDefaultValue("smsnumber");
                        } else if (mItem[i].equals("AutoRegSMSport")) {
                            defaultValue = getDefaultValue("smsport");
                            defaultValue = Integer.toHexString(Integer.valueOf(defaultValue));
                        }
                        // modify:added end

                        SharedPreferences settings = mContext.getSharedPreferences(PREFS_NAME, 2);
                        recordToRead += settings.getString(mItem[i], defaultValue); // modify:0-->defaultValue
                    } else {
                        recordToRead += mContentValue[i];
                    }
                    break;
                }
                DmService.sCCStoredParams.put(uriPath, recordToRead);
                Log.d(TAG.NODEIOHANDLER, "put valueToRead to mCCStoredParams, the value is "
                        + recordToRead);
            }
        }
        if (TextUtils.isEmpty(recordToRead)) {
            return 0;
        } else {
            byte[] temp = recordToRead.getBytes();
            if (arg1 == null) {
                return temp.length;
            }
            int numberRead = 0;
            for (; numberRead < arg1.length - arg0; numberRead++) {
                if (numberRead < temp.length) {
                    arg1[numberRead] = temp[arg0 + numberRead];
                } else {
                    break;
                }
            }
            if (numberRead < arg1.length - arg0) {
                recordToRead = null;
            } else if (numberRead < temp.length) {
                recordToRead = recordToRead.substring(arg1.length - arg0);
            }
            return numberRead;
        }
    }

    public void write(int arg0, byte[] arg1, int arg2) throws MdmException {
        Log.i(TAG.NODEIOHANDLER, "mUri: " + mUri.getPath());
        Log.i(TAG.NODEIOHANDLER, "arg1: " + new String(arg1));
        Log.i(TAG.NODEIOHANDLER, "arg0: " + arg0);
        Log.i(TAG.NODEIOHANDLER, "arg2: " + arg2);

        mRecordToWrite = new String(arg1);
        if (mRecordToWrite.length() == arg2) {
            for (int i = 0; i < mItem.length; i++) {
                if (mUri.getPath().contains(mItem[i])) {
                    SharedPreferences settings = mContext.getSharedPreferences(PREFS_NAME, 0);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString(mItem[i], mRecordToWrite);
                    editor.commit();

                }
            }
        }
    }

    private String getDefaultValue(String key) {
        if (mConfigReader == null) {
            mConfigReader = new DmXMLParser(DmConst.Path.getPathInSystem(DmConst.Path.SMSREG_CONFIG_FILE));
        }
        String value = mConfigReader.getValByTagName(key);
        return value;
    }

}
