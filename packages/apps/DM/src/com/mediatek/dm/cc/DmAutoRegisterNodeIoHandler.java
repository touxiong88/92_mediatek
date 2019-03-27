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

package com.mediatek.dm.cc;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.mediatek.dm.DmConst;
import com.mediatek.dm.DmConst.TAG;
import com.mediatek.dm.DmService;
import com.mediatek.dm.xml.DmXMLParser;
import com.redbend.vdm.NodeIoHandler;
import com.redbend.vdm.VdmException;

import java.util.HashMap;
import java.util.Map;

/**
 * Hander for transfer AutoRegister parameters to session processor.
 * This is only for CU now.
 */
public class DmAutoRegisterNodeIoHandler implements NodeIoHandler {

    private Context mContext;
    private Uri mUri;
    private String mRecordToWrite;
    private DmXMLParser mConfigReader;
    
    private final String mAutoRegSMSC = "AutoRegSMSC";
    private final String mAutoRegSMSPort = "AutoRegSMSport";

    private String[] mItem = { mAutoRegSMSC, mAutoRegSMSPort };
    private String[] mContentValue = { null, null };
    private String[] mProjection = { "AutoRegSMSC", "AutoRegSMSport" };

    private static final String PREFS_NAME = "SmsRegSelf";

    private Map<String, String> mMap = new HashMap<String, String>();

    /**
     * Constructor.
     * @param ctx context
     * @param treeUri AutoRegister node path in tree.xml
     */
    public DmAutoRegisterNodeIoHandler(Context ctx, Uri treeUri) {
        mContext = ctx;
        mUri = treeUri;
        for (int i = 0; i < mItem.length; i++) {
            mMap.put(mItem[i], mProjection[i]);
        }
    }


    public int read(int offset, byte[] data) throws VdmException {
        String recordToRead = null;
        String uriPath = mUri.getPath();
        Log.i(TAG.NODE_IO_HANDLER, "uri: " + uriPath);
        Log.i(TAG.NODE_IO_HANDLER, "offset: " + offset);

        if (DmService.sCCStoredParams.containsKey(uriPath)) {
            recordToRead = DmService.sCCStoredParams.get(uriPath);
            Log.d(TAG.NODE_IO_HANDLER,
                    "get valueToRead from mCCStoredParams, the value is "
                            + recordToRead);
        } else {
            int itemLength = mItem.length;
            for (int i = 0; i < itemLength; i++) { //TODO: extract length and item[i]
                String item = mItem[i];
                if (mUri.getPath().contains(item)) {
                    if ((String) mMap.get(item) != null) {
                        String defaultValue = "";
                        if (mAutoRegSMSC.equals(item)) {
                            defaultValue = getDefaultValue("smsnumber");
                        } else if (mAutoRegSMSPort.equals(item)) {
                            defaultValue = getDefaultValue("smsport");
                            defaultValue = Integer.toHexString(Integer.valueOf(
                                    defaultValue));
                        }
                        SharedPreferences settings = mContext
                                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                        recordToRead = settings.getString(item,
                                defaultValue);
                    } else {
                        recordToRead = mContentValue[i];
                    }
                    break;
                }
                DmService.sCCStoredParams.put(uriPath, recordToRead);
                Log.d(TAG.NODE_IO_HANDLER,
                        "put valueToRead to mCCStoredParams, the value is "
                                + recordToRead);
            }
        }
        if (TextUtils.isEmpty(recordToRead)) {
            return 0;
        } else {
            byte[] temp = recordToRead.getBytes();
            if (data == null) {
                return temp.length;
            }
            int numberRead = 0;
            for (; numberRead < data.length - offset; numberRead++) {
                if (numberRead < temp.length) {
                    data[numberRead] = temp[offset + numberRead];
                } else {
                    break;
                }
            }
            if (numberRead < data.length - offset) {
                recordToRead = null;
            } else if (numberRead < temp.length) {
                recordToRead = recordToRead.substring(data.length - offset);
            }
            return numberRead;
        }
    }

    public void write(int offset, byte[] data, int totalSize) throws VdmException {
        Log.i(TAG.NODE_IO_HANDLER, "uri: " + mUri.getPath());
        Log.i(TAG.NODE_IO_HANDLER, "data: " + new String(data));
        Log.i(TAG.NODE_IO_HANDLER, "offset: " + offset);
        Log.i(TAG.NODE_IO_HANDLER, "total size: " + totalSize);

        mRecordToWrite = new String(data);
        if (mRecordToWrite.length() == totalSize) {
            for (int i = 0; i < mItem.length; i++) {
                if (mUri.getPath().contains(mItem[i])) {
                    SharedPreferences settings = mContext.getSharedPreferences(
                            PREFS_NAME, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString(mItem[i], mRecordToWrite);
                    editor.commit();

                }
            }
        }
    }

    private String getDefaultValue(String key) {
        if (mConfigReader == null) {
            mConfigReader = new DmXMLParser(DmConst.PathName.SMSREG_CONFIG_FILE);
        }
        String value = mConfigReader.getValByTagName(key);
        return value;
    }

}
