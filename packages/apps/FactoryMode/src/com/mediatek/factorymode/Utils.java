
package com.mediatek.factorymode;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class Utils{

    public static final String RESULT_ACTION = "com.mediatek.factorymode.writeNvRAM";
    public static final String ACTION_FACTORYMODE_PASS_VERIFICATION = "com.android.phone.FACTORYMODE_PASS_VERIFICATION";

    public static void SetPreferences(Context context, SharedPreferences sp, int name, String flag) {
        String nameStr = context.getResources().getString(name);
        Editor editor = sp.edit();
        editor.putString(nameStr, flag);
        editor.commit();
        context.sendBroadcast(new Intent(RESULT_ACTION));
    }
}
