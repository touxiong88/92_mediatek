package com.mediatek.mediatekdm;

import android.os.Message;

import com.mediatek.mediatekdm.DmController.DmAction;
import com.mediatek.mediatekdm.DmService.IServiceMessage;
import com.mediatek.mediatekdm.mdm.MdmException.MdmError;
import com.mediatek.mediatekdm.mdm.SessionStateObserver.SessionState;
import com.mediatek.mediatekdm.mdm.SessionStateObserver.SessionType;

/***************** Component management end *********************************/

public abstract class SessionHandler {
    protected final DmService mService;

    /**
     * @param dmService
     */
    public SessionHandler(DmService service) {
        mService = service;
    }

    protected void dmStart() {
        // TODO Use UIResponse info in operation instead.
        if (DmFeatureSwitch.DM_SCOMO && mService.getScomoManager() != null) {
            mService.getScomoManager().setVerbose(false);
        }
    }

    /**
     * The default implementation will clear DmFumoNotification and finish current operation.
     */
    protected void dmComplete(DmAction action) {
        if (mService.mDmNotification != null) {
            mService.mDmNotification.clear();
        }
        mService.mOperationManager.finishCurrent();
    }

    protected void dmAbort(int lastError) {
        DmOperation operation = mService.mOperationManager.current();
        if (lastError == MdmError.COMMS_SOCKET_ERROR.val && operation.getRetry() > 0) {
            mService.mOperationManager.notifyCurrentAborted();
            Message msg = mService.mServiceHandler.obtainMessage(IServiceMessage.MSG_OPERATION_TIME_OUT, operation);
            mService.mServiceHandler.sendMessageDelayed(msg, operation.timeout);
        } else {
            mService.mOperationManager.finishCurrent();
        }
    }

    protected void dlStart() {
        // do nothing
    }

    protected void dlComplete() {
        mService.mOperationManager.finishCurrent();
    }

    protected void dlAbort(int lastError) {
        DmOperation operation = mService.mOperationManager.current();
        if (lastError == MdmError.COMMS_SOCKET_ERROR.val && operation.getRetry() > 0) {
            mService.mOperationManager.notifyCurrentAborted();
            Message msg = mService.mServiceHandler.obtainMessage(IServiceMessage.MSG_OPERATION_TIME_OUT, operation);
            mService.mServiceHandler.sendMessageDelayed(msg, operation.timeout);
        } else {
            mService.mOperationManager.finishCurrent();
        }
    }

    /**
     * Dispatcher method.
     * 
     * @param type
     * @param state
     * @param lastError
     */
    public final void onSessionStateChange(SessionType type, SessionState state, int lastError, DmAction action) {
        if (type == SessionType.DM) {
            if (state == SessionState.STARTED) {
                dmStart();
            } else if (state == SessionState.COMPLETE) {
                dmComplete(action);
            } else if (state == SessionState.ABORTED) {
                dmAbort(lastError);
            }
        } else if (type == SessionType.DL) {
            if (state == SessionState.STARTED) {
                dlStart();
            } else if (state == SessionState.ABORTED) {
                dlAbort(lastError);
            } else if (state == SessionState.COMPLETE) {
                dlComplete();
            }
        }
    }
}