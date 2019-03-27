
package com.mediatek.mediatekdm.mdm.lawmo;

import android.util.Log;

import com.mediatek.mediatekdm.DmConst;
import com.mediatek.mediatekdm.mdm.MdmEngine;
import com.mediatek.mediatekdm.mdm.MdmException;
import com.mediatek.mediatekdm.mdm.MdmException.MdmError;
import com.mediatek.mediatekdm.mdm.MdmTree;
import com.mediatek.mediatekdm.mdm.SimpleSessionInitiator;

public class MdmLawmo {

    static final class AlertType {
        public static final String OPERATION_COMPLETE = "urn:oma:at:lawmo:1.0:OperationComplete";
    }

    static final class Registry {
        public static final String EXEC_CORRELATOR = "mdm.lawmo.exec.correlator";
        public static final String EXEC_URI = "mdm.lawmo.exec.uri";
        public static final String EXEC_ACCOUNT = "mdm.scomo.lawmo.account";
    }

    static final class Uri {
        public static final String STATE = "State";
        public static final String AVAILABLEWIPELIST = "AvailableWipeList";
        public static final String LAWMOCONFIG = "LAWMOConfig";
        public static final String OPERATIONS = "Operations";
        public static final String NOTIFYUSER = "NotifyUser";
        public static final String FULLYLOCK = "FullyLock";
        public static final String PARTIALLYLOCK = "PartiallyLock";
        public static final String UNLOCK = "Unlock";
        public static final String FACTORYRESET = "FactoryReset";
        public static final String WIPE = "Wipe";
    }

    static final String ROOT_URI_TYPE = "urn:oma:mo:oma-lawmo:1.0";

    public static final String SESSION_INITIATOR_PREFIX = "MDM_LAWMO";
    public static final String SESSION_INITIATOR_REPORT = SESSION_INITIATOR_PREFIX + "|REPORT";

    public static final String SESSION_ACTION_KEY = "LAWMO";

    private final String mRootUri;
    private LawmoHandler mHandler;
    private MdmTree mTree;

    private MdmEngine mEngine;

    private static final String STATE = "/State";
    private static final String NOTIFY = "/LAWMOConfig/NotifyUser";
    private static final String OPERATION = "/Operations/";
    private static final String WIPELIST = "/AvailableWipeList";
    private static final String LIST_ITEM = WIPELIST + "/ListItemName";
    private static final String TO_BE_WIPED = WIPELIST + "/ToBeWiped";

    private String[] mOpProj = {
            "FullyLock", "PartiallyLock", "UnLock", "FactoryReset", "Wipe"
    };

    // TODO
    public MdmLawmo(String lawmoRootURI, LawmoHandler handler) {
        Log.w(DmConst.TAG.LAWMO, "the lawmo root uri is : " + lawmoRootURI);

        if (lawmoRootURI.charAt(lawmoRootURI.length() - 1) == '/') {
            mRootUri = lawmoRootURI.substring(0, lawmoRootURI.length() - 1);
        } else {
            mRootUri = lawmoRootURI;
        }
        mHandler = handler;
        mTree = new MdmTree();
        mEngine = MdmEngine.getInstance();

        registerLawmohandler();
    }

    public void registerLawmohandler() {
        Log.i(DmConst.TAG.LAWMO, "registerLawmohandler..");
        Log.e(DmConst.TAG.LAWMO, "................registerLawmohandler..");

        String uri;
        try {
            for (int i = 0; i < mOpProj.length; i++) {
                uri = mRootUri + OPERATION + mOpProj[i];
                Log.w(DmConst.TAG.LAWMO, "will register handler to uri: " + uri);
                registerLawmoHandler(uri, mHandler, i);
            }
        } catch (MdmException e) {
            e.printStackTrace();
        }
    }

    public void destroy() {
        Log.i(DmConst.TAG.LAWMO, "destroy lawmo..");
        String uri;
        try {
            for (int i = 0; i < mOpProj.length; i++) {
                uri = mRootUri + OPERATION + mOpProj[i];
                Log.w(DmConst.TAG.LAWMO, "will unregister handler of uri: " + uri);
                unregisterLawmoHandler(uri);
            }
        } catch (MdmException e) {
            e.printStackTrace();
        }
    }

    public boolean getNotifyUser() throws MdmException {
        return mTree.getBoolValue(MdmTree.makeUri(mRootUri, Uri.LAWMOCONFIG, Uri.NOTIFYUSER));
    }

    public LawmoState getState() throws MdmException {
        int value = mTree.getIntValue(MdmTree.makeUri(mRootUri, Uri.STATE));
        return LawmoState.fromInt(value);
    }

    public void triggerReportSession(LawmoResultCode resultCode)
            throws MdmException {
        Log.i(DmConst.TAG.LAWMO, "triggerReportSession..");

        MdmEngine engine = MdmEngine.getInstance();
        String uri = engine.getPLRegistry().getStringValue(Registry.EXEC_URI);
        String account = engine.getPLRegistry().getStringValue(Registry.EXEC_ACCOUNT);
        String correlator = engine.getPLRegistry().getStringValue(Registry.EXEC_CORRELATOR);
        engine.triggerReportSession(uri,
                resultCode.mResultCode,
                account,
                AlertType.OPERATION_COMPLETE,
                correlator,
                new SimpleSessionInitiator(SESSION_INITIATOR_REPORT));
    }

    public void deleteFromAvailableWipeList(String listItemName) throws MdmException {
        Log.i(DmConst.TAG.LAWMO, "deleteFromAvailableWipeList..list item name: " + listItemName);
    }

    public void setAvailableWipeList(String listItemName, boolean isToBeWiped) throws MdmException {
        Log.i(DmConst.TAG.LAWMO, "setAvailableWipeList..list item name:" + listItemName + ", wiped: " + isToBeWiped);
    }

    public int querySessionActions() {
        return mEngine.getSessionActions(SESSION_ACTION_KEY) & LawmoAction.ALL;
    }

    /* register handler */

    private void registerLawmoHandler(String nodeUri, LawmoHandler handler, int i) throws MdmException {
        if (!registerLawmoHandlerN(nodeUri, handler, i)) {
            throw new MdmException(MdmError.INTERNAL);
        }
    }

    private void unregisterLawmoHandler(String nodeUri) throws MdmException {
        if (!unregisterLawmoHandlerN(nodeUri)) {
            throw new MdmException(MdmError.INTERNAL);
        }
    }

    private native boolean registerLawmoHandlerN(String nodeUri, LawmoHandler handler, int i);

    private native boolean unregisterLawmoHandlerN(String nodeUri);
}
