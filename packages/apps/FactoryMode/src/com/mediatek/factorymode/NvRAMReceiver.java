
package com.mediatek.factorymode;

import java.util.ArrayList;
import java.util.List;
import com.mediatek.factorymode.signal.SnNumber;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageParser.NewPermissionInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.widget.Toast;

public class NvRAMReceiver extends BroadcastReceiver {

    private SharedPreferences mSp = null;
    private static List<String> mSucessList = null;
    String TAG = "NvRAMReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(Utils.RESULT_ACTION)) {
            mSp = context.getSharedPreferences("FactoryMode", Context.MODE_PRIVATE);
            mSucessList = new ArrayList<String>();
            for (int i = 0; i < FactoryMode.itemString.size(); i++) {
                if (AppDefine.FT_SUCCESS.equals(mSp.getString(context.getString(FactoryMode.itemString.get(i)), null))) {
                    mSucessList.add(context.getString(FactoryMode.itemString.get(i)));
                }
            }
            Log.i(TAG, "Sucess="
                    + mSucessList.size()
                    + "="
                    + "total="
                    + context.getSharedPreferences("PassVerification", Context.MODE_PRIVATE)
                            .getInt("TotalItems", 0));
            if (mSucessList.size() >= context.getSharedPreferences("PassVerification",
                    Context.MODE_PRIVATE).getInt("TotalItems", 0)) {
                if (NvRAMAdapter.readNvRAM() != 1) {
                    NvRAMAdapter.writeNvRam(1);
                    Toast.makeText(context,
                            context.getResources().getString(R.string.pass_verification),
                            Toast.LENGTH_LONG).show();
                }
            }
        }
    }

}
