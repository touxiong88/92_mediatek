
package com.mediatek.mediatekdm.mdm;

public interface BootMsgHandler {
    int ADDR_TYPE_HTTP = 1;
    int ADDR_TYPE_WSP = 2;
    int ADDR_TYPE_OBEX = 3;

    void getPin() throws MdmException;

    int getNss(byte[] buffer) throws MdmException;

    int getAddrType(String addr) throws MdmException;
}
