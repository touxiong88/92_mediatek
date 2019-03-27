/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

/* //device/content/providers/telephony/TelephonyProvider.java
 **
 ** Copyright 2006, The Android Open Source Project
 **
 ** Licensed under the Apache License, Version 2.0 (the "License");
 ** you may not use this file except in compliance with the License.
 ** You may obtain a copy of the License at
 **
 **     http://www.apache.org/licenses/LICENSE-2.0
 **
 ** Unless required by applicable law or agreed to in writing, software
 ** distributed under the License is distributed on an "AS IS" BASIS,
 ** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ** See the License for the specific language governing permissions and
 ** limitations under the License.
 */

package com.mediatek.mediatekdm.conn;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri.Builder;
import android.util.Log;

import com.mediatek.mediatekdm.DmConst;
import com.mediatek.mediatekdm.DmConst.TAG;
import com.mediatek.mediatekdm.ext.MTKPhone;
import com.mediatek.mediatekdm.util.Utilities;
import com.mediatek.mediatekdm.xml.DmXMLParser;
import com.mediatek.telephony.TelephonyManagerEx;

import org.w3c.dom.Node;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DmDatabase {
    public static final int GEMINI_SIM_1 = 0;
    public static final int GEMINI_SIM_2 = 1;

    private TelephonyManagerEx mTelephonyManager;

    private ContentResolver mContentResolver;
    private Cursor mCursor;
    private static final String DEFAULTPROXYADDR = "10.0.0.172";
    private static final int DEFAULTPROXYPORT = 80;

    /**
     * Information of apn for DM.
     */
    public static class DmApnInfo {
        public Integer id;
        public String name;
        public String numeric;
        public String mcc;
        public String mnc;
        public String apn;
        public String user;
        public String password;
        public String server;
        public String proxy;
        public String port;
        public String type;
    }

    private ArrayList<DmApnInfo> mApnInfo;
    private static int sRegisterSimId;
    private Context mContext;
    private Builder mBuilder;

    public DmDatabase(Context context) {
        mContext = context;
        mContentResolver = mContext.getContentResolver();
        mTelephonyManager = TelephonyManagerEx.getDefault();
    }

    public boolean dmApnReady(int simId) {
        if (simId != GEMINI_SIM_1 && simId != GEMINI_SIM_2) {
            Log.e(TAG.DATABASE, "simId = [" + simId + "]is error! ");
            return false;
        }
        Log.i(TAG.DATABASE, "Sim Id = " + simId);
        sRegisterSimId = simId;
        Cursor cursor = null;
        boolean ret = false;
        int count = 0;

        cursor = mContentResolver.query(MTKPhone.getDMUri(simId), null, null,
                null, null);

        if (cursor != null) {
            count = cursor.getCount();
            cursor.close();
        }
        
        if (count > 0) {
            Log.w(TAG.DATABASE,
                    "There are apn data in dm apn mTable, the record is "
                            + count);
            return true;
        }

        Log.w(TAG.DATABASE, "There is no data in dm apn mTable");

        getApnValuesFromConfFile();
        ret = initDmApnTable(mApnInfo);
        if (!ret) {
            Log.e(TAG.DATABASE, "Init Apn mTable error!");
        }
        return ret;
    }

    public static void clearDB(Context context) {
        ContentResolver cr = context.getContentResolver();
        for (int simId = 0; simId < 2; simId++) {
            cr.delete(MTKPhone.getDMUri(simId), null, null);
        }
    }

    // read config file to get the apn values
    private void getApnValuesFromConfFile() {
        // init mApnInfo
        Log.i(TAG.DATABASE, "getApnValuesFromConfFile");
        TelephonyManagerEx teleMgr = TelephonyManagerEx.getDefault();

        String operatorName = null;
        operatorName = teleMgr.getSimOperatorName(sRegisterSimId);

        Log.i(TAG.DATABASE, "getApnValuesFromConfFile():operatorName = "
                + operatorName);

        File dmApnFile = new File(DmConst.Path.getPathInSystem(DmConst.Path.DM_APN_INFO_FILE));
        if (dmApnFile == null || (!dmApnFile.exists())) {
            Log.e(TAG.DATABASE, "Apn file is not exists or dmApnFile is null");
            return;
        }
        DmXMLParser xmlParser = new DmXMLParser(dmApnFile.getAbsolutePath());
        List<Node> nodeList = new ArrayList<Node>();
        xmlParser.getChildNode(nodeList, "dmapn");
        if (nodeList != null && nodeList.size() > 0) {
            Log.i(TAG.DATABASE, "dmapn node list size = " + nodeList.size());
            operatorName = Utilities.getOperatorName();
            if (operatorName == null) {
                Log.e(TAG.DATABASE,
                        "Get operator name from config file is null");
                return;
            }

            Log.i(TAG.DATABASE, "Operator  = " + operatorName);
            Node node = nodeList.get(0);
            List<Node> operatorList = new ArrayList<Node>();
            // xmlParser.getChildNode(node, operatorList, operatorName);
            xmlParser.getChildNode(node, operatorList, operatorName);
            int operatorSize = operatorList.size();
            Log.i(TAG.DATABASE, "OperatorList size  =  " + operatorSize);
            mApnInfo = new ArrayList<DmApnInfo>(operatorSize);
            for (int i = 0; i < operatorSize; i++) {
                DmApnInfo mDmApnInfo = new DmApnInfo();
                Log.i(TAG.DATABASE, "this is the [" + i + "] operator apn");
                Node operatorNode = operatorList.get(i);
                List<Node> operatorLeafNodeList = new ArrayList<Node>();
                xmlParser.getLeafNode(operatorNode, operatorLeafNodeList, "id");
                if (operatorLeafNodeList != null
                        && operatorLeafNodeList.size() > 0) {
                    int operatorLeafSize = operatorLeafNodeList.size();
                    Log.i(TAG.DATABASE, "OperatorLeafList size  =  "
                            + operatorLeafSize);
                    String nodeStr = operatorLeafNodeList
                            .get(operatorLeafSize - 1).getFirstChild()
                            .getNodeValue();
                    Log.i(TAG.DATABASE, "node str id = " + nodeStr);
                    mDmApnInfo.id = Integer.parseInt(nodeStr);
                }

                xmlParser.getLeafNode(operatorNode, operatorLeafNodeList,
                        "name");
                if (operatorLeafNodeList != null
                        && operatorLeafNodeList.size() > 0) {
                    int operatorLeafSize = operatorLeafNodeList.size();
                    Log.i(TAG.DATABASE, "OperatorLeafList name size  =  "
                            + operatorLeafSize);
                    String nodeStr = operatorLeafNodeList
                            .get(operatorLeafSize - 1).getFirstChild()
                            .getNodeValue();
                    Log.i(TAG.DATABASE, "node nodeStr name  = " + nodeStr);
                    mDmApnInfo.name = nodeStr;

                }
                // numberic
                xmlParser.getLeafNode(operatorNode, operatorLeafNodeList,
                        "numeric");
                if (operatorLeafNodeList != null
                        && operatorLeafNodeList.size() > 0) {
                    int operatorLeafSize = operatorLeafNodeList.size();
                    Log.i(TAG.DATABASE, "OperatorLeafList numberic size  =  "
                            + operatorLeafSize);
                    String nodeStr = null;
                    nodeStr = operatorLeafNodeList.get(operatorLeafSize - 1)
                            .getFirstChild().getNodeValue();
                    Log.i(TAG.DATABASE, "node node numberic  = " + nodeStr);
                    mDmApnInfo.numeric = nodeStr;

                }
                // mcc
                xmlParser
                        .getLeafNode(operatorNode, operatorLeafNodeList, "mcc");
                if (operatorLeafNodeList != null
                        && operatorLeafNodeList.size() > 0) {
                    int operatorLeafSize = operatorLeafNodeList.size();
                    Log.i(TAG.DATABASE, "OperatorLeafList mcc size  =  "
                            + operatorLeafSize);
                    String nodeStr = operatorLeafNodeList
                            .get(operatorLeafSize - 1).getFirstChild()
                            .getNodeValue();
                    Log.i(TAG.DATABASE, "node node mcc  = " + nodeStr);
                    mDmApnInfo.mcc = nodeStr;

                }
                // mnc
                xmlParser
                        .getLeafNode(operatorNode, operatorLeafNodeList, "mnc");
                if (operatorLeafNodeList != null
                        && operatorLeafNodeList.size() > 0) {
                    int operatorLeafSize = operatorLeafNodeList.size();
                    Log.i(TAG.DATABASE, "OperatorLeafList mnc size  =  "
                            + operatorLeafSize);
                    String nodeStr = operatorLeafNodeList
                            .get(operatorLeafSize - 1).getFirstChild()
                            .getNodeValue();
                    Log.i(TAG.DATABASE, "node node mnc  = " + nodeStr);
                    mDmApnInfo.mnc = nodeStr;

                }

                // apn
                xmlParser
                        .getLeafNode(operatorNode, operatorLeafNodeList, "apn");
                if (operatorLeafNodeList != null
                        && operatorLeafNodeList.size() > 0) {
                    int operatorLeafSize = operatorLeafNodeList.size();
                    Log.i(TAG.DATABASE, "OperatorLeafList apn size  =  "
                            + operatorLeafSize);
                    String nodeStr = operatorLeafNodeList
                            .get(operatorLeafSize - 1).getFirstChild()
                            .getNodeValue();
                    Log.i(TAG.DATABASE, "node node apn  = " + nodeStr);
                    mDmApnInfo.apn = nodeStr;

                }

                // type
                xmlParser.getLeafNode(operatorNode, operatorLeafNodeList,
                        "type");
                if (operatorLeafNodeList != null
                        && operatorLeafNodeList.size() > 0) {
                    int operatorLeafSize = operatorLeafNodeList.size();
                    Log.i(TAG.DATABASE, "OperatorLeafList type size  =  "
                            + operatorLeafSize);
                    String nodeStr = operatorLeafNodeList
                            .get(operatorLeafSize - 1).getFirstChild()
                            .getNodeValue();
                    Log.i(TAG.DATABASE, "node type   = " + nodeStr);
                    mDmApnInfo.type = nodeStr;

                }

                // user
                xmlParser.getLeafNode(operatorNode, operatorLeafNodeList,
                        "user");
                if (operatorLeafNodeList != null
                        && operatorLeafNodeList.size() > 0) {
                    int operatorLeafSize = operatorLeafNodeList.size();
                    Log.i(TAG.DATABASE, "OperatorLeafList user size  =  "
                            + operatorLeafSize);
                    String nodeStr = operatorLeafNodeList
                            .get(operatorLeafSize - 1).getFirstChild()
                            .getNodeValue();
                    Log.i(TAG.DATABASE, "node user   = " + nodeStr);
                    mDmApnInfo.user = nodeStr;

                }
                // password
                xmlParser.getLeafNode(operatorNode, operatorLeafNodeList,
                        "password");
                if (operatorLeafNodeList != null
                        && operatorLeafNodeList.size() > 0) {
                    int operatorLeafSize = operatorLeafNodeList.size();
                    Log.i(TAG.DATABASE, "OperatorLeafList password size  =  "
                            + operatorLeafSize);
                    String nodeStr = operatorLeafNodeList
                            .get(operatorLeafSize - 1).getFirstChild()
                            .getNodeValue();
                    Log.i(TAG.DATABASE, "node password   = " + nodeStr);
                    mDmApnInfo.password = nodeStr;

                }
                // server
                xmlParser.getLeafNode(operatorNode, operatorLeafNodeList,
                        "server");
                if (operatorLeafNodeList != null
                        && operatorLeafNodeList.size() > 0) {
                    int operatorLeafSize = operatorLeafNodeList.size();
                    Log.i(TAG.DATABASE, "OperatorLeafList server size  =  "
                            + operatorLeafSize);
                    String nodeStr = operatorLeafNodeList
                            .get(operatorLeafSize - 1).getFirstChild()
                            .getNodeValue();
                    Log.i(TAG.DATABASE, "node server   = " + nodeStr);
                    mDmApnInfo.server = nodeStr;

                }
                // proxy
                xmlParser.getLeafNode(operatorNode, operatorLeafNodeList,
                        "proxy");
                if (operatorLeafNodeList != null
                        && operatorLeafNodeList.size() > 0) {
                    int operatorLeafSize = operatorLeafNodeList.size();
                    Log.i(TAG.DATABASE, "OperatorLeafList proxy size  =  "
                            + operatorLeafSize);
                    String nodeStr = operatorLeafNodeList
                            .get(operatorLeafSize - 1).getFirstChild()
                            .getNodeValue();
                    Log.i(TAG.DATABASE, "node proxy   = " + nodeStr);
                    mDmApnInfo.proxy = nodeStr;

                }

                // port
                xmlParser.getLeafNode(operatorNode, operatorLeafNodeList,
                        "port");
                if (operatorLeafNodeList != null
                        && operatorLeafNodeList.size() > 0) {
                    int operatorLeafSize = operatorLeafNodeList.size();
                    Log.i(TAG.DATABASE, "OperatorLeafList port size  =  "
                            + operatorLeafSize);
                    String nodeStr = operatorLeafNodeList
                            .get(operatorLeafSize - 1).getFirstChild()
                            .getNodeValue();
                    Log.i(TAG.DATABASE, "node port   = " + nodeStr);
                    mDmApnInfo.port = nodeStr;

                }

                Log.i(TAG.DATABASE, "Before add to array mDmApnInfo[" + i
                        + "] = " + mDmApnInfo.id);
                mApnInfo.add(mDmApnInfo);

            }
        }
    }

    // init the mTable if need
    private boolean initDmApnTable(ArrayList<DmApnInfo> apnInfo) {
        Log.i(TAG.DATABASE, "Enter init Dm Apn Table");
        if (apnInfo == null || apnInfo.size() <= 0) {
            Log.e(TAG.DATABASE, "Apn that read from apn configure file is null");
            return false;
        }

        int size = apnInfo.size();
        Log.i(TAG.DATABASE, "apnInfo size = " + size);
        ArrayList<ContentValues> apnInfoEntry = new ArrayList<ContentValues>(
                size);

        for (int i = 0; i < size; i++) {
            Log.i(TAG.DATABASE, "insert i = " + i);
            Log.i(TAG.DATABASE, "apnInfo.get(" + i + ").id = "
                    + apnInfo.get(i).id);
            ContentValues v = new ContentValues();
            if (apnInfo.get(i) == null || apnInfo.get(i).id == null) {
                Log.w(TAG.DATABASE,
                        "before continue apnInfo.get.id " + apnInfo.get(i).id);
                continue;
            }

            v.put("_id", apnInfo.get(i).id);
            if (apnInfo.get(i).name != null) {
                v.put("name", apnInfo.get(i).name);
            }
            if (apnInfo.get(i).numeric != null) {
                v.put("numeric", apnInfo.get(i).numeric);
            }
            if (apnInfo.get(i).mcc != null) {
                v.put("mcc", apnInfo.get(i).mcc);
            }
            if (apnInfo.get(i).mnc != null) {
                v.put("mnc", apnInfo.get(i).mnc);
            }

            if (apnInfo.get(i).apn != null) {
                v.put("apn", apnInfo.get(i).apn);
            }
            if (apnInfo.get(i).type != null) {
                v.put("type", apnInfo.get(i).type);
            }

            if (apnInfo.get(i).user != null) {
                v.put("user", apnInfo.get(i).user);
            }
            if (apnInfo.get(i).server != null) {
                v.put("server", apnInfo.get(i).server);
            }
            if (apnInfo.get(i).password != null) {
                v.put("password", apnInfo.get(i).password);
            }
            if (apnInfo.get(i).proxy != null) {
                v.put("proxy", apnInfo.get(i).proxy);
            }
            if (apnInfo.get(i).port != null) {
                v.put("port", apnInfo.get(i).port);
            }

            apnInfoEntry.add(v);

        }
        int insertSize = apnInfoEntry.size();
        Log.i(TAG.DATABASE, "insert size = " + insertSize);
        if (insertSize > 0) {
            mBuilder = MTKPhone.getDMUri(sRegisterSimId).buildUpon();
            ContentValues[] values = new ContentValues[apnInfoEntry.size()];
            for (int i = 0; i < insertSize; i++) {
                Log.i(TAG.DATABASE, "insert to values i = [" + i + "]");
                values[i] = apnInfoEntry.get(i);
            }
            // bulk insert
            mContentResolver.bulkInsert(mBuilder.build(), values);
        }

        Log.i(TAG.DATABASE, "Init Dm database finish");
        return true;
    }

    public String getApnProxyFromSettings() {
        // waiting for Yuhui's interface
        String proxyAddr = null;
        String simOperator = null;
        String where = null;
        int simId = sRegisterSimId;
        if (simId == -1) {
            Log.e(TAG.DATABASE, "Get Register SIM ID error");
            return null;
        }

        Log.i(TAG.DATABASE, "simId = " + simId);
        simOperator = mTelephonyManager.getSimOperator(simId);
        Log.i(TAG.DATABASE, "simOperator numberic = " + simOperator);
        if (simOperator == null || simOperator.equals("")) {
            Log.e(TAG.DATABASE, "Get sim operator wrong ");
            return DEFAULTPROXYADDR;
        }
        where = "numeric =" + simOperator;
        if (simId >= 0) {
            mCursor = mContentResolver.query(MTKPhone.getDMUri(simId), null,
                    where, null, null);
        } else {
            Log.e(TAG.DATABASE, "There is no right the sim card");
            return null;
        }

        if (mCursor == null || mCursor.getCount() <= 0) {
            Log.e(TAG.DATABASE, "Get cursor error or cursor is no record");
            if (mCursor != null) {
                mCursor.close();
            }
            return null;
        }
        mCursor.moveToFirst();
        int proxyAddrID = mCursor.getColumnIndex("proxy");
        proxyAddr = mCursor.getString(proxyAddrID);
        if (mCursor != null) {
            mCursor.close();
        }
        Log.i(TAG.DATABASE, "proxy address = " + proxyAddr);
        return proxyAddr;
    }

    public int getApnProxyPortFromSettings() {
        // waiting for Yuhui's interface
        String port = null;
        String simOperator = null;
        String where = null;
        int simId = sRegisterSimId;
        if (simId == -1) {
            Log.e(TAG.DATABASE, "Get Register SIM ID error");
            return -1;
        }

        Log.i(TAG.DATABASE, "Sim Id = " + simId);

        // for gemini
        simOperator = mTelephonyManager.getSimOperator(simId);
        Log.i(TAG.DATABASE, "simOperator numberic = " + simOperator);
        if (simOperator == null || simOperator.equals("")) {
            Log.e(TAG.DATABASE, "Get sim operator wrong");
            return DEFAULTPROXYPORT;
        }
        where = "numeric =" + simOperator;
        if (simId >= 0) {
            mCursor = mContentResolver.query(MTKPhone.getDMUri(simId), null,
                    where, null, null);
        } else {
            Log.e(TAG.DATABASE, "There is no right the sim card");
            return -1;
        }
        
        if (mCursor == null || mCursor.getCount() <= 0) {
            Log.e(TAG.DATABASE, "Get cursor error or cursor is no record");
            if (mCursor != null) {
                mCursor.close();
            }
            return -1;
        }
        mCursor.moveToFirst();
        // int serverAddrID = mCursor.getColumnIndex("server");
        int portId = mCursor.getColumnIndex("port");
        port = mCursor.getString(portId);
        if (mCursor != null) {
            mCursor.close();
        }

        // Log.w(TAG,"server address = " + serverAddr);
        Log.i(TAG.DATABASE, "proxy port = " + port);
        // return lookupHost(serverAddr);
        return (Integer.parseInt(port));
    }

    public String getDmAddressFromSettings() {
        // waiting for Yuhui's interface
        String serverAddr = null;
        String simOperator = null;
        String where = null;
        int simId = sRegisterSimId;
        if (simId == -1) {
            Log.e(TAG.DATABASE, "Get Register SIM ID error");
            return null;
        }

        Log.i(TAG.DATABASE, "Sim Id register = " + sRegisterSimId);
        // for gemini
        simOperator = mTelephonyManager.getSimOperator(simId);
        Log.i(TAG.DATABASE, "simOperator numberic = " + simOperator);
        if (simOperator == null || simOperator.equals("")) {
            Log.e(TAG.DATABASE, "Get sim operator wrong");
            return null;
        }
        where = "numeric =" + simOperator;
        if (simId >= 0) {
            mCursor = mContentResolver.query(MTKPhone.getDMUri(simId), null, where, null, null);
        } else {
            Log.e(TAG.DATABASE, "There is no right the sim card");
            return null;
        }

        if (mCursor == null || mCursor.getCount() <= 0) {
            Log.e(TAG.DATABASE, "Get cursor error or cursor is no record");
            if (mCursor != null) {
                mCursor.close();
            }
            return null;
        }
        mCursor.moveToFirst();
        int serverAddrID = mCursor.getColumnIndex("server");
        serverAddr = mCursor.getString(serverAddrID);
        if (mCursor != null) {
            mCursor.close();
        }

        Log.i(TAG.DATABASE, "server address = " + serverAddr);
        return serverAddr;
    }

}
