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

package com.mediatek.mediatekdm;

import android.content.Context;
import android.util.Log;

import com.mediatek.mediatekdm.DmConst.TAG;
import com.mediatek.mediatekdm.conn.DmDatabase;
import com.mediatek.mediatekdm.mdm.MdmConfig;
import com.mediatek.mediatekdm.mdm.MdmConfig.DmAccConfiguration;
import com.mediatek.mediatekdm.mdm.MdmException;
import com.mediatek.mediatekdm.option.Options;
import com.mediatek.telephony.SimInfoManager;
import com.mediatek.telephony.SimInfoManager.SimInfoRecord;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

public class DmConfig {
    public DmConfig(Context ctx) {
        mContext = ctx;
        mConfig = new MdmConfig();
        mConfigFile = DmConst.Path.getPathInSystem(DmConst.Path.DM_CONFIG_FILE);
        try {
            InputStream cfgStream = new FileInputStream(mConfigFile);

            if (cfgStream != null) {
                mParamTable = new Properties();
                mParamTable.loadFromXML(cfgStream);
                cfgStream.close();
            }
        } catch (IOException e) {
            Log.w(TAG.COMMON, "MdmcConfig:Caught exception " + e.getMessage());
        }
    }

    public void configure() {
        try {
            if (Options.USEDIRECTINTERNET) {
                // for ZTE DM server(via wifi/net), MUST not be set!
                Log.i(TAG.COMMON, "[DMConfig] skip setting proxy for direct internet.");
            } else {
                // for cmcc/cu DM server, proxy MUST be set!
                Log.i(TAG.COMMON, "[DMConfig] setting proxy for WAP.");
                DmDatabase dmDB = new DmDatabase(mContext);
                List<SimInfoRecord> simList = SimInfoManager.getInsertedSimInfoList(mContext);
                for (SimInfoRecord sim : simList) {
                    boolean result;
                    int simId;
                    simId = sim.mSimSlotId;
                    Log.i(TAG.COMMON, "SIM Slot ID is : " + simId);
                    result = dmDB.dmApnReady(simId);
                    Log.i(TAG.COMMON, "Dm apn mTable set result :" + result);
                }
                String proxyAddr = dmDB.getApnProxyFromSettings();
                int proxyPort = dmDB.getApnProxyPortFromSettings();
                Log.i(TAG.COMMON, "Proxy addr = " + proxyAddr + ", port = " + proxyPort);
                
                if (proxyAddr != null && proxyPort > 0) {
                    mConfig.setDmProxy("http://" + proxyAddr + ":" + proxyPort);
                    mConfig.setDlProxy("http://" + proxyAddr + ":" + proxyPort);
                } else {
                    Log.w(TAG.COMMON, "DM_PROXY not configed");
                }
            }

            mConfig.setEncodeWBXMLMsg(false);
            mConfig.setResendCommandInAuth(true);
            // Use application level retry
            mConfig.setMaxNetRetries(3);
            mConfig.setDDVersionCheck(false);
            mConfig.setNotificationVerificationMode(MdmConfig.NotifVerificationMode.DISABLED);
            if (mParamTable != null) {

                configureDMAcc();

                if (mParamTable.containsKey(DM_HEXSID)) {
                    String dmServer = mParamTable.getProperty(DM_HEXSID);
                    if (dmServer != null) {
                        if (dmServer.equals("true")) {
                            mConfig.setSessionIDAsDec(false);
                        } else {
                            mConfig.setSessionIDAsDec(true);
                        }
                    }
                } else {
                    mConfig.setSessionIDAsDec(true);
                    Log.w(TAG.COMMON, "DM_HEXSID not configed");
                }

                if (mParamTable.containsKey(DM_PROXY)) {
                    String dmProxy = mParamTable.getProperty(DM_PROXY);
                    mConfig.setDmProxy(dmProxy);
                } else {
                    Log.w(TAG.COMMON, "DM_PROXY not configed");
                }

                if (mParamTable.containsKey(DL_PROXY)) {
                    String dlProxy = mParamTable.getProperty(DL_PROXY);
                    mConfig.setDlProxy(dlProxy);
                } else {
                    Log.w(TAG.COMMON, "DL_PROXY not configed");
                }

                if (mParamTable.containsKey(ENCODE)) {
                    String encode = mParamTable.getProperty(ENCODE);
                    if (encode != null) {
                        if (encode.compareTo(XML) == 0) {
                            Log.i(TAG.COMMON, "Call mConfig.setEncodeWBXMLMsg(false)");
                            mConfig.setEncodeWBXMLMsg(false);
                        }
                    }
                } else {
                    Log.w(TAG.COMMON, "ENCODE not configed");
                }

                if (mParamTable.containsKey(SERVER_202_UNSUPPORTED)) {
                    String server202NotSupported = mParamTable.getProperty(SERVER_202_UNSUPPORTED);
                    if (server202NotSupported != null) {
                        if (server202NotSupported.charAt(0) == 'T'
                                || server202NotSupported.charAt(0) == 't') {
                            mConfig.set202statusCodeNotSupportedByServer(true);
                        }
                    }
                } else {
                    Log.w(TAG.COMMON, "SERVER_202_UNSUPPORTED not configed");
                }

                if (mParamTable.containsKey(INSTALL_NOTIFY_SUCCESS_ONLY)) {
                    String installNotify = mParamTable.getProperty(INSTALL_NOTIFY_SUCCESS_ONLY);
                    if (installNotify != null) {
                        if (installNotify.charAt(0) == 'T' || installNotify.charAt(0) == 't') {
                            mConfig.setInstallNotifySuccessOnly(true);
                        }
                    }
                } else {
                    Log.w(TAG.COMMON, "INSTALL_NOTIFY_SUCCESS_ONLY not configed");
                }
            } else {
                mConfig.setSessionIDAsDec(true);
            }
        } catch (MdmException e) {
            Log.w(TAG.COMMON, "MdmcConfig:Caught exception " + e.getMessage());
        }
    }

    private void configureDMAcc() throws MdmException {
        DmAccConfiguration dmacc = mConfig.new DmAccConfiguration();
        dmacc.activeAccountDmVersion = MdmConfig.DmVersion.DM_1_2;
        dmacc.dm12root = "./DMAcc/OMSAcc";
        dmacc.isExclusive = false;
        dmacc.updateInactiveDmAccount = false;
        mConfig.setDmAccConfiguration(dmacc);
    }


    private Context mContext;
    private MdmConfig mConfig;
    private Properties mParamTable;
    private final String mConfigFile;

    private static final String ENCODE = "encode";
    private static final String XML = "xml";
    private static final String DL_PROXY = "dlproxy";
    private static final String DM_PROXY = "dmproxy";
    private static final String INSTALL_NOTIFY_SUCCESS_ONLY = "installnotifysuccessonly";
    private static final String SERVER_202_UNSUPPORTED = "server_202_unsupported";
    private static final String DM_HEXSID = "hexsid";

}
