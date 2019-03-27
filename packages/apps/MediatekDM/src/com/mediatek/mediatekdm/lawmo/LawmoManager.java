package com.mediatek.mediatekdm.lawmo;

import android.content.Intent;
import android.os.RemoteException;
import android.util.Log;

import com.android.internal.os.storage.ExternalStorageFormatter;
import com.mediatek.common.dm.DmAgent;
import com.mediatek.mediatekdm.DmConst;
import com.mediatek.mediatekdm.DmConst.TAG;
import com.mediatek.mediatekdm.DmController.DmAction;
import com.mediatek.mediatekdm.DmService;
import com.mediatek.mediatekdm.ILawmoManager;
import com.mediatek.mediatekdm.SessionHandler;
import com.mediatek.mediatekdm.ext.MTKPhone;
import com.mediatek.mediatekdm.mdm.MdmException;
import com.mediatek.mediatekdm.mdm.MdmTree;
import com.mediatek.mediatekdm.mdm.lawmo.LawmoAction;
import com.mediatek.mediatekdm.mdm.lawmo.MdmLawmo;

public class LawmoManager extends SessionHandler implements ILawmoManager {

    private int mPendingAction = LawmoAction.NONE;
    private MdmLawmo mLawmo;

    public LawmoManager(DmService service) {
        super(service);
        mLawmo = new MdmLawmo(DmConst.NodeUri.LAWMO_ROOT, new DmLawmoHandler(mService));
        syncLawmoStatus();
    }

    @Override
    protected void dmComplete(DmAction action) {
        super.dmComplete(action);
        // If we need to support other actions, revise this.
        if (mPendingAction == LawmoAction.FACTORY_RESET_EXECUTED) {
            clearPendingAction();
            // Erase SD card & Factory reset
            Intent intent = new Intent(ExternalStorageFormatter.FORMAT_AND_FACTORY_RESET);
            intent.setComponent(ExternalStorageFormatter.COMPONENT_NAME);
            intent.putExtra("lawmo_wipe", true);
            mService.startService(intent);
            Log.i(TAG.LAWMO, "Start Service ExternalStorageFormatter.FORMAT_AND_FACTORY_RESET");
        }
    }

    @Override
    public void setPendingAction(int action) {
        mPendingAction = action;
    }

    @Override
    public void clearPendingAction() {
        mPendingAction = LawmoAction.NONE;
    }

    
    @Override
    public SessionHandler getSessionHandler() {
        return this;
    }
    
    @Override
    public int queryActions() {
        return mLawmo.querySessionActions();
    }

    @Override
    public boolean isLawmoInitiator(String initiator) {
        return initiator.startsWith(MdmLawmo.SESSION_INITIATOR_PREFIX);
    }
    
    @Override
    public void destroy() {
        mLawmo.destroy();
    }
    
    private void syncLawmoStatus() {
        boolean isFullyLock = false;
        int lockStatus = -1;
        String lawmoUri = "./LAWMO/State";
        MdmTree tree = new MdmTree();
        
        try {
            DmAgent agent = MTKPhone.getDmAgent();
            if (agent != null) {
                Log.i(TAG.LAWMO, "The device lock status is " + agent.isLockFlagSet());
                if (agent.isLockFlagSet()) {
                    // the status is locked, if it is full lock
                    // isPartillyLock = agent.getLockType();
                    isFullyLock = (agent.getLockType() == 1);
                    Log.i(TAG.LAWMO, "is fully lock is " + isFullyLock);
                    if (!isFullyLock) {
                        lockStatus = DmConst.LawmoStatus.PARTIALY_LOCK;
                    } else {
                        lockStatus = DmConst.LawmoStatus.FULLY_LOCK;
                    }
                    Log.i(TAG.LAWMO, "Lock status is " + lockStatus);
                    if (lockStatus == DmConst.LawmoStatus.FULLY_LOCK
                            || lockStatus == DmConst.LawmoStatus.PARTIALY_LOCK) {
                        int treeLawmoStatus = tree.getIntValue(lawmoUri);
                        Log.i(TAG.LAWMO, "ILawmoManager status in tree is " + treeLawmoStatus);
                        if (lockStatus != treeLawmoStatus) {
                            // need to write DM tree to sync LAWMO status
                            tree.replaceIntValue(lawmoUri, lockStatus);
                            tree.writeToPersistentStorage();
                            Log.i(TAG.LAWMO, "After write status, the lawmo staus is " + tree.getIntValue(lawmoUri));
                        }

                    }
                }
            } else {
                Log.e(TAG.LAWMO, "DmAgent is null");
                return;
            }
        } catch (RemoteException e) {
            Log.e(TAG.LAWMO, "RemoteException:" + e);
            e.printStackTrace();
        } catch (MdmException e) {
            Log.e(TAG.LAWMO, "MdmException:" + e);
            e.printStackTrace();
        }
    }

}