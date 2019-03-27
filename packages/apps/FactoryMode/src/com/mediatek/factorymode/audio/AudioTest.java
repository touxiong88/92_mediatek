
package com.mediatek.factorymode.audio;

import java.io.IOException;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.mediatek.factorymode.AppDefine;
import com.mediatek.factorymode.R;
import com.mediatek.factorymode.Utils;

public class AudioTest extends Activity implements OnClickListener {
    private SharedPreferences mSp;

    private MediaPlayer mPlayer;

    private Button mBtOk;

    private Button mBtFailed;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.audio_test);
        mSp = getSharedPreferences("FactoryMode", Context.MODE_PRIVATE);
        mBtOk = (Button) findViewById(R.id.audio_bt_ok);
        mBtOk.setOnClickListener(this);
        mBtFailed = (Button) findViewById(R.id.audio_bt_failed);
        mBtFailed.setOnClickListener(this);

        AudioManager audioManager = (AudioManager) this.getSystemService(AUDIO_SERVICE);
        audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                AudioManager.FLAG_PLAY_SOUND);
    }

    protected void onResume() {
        super.onResume();
        initMediaPlayer();
    }

    protected void onPause() {
        super.onPause();
        mPlayer.stop();
    }

    protected void onDestroy() {
        super.onDestroy();
        mPlayer.stop();
    }

    private void initMediaPlayer() {
/* Vanzo:suofengfeng on: Wed, 22 Jan 2014 10:27:32 +0800
 * bugfix #64305 change factory mode radio file
        mPlayer = MediaPlayer.create(getApplicationContext(), R.raw.tada);
 */
        mPlayer = MediaPlayer.create(getApplicationContext(), R.raw.speaker);
// End of Vanzo: suofengfeng
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

    public void onClick(View v) {
        Utils.SetPreferences(this, mSp, R.string.speaker_name,
                (v.getId() == mBtOk.getId()) ? AppDefine.FT_SUCCESS : AppDefine.FT_FAILED);
        finish();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            finish();
        }
        return true;
    }
}
