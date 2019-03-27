/*
 * Copyright 2013, The Android Open Source Project
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
#define LOG_TAG "VideoFormats"
#include <utils/Log.h>

#include "VideoFormats.h"

#include <media/stagefright/foundation/ADebug.h>


#include <cutils/properties.h>
#include <stdlib.h>


namespace android {

VideoFormats::VideoFormats() {
    for (size_t i = 0; i < kNumResolutionTypes; ++i) {
        mResolutionEnabled[i] = 0;
    }

    setNativeResolution(RESOLUTION_CEA, 0);  // default to 640x480 p60
#ifndef ANDROID_DEFAULT_CODE     
    mSupportCBPOnly = true;
#endif    
}

void VideoFormats::setNativeResolution(ResolutionType type, size_t index) {
    CHECK_LT(type, kNumResolutionTypes);
    CHECK(GetConfiguration(type, index, NULL, NULL, NULL, NULL));

    mNativeType = type;
    mNativeIndex = index;
    
    setResolutionEnabled(type, index);
}

void VideoFormats::getNativeResolution(
        ResolutionType *type, size_t *index) const {
    *type = mNativeType;
    *index = mNativeIndex;
}

void VideoFormats::disableAll() {
    for (size_t i = 0; i < kNumResolutionTypes; ++i) {
        mResolutionEnabled[i] = 0;
    }
}

void VideoFormats::enableAll() {
    for (size_t i = 0; i < kNumResolutionTypes; ++i) {
        mResolutionEnabled[i] = 0xffffffff;
    }
}

void VideoFormats::setResolutionEnabled(
        ResolutionType type, size_t index, bool enabled) {
    CHECK_LT(type, kNumResolutionTypes);
    CHECK(GetConfiguration(type, index, NULL, NULL, NULL, NULL));

    if (enabled) {
        mResolutionEnabled[type] |= (1ul << index);
    } else {
        mResolutionEnabled[type] &= ~(1ul << index);
    }
}

bool VideoFormats::isResolutionEnabled(
        ResolutionType type, size_t index) const {
    CHECK_LT(type, kNumResolutionTypes);
    CHECK(GetConfiguration(type, index, NULL, NULL, NULL, NULL));

    return mResolutionEnabled[type] & (1ul << index);
}

// static
bool VideoFormats::GetConfiguration(
        ResolutionType type,
        size_t index,
        size_t *width, size_t *height, size_t *framesPerSecond,
        bool *interlaced) {
    CHECK_LT(type, kNumResolutionTypes);

    if (index >= 32) {
        return false;
    }

    static const struct config_t {
        size_t width, height, framesPerSecond;
        bool interlaced;
    } kConfigs[kNumResolutionTypes][32] = {
        {
            // CEA Resolutions
            { 640, 480, 60, false },
            { 720, 480, 60, false },
            { 720, 480, 60, true },
            { 720, 576, 50, false },
            { 720, 576, 50, true },
            { 1280, 720, 30, false },
            { 1280, 720, 60, false },
            { 1920, 1080, 30, false },
            { 1920, 1080, 60, false },
            { 1920, 1080, 60, true },
            { 1280, 720, 25, false },
            { 1280, 720, 50, false },
            { 1920, 1080, 25, false },
            { 1920, 1080, 50, false },
            { 1920, 1080, 50, true },
            { 1280, 720, 24, false },
            { 1920, 1080, 24, false },
            { 0, 0, 0, false },
            { 0, 0, 0, false },
            { 0, 0, 0, false },
            { 0, 0, 0, false },
            { 0, 0, 0, false },
            { 0, 0, 0, false },
            { 0, 0, 0, false },
            { 0, 0, 0, false },
            { 0, 0, 0, false },
            { 0, 0, 0, false },
            { 0, 0, 0, false },
            { 0, 0, 0, false },
            { 0, 0, 0, false },
            { 0, 0, 0, false },
            { 0, 0, 0, false },
        },
        {
            // VESA Resolutions
            { 800, 600, 30, false },
            { 800, 600, 60, false },
            { 1024, 768, 30, false },
            { 1024, 768, 60, false },
            { 1152, 864, 30, false },
            { 1152, 864, 60, false },
            { 1280, 768, 30, false },
            { 1280, 768, 60, false },
            { 1280, 800, 30, false },
            { 1280, 800, 60, false },
            { 1360, 768, 30, false },
            { 1360, 768, 60, false },
            { 1366, 768, 30, false },
            { 1366, 768, 60, false },
            { 1280, 1024, 30, false },
            { 1280, 1024, 60, false },
            { 1400, 1050, 30, false },
            { 1400, 1050, 60, false },
            { 1440, 900, 30, false },
            { 1440, 900, 60, false },
            { 1600, 900, 30, false },
            { 1600, 900, 60, false },
            { 1600, 1200, 30, false },
            { 1600, 1200, 60, false },
            { 1680, 1024, 30, false },
            { 1680, 1024, 60, false },
            { 1680, 1050, 30, false },
            { 1680, 1050, 60, false },
            { 1920, 1200, 30, false },
            { 1920, 1200, 60, false },
            { 0, 0, 0, false },
            { 0, 0, 0, false },
        },
        {
            // HH Resolutions
            { 800, 480, 30, false },
            { 800, 480, 60, false },
            { 854, 480, 30, false },
            { 854, 480, 60, false },
            { 864, 480, 30, false },
            { 864, 480, 60, false },
            { 640, 360, 30, false },
            { 640, 360, 60, false },
            { 960, 540, 30, false },
            { 960, 540, 60, false },
            { 848, 480, 30, false },
            { 848, 480, 60, false },
            { 0, 0, 0, false },
            { 0, 0, 0, false },
            { 0, 0, 0, false },
            { 0, 0, 0, false },
            { 0, 0, 0, false },
            { 0, 0, 0, false },
            { 0, 0, 0, false },
            { 0, 0, 0, false },
            { 0, 0, 0, false },
            { 0, 0, 0, false },
            { 0, 0, 0, false },
            { 0, 0, 0, false },
            { 0, 0, 0, false },
            { 0, 0, 0, false },
            { 0, 0, 0, false },
            { 0, 0, 0, false },
            { 0, 0, 0, false },
            { 0, 0, 0, false },
            { 0, 0, 0, false },
            { 0, 0, 0, false },
        }
    };

    const config_t *config = &kConfigs[type][index];

    if (config->width == 0) {
        return false;
    }

    if (width) {
        *width = config->width;
    }

    if (height) {
        *height = config->height;
    }

    if (framesPerSecond) {
        *framesPerSecond = config->framesPerSecond;
    }

    if (interlaced) {
        *interlaced = config->interlaced;
    }

    return true;
}

bool VideoFormats::parseFormatSpec(const char *spec) {
    CHECK_EQ(kNumResolutionTypes, 3);

    unsigned native, dummy;
    unsigned profile, level;
    
    if (sscanf(
            spec,
            "%02x %02x %02x %02x %08X %08X %08X",
            &native,
            &dummy,
            &profile,
            &level,
            &mResolutionEnabled[0],
            &mResolutionEnabled[1],
            &mResolutionEnabled[2]) != 7) {
        return false;
    }

#ifndef ANDROID_DEFAULT_CODE 
    if (profile > (1ul << PROFILE_CBP)){
        mSupportCBPOnly = false; 
    }
#endif  


    mNativeIndex = native >> 3;
    mNativeType = (ResolutionType)(native & 7);

    
    ALOGI("parseFormatSpec(), type: %d, index: %d", mNativeType, mNativeIndex);

    if (mNativeType >= kNumResolutionTypes) {
        return false;
    }

///M: Support Video Format @{

    if (mNativeType >= 32) {
        return false;
    }
    
///@}

// Ignore to check sink's native resolution
///M: Support Video Format @{ 
    return true;

    //return GetConfiguration(mNativeType, mNativeIndex, NULL, NULL, NULL, NULL);
///@}
}

#ifndef ANDROID_DEFAULT_CODE 
bool VideoFormats::supportCBPOnly() {
    return mSupportCBPOnly;
}

AString VideoFormats::getCBPFormatSpec(bool forM4Message) const {
    CHECK_EQ(kNumResolutionTypes, 3);
    
    return StringPrintf(
            "%02x 00 01 01 %08x %08x %08x 00 0000 0000 00 none none",
            forM4Message ? 0x00 : ((mNativeIndex << 3) | mNativeType),
            mResolutionEnabled[0],
            mResolutionEnabled[1],
            mResolutionEnabled[2]);
}

#endif 

AString VideoFormats::getFormatSpec(bool forM4Message) const {
    CHECK_EQ(kNumResolutionTypes, 3);

    // wfd_video_formats:
    // 1 byte "native"
    // 1 byte "preferred-display-mode-supported" 0 or 1
    // one or more avc codec structures
    //   1 byte profile
    //   1 byte level
    //   4 byte CEA mask
    //   4 byte VESA mask
    //   4 byte HH mask
    //   1 byte latency
    //   2 byte min-slice-slice
    //   2 byte slice-enc-params
    //   1 byte framerate-control-support
    //   max-hres (none or 2 byte)
    //   max-vres (none or 2 byte)

    return StringPrintf(
            "%02x 00 02 02 %08x %08x %08x 00 0000 0000 00 none none",
            forM4Message ? 0x00 : ((mNativeIndex << 3) | mNativeType),
            mResolutionEnabled[0],
            mResolutionEnabled[1],
            mResolutionEnabled[2]);
}

// static
bool VideoFormats::PickBestFormat(
        const VideoFormats &sinkSupported,
        const VideoFormats &sourceSupported,
        ResolutionType *chosenType,
        size_t *chosenIndex) {
#if 0
    // Support for the native format is a great idea, the spec includes
    // these features, but nobody supports it and the tests don't validate it.

    ResolutionType nativeType;
    size_t nativeIndex;
    sinkSupported.getNativeResolution(&nativeType, &nativeIndex);
    if (sinkSupported.isResolutionEnabled(nativeType, nativeIndex)) {
        if (sourceSupported.isResolutionEnabled(nativeType, nativeIndex)) {
            ALOGI("Choosing sink's native resolution");
            *chosenType = nativeType;
            *chosenIndex = nativeIndex;
            return true;
        }
    } else {
        ALOGW("Sink advertised native resolution that it doesn't "
              "actually support... ignoring");
    }

    sourceSupported.getNativeResolution(&nativeType, &nativeIndex);
    if (sourceSupported.isResolutionEnabled(nativeType, nativeIndex)) {
        if (sinkSupported.isResolutionEnabled(nativeType, nativeIndex)) {
            ALOGI("Choosing source's native resolution");
            *chosenType = nativeType;
            *chosenIndex = nativeIndex;
            return true;
        }
    } else {
        ALOGW("Source advertised native resolution that it doesn't "
              "actually support... ignoring");
    }
#endif

    bool first = true;
    uint32_t bestScore = 0;
    size_t bestType = 0;
    size_t bestIndex = 0;
    for (size_t i = 0; i < kNumResolutionTypes; ++i) {
        for (size_t j = 0; j < 32; ++j) {
            size_t width, height, framesPerSecond;
            bool interlaced;
            if (!GetConfiguration(
                        (ResolutionType)i,
                        j,
                        &width, &height, &framesPerSecond, &interlaced)) {
                break;
            }

            if (!sinkSupported.isResolutionEnabled((ResolutionType)i, j)
                    || !sourceSupported.isResolutionEnabled(
                        (ResolutionType)i, j)) {
                continue;
            }

            ALOGI("type %u, index %u, %u x %u %c%u supported",
                  i, j, width, height, interlaced ? 'i' : 'p', framesPerSecond);

            uint32_t score = width * height * framesPerSecond;
            if (!interlaced) {
                score *= 2;
            }

            if (first || score > bestScore) {
                bestScore = score;
                bestType = i;
                bestIndex = j;

                first = false;
            }
        }
    }

    if (first) {
        return false;
    }

    *chosenType = (ResolutionType)bestType;
    *chosenIndex = bestIndex;

    return true;
}

void VideoFormats:: SetDefaultConfiguration(){

    disableAll();

    //Mandatory support resolution
    setNativeResolution(VideoFormats::RESOLUTION_CEA, 0);
    setNativeResolution(VideoFormats::RESOLUTION_CEA, 5);

    char val[PROPERTY_VALUE_MAX];
    if (property_get("media.wfd.video-format", val, NULL)) {
        ALOGI("media.wfd.video-format:%s", val);
        int videoFormat = atoi(val);
        if(videoFormat >=0 && videoFormat <= 16){
            setNativeResolution(VideoFormats::RESOLUTION_CEA, videoFormat);
            return;
        }
    }

//Optional support resolution
#if (WFD_VIDEO_FORMAT == 2)
    setNativeResolution(VideoFormats::RESOLUTION_CEA, 7);
#elif (WFD_VIDEO_FORMAT == 3)
    setNativeResolution(VideoFormats::RESOLUTION_CEA, 16);
#endif


}

}  // namespace android

