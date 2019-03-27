package com.mediatek.common.sms;
import android.content.ContentResolver;
import java.util.List;


public interface IDefaultSmsSimSettingsExt {

    public void setSmsTalkDefaultSim(ContentResolver contentResolver,long[] simIdForSlot, int nSIMCount);
    public int getSmsDefaultSim();
}
