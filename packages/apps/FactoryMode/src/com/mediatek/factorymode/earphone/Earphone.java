
package com.mediatek.factorymode.earphone;

import java.io.IOException;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import java.util.Timer;
import java.util.TimerTask;
import com.mediatek.factorymode.AppDefine;
import com.mediatek.factorymode.R;
import com.mediatek.factorymode.Utils;

public class Earphone extends Activity implements OnClickListener {
    private SharedPreferences mSp;

    private MediaPlayer mPlayer;

    private Button mBtOk;

    private Button mBtFailed;

    private AudioManager mAudioManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.audio_test);
        mSp = getSharedPreferences("FactoryMode", Context.MODE_PRIVATE);
        mBtOk = (Button) findViewById(R.id.audio_bt_ok);
        mBtOk.setOnClickListener(this);
        mBtFailed = (Button) findViewById(R.id.audio_bt_failed);
        mBtFailed.setOnClickListener(this);
        mAudioManager = (AudioManager) this.getSystemService(AUDIO_SERVICE);
        mAudioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
        mAudioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL,
        mAudioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL),
                       AudioManager.FLAG_PLAY_SOUND);
        setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
        mAudioManager.setMode(AudioManager.MODE_IN_CALL);
    }
    protected void onResume() {
        TimerTask task = new TimerTask(){
            public void run() {
                initMediaPlayer();
            }};
        super.onResume();
        Timer message_timer=new Timer();
        message_timer.schedule(task,300);
    }

    protected void onPause() {
        super.onPause();
        if (mPlayer != null)
            mPlayer.stop();
    }

    protected void onDestroy() {
        if (mPlayer != null) {
            mPlayer.stop();
        }
        mAudioManager.setMode(AudioManager.MODE_NORMAL);
        super.onDestroy();
    }

    private void initMediaPlayer() {
        mPlayer =  new MediaPlayer();
        mPlayer.setAudioStreamType(AudioManager.STREAM_VOICE_CALL);
        String fileName = "android.resource://" + getPackageName() + "/" + R.raw.tada;
        try {
            mPlayer.setDataSource(this, Uri.parse(fileName));
            mPlayer.setLooping(true);
            mPlayer.prepare();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mPlayer.start();
    }

    public void onClick(View v) {
        Utils.SetPreferences(this, mSp, R.string.earphone_name,
                (v.getId() == mBtOk.getId()) ? AppDefine.FT_SUCCESS : AppDefine.FT_FAILED);
        finish();
    }
}
