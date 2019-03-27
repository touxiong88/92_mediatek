package com.orangelabs.rcs.service.api.client.media.video;

import android.media.MediaCodec;

public class NativeMediaCodecWrapper {
    
    public static int INFO_OUTPUT_BUFFERS_CHANGED = MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED;
    public static int INFO_OUTPUT_FORMAT_CHANGED = MediaCodec.INFO_OUTPUT_FORMAT_CHANGED;
    public static int BUFFER_FLAG_END_OF_STREAM = MediaCodec.BUFFER_FLAG_END_OF_STREAM;
    
    public static MediaCodec createDecoderByType(String mime)
    {
        return MediaCodec.createDecoderByType(mime);
    }
}
