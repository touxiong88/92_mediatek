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

import com.orangelabs.rcs.core.ims.protocol.rtp.DummyPacketGenerator;
import com.orangelabs.rcs.core.ims.protocol.rtp.MediaRegistry;
import com.orangelabs.rcs.core.ims.protocol.rtp.MediaRtpReceiver;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h263.H263Config;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h263.decoder.NativeH263Decoder;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.H264Config;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.decoder.NativeH264Decoder;
import com.orangelabs.rcs.core.ims.protocol.rtp.format.video.H263VideoFormat;
import com.orangelabs.rcs.core.ims.protocol.rtp.format.video.H264VideoFormat;
import com.orangelabs.rcs.core.ims.protocol.rtp.format.video.VideoFormat;
import com.orangelabs.rcs.core.ims.protocol.rtp.media.MediaOutput;
import com.orangelabs.rcs.core.ims.protocol.rtp.media.MediaSample;
import com.orangelabs.rcs.core.ims.protocol.rtp.media.RtpExtensionHeader;
import com.orangelabs.rcs.platform.network.DatagramConnection;
import com.orangelabs.rcs.platform.network.NetworkFactory;
import com.orangelabs.rcs.service.api.client.media.IMediaEventListener;
import com.orangelabs.rcs.service.api.client.media.IMediaRenderer;
import com.orangelabs.rcs.service.api.client.media.MediaCodec;
import com.orangelabs.rcs.utils.NetworkRessourceManager;
import com.orangelabs.rcs.utils.logger.Logger;

import android.graphics.Bitmap;
import android.media.MediaCodec.BufferInfo;
import android.os.RemoteException;
import android.os.SystemClock;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Vector;
 
/**
 * Video RTP renderer. Supports only H.263 and H264 QCIF formats.
 *
 * @author jexa7410
 */
public class VideoRenderer extends IMediaRenderer.Stub {

    /**
     * List of supported video codecs
     */
    public static MediaCodec[] supportedMediaCodecs = {
            new VideoCodec(H264Config.CODEC_NAME, H264VideoFormat.PAYLOAD, H264Config.CLOCK_RATE, H264Config.CODEC_PARAMS,
                    H264Config.FRAME_RATE, H264Config.BIT_RATE, H264Config.VIDEO_WIDTH,
                    H264Config.VIDEO_HEIGHT).getMediaCodec(),
            new VideoCodec(H263Config.CODEC_NAME, H263VideoFormat.PAYLOAD, H263Config.CLOCK_RATE, H263Config.CODEC_PARAMS,
                    H263Config.FRAME_RATE, H263Config.BIT_RATE, H263Config.VIDEO_WIDTH,
                    H263Config.VIDEO_HEIGHT).getMediaCodec()
    };

    /**
     * Selected video codec
     */
    private VideoCodec selectedVideoCodec = null;

    /**
     * Video format
     */
    private VideoFormat videoFormat;

    /**
     * Local RTP port
     */
    private int localRtpPort;

    /**
     * RTP receiver session
     */
    private MediaRtpReceiver rtpReceiver = null;

    /**
     * RTP dummy packet generator
     */
    private DummyPacketGenerator rtpDummySender = null;

    /**
     * RTP media output
     */
    private MediaRtpOutput rtpOutput = null;

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
     * Video surface
     */
    private VideoSurfaceView surface = null;

    /**
     * Media event listeners
     */
    private Vector<IMediaEventListener> listeners = new Vector<IMediaEventListener>();

    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());
    
    /**
     * M: Added to match the plug-in mechanism while implemented the video
     * share. @{
     */
    private IVideoSurfaceViewListener mVideoSurfaceViewListener = null;
    /**
     * @}
     */
    
    /**
     * Temporary connection to reserve the port
     */
    private DatagramConnection temporaryConnection = null;
    /**
     * M: Added for video share progress control @{
     */
    private long mCurrentDuration = -1L;
    private long mCurrentTimestamp = -1L;
    private boolean mStarted = false;
    private Object mLock = new Object();

    /** @} */
    /**
     * Constructor.
     */
    public VideoRenderer() {
        // Set the local RTP port
        localRtpPort = NetworkRessourceManager.generateLocalRtpPort();
        reservePort(localRtpPort);
    }

    /**
     * M: Added to match the plug-in mechanism while implemented the video
     * share. @{
     */
    /**
     * Add listener for video surface view.
     * 
     * @param videoSurfaceViewListener The listener for video surface view.
     */
    public void addVideoSurfaceViewListener(IVideoSurfaceViewListener videoSurfaceViewListener) {
        mVideoSurfaceViewListener = videoSurfaceViewListener;
    }

    /**
     * Remove the listener for video surface view.
     */
    public void removeVideoSurfaceViewListener() {
        mVideoSurfaceViewListener = null;
    }
    /**
     * @}
     */

    /**
     * Constructor Force a video codec.
     *
     * @param codec Video codec
     */
    public VideoRenderer(VideoCodec codec) {
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
     */
    public VideoRenderer(String codec) {
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
     * Open the renderer
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
            // Init the video decoder
            int result;
            if (selectedVideoCodec.getCodecName().equalsIgnoreCase(H264Config.CODEC_NAME)) {
                result = NativeH264Decoder.InitDecoder(selectedVideoCodec.getWidth(),
                        selectedVideoCodec.getHeight());
            } else { // default H263
                result = NativeH263Decoder.InitDecoder(selectedVideoCodec.getWidth(),
                        selectedVideoCodec.getHeight());
            }
            if (result == 0) {
                notifyPlayerEventError("Decoder init failed with error code " + result);
                return;
            }
        } catch (UnsatisfiedLinkError e) {
            notifyPlayerEventError(e.getMessage());
            return;
        }

        try {
            // Init the RTP layer
            releasePort();
            rtpReceiver = new MediaRtpReceiver(localRtpPort);
            rtpDummySender = new DummyPacketGenerator();
            rtpOutput = new MediaRtpOutput();
            rtpOutput.open();
            rtpReceiver.prepareSession(remoteHost, remotePort, rtpOutput, videoFormat);
            rtpDummySender.prepareSession(remoteHost, remotePort, rtpReceiver.getInputStream());
            rtpDummySender.startSession();
        } catch (Exception e) {
            notifyPlayerEventError(e.getMessage());
            return;
        }

        // Player is opened
        opened = true;
        notifyPlayerEventOpened();
    }

    /**
     * M: Added for share progress control, and fix potential NE. @{
     */
    /**
     * Close the renderer
     */
    public void close() {
        if (!opened) {
            // Already closed
            return;
        }

        // Close the RTP layer
        rtpOutput.close();
        rtpReceiver.stopSession();
        rtpDummySender.stopSession();

        // Player is closed
        opened = false;
        notifyPlayerEventClosed();
    }

    private void closeDecoder() {
        if (logger.isActivated()) {
            logger.debug("closeDecoder() entry");
        }
        try {
            // Close the video decoder
            if (selectedVideoCodec.getCodecName().equalsIgnoreCase(H264Config.CODEC_NAME)) {
                NativeH264Decoder.DeinitDecoder();
            } else { // default H263
                NativeH263Decoder.DeinitDecoder();
            }
        } catch (UnsatisfiedLinkError e) {
            if (logger.isActivated()) {
                logger.error("Can't close correctly the video decoder", e);
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

        // Start RTP layer
        rtpReceiver.startSession();

        // Renderer is started
        videoStartTime = SystemClock.uptimeMillis();
        started = true;
        notifyPlayerEventStarted();
    }

    /**
     * Stop the renderer
     */
    public void stop() {
        if (!started) {
            return;
        }

        // Stop RTP layer
        if (rtpReceiver != null) {
            rtpReceiver.stopSession();
        }
        if (rtpDummySender != null) {
            rtpDummySender.stopSession();
        }
        if (rtpOutput != null) {
            rtpOutput.close();
        }
        /**
         * M: Added for share progress control @{
         */
        // Force black screen
        if (surface != null) {
            surface.clearImage();
        }
        /** @} */
        // Stop decoder
        // TODO

        // Renderer is stopped
        started = false;
        videoStartTime = 0L;
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
     * Media RTP output
     */
    private class MediaRtpOutput implements MediaOutput {
        /**
         * Video frame
         */
        private int decodedFrame[];

        /**
         * Bitmap frame
         */
        private Bitmap rgbFrame;

        /**
         * M: Added for share progress control, and fix potential NE. @{
         */
        private boolean mOpened = false;

        /** @} */

        /**
         * Constructor
         */
        public MediaRtpOutput() {
            decodedFrame = new int[selectedVideoCodec.getWidth() * selectedVideoCodec.getHeight()];
            rgbFrame = Bitmap.createBitmap(selectedVideoCodec.getWidth(),
                    selectedVideoCodec.getHeight(),
                    Bitmap.Config.RGB_565);
        }

        /**
         * M: Added for share progress control, and fix potential NE. @{
         */
        /**
         * Open the renderer
         */
        public void open() {
            // Nothing to do
            mOpened = true;
        }

        /** @} */

        /**
         * M: Added for share progress control, and fix potential NE. @{
         */
        /**
         * Close the renderer
         */
        public synchronized void close() {
            if (mOpened) {
                closeDecoder();
                mOpened = false;
            }
        }

        /**
         * Write a media sample
         * 
         * @param sample Sample
         */
        public synchronized void writeSample(MediaSample sample) {
            if (!mOpened) {
                if (logger.isActivated()) {
                    logger.debug("writeSample() player is closed, do not decode");
                }
                return;
            }
            /**
             * M: Added for H264 Support @{
             */
            if (selectedVideoCodec.getCodecName().equalsIgnoreCase(H264Config.CODEC_NAME)) {
                if (checkStatus(sample)) {
                    if (logger.isActivated()) {
                        logger.error("writeSample() H264 it's pause/resume event");
                    }
                    return;
                }
                
                if (NativeH264Decoder.DecodeAndConvert(sample.getData(), decodedFrame) == 1) {
                    if ((decodedFrame.length > 0)) {
                    rgbFrame.setPixels(decodedFrame, 0, selectedVideoCodec.getWidth(), 0, 0,
                            selectedVideoCodec.getWidth(), selectedVideoCodec.getHeight());
                    if (surface != null) {
                        surface.setImage(rgbFrame);
                    }
                    try {
                        if (mVideoSurfaceViewListener != null) {
                            mVideoSurfaceViewListener.onSampleReceived(rgbFrame, mCurrentDuration);
                        }
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    }
                } else {
                    if (logger.isActivated()) {
                        logger.error("Decoding error");
                    }
                }
            } else { // default H263
            /** @} */
                if (checkStatus(sample)) {
                    if (logger.isActivated()) {
                        logger.error("writeSample() H263 it's pause/resume event");
                    }
                    return;
                }
                if (NativeH263Decoder.DecodeAndConvert(sample.getData(), decodedFrame,
                        sample.getTimeStamp()) == 1) {
                    rgbFrame.setPixels(decodedFrame, 0, selectedVideoCodec.getWidth(), 0, 0,
                            selectedVideoCodec.getWidth(), selectedVideoCodec.getHeight());
                    if (surface != null) {
                        surface.setImage(rgbFrame);
                    }
                    try {
                        if (mVideoSurfaceViewListener != null) {
                            mVideoSurfaceViewListener.onSampleReceived(rgbFrame, mCurrentDuration);
                        }
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                } else {
                    if (logger.isActivated()) {
                        logger.error("Decoding error");
                    }
                }
            }
        }

        /** @} */

        /**
         * M: Added for share progress control @{
         */
        private boolean checkStatus(MediaSample sample) {
            try {
                RtpExtensionHeader extensionHeader = sample.getExtensionHeader();
                long timestamp = sample.getTimeStamp();
                long curDuration = -1L;
                boolean started = false;
                if (extensionHeader == null) {
                    if (timestamp > mCurrentTimestamp) {
                        mCurrentTimestamp = timestamp;
                    }
                    if (logger.isActivated()) {
                        logger.debug("checkStatus() extensionHeader is null");
                    }
                    return false;
                } else {
                    started = extensionHeader.isStarted();
                    curDuration = extensionHeader.getDuration();
                    if (logger.isActivated()) {
                        logger.debug("checkStatus() extensionHeader started: " + started
                                + " progress: " + curDuration);
                    }
                }
                if (timestamp > mCurrentTimestamp) {
                    mCurrentDuration = curDuration;
                    mCurrentTimestamp = timestamp;
                    if (mStarted != started) {
                        mStarted = started;
                        if (started) {
                            mVideoSurfaceViewListener.onResumed(mCurrentDuration);
                        } else {
                            mVideoSurfaceViewListener.onPaused(mCurrentDuration);
                        }
                        return true;
                    }
                } else {
                    if (logger.isActivated()) {
                        logger.debug("checkStatus() current packet is in wrong order: " + timestamp);
                    }
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            return false;
        }
        /** @} */
    }
}

