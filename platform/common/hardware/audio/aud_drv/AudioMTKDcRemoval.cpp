#define LOG_TAG  "AudioMTKDCRemoval"

#include <cutils/compiler.h>


#include "AudioMTKDcRemoval.h"
#include <cutils/xlog.h>


#define ENABLE_DC_REMOVE
//#define DUMP_DCR_DEBUG

namespace android
{

DcRemove::DcRemove()
    : mHandle(NULL)
{
}
DcRemove::~DcRemove()
{
    close();
}

status_t  DcRemove::init(uint32 channel, uint32 samplerate, uint32 dcrMode)
{
    Mutex::Autolock _l(&mLock);
    if (!mHandle)
    {
        mHandle = DCR_Open(channel, samplerate, dcrMode);
    }
    else
    {
        mHandle = DCR_ReConfig(mHandle, channel, samplerate, dcrMode);
        // SXLOGD("DcRemove::Reconfig channel=%u,samplerate=%u,dcrMode=%u", channel, samplerate, dcrMode);
    }
    if (!mHandle)
    {
        SXLOGW("Fail to get DCR Handle");
        return NO_INIT;
    }
    return NO_ERROR;
}

status_t  DcRemove::close()
{
    Mutex::Autolock _l(&mLock);
    SXLOGV("DcRemove::deinit");
    if (mHandle)
    {
        DCR_Close(mHandle);
        mHandle = NULL;
    }
    return NO_ERROR;
}

size_t DcRemove::process(const void *inbuffer, size_t bytes, void *outbuffer)
{
    Mutex::Autolock _l(&mLock);
#ifdef ENABLE_DC_REMOVE
    if (mHandle)
    {
        size_t outputBytes = 0;
        uint32 inputBufSize  = bytes;
        uint32 outputBufSize = bytes;

#ifdef DUMP_DCR_DEBUG
        FILE *pDumpDcrIn;
        pDumpDcrIn = fopen("/sdcard/before_dcr.pcm", "ab");
        if (pDumpDcrIn == NULL) ALOGW("Fail to Open pDumpDcrIn");
        fwrite(inbuffer, sizeof(long), outputBufSize/sizeof(long), pDumpDcrIn);
        fclose(pDumpDcrIn);
#endif

        outputBytes = DCR_Process_24(mHandle, (long *)inbuffer, &inputBufSize, (long *)outbuffer, &outputBufSize);

        //ALOGD("DcRemove::process inputBufSize = %d,outputBufSize=%d,outputBytes=%d ", inputBufSize, outputBufSize, outputBytes);

#ifdef DUMP_DCR_DEBUG
        FILE *pDumpDcrOut;
        pDumpDcrOut = fopen("/sdcard/after_dcr.pcm", "ab");
        if (pDumpDcrOut == NULL) ALOGW("Fail to Open pDumpDcrOut");
        fwrite(outbuffer, sizeof(long), outputBufSize/sizeof(long), pDumpDcrOut);
        fclose(pDumpDcrOut);
#endif
        return outputBytes;
    }
    //SXLOGW("DcRemove::process Dcr not initialized");
#endif
    return 0;
}

}

