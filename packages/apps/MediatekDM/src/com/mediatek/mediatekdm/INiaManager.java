package com.mediatek.mediatekdm;

import com.mediatek.mediatekdm.mdm.NIAMsgHandler;
import com.mediatek.mediatekdm.mdm.SessionInitiator;

public interface INiaManager extends NIAMsgHandler, SessionInitiator {
    String INITIATOR = "Network Inited";
    int DEFAULT_NOTIFICATION_INTERACT_TIMEOUT = 10 * 60;
    int DEFAULT_NOTIFICATION_VISIBLE_TIMEOUT = 10;
}