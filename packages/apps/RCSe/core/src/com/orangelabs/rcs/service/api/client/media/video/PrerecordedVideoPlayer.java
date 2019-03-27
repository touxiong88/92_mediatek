/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.orangelabs.rcs.service.api.client.media.video;

import com.orangelabs.rcs.core.ims.protocol.rtp.MediaRegistry;
import com.orangelabs.rcs.core.ims.protocol.rtp.MediaRtpSender;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h263.H263Config;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h263.decoder.NativeH263Decoder;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h263.decoder.VideoSample;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.H264Config;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.JavaPacketizer;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.decoder.NativeH264Decoder;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.encoder.NativeH264Encoder;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.encoder.NativeH264EncoderParams;
import com.orangelabs.rcs.core.ims.protocol.rtp.format.video.H263VideoFormat;
import com.orangelabs.rcs.core.ims.protocol.rtp.format.video.H264VideoFormat;
import com.orangelabs.rcs.core.ims.protocol.rtp.format.video.VideoFormat;
import com.orangelabs.rcs.core.ims.protocol.rtp.media.MediaException;
import com.orangelabs.rcs.core.ims.protocol.rtp.media.MediaInput;
import com.orangelabs.rcs.core.ims.protocol.rtp.media.MediaSample;
import com.orangelabs.rcs.core.ims.protocol.rtp.media.RtpExtensionHeader;
import com.orangelabs.rcs.platform.network.DatagramConnection;
import com.orangelabs.rcs.platform.network.NetworkFactory;
import com.orangelabs.rcs.service.api.client.media.IMediaEventListener;
import com.orangelabs.rcs.service.api.client.media.IMediaPlayer;
import com.orangelabs.rcs.service.api.client.media.MediaCodec;
import com.orangelabs.rcs.utils.FifoBuffer;
import com.orangelabs.rcs.utils.NetworkRessourceManager;
import com.orangelabs.rcs.utils.logger.Logger;
import com.orangelabs.rcs.service.api.client.media.video.NativeMediaCodecWrapper;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaCodec.BufferInfo;
import android.os.RemoteException;
import android.os.SystemClock;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Vector;

/**
 * Pre-recorded video RTP player. Supports only H.263 QCIF format.
 * 
 * @author jexa7410
 */
public class PrerecordedVideoPlayer extends IMediaPlayer.Stub {

    /**
     * List of supported video codecs
     */

    public static MediaCodec[] supportedMediaCodecs = {
        /**
         * M: Added for H264 Support @{
         */ 
        new VideoCodec(H264Config.CODEC_NAME, H264VideoFormat.PAYLOAD, H264Config.CLOCK_RATE, H264Config.CODEC_PARAMS,
                H264Config.FRAME_RATE, H264Config.BIT_RATE, H264Config.VIDEO_WIDTH,
                H264Config.VIDEO_HEIGHT).getMediaCodec(),
                /** @} */
                new VideoCodec(H263Config.CODEC_NAME, H263VideoFormat.PAYLOAD, H263Config.CLOCK_RATE, H263Config.CODEC_PARAMS,
                        H263Config.FRAME_RATE, H263Config.BIT_RATE, H263Config.VIDEO_WIDTH,
                        H263Config.VIDEO_HEIGHT).getMediaCodec()
    };

    /**
     * Selected video codec
     */
    private VideoCodec selectedVideoCodec = null;

    /**
     * Video filename to be streamed
     */
    private String filename;

    /**
     * Local RTP port
     */
    private int localRtpPort;

    /**
     * Video format
     */
    private VideoFormat videoFormat;

    /**
     * RTP sender session
     */
    private MediaRtpSender rtpSender = null;

    /**
     * RTP media input
     */
    private MediaRtpInput rtpInput = null;

    /**
     * Is player opened
     */
    private boolean opened = false;

    /**
     * Is player started
     */
    private boolean started = false;

    /**
     * Video start time
     */
    private long videoStartTime = 0L;

    /**
     * Video duration
     */
    private long videoDuration = 0L;

    /**
     * Video width
     */
    private int videoWidth;

    /**
     * Video height
     */
    private int videoHeight;
    /**
     * M: Added for H264 Support @{
     */ 
    /**
     * NAL SPS
     */
    private byte[] sps = new byte[0];

    /**
     * NAL PPS
     */
    private byte[] pps = new byte[0];

    /**
     * Timestamp increment
     */
    private int timestampInc;

    /**
     * NAL initialization
     */
    private boolean nalInit = false;

    /**
     * NAL repeat
     */
    private int nalRepeat = 0;

    /**
     * NAL repeat MAX value
     */
    private static final int NALREPEATMAX = 20;

    /**
     * Native MediaCodec
     */
    private android.media.MediaCodec codec;

    /**
     * Native MediaExtractor
     */
    private MediaExtractor extractor;
    /** @} */

    /** M: add for video sharing plugin @{ */
    /**
     * Video player event listener
     */
    private IVideoPlayerEventListener listener;
    /**
     * Video surface view listener
     */
    private IVideoSurfaceViewListener mVideoSurfaceViewListener = null;
    /** @} */

    /**
     * Surface renderer
     */
    private VideoSurfaceView surface = null;

    /**
     * Media event listeners
     */
    private Vector<IMediaEventListener> listeners = new Vector<IMediaEventListener>();

    /**
     * Temporary connection to reserve the port
     */
    private DatagramConnection temporaryConnection = null;

    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * M: Added for video share progress control @{
     */
    /**
     * Whether video share is started.
     */
    private boolean mShareStarted = false;
    /**
     * Time stamp to seek.
     */
    private long mSeekTimeStamp = -1L;
    /**
     * Total duration.
     */
    private long mTotalDuration = 0;
    /**
     * Dummy package size
     */
    private static final int DUMMY_PACKAGE_SIZE = 3;
    /**
     * Object for synchronizing.
     */
    private Object mObject = new Object();
    /**
     * Time stamp for each package.
     */
    private long mTimeStamp = -1l;
    /**
     * Peroid to sleep.
     */
    private static final int SLEEP_PERIOD = 500;

    private static final String SUPPORT_FORMAT_H264 = "avc";

    /** @} */
    /**
     * Codec width key
     */
    private static final String CODECWIDTH = "codecWidth";

    /**
     * Codec height key
     */
    private static final String CODECHEIGHT = "codecHeight";

    /** M: add for video sharing plugin @{ */
    /**
     * Constructor use setting video codec.
     * 
     * @param codec Video codec
     * @param filename Video filename
     * @param listener Video player listener
     */
    public PrerecordedVideoPlayer(String filename, IVideoPlayerEventListener listener) {
        this.filename = filename;
        this.listener = listener;

        // Set the local RTP port
        localRtpPort = NetworkRessourceManager.generateLocalRtpPort();
        reservePort(localRtpPort);
    }

    /**
     * Constructor Force a video codec.
     * 
     * @param codec Video codec
     * @param filename Video filename
     * @param listener Video player listener
     */
    public PrerecordedVideoPlayer(VideoCodec codec, String filename,
            IVideoPlayerEventListener listener) {
        this.filename = filename;
        this.listener = listener;

        // Set the local RTP port
        localRtpPort = NetworkRessourceManager.generateLocalRtpPort();
        reservePort(localRtpPort);

        // Set the media codec
        setMediaCodec(codec.getMediaCodec());
    }

    /**
     * Constructor Force a video codec.
     * 
     * @param codec Video codec name
     * @param filename Video filename
     * @param listener Video player listener
     */
    public PrerecordedVideoPlayer(String codec, String filename, IVideoPlayerEventListener listener) {
        this.filename = filename;
        this.listener = listener;

        // Set the local RTP port
        localRtpPort = NetworkRessourceManager.generateLocalRtpPort();
        reservePort(localRtpPort);

        // Set the media codec
        for (int i = 0; i < supportedMediaCodecs.length; i++) {
            if (codec.toLowerCase().contains(supportedMediaCodecs[i].getCodecName().toLowerCase())) {
                setMediaCodec(supportedMediaCodecs[i]);
                break;
            }
        }
    }

    /**
     * Constructor Force a video codec.
     * 
     * @param codec Video codec name
     * @param filename Video filename
     * @param listener Video player listener
     * @param width Video file width
     * @param height Video file height
     */
    public PrerecordedVideoPlayer(String codec, String filename, IVideoPlayerEventListener listener, int width, int height) {
        this.filename = filename;
        this.listener = listener;

        // Set the local RTP port
        localRtpPort = NetworkRessourceManager.generateLocalRtpPort();
        reservePort(localRtpPort);

        // Set the media codec
        for (int i = 0; i < supportedMediaCodecs.length; i++) {
            if (codec.toLowerCase().contains(supportedMediaCodecs[i].getCodecName().toLowerCase())) {
                supportedMediaCodecs[i].setIntParam(CODECWIDTH, width);
                supportedMediaCodecs[i].setIntParam(CODECHEIGHT, height);
                setMediaCodec(supportedMediaCodecs[i]);
                break;
            }
        }
    }

    /**
     * Add listener for video surface view.
     * 
     * @param videoSurfaceViewListener The listener for video surface view.
     */
    public void addVideoSurfaceViewListener(IVideoSurfaceViewListener videoSurfaceViewListener) {
        mVideoSurfaceViewListener = videoSurfaceViewListener;
    }

    /** @} */

    /**
     * Returns the local RTP port
     *
     * @return Port
     */
    public int getLocalRtpPort() {
        return localRtpPort;
    }

    /**
     * Reserve a port.
     *
     * @param port the port to reserve
     */
    private void reservePort(int port) {
        if (temporaryConnection == null) {
            try {
                temporaryConnection = NetworkFactory.getFactory().createDatagramConnection();
                temporaryConnection.open(port);
            } catch (IOException e) {
                temporaryConnection = null;
            }
        }
    }

    /**
     * Release the reserved port.
     */
    private void releasePort() {
        if (temporaryConnection != null) {
            try {
                temporaryConnection.close();
            } catch (IOException e) {
                temporaryConnection = null;
            }
        }
    }

    /**
     * Set the surface to render video
     *
     * @param surface Video surface
     */
    public void setVideoSurface(VideoSurfaceView surface) {
        this.surface = surface;
    }

    /**
     * Return the video start time
     *
     * @return Milliseconds
     */
    public long getVideoStartTime() {
        return videoStartTime;
    }

    /**
     * Get video duration
     *
     * @return Milliseconds
     */
    public long getVideoDuration() {
        return videoDuration;
    }

    /**
     * Get video width
     *
     * @return Milliseconds
     */
    public int getVideoWidth() {
        return videoWidth;
    }

    /**
     * Get video height
     *
     * @return Milliseconds
     */
    public int getVideoHeight() {
        return videoHeight;
    }

    /**
     * Is player opened
     *
     * @return Boolean
     */
    public boolean isOpened() {
        return opened;
    }

    /**
     * Is player started
     *
     * @return Boolean
     */
    public boolean isStarted() {
        return started;
    }

    /**
     * Open the player
     *
     * @param remoteHost Remote host
     * @param remotePort Remote port
     */
    public void open(String remoteHost, int remotePort) {
        if (opened) {
            // Already opened
            return;
        }

        // Check video codec
        if (selectedVideoCodec == null) {
            notifyPlayerEventError("Video Codec not selected");
            return;
        }

        try {
            /**
             * M: Added for H264 Support @{
             */        
            if (selectedVideoCodec.getCodecName().equalsIgnoreCase(H264Config.CODEC_NAME)) {
                MediaExtractor extractor = new MediaExtractor();
                extractor.setDataSource(filename);
                for (int i = 0; i < extractor.getTrackCount(); i++) {
                    MediaFormat format = extractor.getTrackFormat(i);
                    String mime = format.getString(MediaFormat.KEY_MIME);
                    if (mime.startsWith("video/")) {
                        if(mime.indexOf(SUPPORT_FORMAT_H264) != -1){
                            videoWidth = format.getInteger(MediaFormat.KEY_WIDTH);
                            videoHeight = format.getInteger(MediaFormat.KEY_HEIGHT);
                            videoDuration = format.getLong(MediaFormat.KEY_DURATION)/1000;
                        }
                        break;
                    }
                }
                extractor.release();
                extractor = null;
                // Check video properties
                if ((videoWidth != selectedVideoCodec.getWidth())
                        || (videoHeight != selectedVideoCodec.getHeight())) {
                    notifyPlayerEventError("Not supported video format");
                    return;
                }
                initEncoderH264();
            }
            else if(selectedVideoCodec.getCodecName().equalsIgnoreCase(H263Config.CODEC_NAME)){
                /** @} */
                int result = NativeH263Decoder.InitParser(filename);
                if (result != 1) {
                    notifyPlayerEventError("Video file parser for h263 init failed with error code " + result);
                    return;
                }

                // Get video properties
                videoDuration = NativeH263Decoder.getVideoLength();
                videoWidth = NativeH263Decoder.getVideoWidth();
                videoHeight = NativeH263Decoder.getVideoHeight();

                // Check video properties
                if ((videoWidth != selectedVideoCodec.getWidth())
                        || (videoHeight != selectedVideoCodec.getHeight())) {
                    notifyPlayerEventError("Not supported video format");
                    return;
                }
            }
        } catch (UnsatisfiedLinkError e) {
            notifyPlayerEventError(e.getMessage());
            return;
        }

        try {
            // Init the RTP layer
            releasePort();
            rtpSender = new MediaRtpSender(videoFormat, localRtpPort);
            rtpInput = new MediaRtpInput();
            rtpInput.open();
            rtpSender.prepareSession(rtpInput, remoteHost, remotePort);
        } catch (Exception e) {
            notifyPlayerEventError(e.getMessage());
            return;
        }

        // Player is opened
        opened = true;
        notifyPlayerEventOpened();
    }

    /**
     * M: fix NE for video share, which is caused by NOT synchronization between
     * EncodeFrame and DeinitEncoder @{
     */
    /**
     * Close the player
     */
    public void close() {
        if (!opened) {
            // Already closed
            return;
        }

        // Close the RTP layer
        rtpInput.close();
        rtpSender.stopSession();

        // Player is closed
        opened = false;
        notifyPlayerEventClosed();
    }

    private void closeDecoder() {
        try {
            // Close the video file parser.
            /**
             * M: Added for H264 Support @{
             */        
            if(selectedVideoCodec.getCodecName().equalsIgnoreCase(H264Config.CODEC_NAME)){
                extractor.release();
                extractor = null;
                codec.stop();
                codec.release();
                codec = null;
                NativeH264Encoder.DeinitEncoder();
            }
            else if(selectedVideoCodec.getCodecName().equalsIgnoreCase(H263Config.CODEC_NAME)){
                NativeH263Decoder.DeinitParser();
            } 
            /** @} */
        } catch (UnsatisfiedLinkError e) {
            if (logger.isActivated()) {
                logger.error("Can't deallocate correctly the video file parser", e);
            }
        }
    }

    /** @} */

    /**
     * Start the player
     */
    public void start() {
        if (!opened) {
            // Player not opened
            return;
        }

        if (started) {
            // Already started
            return;
        }
        /**
         * M: Added for H264 Support @{
         */
        // Init NAL
        if(selectedVideoCodec.getCodecName().equalsIgnoreCase(H264Config.CODEC_NAME)){
            if (!initNAL()) {
                return;
            }
            nalInit = false;
            mTimeStamp = 0;
            nalRepeat = 0;
        }
        /** @} */

        // Start RTP layer
        rtpSender.startSession();

        // Start reading file
        readingThread.start();

        // Player is started
        videoStartTime = SystemClock.uptimeMillis();
        started = true;
        notifyPlayerEventStarted();
    }

    /**
     * Stop the player
     */
    public void stop() {
        if (!opened) {
            // Player not opened
            return;
        }

        if (!started) {
            // Already stopped
            return;
        }

        // Stop reading file
        try {
            readingThread.interrupt();
        } catch (Exception e) {
        }

        // Player is stopped
        videoStartTime = 0L;
        started = false;
        notifyPlayerEventStopped();
    }

    /**
     * Add a media event listener
     *
     * @param listener Media event listener
     */
    public void addListener(IMediaEventListener listener) {
        listeners.addElement(listener);
    }

    /**
     * Remove all media event listeners
     */
    public void removeAllListeners() {
        listeners.removeAllElements();
    }

    /**
     * Get supported media codecs
     * 
     * @return media Codecs list
     */
    public MediaCodec[] getSupportedMediaCodecs() {
        return supportedMediaCodecs;
    }

    /**
     * Get media codec
     * 
     * @return Media codec
     */
    public MediaCodec getMediaCodec() {
        if (selectedVideoCodec == null)
            return null;
        else
            return selectedVideoCodec.getMediaCodec();
    }

    /**
     * Set media codec
     * 
     * @param mediaCodec Media codec
     */
    public void setMediaCodec(MediaCodec mediaCodec) {
        if (VideoCodec.checkVideoCodec(supportedMediaCodecs, new VideoCodec(mediaCodec))) {
            selectedVideoCodec = new VideoCodec(mediaCodec);
            videoFormat = (VideoFormat) MediaRegistry.generateFormat(mediaCodec.getCodecName());
        } else {
            notifyPlayerEventError("Codec not supported");
        }
    }

    /**
     * Notify player event started
     */
    private void notifyPlayerEventStarted() {
        if (logger.isActivated()) {
            logger.debug("Player is started");
        }
        Iterator<IMediaEventListener> ite = listeners.iterator();
        while (ite.hasNext()) {
            try {
                ((IMediaEventListener)ite.next()).mediaStarted();
            } catch (RemoteException e) {
                if (logger.isActivated()) {
                    logger.error("Can't notify listener", e);
                }
            }
        }
    }

    /**
     * Notify player event stopped
     */
    private void notifyPlayerEventStopped() {
        if (logger.isActivated()) {
            logger.debug("Player is stopped");
        }
        Iterator<IMediaEventListener> ite = listeners.iterator();
        while (ite.hasNext()) {
            try {
                ((IMediaEventListener)ite.next()).mediaStopped();
            } catch (RemoteException e) {
                if (logger.isActivated()) {
                    logger.error("Can't notify listener", e);
                }
            }
        }
    }

    /**
     * Notify player event opened
     */
    private void notifyPlayerEventOpened() {
        if (logger.isActivated()) {
            logger.debug("Player is opened");
        }
        Iterator<IMediaEventListener> ite = listeners.iterator();
        while (ite.hasNext()) {
            try {
                ((IMediaEventListener)ite.next()).mediaOpened();
            } catch (RemoteException e) {
                if (logger.isActivated()) {
                    logger.error("Can't notify listener", e);
                }
            }
        }
    }

    /**
     * Notify player event closed
     */
    private void notifyPlayerEventClosed() {
        if (logger.isActivated()) {
            logger.debug("Player is closed");
        }
        Iterator<IMediaEventListener> ite = listeners.iterator();
        while (ite.hasNext()) {
            try {
                ((IMediaEventListener)ite.next()).mediaClosed();
            } catch (RemoteException e) {
                if (logger.isActivated()) {
                    logger.error("Can't notify listener", e);
                }
            }
        }
    }

    /**
     * Notify player event error
     */
    private void notifyPlayerEventError(String error) {
        if (logger.isActivated()) {
            logger.debug("Player error: " + error);
        }

        Iterator<IMediaEventListener> ite = listeners.iterator();
        while (ite.hasNext()) {
            try {
                ((IMediaEventListener)ite.next()).mediaError(error);
            } catch (RemoteException e) {
                if (logger.isActivated()) {
                    logger.error("Can't notify listener", e);
                }
            }
        }
    }

    /**
     * Video reading thread
     */
    private Thread readingThread = new Thread() {
        /**
         * Duration
         */
        private long totalDuration = 0;

        /**
         * End of media
         */
        private boolean endOfMedia = false;
        /**
         * End of Frames in File
         */
        private boolean endOfFrame = false;


        /**
         * M: Added for H264 Support @{
         */

        /**
         * Byte buffer for Android MediaCodec input buffer
         */
        ByteBuffer[] codecInputBuffers;

        /**
         * Byte buffer for Android MediaCodec output buffer
         */
        ByteBuffer[] codecOutputBuffers;

        /**
         * Buffer Info for Android MediaCodec
         */
        BufferInfo info;

        Bitmap image;

        int[] decodedFrame;

        VideoSample sample;

        long timestamp;

        long lastTimeStamp;

        Queue mSampleTime;

        boolean isSeek = false;

        String mCodecName = H264Config.CODEC_NAME;
        /** @} */
        /**
         * Processing
         */
        public void run() {
            if (rtpInput == null) {
                return;
            }

            image = Bitmap.createBitmap(videoWidth, videoHeight, Bitmap.Config.RGB_565);
            decodedFrame = new int[videoWidth * videoHeight];
            /**
             * M: Added for H264 Support @{
             */
            if(selectedVideoCodec.getCodecName().equalsIgnoreCase(H264Config.CODEC_NAME)){
                initExtractorAndDecoder();
                codecInputBuffers = codec.getInputBuffers( );
                codecOutputBuffers = codec.getOutputBuffers( );
                info = new BufferInfo();
                mSampleTime = new LinkedList();
            }else if(selectedVideoCodec.getCodecName().equalsIgnoreCase(H263Config.CODEC_NAME)){
                mCodecName = H263Config.CODEC_NAME;
            }
            /** @} */
            while (!endOfMedia && started) {

                /**
                 * M: Added for share progress control @{
                 */
                checkStatus();
                /** @} */

                // Set timestamp
                timestamp = System.currentTimeMillis();

                // Get video sample from file
                sample = null;
                if (!opened) {
                    if (logger.isActivated()) {
                        logger.debug("player is closed, do closeDecoder");
                    }
                    closeDecoder();
                    break;
                }
                /**
                 * M: Added for H264 Support @{
                 */
                if(mCodecName.equalsIgnoreCase(H264Config.CODEC_NAME)){
                    H264VideoSupport();
                }else if(mCodecName.equalsIgnoreCase(H263Config.CODEC_NAME)){
                    H263VideoSupport();
                } 
            }
            // Notify listener
            try {
                listener.endOfStream();
                /**
                 * M: Added for H264 Support @{
                 */
                if(selectedVideoCodec.getCodecName().equalsIgnoreCase(H264Config.CODEC_NAME)){
                    extractor.release();
                    extractor = null;
                    codec.stop();
                    codec.release();
                    codec = null;
                    NativeH264Encoder.DeinitEncoder();
                }
                /** @} */
            } catch (RemoteException e) {
                if(logger.isActivated()){
                    logger.error("endOfStream fail");
                }
                e.printStackTrace();
            }
        }

        public void H264VideoSupport() {
            if(!endOfFrame)
            {
                int inputBufferIndex = codec.dequeueInputBuffer(10000);
                if (inputBufferIndex >= 0) {
                    // fill codecInputBuffers[inputBufferIndex] with valid data
                    ByteBuffer buffer = codecInputBuffers[inputBufferIndex];
                    if (mSeekTimeStamp != -1L) {
                        synchronized (mObject) {
                            isSeek = true;
                            if(!mSampleTime.isEmpty())
                            {
                                lastTimeStamp = (Long) mSampleTime.peek();
                                mSampleTime.clear();
                            }
                            codec.flush();
                            //mSampleTime.add(lastTimeStamp);
                            extractor.seekTo(mSeekTimeStamp * 1000, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
                            mSeekTimeStamp = -1L;
                            try {
                                Thread.sleep(40);
                            } catch (InterruptedException e) {
                                if (logger.isActivated()) {
                                    logger.debug("readingThread error");
                                }
                                e.printStackTrace();
                            }
                            return;
                        }
                    }
                    int sampleSize = extractor.readSampleData(buffer, 0);
                    if (sampleSize < 0) {
                        codec.queueInputBuffer(inputBufferIndex, 0, 0, 0, NativeMediaCodecWrapper.BUFFER_FLAG_END_OF_STREAM);
                        endOfFrame = true;
                    } else {
                        codec.queueInputBuffer(inputBufferIndex, 0, sampleSize, extractor.getSampleTime(), 0);                    
                        mSampleTime.add(extractor.getSampleTime()/1000);
                        extractor.advance();
                    }
                }
            }
            int outputBufferIndex = codec.dequeueOutputBuffer(info, 10000);
            if (outputBufferIndex >= 0) {
                // outputBuffer is ready to be processed or rendered.
                ByteBuffer buffer = codecOutputBuffers[outputBufferIndex]; 
                final byte[] chunk = new byte[ info.size ];
                buffer.get( chunk ); // Read the buffer all at once
                buffer.clear(); // ** MUST DO!!! OTHERWISE THE NEXT TIME YOU GET THIS SAME BUFFER BAD THINGS WILL HAPPEN
                if( chunk.length > 0 )
                {
                    long sampleTime = 0;
                    if(!mSampleTime.isEmpty())
                    {
                        sampleTime = (Long) mSampleTime.peek() - lastTimeStamp;
                        lastTimeStamp = (Long) mSampleTime.poll();
                    }
                    mTotalDuration = mTotalDuration + sampleTime;
                    if(mTotalDuration < 0)
                    {
                        mTotalDuration = 0;
                    }else if(mTotalDuration > videoDuration){
                        endOfMedia = true;
                        mTotalDuration = videoDuration;
                    }
                    mTimeStamp = mTimeStamp + Math.abs(sampleTime);
                    sendParameterSet();
                    // Encode and send frame 
                    byte[] encoded = NativeH264Encoder.EncodeFrame(chunk ,mTimeStamp);
                    int encodeResult = NativeH264Encoder.getLastEncodeStatus();
                    if ((encodeResult == 0) && (encoded.length > 0)) {
                        //rtpInput.addFrame(encoded, timeStamp, true);
                        rtpInput.addFrame(encoded, mTimeStamp, new RtpExtensionHeader(
                                mTotalDuration, mShareStarted));
                    }
                    // Display Decoded frame
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    YuvImage yuvImage = new YuvImage(chunk, ImageFormat.NV21, videoWidth, videoHeight, null);
                    yuvImage.compressToJpeg(new Rect(0, 0, videoWidth, videoHeight), 100, out);
                    byte[] imageBytes = out.toByteArray();
                    image = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

                    try {
                        listener.updateDuration(mTotalDuration);
                        if (mVideoSurfaceViewListener != null) {
                            mVideoSurfaceViewListener.onSampleReceived(image, mTotalDuration);
                        }
                    } catch (RemoteException e) {
                        if(logger.isActivated()){
                            logger.error("updateDuration fail");
                        }
                    }
                    long delta = System.currentTimeMillis() - timestamp;
                    if (delta < sampleTime && !isSeek) {
                        try {
                            Thread.sleep(sampleTime - delta);
                        } catch (InterruptedException e) {
                            if (logger.isActivated()) {
                                logger.debug("readingThread error");
                            }
                            e.printStackTrace();
                        }
                    }
                    isSeek = false;
                }
                codec.releaseOutputBuffer(outputBufferIndex, true);
            } else if (outputBufferIndex == NativeMediaCodecWrapper.INFO_OUTPUT_BUFFERS_CHANGED) {
                codecOutputBuffers = codec.getOutputBuffers();
            } else if (outputBufferIndex == NativeMediaCodecWrapper.INFO_OUTPUT_FORMAT_CHANGED) {
                // Subsequent data will conform to new format.
                MediaFormat format1 = codec.getOutputFormat();
            }
            if(mSampleTime.isEmpty() && endOfFrame)
            {
                endOfMedia = true;
            }
        }
        public void H263VideoSupport() {

            /** @} */
            /**
             * M: Added for share progress control @{
             */
            if (mSeekTimeStamp != -1L) {
                synchronized (mObject) {
                    NativeH263Decoder.seekTo(mSeekTimeStamp);
                    sample = NativeH263Decoder.getVideoSample(decodedFrame);
                    if (sample != null) {
                        mTotalDuration = mSeekTimeStamp + sample.timestamp;
                        mTimeStamp = mTimeStamp + sample.timestamp;
                    } else {
                        mTotalDuration = mSeekTimeStamp;
                        mTimeStamp++;
                        if (logger.isActivated()) {
                            logger.debug("getVideoSampleAt sample is null");
                        }
                    }
                    mSeekTimeStamp = -1L;
                }
            } else {
                sample = NativeH263Decoder.getVideoSample(decodedFrame);
                if (sample != null) {
                    mTotalDuration = mTotalDuration + sample.timestamp;
                    mTimeStamp = mTimeStamp + sample.timestamp;
                } else {
                    mTimeStamp++;
                    if (logger.isActivated()) {
                        logger.debug("getVideoSample sample is null");
                    }
                }
            }
            /** @} */
            if (sample != null) {
                // Display decoded frame
                image.setPixels(decodedFrame, 0, videoWidth, 0, 0, videoWidth, videoHeight);
                if (surface != null) {
                    surface.setImage(image);
                }
                try {
                    listener.updateDuration(mTotalDuration);
                } catch (RemoteException e) {
                    if(logger.isActivated()){
                        logger.error("updateDuration fail");
                    }
                    e.printStackTrace();
                }
                /**
                 * M: Added for share progress control @{
                 */
                try {
                    if (mVideoSurfaceViewListener != null) {
                        mVideoSurfaceViewListener.onSampleReceived(image, mTotalDuration);
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                // Send encoded frame
                rtpInput.addFrame(sample.data, mTimeStamp, new RtpExtensionHeader(
                        mTotalDuration, mShareStarted));
                /** @} */
                // Wait before next frame
                long delta = System.currentTimeMillis() - timestamp;
                if (delta < sample.timestamp) {
                    try {
                        Thread.sleep(sample.timestamp - delta);
                    } catch (InterruptedException e) {
                        if (logger.isActivated()) {
                            logger.debug("readingThread error");
                        }
                        e.printStackTrace();
                    }
                }
            } else {
                if (logger.isActivated()) {
                    logger.debug("End of media");
                }
                endOfMedia = true;
            }

        }
    };

    /**
     * Media RTP input
     */
    private static class MediaRtpInput implements MediaInput {
        /**
         * Received frames
         */
        private FifoBuffer fifo = null;

        /**
         * Constructor
         */
        public MediaRtpInput() {
        }

        /**
         * Add a new video frame
         *
         * @param data Data
         * @param timestamp Timestamp
         */
        public void addFrame(byte[] data, long timestamp) {
            if (fifo != null) {
                fifo.addObject(new MediaSample(data, timestamp));
            }
        }

        /**
         * Add a new video frame
         *
         * @param data Data
         * @param timestamp Timestamp
         * @param marker Marker bit 
         */
        public void addFrame(byte[] data, long timestamp, boolean marker) {
            if (fifo != null) {
                fifo.addObject(new MediaSample(data, timestamp, marker));
            }
        }

        /**
         * M: Added for share progress control @{
         */
        /**
         * Add a new video frame
         * 
         * @param data Data
         * @param timestamp Timestamp
         */
        public void addFrame(byte[] data, long timestamp, RtpExtensionHeader extentionHeader) {
            if (fifo != null) {
                fifo.addObject(new MediaSample(data, timestamp, extentionHeader));
            }
        }

        /** @} */

        /**
         * Open the player
         */
        public void open() {
            fifo = new FifoBuffer();
        }

        /**
         * Close the player
         */
        public void close() {
            fifo.close();
            fifo = null;
        }

        /**
         * Read a media sample (blocking method)
         *
         * @return Media sample
         * @throws MediaException
         */
        public MediaSample readSample() throws MediaException {
            try {
                if (fifo != null) {
                    return (MediaSample)fifo.getObject();
                } else {
                    throw new MediaException("Media input not opened");
                }
            } catch (Exception e) {
                throw new MediaException("Can't read media sample");
            }
        }
    }

    /**
     * M: Added to match the plug-in mechanism while implemented the video
     * share. @{
     */
    /**
     * Preview frame from the camera
     * 
     * @param data Frame
     */
    public void onPreviewFrame(byte[] data) {
        //nothing to do
    }
    /**
     * @}
     */

    /**
     * M: Added for video share progress control @{
     */
    /**
     * Resume video share
     */
    public void resume() {
        if (logger.isActivated()) {
            logger.debug("resume()");
        }
        mShareStarted = true;
    }

    /**
     * Pause video share
     */
    public void pause() {
        if (logger.isActivated()) {
            logger.debug("pause()");
        }
        mShareStarted = false;
    }

    /**
     * Drag to a specific timestamp to share video
     * 
     * @param timestamp The timestamp away from the first frame, in
     *            milliseconds.
     * @return The code to identify whether it is successful.
     */
    public int seekTo(long timestamp) {
        if (logger.isActivated()) {
            logger.debug("seekTo() timestamp: " + timestamp);
        }
        synchronized (mObject) {
            mSeekTimeStamp = timestamp;
        }
        return 0;
    }

    /**
     * Get the total duration of the shared video
     * 
     * @return The total duration
     */
    public long getTotalDuration() {
        if (logger.isActivated()) {
            logger.debug("getTotalDuration() videoDuration: " + videoDuration);
        }
        return videoDuration;
    }

    /**
     * Set the total duration of the shared video
     * 
     * @param duration The total duration
     */
    public void setTotalDuration(long duration) {
        if (logger.isActivated()) {
            logger.debug("setTotalDuration() duration: " + duration);
        }
        videoDuration = duration;
    }

    private void checkStatus() {
        while (!mShareStarted) {
            mTimeStamp++;
            rtpInput.addFrame(new byte[DUMMY_PACKAGE_SIZE], mTimeStamp, new RtpExtensionHeader(
                    mTotalDuration, mShareStarted));
            try {
                Thread.sleep(SLEEP_PERIOD);
            } catch (Exception e) {
                if (logger.isActivated()) {
                    logger.debug("checkStatus error");
                }
                e.printStackTrace();
            } finally {
                if (logger.isActivated()) {
                    logger.debug("checkStatus() add a dummy frame, mTotalDuration: "
                            + mTotalDuration + " mShareStarted: " + mShareStarted);
                }
            }
        }
    }
    /** @} */

    /**
     * M: Added for H264 Support @{
     */
    private void sendParameterSet(){
        // Send SPS/PPS if necessary
        nalRepeat++;
        if (nalRepeat > NALREPEATMAX) {
            nalInit = false;
            nalRepeat = 0;
        }
        if (!nalInit) {
            if (logger.isActivated()) {
                logger.debug("Send SPS");
            }
            mTimeStamp += timestampInc;
            rtpInput.addFrame(sps, mTimeStamp, new RtpExtensionHeader(
                    mTotalDuration, mShareStarted));
            //rtpInput.addFrame(sps, mTimeStamp, false);

            if (logger.isActivated()) {
                logger.debug("Send PPS");
            }
            mTimeStamp += timestampInc;
            rtpInput.addFrame(pps, mTimeStamp, new RtpExtensionHeader(
                    mTotalDuration, mShareStarted));
            //rtpInput.addFrame(pps, mTimeStamp, false);

            nalInit = true;
        }
    }


    private void initEncoderH264(){
        try{
            // H264
            timestampInc = 90000 / selectedVideoCodec.getFramerate();
            NativeH264EncoderParams nativeH264EncoderParams = new NativeH264EncoderParams();
            // Codec dimensions
            nativeH264EncoderParams.setFrameWidth(selectedVideoCodec.getWidth());
            nativeH264EncoderParams.setFrameHeight(selectedVideoCodec.getHeight());
            nativeH264EncoderParams.setFrameRate(selectedVideoCodec.getFramerate());
            nativeH264EncoderParams.setBitRate(selectedVideoCodec.getBitrate());

            // Codec profile and level
            nativeH264EncoderParams.setProfilesAndLevel(selectedVideoCodec.getCodecParams());

            // Codec settings optimization
            nativeH264EncoderParams.setBitRate(96000);
            nativeH264EncoderParams.setFrameRate(10);
            nativeH264EncoderParams.setEncMode(NativeH264EncoderParams.ENCODING_MODE_STREAMING);
            nativeH264EncoderParams.setPacketSize(JavaPacketizer.H264_MAX_PACKET_FRAME_SIZE);
            nativeH264EncoderParams.setSceneDetection(true);
            nativeH264EncoderParams.setIFrameInterval(10);
            nativeH264EncoderParams.setFrameOrientation(0);

            if (logger.isActivated()) {
                logger.info("Init H264Encoder " + selectedVideoCodec.getCodecParams() + " " +
                        selectedVideoCodec.getWidth() + "x" + selectedVideoCodec.getHeight() + " " +
                        selectedVideoCodec.getFramerate() + " "+ selectedVideoCodec.getBitrate());
            }
            int result = NativeH264Encoder.InitEncoder(nativeH264EncoderParams);
            if (result != 0) {
                notifyPlayerEventError("Encoder init failed with error code " + result);
                return;
            }
        }
        catch(Exception e){
            if (logger.isActivated()) {
                logger.debug("Error in Encoder init");
            }
        }
    }

    private void initExtractorAndDecoder(){
        extractor = new MediaExtractor();
                 extractor.setDataSource(filename);
        for (int i = 0; i < extractor.getTrackCount(); i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);

            if (mime.startsWith("video/")) {
                extractor.selectTrack(i);
                videoWidth = format.getInteger(MediaFormat.KEY_WIDTH);
                videoHeight = format.getInteger(MediaFormat.KEY_HEIGHT);
                videoDuration = format.getLong(MediaFormat.KEY_DURATION);
                codec = NativeMediaCodecWrapper.createDecoderByType(mime);
                codec.configure(format, null, null, 0);
                break;
            }
        }
        codec.start();
    }


    /**
     * Init sps and pps
     *
     * @return true if done
     */
    private boolean initNAL() {
        boolean ret = initOneNAL();
        if (ret) {
            ret = initOneNAL();
        }
        return ret;
    }

    /**
     * Init sps or pps
     *
     * @return true if done
     */
    private boolean initOneNAL() {
        byte[] nal = NativeH264Encoder.getNAL();
        if ((nal != null) && (nal.length > 0)) {
            int type = (nal[0] & 0x1f);
            if (type == JavaPacketizer.AVC_NALTYPE_SPS) {
                sps = nal;
                return true;
            } else if (type == JavaPacketizer.AVC_NALTYPE_PPS) {
                pps = nal;
                return true;
            }
        }
        return false;
    }

    /** @} */
}


