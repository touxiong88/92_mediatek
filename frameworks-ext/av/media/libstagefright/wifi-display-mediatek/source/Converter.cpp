/*
 * Copyright 2012, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//#define LOG_NDEBUG 0
#define LOG_TAG "Converter"
#include <utils/Log.h>

#include "Converter.h"

#include "MediaPuller.h"

#include <cutils/properties.h>
#include <gui/SurfaceTextureClient.h>
#include <media/ICrypto.h>
#include <media/stagefright/foundation/ABuffer.h>
#include <media/stagefright/foundation/ADebug.h>
#include <media/stagefright/foundation/AMessage.h>
#include <media/stagefright/MediaBuffer.h>
#include <media/stagefright/MediaCodec.h>
#include <media/stagefright/MediaDefs.h>
#include <media/stagefright/MediaErrors.h>

#include <OMX_Video.h>


#ifndef ANDROID_DEFAULT_CODE
#include <dlfcn.h>
#include "../DataPathTrace.h"

#define MAX_VIDEO_QUEUE_BUFFER (3)
#define MAX_AUDIO_QUEUE_BUFFER (5)

#ifdef USE_MMPROFILE
#include <linux/mmprofile.h>
MMP_Event MMP_WFD_CONVERTER;
MMP_Event MMP_WFD_CONVERTERQI[2];
MMP_Event MMP_WFD_CONVERTERDO[2];
#endif

#define SUPPORT_SLICE_MODE_PADDING (0)

#endif

//#define SEC_WFD_VIDEO_PATH_TEST

namespace android {

Converter::Converter(
        const sp<AMessage> &notify,
        const sp<ALooper> &codecLooper,
        const sp<AMessage> &format,
        bool usePCMAudio)
    : mInitCheck(NO_INIT),
      mNotify(notify),
      mCodecLooper(codecLooper),
      mInputFormat(format),
      mIsVideo(false),
      mIsPCMAudio(usePCMAudio),
      mNeedToManuallyPrependSPSPPS(false),
      mDoMoreWorkPending(false)
#if ENABLE_SILENCE_DETECTION
      ,mFirstSilentFrameUs(-1ll)
      ,mInSilentMode(false)
#endif
      ,mNumFramesToDrop(0)
#ifdef USE_MTKSOURCE
      ,mUsebufferPointer(0)
#endif
#ifndef ANDROID_DEFAULT_CODE
	,mForceBlackScreenNow(false)
#endif
    {
    AString mime;
    CHECK(mInputFormat->findString("mime", &mime));

    if (!strncasecmp("video/", mime.c_str(), 6)) {
        mIsVideo = true;
    }
	
#ifdef USE_MTKSOURCE  
 
    if(mInputFormat->findInt32("usebufferpointer", &mUsebufferPointer))
    {
        ALOGI("mUsebufferPointer fournd and mUsebufferPointer = %d", mUsebufferPointer);
    }else{
        ALOGI("mUsebufferPointer is not found");
    }
#endif

#ifdef USE_MMPROFILE

    char name[128];
    

    MMP_Event  MMP_WFD_DEBUG = MMProfileFindEvent(MMP_RootEvent, "WFD_Source");
    if(MMP_WFD_DEBUG !=0){

	    MMP_WFD_CONVERTER = MMProfileRegisterEvent(MMP_WFD_DEBUG, "ConverterEvent");
	    MMProfileEnableEvent(MMP_WFD_CONVERTER,1); 
	   
	    sprintf(name, "%s", mIsVideo?"videoConverterQueueIn":"audioConverterQueueIn"); 
	    MMP_WFD_CONVERTERQI[mIsVideo] = MMProfileRegisterEvent(MMP_WFD_DEBUG, name);
	    MMProfileEnableEvent(MMP_WFD_CONVERTERQI[mIsVideo],1); 
		
	    sprintf(name, "%s", mIsVideo?"videoConverterDeQueueOut":"audioConverterDeQueueOut"); 
	    MMP_WFD_CONVERTERDO[mIsVideo] = MMProfileRegisterEvent(MMP_WFD_DEBUG, name);
	    MMProfileEnableEvent(MMP_WFD_CONVERTERDO[mIsVideo],1); 
    }else{

		ALOGE("%s can not find the WFD_Source Event",name);
	}
    
#endif  


    CHECK(!usePCMAudio || !mIsVideo);

#ifdef USE_MMPROFILE
      if(MMP_WFD_CONVERTER !=0){
		MMProfileLogMetaStringEx(MMP_WFD_CONVERTER, MMProfileFlagStart, mIsVideo,usePCMAudio,"initEncoder:mIsVideo+usePCMAudio");
	}
#endif

    mInitCheck = initEncoder();

    if (mInitCheck != OK) {
        if (mEncoder != NULL) {
            mEncoder->release();
            mEncoder.clear();
        }
    }
#ifdef USE_MMPROFILE
      if(MMP_WFD_CONVERTER!=0){
		MMProfileLogMetaStringEx(MMP_WFD_CONVERTER, MMProfileFlagEnd, mIsVideo,usePCMAudio,"initEncoder done:mIsVideo+usePCMAudio");
	}
#endif	

#ifndef ANDROID_DEFAULT_CODE
    if (mIsVideo)
    {
        mBitrateCtrler.bcHandle = NULL;
        mBitrateCtrler.InitBC = NULL;
        mBitrateCtrler.SetOneFrameBits = NULL;
        mBitrateCtrler.CheckSkip = NULL;
        mBitrateCtrler.UpdownLevel = NULL;
        mBitrateCtrler.GetStatus = NULL;
        mBitrateCtrler.SetTolerantBitrate = NULL;
        mBitrateCtrler.DeInitBC = NULL;
        mBitrateCtrlerLib = dlopen("/system/lib/libbrctrler.so", RTLD_LAZY);
        if (mBitrateCtrlerLib == NULL)
        {
            ALOGE("[ERROR] dlopen failed, %s", dlerror());
        }
        else
        {
            mBitrateCtrler.InitBC = (int (*)(void **))dlsym(mBitrateCtrlerLib, "InitBC");
            mBitrateCtrler.SetOneFrameBits = (int (*)(void *, int, bool))dlsym(mBitrateCtrlerLib, "SetOneFrameBits");
            mBitrateCtrler.CheckSkip = (bool (*)(void *))dlsym(mBitrateCtrlerLib, "CheckSkip");
            mBitrateCtrler.UpdownLevel = (int (*)(void*, int))dlsym(mBitrateCtrlerLib, "UpdownLevel");
            mBitrateCtrler.GetStatus = (int (*)(void *, int))dlsym(mBitrateCtrlerLib, "GetStatus");
            mBitrateCtrler.SetTolerantBitrate = (int (*)(void *, int))dlsym(mBitrateCtrlerLib, "SetTolerantBitrate");
            mBitrateCtrler.DeInitBC = (int (*)(void *))dlsym(mBitrateCtrlerLib, "DeInitBC");

            mBitrateCtrler.InitBC(&mBitrateCtrler.bcHandle);
            int32_t video_bitrate=1;
            mOutputFormat->findInt32("bitrate", &video_bitrate);
            if (mBitrateCtrler.bcHandle != NULL)
            {
                mBitrateCtrler.SetTolerantBitrate(mBitrateCtrler.bcHandle, video_bitrate);
            }
        }
    }
    else
    {
        mBitrateCtrlerLib = NULL;
    }
#endif
}


static void ReleaseMediaBufferReference(const sp<ABuffer> &accessUnit) {
    void *mbuf;
    if (accessUnit->meta()->findPointer("mediaBuffer", &mbuf)
            && mbuf != NULL) {
        ALOGV("releasing mbuf %p", mbuf);

        accessUnit->meta()->setPointer("mediaBuffer", NULL);

        static_cast<MediaBuffer *>(mbuf)->release();
        mbuf = NULL;
    }
}

Converter::~Converter() {
   // CHECK(mEncoder == NULL);
#ifndef ANDROID_DEFAULT_CODE
    releaseEncoder();
    if (mBitrateCtrlerLib != NULL)
    {
        if (mBitrateCtrler.bcHandle != NULL)
        {
            mBitrateCtrler.DeInitBC(mBitrateCtrler.bcHandle);
        }
        dlclose(mBitrateCtrlerLib);
    }
#endif
}

void Converter::shutdownAsync() {
    ALOGI("shutdown %s",mIsVideo ? "video" : "audio");
    (new AMessage(kWhatShutdown, id()))->post();
}

status_t Converter::initCheck() const {
    return mInitCheck;
}

size_t Converter::getInputBufferCount() const {
   
    return mEncoderInputBuffers.size();
}

sp<AMessage> Converter::getOutputFormat() const {
    return mOutputFormat;
}

bool Converter::needToManuallyPrependSPSPPS() const {
    return mNeedToManuallyPrependSPSPPS;
}

static int32_t getBitrate(const char *propName, int32_t defaultValue) {
    char val[PROPERTY_VALUE_MAX];
    if (property_get(propName, val, NULL)) {
        char *end;
        unsigned long x = strtoul(val, &end, 10);

        if (*end == '\0' && end > val && x > 0) {
            return x;
        }
    }

    return defaultValue;
}
void Converter::releaseEncoder() {
    if (mEncoder == NULL) {
        return;
    }

    mEncoder->release();
    mEncoder.clear();

    while (!mInputBufferQueue.empty()) {
        sp<ABuffer> accessUnit = *mInputBufferQueue.begin();
        mInputBufferQueue.erase(mInputBufferQueue.begin());

        ReleaseMediaBufferReference(accessUnit);
    }

    for (size_t i = 0; i < mEncoderInputBuffers.size(); ++i) {
        sp<ABuffer> accessUnit = mEncoderInputBuffers.itemAt(i);
        ReleaseMediaBufferReference(accessUnit);
    }

    mEncoderInputBuffers.clear();
    mEncoderOutputBuffers.clear();
}
status_t Converter::initEncoder() {
    AString inputMIME;
    CHECK(mInputFormat->findString("mime", &inputMIME));

    AString outputMIME;
    bool isAudio = false;
    if (!strcasecmp(inputMIME.c_str(), MEDIA_MIMETYPE_AUDIO_RAW)) {
        if (mIsPCMAudio) {
            outputMIME = MEDIA_MIMETYPE_AUDIO_RAW;
        } else {
            outputMIME = MEDIA_MIMETYPE_AUDIO_AAC;
        }
        isAudio = true;
    } else if (!strcasecmp(inputMIME.c_str(), MEDIA_MIMETYPE_VIDEO_RAW)) {
        outputMIME = MEDIA_MIMETYPE_VIDEO_AVC;
    } else {
        TRESPASS();
    }

    if (!mIsPCMAudio) {
        mEncoder = MediaCodec::CreateByType(
                mCodecLooper, outputMIME.c_str(), true /* encoder */);

        if (mEncoder == NULL) {
            return ERROR_UNSUPPORTED;
        }
    }

    mOutputFormat = mInputFormat->dup();

    if (mIsPCMAudio) {
        return OK;
    }

    mOutputFormat->setString("mime", outputMIME.c_str());

     int32_t videoBitrate = getBitrate("media.wfd.video-bitrate", 5000000);
    int32_t audioBitrate = getBitrate("media.wfd.audio-bitrate", 128000);
	

    ALOGI("using audio bitrate of %d bps, video bitrate of %d bps",
          audioBitrate, videoBitrate);

    if (isAudio) {
        mOutputFormat->setInt32("bitrate", audioBitrate);
#ifndef ANDROID_DEFAULT_CODE
     //   mOutputFormat->setInt32("inputbuffercnt", 8);
#endif
    } else {
        mOutputFormat->setInt32("bitrate", videoBitrate);
        mOutputFormat->setInt32("bitrate-mode", OMX_Video_ControlRateConstant);
	mOutputFormat->setInt32("frame-rate", 30);
        
        mOutputFormat->setInt32("i-frame-interval", 4);  // 4.2.2 default is 15
        
        // Configure encoder to use intra macroblock refresh mode
        mOutputFormat->setInt32("intra-refresh-mode", OMX_VIDEO_IntraRefreshCyclic);

        int width, height, mbs;
        if (!mOutputFormat->findInt32("width", &width)
                || !mOutputFormat->findInt32("height", &height)) {
            return ERROR_UNSUPPORTED;
        }

#ifndef ANDROID_DEFAULT_CODE
	 int32_t profile;
	   int32_t level;
	if(mInputFormat->findInt32("profile", &profile)){
		mOutputFormat->setInt32("profile", profile);
	}
	if(mInputFormat->findInt32("level", &level)){
		mOutputFormat->setInt32("level", level);
	}
	int32_t fps=30;
	if(mInputFormat->findInt32("frame-rate", &fps)){
			mOutputFormat->setInt32("frame-rate", fps);
	}
	
	
	int32_t video_bitrate=4800000;
	if(width >= 1920 && height >= 1080){
		video_bitrate=11000000;
	}
	char video_bitrate_char[PROPERTY_VALUE_MAX];

	 if (property_get("media.wfd.video-bitrate", video_bitrate_char, NULL)){		
		int32_t temp_video_bitrate = atoi(video_bitrate_char);
		if(temp_video_bitrate > 0){
			video_bitrate = temp_video_bitrate ;
		}
	}
	

	   
      mOutputFormat->setInt32("bitrate", video_bitrate);

	ALOGI("reset video encoder parameters:bitrate=%d,fps=%d,profile=%d,level=%d",
				video_bitrate,fps,profile,level);
		 
#endif
		

        // Update macroblocks in a cyclic fashion with 10% of all MBs within
        // frame gets updated at one time. It takes about 10 frames to
        // completely update a whole video frame. If the frame rate is 30,
        // it takes about 333 ms in the best case (if next frame is not an IDR)
        // to recover from a lost/corrupted packet.
        mbs = (((width + 15) / 16) * ((height + 15) / 16) * 10) / 100;
        mOutputFormat->setInt32("intra-refresh-CIR-mbs", mbs);

#ifndef ANDROID_DEFAULT_CODE
       int32_t useSliceMode=0;
		  
       if(mInputFormat->findInt32("slice-mode", &useSliceMode) && useSliceMode ==1){		 
		  ALOGI("useSliceMode =%d",useSliceMode);
	         int32_t buffersize=60*1024;
		 
		  char buffersize_value[PROPERTY_VALUE_MAX];
		  if(property_get("wfd.slice.size", buffersize_value, NULL) ){
		  	buffersize = atoi(buffersize_value)*1024;
		  }
		  mOutputFormat->setInt32("outputbuffersize", buffersize); // bs buffer size is 15k for slice mode
		  ALOGI("slice mode: output buffer size=%d KB",buffersize/1024);
       }

        mOutputFormat->setInt32("inputbuffercnt", 4); // yuv buffer count is 4        
        mOutputFormat->setInt32("bitrate-mode", 0x7F000001);//OMX_Video_ControlRateMtkWFD
#endif
    }

    ALOGI("output format is '%s'", mOutputFormat->debugString(0).c_str());

    mNeedToManuallyPrependSPSPPS = false;

    status_t err = NO_INIT;

    if (!isAudio) {
        sp<AMessage> tmp = mOutputFormat->dup();
        tmp->setInt32("prepend-sps-pps-to-idr-frames", 1);

        err = mEncoder->configure(
                tmp,
                NULL /* nativeWindow */,
                NULL /* crypto */,
                MediaCodec::CONFIGURE_FLAG_ENCODE);

        if (err == OK) {
            // Encoder supported prepending SPS/PPS, we don't need to emulate
            // it.
            mOutputFormat = tmp;
        } else {
            mNeedToManuallyPrependSPSPPS = true;

            ALOGI("We going to manually prepend SPS and PPS to IDR frames.");
        }
    }

    if (err != OK) {
        // We'll get here for audio or if we failed to configure the encoder
        // to automatically prepend SPS/PPS in the case of video.

        err = mEncoder->configure(
                    mOutputFormat,
                    NULL /* nativeWindow */,
                    NULL /* crypto */,
                    MediaCodec::CONFIGURE_FLAG_ENCODE);
    }


    if (err != OK) {
        return err;
    }

    err = mEncoder->start();

    if (err != OK) {
        return err;
    }

    err = mEncoder->getInputBuffers(&mEncoderInputBuffers);

    if (err != OK) {
        return err;
    }

    return mEncoder->getOutputBuffers(&mEncoderOutputBuffers);
}

void Converter::notifyError(status_t err) {
    sp<AMessage> notify = mNotify->dup();
    notify->setInt32("what", kWhatError);
    notify->setInt32("err", err);
    notify->post();
}

// static
bool Converter::IsSilence(const sp<ABuffer> &accessUnit) {
    const uint8_t *ptr = accessUnit->data();
    const uint8_t *end = ptr + accessUnit->size();
    while (ptr < end) {
        if (*ptr != 0) {
            return false;
        }
        ++ptr;
    }

    return true;
}

void Converter::onMessageReceived(const sp<AMessage> &msg) {
    switch (msg->what()) {
        case kWhatMediaPullerNotify:
        {
            int32_t what;
            CHECK(msg->findInt32("what", &what));

            if (!mIsPCMAudio && mEncoder == NULL) {
                ALOGI("got msg '%s' after encoder shutdown.",
                      msg->debugString().c_str());

                if (what == MediaPuller::kWhatAccessUnit) {
                    sp<ABuffer> accessUnit;
                    CHECK(msg->findBuffer("accessUnit", &accessUnit));

                    ReleaseMediaBufferReference(accessUnit);
                    }
                break;
            }

            if (what == MediaPuller::kWhatEOS) {
                mInputBufferQueue.push_back(NULL);

                feedEncoderInputBuffers();

                scheduleDoMoreWork();
            } else {
                CHECK_EQ(what, MediaPuller::kWhatAccessUnit);

                sp<ABuffer> accessUnit;
                CHECK(msg->findBuffer("accessUnit", &accessUnit));
                if (mIsVideo && mNumFramesToDrop) {
                    --mNumFramesToDrop;
                    ALOGI("dropping frame mNumFramesToDrop=%d ",mNumFramesToDrop);
                    ReleaseMediaBufferReference(accessUnit);
                    break;
                }

#if 0
                void *mbuf;
                if (accessUnit->meta()->findPointer("mediaBuffer", &mbuf)
                        && mbuf != NULL) {
                    ALOGI("queueing mbuf %p", mbuf);
                }
#endif

#if ENABLE_SILENCE_DETECTION
                if (!mIsVideo) {
                    if (IsSilence(accessUnit)) {
                        if (mInSilentMode) {
                            break;
                        }

                        int64_t nowUs = ALooper::GetNowUs();

                        if (mFirstSilentFrameUs < 0ll) {
                            mFirstSilentFrameUs = nowUs;
                        } else if (nowUs >= mFirstSilentFrameUs + 10000000ll) {
                            mInSilentMode = true;
                            ALOGI("audio in silent mode now.");
                            break;
                        }
                    } else {
                        if (mInSilentMode) {
                            ALOGI("audio no longer in silent mode.");
                        }
                        mInSilentMode = false;
                        mFirstSilentFrameUs = -1ll;
                    }
                }
#endif

#ifndef ANDROID_DEFAULT_CODE
                int32_t maxSize = mIsVideo ? MAX_VIDEO_QUEUE_BUFFER : MAX_AUDIO_QUEUE_BUFFER;
                if(mInputBufferQueue.size() >= maxSize)//for rsss warning as pull new buffer 
                {
                    void *mediaBuffer;
                
                    sp<ABuffer> tmpbuffer = *mInputBufferQueue.begin();
                    if (tmpbuffer->meta()->findPointer("mediaBuffer", &mediaBuffer)
                         && mediaBuffer != NULL) {
                
                #ifdef USE_MMPROFILE
                        if(MMP_WFD_CONVERTER !=0){
                
                		    int64_t timeUs = 0ll;
                		    tmpbuffer->meta()->findInt64("timeUs", &timeUs);			
                		    MMProfileLogMetaStringEx(MMP_WFD_CONVERTER, MMProfileFlagPulse,  0, timeUs/1000,"video mInputBufferQueue>MAX_QUEUE_BUFFER");
                	    }
                #endif
                	    ALOGI("[video buffer]video queuebuffer >= %d release oldest buffer=%p,refcount=%d",maxSize,mediaBuffer,((MediaBuffer *)mediaBuffer)->refcount());			
                        ReleaseMediaBufferReference(tmpbuffer);
                	    mediaBuffer = NULL;	
                    }
                    
                    if(!mIsVideo) ALOGI("[audio buffer]audio queuebuffer >= %d release oldest buffer",maxSize);			
                    mInputBufferQueue.erase(mInputBufferQueue.begin());
                }
#endif
                mInputBufferQueue.push_back(accessUnit);

                feedEncoderInputBuffers();

                scheduleDoMoreWork();
            }
            break;
        }

        case kWhatEncoderActivity:
        {
#if 0
            int64_t whenUs;
            if (msg->findInt64("whenUs", &whenUs)) {
                int64_t nowUs = ALooper::GetNowUs();
                ALOGI("[%s] kWhatEncoderActivity after %lld us",
                      mIsVideo ? "video" : "audio", nowUs - whenUs);
            }
#endif

            mDoMoreWorkPending = false;

            if (mEncoder == NULL) {
                break;
            }

            status_t err = doMoreWork();

            if (err != OK) {
                notifyError(err);
            } else {
                scheduleDoMoreWork();
            }
            break;
        }

        case kWhatRequestIDRFrame:
        {
            if (mEncoder == NULL) {
                break;
            }

            if (mIsVideo) {
                ALOGI("requesting IDR frame");
                mEncoder->requestIDRFrame();
            }
            break;
        }

        case kWhatShutdown:
        {
            ALOGI("shutting down %s encoder", mIsVideo ? "video" : "audio");

	     releaseEncoder();

            AString mime;
            CHECK(mInputFormat->findString("mime", &mime));
            ALOGI("encoder (%s) shut down.", mime.c_str());
            break;
        }

        case kWhatDropAFrame:
        {
            //++mNumFramesToDrop;
	     ALOGI("kWhatDropAFrame mNumFramesToDrop=%d ",mNumFramesToDrop);
            break;
        }

        default:
            TRESPASS();
    }
}

void Converter::scheduleDoMoreWork() {
    if (mIsPCMAudio) {
        // There's no encoder involved in this case.
        return;
    }

    if (mDoMoreWorkPending) {
        return;
    }

    mDoMoreWorkPending = true;

#if 1
    if (mEncoderActivityNotify == NULL) {
        mEncoderActivityNotify = new AMessage(kWhatEncoderActivity, id());
    }
    mEncoder->requestActivityNotification(mEncoderActivityNotify->dup());
#else
    sp<AMessage> notify = new AMessage(kWhatEncoderActivity, id());
    notify->setInt64("whenUs", ALooper::GetNowUs());
    mEncoder->requestActivityNotification(notify);
#endif
}

status_t Converter::feedRawAudioInputBuffers() {
    // Split incoming PCM audio into buffers of 6 AUs of 80 audio frames each
    // and add a 4 byte header according to the wifi display specs.

    while (!mInputBufferQueue.empty()) {
        sp<ABuffer> buffer = *mInputBufferQueue.begin();
        mInputBufferQueue.erase(mInputBufferQueue.begin());

        int16_t *ptr = (int16_t *)buffer->data();
        int16_t *stop = (int16_t *)(buffer->data() + buffer->size());
        while (ptr < stop) {
            *ptr = htons(*ptr);
            ++ptr;
        }

        static const size_t kFrameSize = 2 * sizeof(int16_t);  // stereo
        static const size_t kFramesPerAU = 80;
        static const size_t kNumAUsPerPESPacket = 6;

        if (mPartialAudioAU != NULL) {
            size_t bytesMissingForFullAU =
                kNumAUsPerPESPacket * kFramesPerAU * kFrameSize
                - mPartialAudioAU->size() + 4;

            size_t copy = buffer->size();
            if(copy > bytesMissingForFullAU) {
                copy = bytesMissingForFullAU;
            }

            memcpy(mPartialAudioAU->data() + mPartialAudioAU->size(),
                   buffer->data(),
                   copy);

            mPartialAudioAU->setRange(0, mPartialAudioAU->size() + copy);

            buffer->setRange(buffer->offset() + copy, buffer->size() - copy);

            int64_t timeUs;
            CHECK(buffer->meta()->findInt64("timeUs", &timeUs));

            int64_t copyUs = (int64_t)((copy / kFrameSize) * 1E6 / 48000.0);
            timeUs += copyUs;
            buffer->meta()->setInt64("timeUs", timeUs);

            if (bytesMissingForFullAU == copy) {
                sp<AMessage> notify = mNotify->dup();
                notify->setInt32("what", kWhatAccessUnit);
                notify->setBuffer("accessUnit", mPartialAudioAU);
                notify->post();

                mPartialAudioAU.clear();
            }
        }

        while (buffer->size() > 0) {
            sp<ABuffer> partialAudioAU =
                new ABuffer(
                        4
                        + kNumAUsPerPESPacket * kFrameSize * kFramesPerAU);

            uint8_t *ptr = partialAudioAU->data();
            ptr[0] = 0xa0;  // 10100000b
            ptr[1] = kNumAUsPerPESPacket;
            ptr[2] = 0;  // reserved, audio _emphasis_flag = 0

            static const unsigned kQuantizationWordLength = 0;  // 16-bit
            static const unsigned kAudioSamplingFrequency = 2;  // 48Khz
            static const unsigned kNumberOfAudioChannels = 1;  // stereo

            ptr[3] = (kQuantizationWordLength << 6)
                    | (kAudioSamplingFrequency << 3)
                    | kNumberOfAudioChannels;

            size_t copy = buffer->size();
            if (copy > partialAudioAU->size() - 4) {
                copy = partialAudioAU->size() - 4;
            }

            memcpy(&ptr[4], buffer->data(), copy);

            partialAudioAU->setRange(0, 4 + copy);
            buffer->setRange(buffer->offset() + copy, buffer->size() - copy);

            int64_t timeUs;
            CHECK(buffer->meta()->findInt64("timeUs", &timeUs));

            partialAudioAU->meta()->setInt64("timeUs", timeUs);

            int64_t copyUs = (int64_t)((copy / kFrameSize) * 1E6 / 48000.0);
            timeUs += copyUs;
            buffer->meta()->setInt64("timeUs", timeUs);

            if (copy == partialAudioAU->capacity() - 4) {
                sp<AMessage> notify = mNotify->dup();
                notify->setInt32("what", kWhatAccessUnit);
                notify->setBuffer("accessUnit", partialAudioAU);
                notify->post();

                partialAudioAU.clear();
                continue;
            }

            mPartialAudioAU = partialAudioAU;
        }
    }

    return OK;
}

status_t Converter::feedEncoderInputBuffers() {
    if (mIsPCMAudio) {
        return feedRawAudioInputBuffers();
    }

    while (!mInputBufferQueue.empty()
            && !mAvailEncoderInputIndices.empty()) {
        sp<ABuffer> buffer = *mInputBufferQueue.begin();
        mInputBufferQueue.erase(mInputBufferQueue.begin());//release the mediabuffer newed in MediaPuller 

        size_t bufferIndex = *mAvailEncoderInputIndices.begin();
        mAvailEncoderInputIndices.erase(mAvailEncoderInputIndices.begin());

        int64_t timeUs = 0ll;
        uint32_t flags = 0;

        if (buffer != NULL) {
            CHECK(buffer->meta()->findInt64("timeUs", &timeUs));

#ifdef USE_MTKSOURCE
           if (mUsebufferPointer == 1)
           {
                  void* data = (void*)(*((int32_t *)(buffer->data())));
     		     int32_t framesize = *((int32_t *)((int32_t)(buffer->data()) + 4));
		     ALOGV("[video buffer]feedEncoderInputBuffers data = %x, framesize = %d", data, framesize);
                   memcpy(mEncoderInputBuffers.itemAt(bufferIndex)->data(),
                        data,
                        framesize);					
            

	    } else{
		     memcpy(mEncoderInputBuffers.itemAt(bufferIndex)->data(),
                   buffer->data(),
                   buffer->size());
	    }

#else
            memcpy(mEncoderInputBuffers.itemAt(bufferIndex)->data(),
                   buffer->data(),
                   buffer->size());
#endif



#ifndef ANDROID_DEFAULT_CODE		 
		int64_t timeNow = ALooper::GetNowUs();
		sp<WfdDebugInfo> debugInfo= defaultWfdDebugInfo();
		debugInfo->addTimeInfoByKey(mIsVideo, timeUs, "EnIn", timeNow/1000);
#endif


            void *mediaBuffer;
            if (buffer->meta()->findPointer("mediaBuffer", &mediaBuffer)
                    && mediaBuffer != NULL) {
                mEncoderInputBuffers.itemAt(bufferIndex)->meta()
                    ->setPointer("mediaBuffer", mediaBuffer);
               // ALOGI("[video buffer]setPointer  mediaBuffer=%p",mediaBuffer);
               buffer->meta()->setPointer("mediaBuffer", NULL);
            }
        } else {
            flags = MediaCodec::BUFFER_FLAG_EOS;
        }
#ifndef ANDROID_DEFAULT_CODE
        if (mBitrateCtrlerLib != NULL && mBitrateCtrler.CheckSkip != NULL)
        {
            if (mBitrateCtrler.CheckSkip(mBitrateCtrler.bcHandle))
            {
                mEncoder->setVEncSkipFrame();
            }
        }
#endif

        status_t err = mEncoder->queueInputBuffer(
                bufferIndex, 0, (buffer == NULL) ? 0 : buffer->size(),
                timeUs, flags);

        if (err != OK) {
	     ALOGE("% queueInputBuffer fail",mIsVideo?"video":"audio");
            return err;
        }
		
#ifdef USE_MMPROFILE
      if(MMP_WFD_CONVERTERQI[mIsVideo] !=0){
		MMProfileLogMetaStringEx(MMP_WFD_CONVERTERQI[mIsVideo], MMProfileFlagPulse, (uint32_t)bufferIndex, (uint32_t)(timeUs / 1000),"queueInputBuffer done");
	}
#endif			
    }

    return OK;
}

#if (defined SEC_WFD_VIDEO_PATH_SUPPORT) && (defined SEC_WFD_VIDEO_PATH_TEST)

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/time.h>
#include <uree/system.h>
#include <uree/mem.h>
#include <tz_cross/ta_test.h>
#include <tz_cross/ta_mem.h>
#include <unistd.h>

int i4dump_sec_buf_to_from_norm_buf
(
unsigned int   ui4_sec_buf_handle,
unsigned char* puc_norm_buf,
unsigned int   ui4_sz,
unsigned       dir        /* 0 -> to, 1 -> from*/
)
{
    TZ_RESULT ret;
    UREE_SESSION_HANDLE mem_session_A;
    UREE_SESSION_HANDLE test_ta_sess;

    UREE_SHAREDMEM_PARAM  shm_param;
    UREE_SHAREDMEM_HANDLE shm_handle_src;
    unsigned int cmd;

    MTEEC_PARAM param[4];

    /* check input parameters */
    if( NULL == puc_norm_buf )
    {
        ALOGD("Error: NULL pointer for normal buffer\n");
        return -1;
    }

    /* create memory and mtee img prot inf gen sessions */
    ret = UREE_CreateSession(TZ_TA_MEM_UUID, &mem_session_A);
    if (ret != TZ_RESULT_SUCCESS)
    {
        ALOGD("Error: fail to creat memory session (%s)\n", TZ_GetErrorString(ret));
        return -2;
    }

    ret = UREE_CreateSession(TZ_TA_TEST_UUID, &test_ta_sess);
    if (ret != TZ_RESULT_SUCCESS)
    {
        ALOGD("Error: fail to creat test ta session (%s)\n", TZ_GetErrorString(ret));
        
        ret = UREE_CloseSession(mem_session_A);
        return -3;
    }

    /* register share memory handles */
    shm_param.buffer = (void *) puc_norm_buf;
    shm_param.size = ui4_sz;
    ret = UREE_RegisterSharedmem(mem_session_A, &shm_handle_src, &shm_param);
    if (ret != TZ_RESULT_SUCCESS)
    {
        ALOGD("Error: fail to register share memory for normal buffer (%s)\n", TZ_GetErrorString(ret));
        ret = UREE_CloseSession(test_ta_sess);
        ret = UREE_CloseSession(mem_session_A);
        return -4;
    }

    /* perform operation */
    cmd = ( dir == 0 )? TZCMD_TEST_CP_SBUF2NBUF : TZCMD_TEST_CP_NBUF2SBUF;
    param[0].value.a = ui4_sec_buf_handle;
    param[0].value.b = 0;
    param[1].memref.handle = (uint32_t) shm_handle_src;
    param[1].memref.offset = 0;
    param[1].memref.size = ui4_sz;
    param[2].value.a = ui4_sz;
    param[2].value.b = 0;
    ret = UREE_TeeServiceCall( test_ta_sess,
                               cmd,
                               TZ_ParamTypes3(TZPT_VALUE_INPUT, TZPT_MEMREF_INOUT, TZPT_VALUE_INPUT),
                               param );
    if (ret != TZ_RESULT_SUCCESS)
    {
         ALOGD("Error: fail to invoke function for test ta (%s)\n", TZ_GetErrorString(ret));
         ret = UREE_UnregisterSharedmem(mem_session_A, shm_handle_src);        
         ret = UREE_CloseSession(test_ta_sess);
         ret = UREE_CloseSession(mem_session_A);
         return -5;
    }  

    /* un-register share memory handles */
    ret = UREE_UnregisterSharedmem(mem_session_A, shm_handle_src);
    if (ret != TZ_RESULT_SUCCESS)
    {
        ALOGD("Error: fail to un-register share memory for normal buffer (%s)\n", TZ_GetErrorString(ret));           
        ret = UREE_CloseSession(test_ta_sess);
        ret = UREE_CloseSession(mem_session_A);
        return -6;
    }
    
    /* close memory and mtee img prot inf gen sessions */
    ret = UREE_CloseSession(test_ta_sess);
    if (ret != TZ_RESULT_SUCCESS)
    {
        ALOGD("Error: fail to close test ta session (%d)\n", ret);

        ret = UREE_CloseSession(mem_session_A);
        return -7;        
    }

    ret = UREE_CloseSession(mem_session_A);
    if (ret != TZ_RESULT_SUCCESS)
    {
        ALOGD("Error: fail to close memory session (%d)\n", ret);
        return -8;        
    }

    return 0;
}

#endif

status_t Converter::doMoreWork() {
    status_t err;

    for (;;) {
        size_t bufferIndex;
        err = mEncoder->dequeueInputBuffer(&bufferIndex);

        if (err != OK) {
            break;
        }

        mAvailEncoderInputIndices.push_back(bufferIndex);
    }

    feedEncoderInputBuffers();

    for (;;) {
        size_t bufferIndex;
        size_t offset;
        size_t size;
        int64_t timeUs;
        uint32_t flags;
			
        err = mEncoder->dequeueOutputBuffer(
                &bufferIndex, &offset, &size, &timeUs, &flags);

        if (err != OK) {
#ifndef ANDROID_DEFAULT_CODE             	
        	   if (err == INFO_FORMAT_CHANGED) {
                continue;
            } else if (err == INFO_OUTPUT_BUFFERS_CHANGED) {
                mEncoder->getOutputBuffers(&mEncoderOutputBuffers);
                continue;
            }

#endif        	
            if (err == -EAGAIN) {
                err = OK;
            }
            break;
        }

#ifdef USE_MMPROFILE
      if(MMP_WFD_CONVERTERDO[mIsVideo] !=0){
		MMProfileLogMetaStringEx(MMP_WFD_CONVERTERDO[mIsVideo], MMProfileFlagPulse, (uint32_t)bufferIndex, (uint32_t)(timeUs / 1000),"dequeueOutputBuffer");
	}
#endif	

        if (flags & MediaCodec::BUFFER_FLAG_EOS) {
            sp<AMessage> notify = mNotify->dup();
            notify->setInt32("what", kWhatEOS);
            notify->post();
        } else {
#if SUPPORT_SLICE_MODE_PADDING
           int32_t paddingSize=size;
	    if (flags & MediaCodec::BUFFER_FLAG_ENDOFFRAME) {
		    paddingSize= size +256;
            } 
	    sp<ABuffer> buffer = new ABuffer(paddingSize);
#else        
            sp<ABuffer> buffer = new ABuffer(size);
#endif
            buffer->meta()->setInt64("timeUs", timeUs);

#ifndef ANDROID_DEFAULT_CODE	
	 if (flags & MediaCodec::BUFFER_FLAG_DUMMY) {
		  buffer->meta()->setInt32("dummy-nal", 1);	
	 }else{
		
		  int64_t time  = ALooper::GetNowUs();		
		 sp<WfdDebugInfo> debugInfo= defaultWfdDebugInfo();
		 debugInfo->addTimeInfoByKey(mIsVideo , timeUs, "EnOt", time/1000);
	 }
#endif

            ALOGV("[WFDP][%s] time %lld ms,bufferIndex=%d,size=%d",
                mIsVideo ? "video" : "audio", timeUs/1000, bufferIndex,size);

#ifdef SEC_WFD_VIDEO_PATH_SUPPORT
           if (flags & MediaCodec::BUFFER_FLAG_SECUREBUF)
           {
           
   #ifdef SEC_WFD_VIDEO_PATH_TEST
               int ret;
               unsigned int   ui4_sec_buf_handle = *(unsigned int *)(mEncoderOutputBuffers.itemAt(bufferIndex)->base() + offset);

               ALOGD("[WFDP] Test Converter: Secure buffer handle = 0x%x size = 0x%x", ui4_sec_buf_handle, size);

               ret = i4dump_sec_buf_to_from_norm_buf(ui4_sec_buf_handle, buffer->data(), size, 0);
               if (ret != 0)
               {
                   ALOGD("[WFDP] i4dump_sec_buf_to_from_norm_buf error = %d", ret);
               }     
   #else
               // only copy secure meory handle to buffer, size = 4 for secure memory handle type is integer
               memcpy(buffer->data(),
                      mEncoderOutputBuffers.itemAt(bufferIndex)->base() + offset,
                      4);
               buffer->meta()->setInt32("flags", flags);
               buffer->meta()->setInt32("buffer_index", bufferIndex);

   #endif
           }
           else
           {
               memcpy(buffer->data(),
                      mEncoderOutputBuffers.itemAt(bufferIndex)->base() + offset,
                      size);

               buffer->meta()->setInt32("flags", 0);
           }
#else
            memcpy(buffer->data(),
                   mEncoderOutputBuffers.itemAt(bufferIndex)->base() + offset,
                   size);
#endif

#if SUPPORT_SLICE_MODE_PADDING
	     if (flags & MediaCodec::BUFFER_FLAG_ENDOFFRAME) {
                memset(buffer->data()+buffer->size()-256, 0xFF,256);
            } 
#endif

            if (flags & MediaCodec::BUFFER_FLAG_CODECCONFIG) {
                mOutputFormat->setBuffer("csd-0", buffer);
            } else {
                sp<AMessage> notify = mNotify->dup();
                notify->setInt32("what", kWhatAccessUnit);
                notify->setBuffer("accessUnit", buffer);
                notify->post();
            }
        }

#ifdef SEC_WFD_VIDEO_PATH_SUPPORT
        // for SVP, release output buffer after HDCP encrypt
        if (flags & MediaCodec::BUFFER_FLAG_SECUREBUF)
        {
       #ifdef SEC_WFD_VIDEO_PATH_TEST
            mEncoder->releaseOutputBuffer(bufferIndex);
       #endif
        }
        else
#endif
            mEncoder->releaseOutputBuffer(bufferIndex);

#ifndef ANDROID_DEFAULT_CODE
        if (mBitrateCtrlerLib != NULL && mBitrateCtrler.SetOneFrameBits != NULL)
        {
            mBitrateCtrler.SetOneFrameBits(mBitrateCtrler.bcHandle, size<<3, (flags & MediaCodec::BUFFER_FLAG_SYNCFRAME));
        }
#endif
        if (flags & MediaCodec::BUFFER_FLAG_EOS) {
            break;
        }
    }

    return err;
}

#ifdef SEC_WFD_VIDEO_PATH_SUPPORT
void Converter::releaseOutputBuffer(size_t bufferIndex)
{   
   if (mEncoder != NULL)  mEncoder->releaseOutputBuffer(bufferIndex);
}
#endif

void Converter::requestIDRFrame() {
#ifdef USE_MMPROFILE
      if(MMP_WFD_CONVERTER!=0){
		MMProfileLogMetaString(MMP_WFD_CONVERTER, MMProfileFlagPulse,"requestIDRFrame");
	}
#endif	
    (new AMessage(kWhatRequestIDRFrame, id()))->post();
}
void Converter::dropAFrame() {
    (new AMessage(kWhatDropAFrame, id()))->post();
}
#ifndef ANDROID_DEFAULT_CODE	
status_t Converter::setWfdLevel(int32_t level){
	if(mIsVideo){
        if (mBitrateCtrlerLib != NULL && mBitrateCtrler.UpdownLevel != NULL)
        {
            mBitrateCtrler.UpdownLevel(mBitrateCtrler.bcHandle, level);
        }
		ALOGI("setWfdLevel:level=%d",level);
		return OK;
	} 

	
       ALOGE(" should not audio");
	return ERROR;

	 
}


int  Converter::getWfdParam(int paramType){
	if(mIsVideo){
		 int paramValue=0;
		//call encoder api   paramValue=xxx
        if (mBitrateCtrlerLib == NULL || mBitrateCtrler.GetStatus == NULL)
        {
            return -1;
        }
		switch(paramType){
			case WifiDisplaySource::kExpectedBitRate :
				paramValue =mBitrateCtrler.GetStatus(mBitrateCtrler.bcHandle, 0);
				break;
			case WifiDisplaySource::kCuurentBitRate:
				paramValue =mBitrateCtrler.GetStatus(mBitrateCtrler.bcHandle, 1);
				break;
			case WifiDisplaySource::kSkipRate:
				paramValue =mBitrateCtrler.GetStatus(mBitrateCtrler.bcHandle, 2);
				break;
			default:
				paramValue=-1;
				break;

		}
	   
		ALOGI("getWfdParam:paramValue=%d",paramValue);
		return paramValue;
	} 

	
       ALOGE(" should not audio");
	return -1;

}
void  Converter::forceBlackScreen(bool blackNow){
	mForceBlackScreenNow = blackNow;	
	ALOGI("force black now mForceBlackScreenNow=%d",mForceBlackScreenNow);
    mEncoder->setVEncDrawBlack(blackNow);
}
#endif 
}  // namespace android
