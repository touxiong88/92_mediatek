package com.mediatek.mediatekdm;

public interface ILawmoManager {
    void setPendingAction(int action);
    void clearPendingAction();
    SessionHandler getSessionHandler();
    int queryActions();
    boolean isLawmoInitiator(String initiator);
    void destroy();
}