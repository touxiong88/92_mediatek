/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 * 
 * MediaTek Inc. (C) 2010. All rights reserved.
 * 
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

#ifndef FLV_EXTRACTOR_H_
#define FLV_EXTRACTOR_H_

#include <media/stagefright/MediaExtractor.h>
#include <media/stagefright/MediaBuffer.h>
#include <utils/Vector.h>
#include <utils/threads.h>
#include "parser.h"

#include <cutils/log.h>
#undef LOG_TAG
#define LOG_TAG "FlvExtractor"


namespace android {


#define MAX_VIDEO_INPUT_SIZE (1920*1080*3/2)

#define MAX_AUDIO_INPUT_SIZE (1024*20)

#define FLV_CACHE_POOL_LOW   2
#define FLV_CACHE_POOL_HIGH  10

#define FLV_THUMBNAIL_SCAN_SIZE 10

#define FLV_INITIAL_TAG_COUNT_THD 2048



uint8_t* flv_write_hevc(uint8_t* sps, int sps_len, 
                          uint8_t* pps, int pps_len,
                          int* real_length);

uint32_t flv_io_read_func_ptr(void *aSource, void *aBuffer, uint32_t aSize);
uint32_t flv_io_write_func_ptr(void *aSource, void *aBuffer, uint32_t aSize);
uint64_t flv_io_seek_func_ptr(void *aSource, uint64_t aOffset,FLV_SEEK_FLAG flag);

typedef Vector<flv_tag_str> VideoFrameVector;
typedef Vector<flv_tag_str> AudioFrameVector;


struct TrackInfo {
        uint32_t mTrackNum;
        sp<MetaData> mMeta;    
        void * mCodecSpecificData;
        uint32_t mCodecSpecificSize;       
     };

enum {
   FLV_INIT,
   FLV_PLAY,
   FLV_PAUSE,
   FLV_SEEK,
   FLV_STOP,
}FLV_STATUS;

enum Type {
    VIDEO,
    AUDIO,
    OTHER
};

enum CacheType{

   CACHE_VIDEO_KEY_FRAME ,//cache only video key frames when find thumbnail
   CACHE_FRAME           ,//cache 1 frame in normal flow
   CACHE_ANYHOW          // cache total 5 frame anyhow until EOS for prepare
};

struct flv_mp3_str {
     uint32_t  frame_size;
     uint32_t  sampling_rate;
     uint32_t  channels;
     uint32_t  bitrate;     
};


class FLVExtractor : public MediaExtractor {
public:
    // Extractor assumes ownership of "source".
    FLVExtractor(const sp<DataSource> &source);
    virtual size_t countTracks();
    virtual sp<MediaSource> getTrack(size_t index);
    virtual sp<MetaData> getTrackMetaData(size_t index, uint32_t flags);
    virtual sp<MetaData> getMetaData();
    virtual uint32_t flags() const;
    bool      bIsValidFlvFile;

protected:
    virtual ~FLVExtractor();
    bool ParseFLV();
    bool FLVSeekTo(int64_t targetNPT);    
    void findThumbnail();
    uint32_t  parseNALSize(const uint8_t *data) ;

public:
    FLV_ERROR_TYPE CacheMore(CacheType HowCache);
    MediaBuffer* DequeueVideoFrame(int64_t targetSampleTimeUs = -1);
    MediaBuffer* DequeueAudioFrame(int64_t targetSampleTimeUs = -1);

    void ClearVideoFrameQueue();
    void ClearAudioFrameQueue();

    friend uint32_t flv_io_read_func_ptr(void *aSource, void *aBuffer, uint32_t aSize);
    friend uint32_t flv_io_write_func_ptr(void *aSource, void *aBuffer, uint32_t aSize);
    friend uint64_t flv_io_seek_func_ptr(void *aSource, uint64_t aOffset,FLV_SEEK_FLAG flag);

    friend class FLVSource;

private:
    struct TrackInfo {
        unsigned long mTrackNum;
        sp<MetaData> mMeta;
    };

    flvParser* mflvParser;

    FLVExtractor(const FLVExtractor &);
    FLVExtractor &operator=(const FLVExtractor &);

protected:
	sp<DataSource> mDataSource;
	off64_t	  iDataSourceLength;
	off64_t	  iFlvParserReadOffset;
    uint64_t  iDurationMs;
    bool      bSeekable;
    bool      bThumbnailMode;
    bool      bExtractedThumbnails;
    
    bool      bHaveParsed;
    bool      bHasVideo;
    bool      bHasVideoTrack ;
    bool      bHasAudio;
    uint32_t  iWidth;
    uint32_t  iHeight;
    flv_tag_str* mTag;
    FLV_VIDEO_CODEC_ID video_codec_id;
    FLV_AUDIO_CODEC_ID audio_codec_id;
    uint32_t mStatus;
    int64_t mtargetSampleTimeUs;
    int32_t iChannel_cnt;
    int32_t iSampleRate;
    int32_t iSampleSize;
    uint32_t iDecVideoFramesCnt;
    uint32_t iDecAudioFramesCnt;
	Vector<TrackInfo> mTracks;
	VideoFrameVector mVideoFrames;	    
	AudioFrameVector mAudioFrames; 
    VideoFrameVector mVideoConfigs;	    
	AudioFrameVector mAudioConfigs;	   
    android::Mutex mCacheLock;
     uint32_t mNALLengthSize ;
    

};

}

#endif // FLV_EXTRACTOR_H_
