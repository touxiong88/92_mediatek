package com.mediatek.mediatekdm;

public final class DmOperationManagerFactory {
    private static DmOperationManager sInstance;
    
    public static DmOperationManager getInstance() {
        synchronized (DmOperationManagerFactory.class) {
            if (sInstance == null) {
                sInstance = new DmOperationManager(
                        DmConst.Path.getPathInData(DmApplication.getInstance(), DmConst.Path.DM_OPERATION_FOLDER));
            }
            return sInstance;
        }
    }
}
