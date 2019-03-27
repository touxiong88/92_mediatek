
package com.mediatek.factorymode.signal;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CallLog.Calls;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;

import com.mediatek.factorymode.AppDefine;
import com.mediatek.factorymode.R;
import com.mediatek.factorymode.Utils;

public class Signal extends Activity implements OnClickListener {
    private Button mBtOk;

    private Button mBtFailed;

    SharedPreferences mSp;

    Dialog mSwitchDialog = null;
/* Vanzo:huanghua on: Thu, 14 Jun 2012 23:56:53 +0800
 */
    private boolean mIsNowCall = false;
// End of Vanzo: huanghua

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signal);
        mSp = getSharedPreferences("FactoryMode", Context.MODE_PRIVATE);
        mBtOk = (Button) findViewById(R.id.signal_bt_ok);
        mBtOk.setOnClickListener(this);
        mBtFailed = (Button) findViewById(R.id.signal_bt_failed);
        mBtFailed.setOnClickListener(this);
        Intent intent = new Intent(Intent.ACTION_CALL_PRIVILEGED, Uri.fromParts("tel", "112", null));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mSwitchDialog = createDialog();
/* Vanzo:huanghua on: Thu, 14 Jun 2012 23:57:06 +0800
        mSwitchDialog.show();
 */
        mIsNowCall = true;
// End of Vanzo:huanghua
        startActivity(intent);
    }

/* Vanzo:huanghua on: Thu, 14 Jun 2012 23:57:21 +0800
 */
    @Override
    protected void onRestart() {
        super.onRestart();
        if (mSwitchDialog != null && mIsNowCall) {
            mSwitchDialog.show();
            mIsNowCall = false;
        }
    }
// End of Vanzo: huanghua
    public void onClick(View v) {
        Utils.SetPreferences(this, mSp, R.string.telephone_name,
                (v.getId() == mBtOk.getId()) ? AppDefine.FT_SUCCESS : AppDefine.FT_FAILED);
        deleteNum();
        finish();
    };

    private AlertDialog createDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(Signal.this);
        builder.setCancelable(true);
        builder.setInverseBackgroundForced(true);
        builder.setTitle(R.string.FMRadio_notice);
/* Vanzo:zhangjingzhi on: Wed, 16 Apr 2014 09:42:35 +0800
 * set cancelable false
 */
        builder.setCancelable(false);
// End of Vanzo:zhangjingzhi
        builder.setMessage(R.string.HeadSet_hook_message);
        builder.setPositiveButton(R.string.Success, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                Utils.SetPreferences(getApplicationContext(), mSp, R.string.headsethook_name,
                        AppDefine.FT_SUCCESS);
            }
        });
        builder.setNegativeButton(R.string.Failed, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                Utils.SetPreferences(getApplicationContext(), mSp, R.string.headsethook_name,
                        AppDefine.FT_FAILED);
            }
        });
        AlertDialog alertDialog = builder.create();
/* Vanzo:zhangjingzhi on: Wed, 16 Apr 2014 09:42:30 +0800
 * #75677 remove type
        alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
 */
// End of Vanzo: zhangjingzhi
        return alertDialog;
    }

    private void deleteNum() {
        String where = null;
        where = Calls.NUMBER + "='112'";
        getContentResolver().delete(Calls.CONTENT_URI, where, null);
    }
}
