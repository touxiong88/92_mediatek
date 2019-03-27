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

package com.mediatek.mediatekdm.lawmo;

import android.content.Intent;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.storage.IMountService;
import android.util.Log;

import com.mediatek.common.dm.DmAgent;
import com.mediatek.mediatekdm.DmConst;
import com.mediatek.mediatekdm.DmConst.TAG;
import com.mediatek.mediatekdm.DmOperation.KEY;
import com.mediatek.mediatekdm.DmOperationManagerFactory;
import com.mediatek.mediatekdm.DmService;
import com.mediatek.mediatekdm.ext.MTKPhone;
import com.mediatek.mediatekdm.mdm.MdmException;
import com.mediatek.mediatekdm.mdm.MdmTree;
import com.mediatek.mediatekdm.mdm.lawmo.LawmoAction;
import com.mediatek.mediatekdm.mdm.lawmo.LawmoHandler;
import com.mediatek.mediatekdm.mdm.lawmo.LawmoOperationResult;
import com.mediatek.mediatekdm.mdm.lawmo.LawmoResultCode;
import com.mediatek.mediatekdm.xml.DmXMLParser;

import java.io.File;
import java.io.IOException;

public class DmLawmoHandler implements LawmoHandler {

    public DmLawmoHandler(DmService service) {
        mService = service;
        mDmAgent = MTKPhone.getDmAgent();
        mDmTree = new MdmTree();
    }

    private boolean isUsbMassStorageEnabled() {
        IMountService mountService = IMountService.Stub.asInterface(ServiceManager.getService("mount")); 
        boolean isUsbMassStorageEnabled;
        try {
            isUsbMassStorageEnabled = mountService.isUsbMassStorageEnabled();
        } catch (RemoteException e1) {
            Log.e(TAG.LAWMO, "RemoteException when call mountService.isUsbMassStorageConnected()");
            isUsbMassStorageEnabled = false;
        }
        Log.i(TAG.LAWMO, "isUsbMassStorageEnabled : " + isUsbMassStorageEnabled);
        return isUsbMassStorageEnabled;
    }

    public LawmoOperationResult executeFactoryReset() {
        Log.i(TAG.LAWMO, "+executeFactoryReset()");

        if (isUsbMassStorageEnabled()) {
            Log.w(TAG.LAWMO, "phone is in mass storage state, do not execute factory reset");
            return new LawmoOperationResult(new LawmoResultCode(LawmoResultCode.WIPE_DATA_NOT_PERFORMED));
        }

        try {
            mService.getLawmoManager().setPendingAction(LawmoAction.FACTORY_RESET_EXECUTED);
            // FIXME Dirty Hack!!! Rewrite LAWMO to add actions.
            DmOperationManagerFactory.getInstance().current().setProperty(KEY.LAWMO_TAG, true);
            mDmAgent.setWipeFlag();
            File wipeFile = new File(DmConst.Path.getPathInData(mService, DmConst.Path.WIPE_FILE));
            wipeFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
            Log.i(TAG.LAWMO, "-executeFactoryReset()");
            return new LawmoOperationResult(new LawmoResultCode(LawmoResultCode.WIPE_DATA_FAILED));
        } catch (RemoteException e) {
            e.printStackTrace();
            Log.i(TAG.LAWMO, "-executeFactoryReset()");
            return new LawmoOperationResult(new LawmoResultCode(LawmoResultCode.WIPE_DATA_FAILED));
        }
        
        Log.i(TAG.LAWMO, "-executeFactoryReset()");
        return new LawmoOperationResult(new LawmoResultCode(200));
    }

    public LawmoOperationResult executeFullyLock() {
        // TODO Execute fully lock
        Log.i(TAG.LAWMO, "Execute fully lock");
        return new LawmoOperationResult(new LawmoResultCode(DmConst.LawmoResult.OPERATION_SUCCESSSFUL));
    }

    public LawmoOperationResult executePartiallyLock() {
        Log.i(TAG.LAWMO, "Execute partially lock");
        String lawmoUri = "./LAWMO/State";

        try {
            if (mDmAgent == null) {
                Log.w(TAG.LAWMO, "executePartiallyLock mDmAgent is null");
                return new LawmoOperationResult(
                        new LawmoResultCode(LawmoResultCode.PARTIALLY_LOCK_FAILED));
            }
            mDmAgent.setLockFlag("partially".getBytes());
            Log.i(TAG.LAWMO, "executePartiallyLock partially flag has been set");
            // we use the same intent as redbend solution
            mService.sendBroadcast(new Intent("com.mediatek.dm.LAWMO_LOCK"));
            Log.i(TAG.LAWMO,
                    "executePartiallyLock Intent : com.mediatek.dm.LAWMO_LOCK broadcasted.");

            Log.i(TAG.LAWMO, "isRestartAndroid = " + isRestartAndroid());
            if (isRestartAndroid()) {
                Thread.sleep(1000);
                mDmAgent.restartAndroid();
                Log.i(TAG.LAWMO, "executePartiallyLock restart android");
            }
            // return new LawmoOperationResult(
            // new LawmoResultCode(LawmoResultCode.OPERATION_SUCCESSSFUL));
            Log.i(TAG.LAWMO, "Lock 200");

            mDmTree.replaceIntValue(lawmoUri, 20);
            mDmTree.writeToPersistentStorage();
            Log.i(TAG.LAWMO,
                    "After write status, the lawmo staus is " + mDmTree.getIntValue(lawmoUri));

            return new LawmoOperationResult(new LawmoResultCode(200));
        } catch (RemoteException e) {
            Log.e(TAG.LAWMO, e.getMessage());
            return new LawmoOperationResult(
                    new LawmoResultCode(LawmoResultCode.PARTIALLY_LOCK_FAILED));
        } catch (MdmException e) {
            Log.e(TAG.LAWMO, e.getMessage());
            return new LawmoOperationResult(
                    new LawmoResultCode(LawmoResultCode.PARTIALLY_LOCK_FAILED));
        } catch (InterruptedException e) {
            Log.e(TAG.LAWMO, e.getMessage());
            return new LawmoOperationResult(
                    new LawmoResultCode(LawmoResultCode.PARTIALLY_LOCK_FAILED));
        }
    }

    public LawmoOperationResult executeUnLock() {
        Log.i(TAG.LAWMO, "Execute unlock command");
        String lawmoUri = "./LAWMO/State";

        try {
            if (mDmAgent == null) {
                Log.w(TAG.LAWMO, "executeUnLock mDmAgent is null");
                return new LawmoOperationResult(new LawmoResultCode(LawmoResultCode.UNLOCK_FAILED));
            }
            mDmAgent.clearLockFlag();
            Log.i(TAG.LAWMO, "executeUnLock flag has been cleared");
            // we use the same intent as redbend solution
            mService.sendBroadcast(new Intent("com.mediatek.dm.LAWMO_UNLOCK"));
            Log.i(TAG.LAWMO,
                    "executeUnLock Intent : com.mediatek.dm.LAWMO_UNLOCK broadcasted.");

            if (isRestartAndroid()) {
                Thread.sleep(500);
                mDmAgent.restartAndroid();
                Log.i(TAG.LAWMO, "executeUnLock restart android");
            }
            // return new LawmoOperationResult(
            // new LawmoResultCode(LawmoResultCode.OPERATION_SUCCESSSFUL));
            Log.i(TAG.LAWMO, "UnLock 200");
            mDmTree.replaceIntValue(lawmoUri, 30);
            mDmTree.writeToPersistentStorage();
            Log.i(TAG.LAWMO,
                    "After write status, the lawmo staus is " + mDmTree.getIntValue(lawmoUri));

            return new LawmoOperationResult(new LawmoResultCode(200));

        } catch (RemoteException e) {
            Log.e(TAG.LAWMO, e.getMessage());
            return new LawmoOperationResult(new LawmoResultCode(LawmoResultCode.UNLOCK_FAILED));
        } catch (MdmException e) {
            Log.e(TAG.LAWMO, e.getMessage());
            return new LawmoOperationResult(new LawmoResultCode(LawmoResultCode.UNLOCK_FAILED));
        } catch (InterruptedException e) {
            Log.e(TAG.LAWMO, e.getMessage());
            return new LawmoOperationResult(
                    new LawmoResultCode(LawmoResultCode.PARTIALLY_LOCK_FAILED));
        }
    }

    public LawmoOperationResult executeWipe(String[] dataToWipe) {
        // TODO Execute wipe command
        Log.i(TAG.LAWMO, "executeWipe Execute wipe command");

        if (dataToWipe.length == 0) {
            return new LawmoOperationResult(
                    new LawmoResultCode(LawmoResultCode.OPERATION_SUCCESSSFUL));
        }
        mService.sendBroadcast(new Intent("com.mediatek.mediatekdm.LAWMO_WIPE"));
        return new LawmoOperationResult(new LawmoResultCode(LawmoResultCode.OPERATION_SUCCESSSFUL));
    }

    private boolean isRestartAndroid() {
        Log.i(TAG.LAWMO, "if restart android when lock and unlock");
        boolean ret = false;
        File configFileInSystem = new File(DmConst.Path.getPathInSystem(DmConst.Path.DM_CONFIG_FILE));
        if (configFileInSystem != null && configFileInSystem.exists()) {
            DmXMLParser xmlParser = new DmXMLParser(configFileInSystem.getAbsolutePath());
            if (xmlParser != null) {
                String ifRestartAndroid = xmlParser
                        .getValByTagName("LockRestart");
                Log.i(TAG.LAWMO, "the restart flag is " + ifRestartAndroid);
                if (ifRestartAndroid != null
                        && ifRestartAndroid.equalsIgnoreCase("yes")) {
                    ret = true;
                }
            }

        }
        return ret;
    }

    private DmService mService;
    private DmAgent mDmAgent;
    private MdmTree mDmTree;
}
