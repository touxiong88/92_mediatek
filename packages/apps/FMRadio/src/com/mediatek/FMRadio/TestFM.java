package com.mediatek.FMRadio;
import android.app.Activity;
import android.content.Intent;
import android.media.AudioManager;
import android.media.AudioSystem;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import java.io.IOException;

public class TestFM extends Activity implements OnClickListener {

    public static final String TAG = "FM radio service";

    private static final int FOR_PROPRIETARY = 5;

    private Button mSuccess = null;

    private Button mFail = null;

    private Button mSearch = null;

    private float mFrequency = 97.1f;

    private MediaPlayer mMP = null;

    private boolean mIsSearch = false;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                mSearch.setEnabled(true);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test_view);

        mSuccess = (Button) findViewById(R.id.success);
        mFail = (Button) findViewById(R.id.fail);
        mSearch = (Button) findViewById(R.id.search);
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mSearch.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mIsSearch) {
                    mIsSearch = true;
                    new Thread() {
                        public void run() {
                            mFrequency = autoSearch(mFrequency, true);
                            FMRadioNative.tune(mFrequency);
                            FMRadioNative.setmute(false);
                            mIsSearch = false;
                        }
                    }.start();
                }
            }
        });

        mSearch.setEnabled(false);
        new Thread() {
            public void run() {
                FMRadioNative.opendev();
                mMP = new MediaPlayer();
                try {
                    mMP.setDataSource("MEDIATEK://MEDIAPLAYER_PLAYERTYPE_FM");
                } catch (IOException ex) {
                    Log.e(TAG, "setDataSource: " + ex);
                    return;
                } catch (IllegalArgumentException ex) {
                    Log.e(TAG, "setDataSource: " + ex);
                    return;
                } catch (IllegalStateException ex) {
                    Log.e(TAG, "setDataSource: " + ex);
                    return;
                }
                mMP.setAudioStreamType(AudioManager.STREAM_MUSIC);
                enableFMAudio(true);
                if (isHeadset()) {
                    setForceUseSpeaker(true);
                }
                FMRadioNative.powerup(mFrequency);
                FMRadioNative.setmute(false);
                mHandler.sendEmptyMessage(1);
            }
        }.start();
    }

    @Override
    public void onClick(View v) {
        if (FMRadioNative.isFMPoweredUp() != 0) {
            enableFMAudio(false);
            FMRadioNative.closedev();
        }
        Intent data = new Intent();
        data.putExtra("result", v == mSuccess);
        setResult(RESULT_OK, data);
        finish();
    }

    private void enableFMAudio(boolean bEnable) {
        if (bEnable) {
            if (mMP.isPlaying()) {
                Log.e(TAG, "Error: FM audio is already enabled.");
            } else {
                try {
                    mMP.prepare();
                } catch (Exception e) {
                    Log.e(TAG, "Exception: Cannot call MediaPlayer prepare.");
                }
                mMP.start();
            }
        } else {
            if (mMP.isPlaying()) {
                mMP.stop();
                mMP.reset();
                mMP.release();
            } else {
                Log.e(TAG, "Error: FM audio is already disabled.");
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (FMRadioNative.isFMPoweredUp() != 0) {
            enableFMAudio(false);
            FMRadioNative.closedev();
        }
        setForceUseSpeaker(false);
        FMRadioNative.stopscan();
    }

    private float autoSearch(float action, boolean isUp) {
        return FMRadioNative.seek(action, isUp);
    }

    private void setForceUseSpeaker(boolean isOn) {
        int forcedUseForMedia = isOn ? AudioSystem.FORCE_SPEAKER : AudioSystem.FORCE_NONE;
        AudioSystem.setForceUse(FOR_PROPRIETARY, forcedUseForMedia);
    }

    private boolean isHeadset() {
        return FMRadioUtils.isWiredHeadsetReallyOn();
    }
}
