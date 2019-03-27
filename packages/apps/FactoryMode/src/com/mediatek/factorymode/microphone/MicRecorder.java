
package com.mediatek.factorymode.microphone;

import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.media.AudioSystem;
import com.mediatek.factorymode.FactoryMode;
import android.widget.LinearLayout;
import java.io.BufferedWriter;
import java.io.FileWriter;

import com.mediatek.factorymode.AppDefine;
import com.mediatek.factorymode.R;
import com.mediatek.factorymode.Utils;
import com.mediatek.factorymode.VUMeter;

public class MicRecorder extends Activity implements OnClickListener {

    private Button mRecord;

    private Button mBtMicOk;

    private Button mBtMicFailed;

    private Button mBtSpkOk;

    private Button mBtSpkFailed;

    private LinearLayout mRgLayout;

    private RadioButton mRbMic1;

    private RadioButton mRbMic2;

    private MediaRecorder mRecorder;

    private MediaPlayer mPlayer;

    boolean mMicClick = false;

    boolean mSpkClick = false;

    private File mDualMicFile;

    VUMeter mVUMeter;

    SharedPreferences mSp;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.micrecorder);
        mSp = getSharedPreferences("FactoryMode", Context.MODE_PRIVATE);
        mRecord = (Button) findViewById(R.id.mic_bt_start);
        mRecord.setOnClickListener(this);
        mBtMicOk = (Button) findViewById(R.id.mic_bt_ok);
        mBtMicOk.setOnClickListener(this);
        mBtMicFailed = (Button) findViewById(R.id.mic_bt_failed);
        mBtMicFailed.setOnClickListener(this);
        mBtSpkOk = (Button) findViewById(R.id.speaker_bt_ok);
        mBtSpkOk.setOnClickListener(this);
        mBtSpkFailed = (Button) findViewById(R.id.speaker_bt_failed);
        mBtSpkFailed.setOnClickListener(this);
        mVUMeter = (VUMeter) findViewById(R.id.uvMeter);
        mRgLayout = (LinearLayout) findViewById(R.id.rg_layout);
        mDualMicFile = new File ("/sys/bus/platform/drivers/fm36_driver/fm36_state");
        mRbMic1 = (RadioButton) findViewById(R.id.mic1);
        mRbMic2 = (RadioButton) findViewById(R.id.mic2);
        mRbMic1.setChecked(true);
        mRbMic1.setEnabled(true);
        mRbMic2.setEnabled(true);
        if (FactoryMode.mHavaDualMic) {
            mRgLayout.setVisibility(View.VISIBLE);
        } else {
            mRgLayout.setVisibility(View.GONE);
        }
    }

    protected void onDestroy() {
        super.onDestroy();
        File file = new File("/sdcard/test.mp3");
        file.delete();
        if (mPlayer != null) {
            mPlayer.stop();
        }
        if (mRecorder != null) {
            mRecorder.stop();
        }
    }

    Handler h = new Handler();

    Runnable ra = new Runnable() {
        @Override
        public void run() {
            mVUMeter.invalidate();
            h.postDelayed(this, 100);
        }
    };

    private void start() {
        h.post(ra);
        if (mPlayer != null) {
            mPlayer.stop();
        }
        String sDcString = Environment.getExternalStorageState();
        if (!sDcString.equals(Environment.MEDIA_MOUNTED)) {
            mRecord.setText(R.string.sdcard_tips_failed);
            return;
        }
        try {
            mRecorder = new MediaRecorder();
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mRecorder.setAudioEncoder(3);
            mVUMeter.setRecorder(mRecorder);
            String path = Environment.getExternalStorageDirectory() + File.separator + "test.mp3";
            if (!new File(path).exists())
                new File(path).createNewFile();
            mRecorder.setOutputFile(path);
            mRecorder.prepare();
            mRecorder.start();
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT);
        }
        mRecord.setTag("ing");
        mRecord.setText(R.string.Mic_stop);
    }

    private void stopAndSave() {
        h.removeCallbacks(ra);
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
        mRecord.setText(R.string.Mic_start);
        mRecord.setTag("");
        mVUMeter.SetCurrentAngle(0);
        try {
            mPlayer = new MediaPlayer();
            mPlayer.setDataSource("/sdcard/test.mp3");
            mPlayer.prepare();
            mPlayer.start();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void isFinish(){
        if(mMicClick == true && mSpkClick == true){
            finish();
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == mRecord.getId()) {
            if (mRecord.getTag() == null || !mRecord.getTag().equals("ing")) {
                if (FactoryMode.mHavaDualMic) {
                    if (mRbMic2.isChecked()) {
                        dualMicWriter(mDualMicFile, "5");
                    } else {
                        dualMicWriter(mDualMicFile, "1");
                    }
                }
                mRbMic1.setEnabled(false);
                mRbMic2.setEnabled(false);
                start();
            } else {
                mRbMic1.setEnabled(true);
                mRbMic2.setEnabled(true);
                stopAndSave();
            }
        }
        if (v.getId() == mBtMicOk.getId()) {
            mMicClick = true;
            mBtMicFailed.setBackgroundColor(this.getResources().getColor(R.color.gray));
            mBtMicOk.setBackgroundColor(this.getResources().getColor(R.color.Green));
            Utils.SetPreferences(this, mSp, R.string.microphone_name, AppDefine.FT_SUCCESS);
        } else if (v.getId() == mBtMicFailed.getId()) {
            mMicClick = true;
            mBtMicOk.setBackgroundColor(this.getResources().getColor(R.color.gray));
            mBtMicFailed.setBackgroundColor(this.getResources().getColor(R.color.Red));
            Utils.SetPreferences(this, mSp, R.string.microphone_name, AppDefine.FT_FAILED);
        }
        if (v.getId() == mBtSpkOk.getId()) {
            mSpkClick = true;
            mBtSpkFailed.setBackgroundColor(this.getResources().getColor(R.color.gray));
            mBtSpkOk.setBackgroundColor(this.getResources().getColor(R.color.Green));
            Utils.SetPreferences(this, mSp, R.string.speaker_name, AppDefine.FT_SUCCESS);
        } else if (v.getId() == mBtSpkFailed.getId()) {
            mSpkClick = true;
            mBtSpkOk.setBackgroundColor(this.getResources().getColor(R.color.gray));
            mBtSpkFailed.setBackgroundColor(this.getResources().getColor(R.color.Red));
            Utils.SetPreferences(this, mSp, R.string.speaker_name, AppDefine.FT_FAILED);
        }
        isFinish();
    }

    public static boolean dualMicWriter(File file, String writeValue) {
        boolean writeFlag = true;
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(file));
            bw.write(writeValue);
            bw.flush();
        } catch (Exception e) {
            writeFlag = false;
            e.printStackTrace();
        } finally {
            if (null != bw) {
                try {
                    bw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                bw = null;
            }
        }
        return writeFlag;
    }
}
