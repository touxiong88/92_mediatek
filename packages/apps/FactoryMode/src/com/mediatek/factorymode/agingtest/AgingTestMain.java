
package com.mediatek.factorymode.agingtest;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.mediatek.factorymode.AppDefine;
import com.mediatek.factorymode.R;
import com.mediatek.factorymode.Utils;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;


public class AgingTestMain extends Activity {

    private TextView mTesting = null;
    private MediaPlayer mPlayer = null;
    private android.os.Vibrator mVibrator = null;
    private long[] pattern = {
        0, 1000
    };
    private int mNum = 0;
    private Timer timer = null;
    private AudioManager mAudioManager;
    private MediaRecorder mRecorder;

    private Camera mCamera = null;
    private Camera.Parameters mParams;

    private final int CHANGE_COLOR = 0;
    private final int RECORD_START = 1;
    private final int RECORD_STOP = 2;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.agingtest_main);

        mTesting = (TextView) findViewById(R.id.testing);

        Intent intent = getIntent();
        boolean testLcd = intent.getBooleanExtra("lcd", false);
        boolean testSpeaker = intent.getBooleanExtra("speaker", false);
        boolean testVibrator = intent.getBooleanExtra("vibrator", false);
        boolean testMic = intent.getBooleanExtra("mic", false);
        boolean testEarphone = intent.getBooleanExtra("earphone", false);
        boolean testFlashlight = intent.getBooleanExtra("flashlight", false);
        if (testLcd || testVibrator) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        if (testLcd) {
            timer = new Timer();
            initView();
        }

        if (testVibrator) {
            mVibrator = (android.os.Vibrator) getSystemService(VIBRATOR_SERVICE);
            mVibrator.vibrate(pattern, 0);
        }

        if (testMic) {
            myHandler.sendEmptyMessageDelayed(RECORD_START, 1000);
            return;
        }
        mAudioManager = (AudioManager) this.getSystemService(AUDIO_SERVICE);
        mAudioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                AudioManager.FLAG_PLAY_SOUND);

        if (testSpeaker) {
            initMediaPlayer();
        } else if (testEarphone) {
            mAudioManager.setSpeakerphoneOn(false);
            setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
            mAudioManager.setMode(AudioManager.MODE_IN_CALL);

            TimerTask task = new TimerTask(){
                public void run() {
                    initMediaPlayer();
                }};
            super.onResume();
            Timer message_timer=new Timer();
            message_timer.schedule(task,300);
        }

        if (testFlashlight) {
            if (mCamera == null) {
                mCamera = Camera.open();
            }
            mParams = mCamera.getParameters();
            mParams.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            mCamera.setParameters(mParams);
        }

    }

    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) {
            timer.cancel();
        }
        if (mPlayer != null) {
            mPlayer.stop();
        }
        if (mAudioManager != null) {
            mAudioManager.setMode(AudioManager.MODE_NORMAL);
        }
        if (mVibrator != null) {
            mVibrator.cancel();
        }
        if (mRecorder != null) {
            stopAndDelete();
        }
        myHandler.removeMessages(RECORD_STOP);
        myHandler.removeMessages(RECORD_START);
        if (mCamera != null) {
            mParams = mCamera.getParameters();
            mParams.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            mCamera.setParameters(mParams);
            mCamera.release();
            mCamera = null;
        }
    }

    Handler myHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case CHANGE_COLOR:
                    mNum++;
                    changeColor(mNum);
                    break;
                case RECORD_START:
                    startRecord();
                    sendEmptyMessageDelayed(RECORD_STOP, 60000);
                    break;
                case RECORD_STOP:
                    stopAndDelete();
                    sendEmptyMessageDelayed(RECORD_START, 1000);
                    break;
                default:
                    break;
            }
        }
    };

    private void initView() {

        timer.schedule(new TimerTask() {
            public void run() {
                Message msg = new Message();
                msg.what = CHANGE_COLOR;
                myHandler.sendMessage(msg);
            }
        }, 1000, 1000);
    }

    private void changeColor(int num) {
        switch (num % 4) {
            case 0:
                mTesting.setBackgroundColor(Color.RED);
                break;
            case 1:
                mTesting.setBackgroundColor(Color.GREEN);
                break;
            case 2:
                mTesting.setBackgroundColor(Color.BLUE);
                break;
            case 3:
                mTesting.setBackgroundColor(Color.WHITE);
                break;
        }
    }

    private void initMediaPlayer() {
        mPlayer = MediaPlayer.create(getApplicationContext(), R.raw.tada);
        mPlayer.setLooping(true);
        try {
            mPlayer.prepare();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mPlayer.start();
    }

    private void startRecord() {
        try {
            mRecorder = new MediaRecorder();
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mRecorder.setAudioEncoder(3);
            String path = Environment.getExternalStorageDirectory() + File.separator + "test.mp3";
            if (!new File(path).exists())
                new File(path).createNewFile();
            mRecorder.setOutputFile(path);
            mRecorder.prepare();
            mRecorder.start();
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT);
        }
    }

    private void stopAndDelete() {
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;

        File file = new File("/sdcard/test.mp3");
        file.delete();
    }

}
