
package com.mediatek.mediatekdm.iohandler.setting;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.mediatek.custom.CustomProperties;
import com.mediatek.mediatekdm.DmConst.TAG;
import com.mediatek.mediatekdm.mdm.MdmException;
import com.mediatek.mediatekdm.mdm.NodeIoHandler;

public class DmModNodeIoHandler implements NodeIoHandler {

    protected Context mContext = null;
    protected Uri mUri = null;
    public static final String URI = "./DevInfo/Mod";

    public DmModNodeIoHandler(Context ctx, Uri treeUri) {

        Log.i(TAG.NODEIOHANDLER, "DmModNodeIoHanlder constructed..");

        this.mContext = ctx;
        this.mUri = treeUri;
    }

    public int read(int offset, byte[] data) throws MdmException {

        if (mUri == null) {
            throw new MdmException("MODEL read URI is null!");
        }

        Log.i(TAG.NODEIOHANDLER, "DmManNodeIoHandler.read Uri:" + mUri + ", offset:" + offset);

        String mod = CustomProperties.getString(CustomProperties.MODULE_DM, CustomProperties.MODEL,
                "MTK");
        byte[] src = mod.getBytes();

        if (data == null) {
            return src.length;
        } else {
            for (int i = 0; i < src.length; i++) {
                data[offset + i] = src[i];
            }
        }

        return src.length;
    }

    public void write(int offset, byte[] data, int totalSize) throws MdmException {
        /* This method is intentionally left blank */
    }

}
