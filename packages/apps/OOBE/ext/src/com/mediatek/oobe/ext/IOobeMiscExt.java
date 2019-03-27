package com.mediatek.oobe.ext;

public interface IOobeMiscExt {

    /**
    * Customize strings which contains 'SIM', replace 'SIM' by 'UIM/SIM','UIM','card' etc.
    * @param simString : the strings which contains SIM
    * @param soltId : 1 , slot1 0, slot0 , -1 means always.
    */
    String customizeSimDisplayString(String simString, int slotId);
}
