package com.mediatek.factorymode.agingtest;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import com.mediatek.factorymode.R;

public class AgingTestBegin extends Activity {

    private CheckBox mCheckBoxLcd;
    private CheckBox mCheckBoxSpeaker;
    private CheckBox mCheckBoxVibrator;
    private CheckBox mCheckBoxMic;
    private CheckBox mCheckBoxEarphone;
    private CheckBox mCheckBoxFlashlight;
    private Button mBegintest;

    @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.agingtest_begin);
            mCheckBoxLcd = (CheckBox)findViewById(R.id.checkBoxLcd);
            mCheckBoxSpeaker = (CheckBox)findViewById(R.id.checkBoxSpeaker);
            mCheckBoxSpeaker.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            mCheckBoxMic.setChecked(false);
                        }
                    }
                });
            mCheckBoxVibrator = (CheckBox)findViewById(R.id.checkBoxVibrator);
            mCheckBoxEarphone = (CheckBox)findViewById(R.id.checkBoxEarphone);
            mCheckBoxEarphone.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            mCheckBoxMic.setChecked(false);
                        }
                    }
                });
            mCheckBoxMic = (CheckBox)findViewById(R.id.checkBoxMic);
            mCheckBoxMic.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            mCheckBoxEarphone.setChecked(false);
                            mCheckBoxSpeaker.setChecked(false);
                        }
                    }
                });
            mCheckBoxFlashlight = (CheckBox)findViewById(R.id.checkBoxFlashlight);

            mBegintest = (Button)findViewById(R.id.begintest);
            mBegintest.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent();
                        if (mCheckBoxLcd.isChecked() || mCheckBoxSpeaker.isChecked() || mCheckBoxVibrator.isChecked()
                                || mCheckBoxEarphone.isChecked() || mCheckBoxMic.isChecked() || mCheckBoxFlashlight.isChecked()) {

                            intent.putExtra("lcd", mCheckBoxLcd.isChecked());
                            intent.putExtra("speaker", mCheckBoxSpeaker.isChecked());
                            intent.putExtra("vibrator", mCheckBoxVibrator.isChecked());
                            intent.putExtra("mic", mCheckBoxMic.isChecked());
                            intent.putExtra("earphone", mCheckBoxEarphone.isChecked());
                            intent.putExtra("flashlight", mCheckBoxFlashlight.isChecked());

                            intent.setClassName("com.mediatek.factorymode", "com.mediatek.factorymode.agingtest.AgingTestMain");
                            startActivity(intent);
                        }
                    }
                });
        }

}
