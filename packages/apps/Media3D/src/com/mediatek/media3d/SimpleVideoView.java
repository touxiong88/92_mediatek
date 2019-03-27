package com.mediatek.media3d;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.io.IOException;
import java.util.HashMap;

public class SimpleVideoView extends SurfaceView {
    private static final String TAG = "Media3D.SimpleVideoView";

    public static final int INVALID_SEGMENT_ID = -1;
    public static final int REPLAY_SEGMENT_ID = -2;

    // all possible internal states
    private static final int STATE_ERROR = -1;
    private static final int STATE_IDLE = 0;
    private static final int STATE_PREPARING = 1;
    private static final int STATE_PREPARED = 2;
    private static final int STATE_PLAYING = 3;
    private static final int STATE_PAUSED = 4;
    private static final int STATE_PLAYBACK_COMPLETED = 5;

    private Uri mUri;
    private MediaPlayer mPlayer;
    private SurfaceHolder mSurfaceHolder;
    // <Integer, Pair<Integer, Integer>> : <SegmentId, Pair<StartTime, EndTime>>
    private HashMap<Integer, Pair<Integer, Integer>> mSegments;

    private int mSegmentId = INVALID_SEGMENT_ID;
    private int mDuration;
    private int mLoopStartMs;
    private int mLoopEndMs;
    private int mVideoWidth;
    private int mVideoHeight;
    private int mSurfaceWidth;
    private int mSurfaceHeight;
    private int mMeasureWidth;
    private int mMeasureHeight;
    private int mCurrentState = STATE_IDLE;
    private int mIntentState = STATE_IDLE;
    private Handler mLoopHandler;
    private Runnable mLoopRunner;
    private boolean mEnableMusicPause = false;
    private boolean mEnableLooping = false;

    private final MediaPlayer.OnPreparedListener mPreparedListener = new MediaPlayer.OnPreparedListener() {
        public void onPrepared(MediaPlayer mediaPlayer) {
            setCurrentState(STATE_PREPARED);
            notifyPreparedListener(mediaPlayer);
            try {
                mVideoWidth = mediaPlayer.getVideoWidth();
                mVideoHeight = mediaPlayer.getVideoHeight();
                mDuration = mediaPlayer.getDuration();
            } catch (IllegalStateException e) {
                LogUtil.v(TAG, "MediaPlayer object has been released. Exception : " + e);
                return;
            }

            LogUtil.v(TAG, "MediaPlayer : " + mediaPlayer +
                    ", vwidth : " + mVideoWidth + ", vheight : " + mVideoHeight +
                    ", swidth : " + mSurfaceWidth + ", sheight : " + mSurfaceHeight +
                    ", duration : " + mDuration +
                    ", intentState : " + mIntentState + ", currentState : " + mCurrentState);
            // TODO : seek
            if (mVideoWidth == 0 || mVideoHeight == 0) {
                // TODO : report size issue.
                if (mIntentState == STATE_PLAYING) {
                    LogUtil.v(TAG, "video size has problem. Still try to start seg id : " + mSegmentId);
                    start(mSegmentId);
                }
            } else {
                getHolder().setFixedSize(mVideoWidth, mVideoHeight);
                if (mSurfaceWidth == mVideoWidth &&
                        mSurfaceHeight == mVideoHeight) {
                    if (mIntentState == STATE_PLAYING) {
                        LogUtil.v(TAG, "intent to start, seg id : " + mSegmentId);
                        start(mSegmentId);
                    }
                }
            }
        }
    };

    private final MediaPlayer.OnCompletionListener mCompletionListener = new MediaPlayer.OnCompletionListener() {
        public void onCompletion(MediaPlayer mediaPlayer) {
            LogUtil.v(TAG, "MediaPlayer : " + mediaPlayer);
            setAllState(STATE_PLAYBACK_COMPLETED);
            notifyCompletionListener(mediaPlayer);
        }
    };

    private final MediaPlayer.OnVideoSizeChangedListener mSizeChangedListener = new MediaPlayer.OnVideoSizeChangedListener() {
        public void onVideoSizeChanged(MediaPlayer mediaPlayer, int width, int height) {
            LogUtil.v(TAG, "MediaPlayer : " + mediaPlayer + ", width : " + width + ", height : " + height);
            try {
                mVideoWidth = mediaPlayer.getVideoWidth();
                mVideoHeight = mediaPlayer.getVideoHeight();
            } catch (IllegalStateException e) {
                LogUtil.v(TAG, "MediaPlayer object has been released. Exception : " + e);
                return;
            }

            LogUtil.v(TAG, "MediaPlayer : " + mediaPlayer + ", vwidth : " + mVideoWidth + ", vheight : " + mVideoHeight);
            if (mVideoWidth != 0 && mVideoHeight != 0) {
                getHolder().setFixedSize(mVideoWidth, mVideoHeight);
            }
        }
    };

    private final MediaPlayer.OnErrorListener mErrorListener = new MediaPlayer.OnErrorListener() {
        public boolean onError(MediaPlayer mediaPlayer, int frameworkErr, int implErr) {
            LogUtil.v(TAG, "MediaPlayer : " + mediaPlayer + ",Framework Err : " + frameworkErr + ",Implement Err : " + implErr);
            setAllState(STATE_ERROR);
            if (notifyErrorListener(mediaPlayer,frameworkErr,implErr)){
                return true;
            }
            return true;
        }
    };

    private final SurfaceHolder.Callback mSurfaceHolderCallback = new SurfaceHolder.Callback() {
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            LogUtil.v(TAG, "SurfaceHolder : " + surfaceHolder);
            mSurfaceHolder = surfaceHolder;
            openVideo();
        }

        public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
            LogUtil.v(TAG, "SurfaceHolder : " + surfaceHolder + ", format : " + format +
                    ", swidth : " + width + ", sheight : " + height +
                    ", vwidth : " + mVideoWidth + ", vheight : " + mVideoHeight ) ;
            mSurfaceWidth = width;
            mSurfaceHeight = height;
            final boolean isValidState = (mIntentState == STATE_PLAYING);
            final boolean isValidSize = (mVideoWidth == width && mVideoHeight == height);
            if (mPlayer != null && isValidState && isValidSize) {
                // TODO : seek issue
                start(mSegmentId);
            }
        }

        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            LogUtil.v(TAG, "SurfaceHolder : " + surfaceHolder);
            mSurfaceHolder = null;
            release(true);
        }
    };

    // Send notification to client
    private MediaPlayer.OnCompletionListener mOnCompletionListener;
    private MediaPlayer.OnPreparedListener mOnPreparedListener;
    private MediaPlayer.OnErrorListener mOnErrorListener;

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        LogUtil.v(TAG, "Spec width : " + widthMeasureSpec + ", Spec height : " + heightMeasureSpec);
        int width = getDefaultSize(mVideoWidth, widthMeasureSpec);
        int height = getDefaultSize(mVideoHeight, heightMeasureSpec);
        LogUtil.v(TAG, "Default Width : " + width + ", Default Height : " + height);
        if (mMeasureWidth == 0 || mMeasureHeight == 0) {
            mMeasureWidth = width;
            mMeasureHeight = height;
        }

        // Measure the video view in landscape mode only
        final int finalWidth = (mMeasureWidth > mMeasureHeight) ? mMeasureWidth : mMeasureHeight;
        final int finalHeight = (mMeasureWidth > mMeasureHeight) ? mMeasureHeight : mMeasureWidth;
        LogUtil.v(TAG, "Final Width : " + finalWidth + ", Final Height : " + finalHeight);
        setMeasuredDimension(finalWidth, finalHeight);
    }

    public SimpleVideoView(Context context) {
        super(context);
        initializeView();
    }

    public SimpleVideoView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
        initializeView();
    }

    public SimpleVideoView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initializeView();
    }

    public boolean setLooping(final boolean enableLooping) {
        return (mEnableLooping = enableLooping);
    }

    public boolean setBackgroundMusicPauseEnabled(final boolean pause) {
        return (mEnableMusicPause = pause);
    }

    public void setVideoURI(Uri uri) {
        setVideoURI(uri, null);
    }

    public void setVideoURI(Uri uri, HashMap<Integer, Pair<Integer, Integer>> segments) {
        LogUtil.v(TAG, "Uri : " + uri + ", Segments : " + segments);
        mUri = uri;
        mSegments = segments;
        prepareVideo();
    }

    public void prepareVideo() {
        LogUtil.v(TAG);
        openVideo();
        requestLayout();
        invalidate();
    }

    private void initializeView() {
        mVideoWidth = 0;
        mVideoHeight = 0;
        getHolder().addCallback(mSurfaceHolderCallback);
        getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        setAllState(STATE_IDLE);
    }

    private void openVideo() {
        LogUtil.v(TAG, "Uri : " + mUri + ", Holder : " + mSurfaceHolder);
        if (mUri == null || mSurfaceHolder == null) {
            return;
        }

        if (mEnableMusicPause) {
            sendMusicPauseRequest();
        }

        release(false);
        try {
            mPlayer = new MediaPlayer();
            mPlayer.setOnPreparedListener(mPreparedListener);
            mPlayer.setOnVideoSizeChangedListener(mSizeChangedListener);
            mPlayer.setOnCompletionListener(mCompletionListener);
            mPlayer.setOnErrorListener(mErrorListener);
            mPlayer.setDataSource(getContext(), mUri);
            mPlayer.setDisplay(mSurfaceHolder);
            mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mPlayer.prepareAsync();
            LogUtil.v(TAG, "MediaPlayer.prepareAsync is called. : " + mPlayer);
            setCurrentState(STATE_PREPARING);
        } catch (IOException ex) {
            LogUtil.v(TAG, "IOException : " + ex);
            setAllState(STATE_ERROR);
            mErrorListener.onError(mPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
        } catch (IllegalArgumentException ex) {
            LogUtil.v(TAG, "IllegalArgumentException : " + ex);
            setAllState(STATE_ERROR);
            mErrorListener.onError(mPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
        }
    }

    private void prepareLoopTimer(final int startMs, final int endMs) {
        LogUtil.v(TAG, "loop start : " + startMs + ", loop end : " + endMs + ", seg : " + mSegmentId);
        if (mEnableLooping) {
            if (mLoopHandler == null) {
                mLoopHandler = new Handler();
            }

            mLoopStartMs = startMs;
            mLoopEndMs = endMs;
            if (mLoopRunner == null) {
                mLoopRunner = new Runnable() {
                    public void run() {
                        seekToAndStart(mLoopStartMs, mLoopEndMs);
                    }
                };
            }
            mLoopHandler.postDelayed(mLoopRunner, mLoopEndMs - mLoopStartMs);
        }
    }

    private void removeLoopTimer() {
        LogUtil.v(TAG);
        if (mLoopHandler != null &&
                mLoopRunner != null) {
            mLoopHandler.removeCallbacks(mLoopRunner);
            mLoopRunner = null;
        }
    }

    public void start() {
        LogUtil.v(TAG);
        if (isPlayable()) {
            mPlayer.start();
            setCurrentState(STATE_PLAYING);
        }
        setIntentState(STATE_PLAYING);
    }

    private boolean isValidPeriod(final int startMs, final int endMs) {
        LogUtil.v(TAG, "start : " + startMs + ", end : " + endMs + ", duration : " + mDuration);
        return (startMs <= endMs) &&
                (startMs >= 0 && startMs <= mDuration) &&
                (endMs >= 0 && endMs <= mDuration);
    }

    public void start(int segmentId) {
        LogUtil.v(TAG, "Seg id : " + segmentId + ", last seg id : " + mSegmentId);
        setVisibility(VISIBLE);
        if (segmentId != REPLAY_SEGMENT_ID) {
            mSegmentId = segmentId;
        }
        if (mSegments == null ||
                segmentId == INVALID_SEGMENT_ID) {
            start();
        } else {
            final Pair seg = mSegments.get(mSegmentId);
            if (seg != null) {
                final int startMs = (Integer)seg.first;
                final int endMs = (Integer)seg.second;
                LogUtil.v(TAG, "Seg id : " + mSegmentId + ", start : " + startMs + ", end : " + endMs );
                if (isValidPeriod(startMs, endMs)) {
                    seekToAndStart(startMs, endMs);
                }
            }
        }
        setIntentState(STATE_PLAYING);
    }

    private void
    seekToAndStart(final int startMs, final int endMs) {
        LogUtil.v(TAG, "start : " + startMs + ", end : " + endMs +
                ", currentState : " + mCurrentState + ", intentState : " + mIntentState);
        if (isPlayable()) {
            pause();
            seekTo(startMs);
            start();
            prepareLoopTimer(startMs, endMs);
        }
    }

    public void pause() {
        LogUtil.v(TAG);
        if (isPlayable() && isPlaying()) {
            removeLoopTimer();
            mPlayer.pause();
            setCurrentState(STATE_PAUSED);
        }
        setIntentState(STATE_PAUSED);
    }

    public void stopPlayback() {
        LogUtil.v(TAG);
        if (mPlayer != null) {
            removeLoopTimer();
            mPlayer.stop();
            mPlayer.release();
            mPlayer = null;
            setAllState(STATE_IDLE);
        }
        setVisibility(INVISIBLE);
    }

    public void seekTo(int mSec) {
        LogUtil.v(TAG);
        if (isPlayable()) {
            mPlayer.seekTo(mSec);
        }
    }

    public void release(boolean clearIntent) {
        LogUtil.v(TAG);
        if (mPlayer != null) {
            removeLoopTimer();
            mPlayer.reset();
            mPlayer.release();
            mPlayer = null;
            setCurrentState(STATE_IDLE);
            if (clearIntent) {
                setIntentState(STATE_IDLE);
            }
        }
    }

    public boolean isPlaying() {
        LogUtil.v(TAG, "State : " + mCurrentState);
        return (isPlayable() && mPlayer.isPlaying());
    }

    private boolean isPlayable() {
        LogUtil.v(TAG, "State : " + mCurrentState);
        return (mPlayer != null &&
                mCurrentState != STATE_ERROR &&
                mCurrentState != STATE_IDLE &&
                mCurrentState != STATE_PREPARING);
    }

    public void setOnPreparedListener(MediaPlayer.OnPreparedListener listener) {
        mOnPreparedListener = listener;
    }

    public void setOnCompletionListener(MediaPlayer.OnCompletionListener listener) {
        mOnCompletionListener = listener;
    }

    public void setmOnErrorListener(MediaPlayer.OnErrorListener listener) {
        mOnErrorListener = listener;
    }

    private boolean notifyCompletionListener(MediaPlayer mediaplayer) {
        if (mOnCompletionListener != null){
            mOnCompletionListener.onCompletion(mediaplayer);
            return true;
        }
        return false;
    }

    private boolean notifyPreparedListener(MediaPlayer mediaplayer) {
        if (mOnPreparedListener != null) {
            mOnPreparedListener.onPrepared(mediaplayer);
            return true;
        }
        return false;
    }

    private boolean notifyErrorListener(MediaPlayer mediaplayer, int frameworkErr, int implErr) {
        if (mOnErrorListener != null) {
            mOnErrorListener.onError(mediaplayer, frameworkErr, implErr);
            return true;
        }
        return false;
    }

    private void sendMusicPauseRequest() {
        Intent i = new Intent("com.android.music.musicservicecommand");
        i.putExtra("command", "pause");
        getContext().sendBroadcast(i);
    }

    private void setCurrentState(int state) {
        mCurrentState = state;
    }

    private void setIntentState(int state) {
        mIntentState = state;
    }

    private void setAllState(int state) {
        setCurrentState(state);
        setIntentState(state);
    }
}