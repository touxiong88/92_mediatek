package com.mediatek.mediatekdm;

import android.os.Message;

import com.mediatek.mediatekdm.DmController.DmAction;
import com.mediatek.mediatekdm.DmService.IServiceMessage;
import com.mediatek.mediatekdm.mdm.MdmException;
import com.mediatek.mediatekdm.mdm.MdmException.MdmError;
import com.mediatek.mediatekdm.mdm.SessionInitiator;

class NiaSessionManager extends SessionHandler implements INiaManager {
    NiaSessionManager(DmService service) {
        super(service);
    }

    @Override
    protected void dmComplete(DmAction action) {
        super.dmComplete(action);
        Message msg = mService.mServiceHandler.obtainMessage(IServiceMessage.MSG_OPERATION_PROCESS_NEXT);
        mService.mServiceHandler.sendMessage(msg);
    }

    @Override
    protected void dmAbort(int lastError) {
        super.dmAbort(lastError);
        if (lastError != MdmError.COMMS_SOCKET_ERROR.val) {
            Message msg = mService.mServiceHandler.obtainMessage(IServiceMessage.MSG_OPERATION_PROCESS_NEXT);
            mService.mServiceHandler.sendMessage(msg);
        }
    }

    @Override
    public void notify(UIMode uiMode, short dmVersion, byte[] vendorSpecificData, SessionInitiator initiator)
            throws MdmException {
        DmService.sDmController.proceedNiaSession();
    }

    @Override
    public String getId() {
        return INITIATOR;
    }
}