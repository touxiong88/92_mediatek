
package com.mediatek.factorymode.simcard;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.telephony.TelephonyManager;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.gemini.GeminiPhone;
import com.mediatek.factorymode.AppDefine;
import com.mediatek.factorymode.R;
import com.mediatek.factorymode.Utils;
import com.mediatek.common.featureoption.FeatureOption;

public class SimCard extends Activity {

    private GeminiPhone mGeminiPhone;

    private boolean Sim1State = false;

    private boolean Sim2State = false;

    private String mSimStatus = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            mGeminiPhone = (GeminiPhone) PhoneFactory.getDefaultPhone();
            Sim1State = mGeminiPhone.isSimInsert(PhoneConstants.GEMINI_SIM_1)
                    & mGeminiPhone.isRadioOnGemini(PhoneConstants.GEMINI_SIM_1);
            Sim2State = mGeminiPhone.isSimInsert(PhoneConstants.GEMINI_SIM_2)
                    & mGeminiPhone.isRadioOnGemini(PhoneConstants.GEMINI_SIM_2);
            if (Sim1State == true) {
                mSimStatus += getString(R.string.sim1_info_ok) + "\n";
            } else {
                mSimStatus += getString(R.string.sim1_info_failed) + "\n";
            }
            if (Sim2State == true) {
                mSimStatus += getString(R.string.sim2_info_ok) + "\n";
            } else {
                mSimStatus += getString(R.string.sim2_info_failed) + "\n";
            }
        } else {
            Sim1State = TelephonyManager.getDefault().hasIccCard();
            if (Sim1State == true) {
                mSimStatus += getString(R.string.sim_info_ok) + "\n";
            } else {
                mSimStatus += getString(R.string.sim_info_failed) + "\n";
            }
        }
    }

    public void onResume() {
        super.onResume();
        final Intent intent = new Intent();
        AlertDialog.Builder builder = new AlertDialog.Builder(SimCard.this);
        builder.setTitle(R.string.FMRadio_notice);
        builder.setMessage(mSimStatus);
	 builder.setCancelable(false);
        builder.setPositiveButton(R.string.Success, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                intent.putExtra("result", true);
                setResult(RESULT_OK, intent);
                finish();
            }
        });
        builder.setNegativeButton(getResources().getString(R.string.Failed),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        intent.putExtra("result", false);
                        setResult(RESULT_OK, intent);
                        finish();
                    }
                });
        builder.create().show();
    }
}
