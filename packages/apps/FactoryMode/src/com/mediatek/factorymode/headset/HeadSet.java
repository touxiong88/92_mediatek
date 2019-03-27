
package com.mediatek.factorymode.headset;

import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.mediatek.factorymode.AppDefine;
import com.mediatek.factorymode.R;
import com.mediatek.factorymode.Utils;
import com.mediatek.factorymode.VUMeter;

public class HeadSet extends Activity implements OnClickListener {

    private Button mRecord;

    private Button mBtOk;

    private Button mBtFailed;

    private MediaRecorder mRecorder = null;

    private MediaPlayer mPlayer = null;

    boolean mMicClick = false;

    boolean mSpkClick = false;

    VUMeter mVUMeter;

    SharedPreferences mSp;

    private final static int STATE_HEADSET_PLUG = 0;

    private final static int STATE_HEADSET_UNPLUG = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.headset);
        mSp = getSharedPreferences("FactoryMode", Context.MODE_PRIVATE);
        mRecord = (Button) findViewById(R.id.mic_bt_start);
        mRecord.setOnClickListener(this);
        mRecord.setEnabled(false);
        mBtOk = (Button) findViewById(R.id.bt_ok);
        mBtOk.setOnClickListener(this);
        mBtFailed = (Button) findViewById(R.id.bt_failed);
        mBtFailed.setOnClickListener(this);
        mVUMeter = (VUMeter) findViewById(R.id.uvMeter);

        IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY - 1);
        registerReceiver(mReceiver, filter);
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
        unregisterReceiver(mReceiver);
        h.removeCallbacks(ra);
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
        mRecord.setText(R.string.Mic_start);
        mRecord.setTag("");
        mVUMeter.SetCurrentAngle(0);
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
        try {
            mPlayer = new MediaPlayer();
            mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
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

    @Override
    public void onClick(View v) {
        if (v.getId() == mRecord.getId()) {
            if (mRecord.getTag() == null || !mRecord.getTag().equals("ing")) {
                start();
            } else {
                stopAndSave();
            }
        }
        if (v.getId() == mBtOk.getId()) {
            Utils.SetPreferences(this, mSp, R.string.headset_name, AppDefine.FT_SUCCESS);
            finish();
        } else if (v.getId() == mBtFailed.getId()) {
            Utils.SetPreferences(this, mSp, R.string.headset_name, AppDefine.FT_FAILED);
            finish();
        }
    }

    Handler myHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case STATE_HEADSET_PLUG:
                    mBtOk.setEnabled(true);
                    mRecord.setText(R.string.Mic_start);
                    mRecord.setEnabled(true);
                    break;
                case STATE_HEADSET_UNPLUG:
                    if (mRecorder != null) {
                        mRecorder.stop();
                        mRecord.setTag("");
                        mRecorder = null;
                    }
                    mVUMeter.SetCurrentAngle(0);
                    if (mPlayer != null) {
                        mPlayer.stop();
                        mPlayer.release();
                        mPlayer = null;
                    }
                    mRecord.setText(R.string.HeadSet_tips);
                    mRecord.setEnabled(false);
                    break;
            }
        }
    };

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_HEADSET_PLUG.equals(action)) {
                if (intent.getIntExtra("state", 0) == 1) {
                    myHandler.sendEmptyMessage(STATE_HEADSET_PLUG);
                } else {
                    myHandler.sendEmptyMessage(STATE_HEADSET_UNPLUG);
                }
            }
        }
    };
}
