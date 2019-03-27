#include "AudioSpeechEnhanceInfo.h"
#include <utils/Log.h>
#include <utils/String16.h>
#include "AudioUtility.h"
#include "AudioMTKStreamIn.h"
#include "SpeechEnhancementController.h"
#include <cutils/properties.h>
#include "AudioCustParam.h"

#define LOG_TAG "AudioSpeechEnhanceInfo"


static const char PROPERTY_KEY_VOIP_SPH_ENH_MASKS[PROPERTY_KEY_MAX] = "persist.af.voip.sph_enh_mask";

namespace android
{

AudioSpeechEnhanceInfo *AudioSpeechEnhanceInfo::UniqueAudioSpeechEnhanceInfoInstance = NULL;

AudioSpeechEnhanceInfo *AudioSpeechEnhanceInfo::getInstance()
{
    static Mutex mGetInstanceLock;
    Mutex::Autolock _l(mGetInstanceLock);

    if (UniqueAudioSpeechEnhanceInfoInstance == NULL)
    {
        ALOGD("+AudioSpeechEnhanceInfo");
        UniqueAudioSpeechEnhanceInfoInstance = new AudioSpeechEnhanceInfo();
        ALOGD("-AudioSpeechEnhanceInfo");
    }
    ALOGD("AudioSpeechEnhanceInfo getInstance()");
    ASSERT(UniqueAudioSpeechEnhanceInfoInstance != NULL);
    return UniqueAudioSpeechEnhanceInfoInstance;
}

void AudioSpeechEnhanceInfo::freeInstance()
{
    return;
}

AudioSpeechEnhanceInfo::AudioSpeechEnhanceInfo()
{
    ALOGD("AudioSpeechEnhanceInfo constructor");
    mHdRecScene = -1;
    mIsLRSwitch = false;
    mUseSpecificMic = 0;
    mHDRecTunningEnable = false;
    mForceMagiASR = false;
    mForceAECRec = false;
#ifndef DMNR_TUNNING_AT_MODEMSIDE
    mAPDMNRTuningEnable = false;
    mAPTuningMode = TUNING_MODE_NONE;
#endif

    mEnableNormalModeVoIP = false;
    mStreamOut = NULL;

    // default value (all enhancement on)
    char property_default_value[PROPERTY_VALUE_MAX];
    sprintf(property_default_value, "0x%x", VOIP_SPH_ENH_DYNAMIC_MASK_ALL);

    // get voip sph_enh_mask_struct from property
    char property_value[PROPERTY_VALUE_MAX];
    property_get(PROPERTY_KEY_VOIP_SPH_ENH_MASKS, property_value, property_default_value);

    // parse mask info from property_value
    sscanf(property_value, "0x%x",
           &mVoIPSpeechEnhancementMask);

    ALOGD("mVoIPSpeechEnhancementMask = 0x%x", mVoIPSpeechEnhancementMask);

}

AudioSpeechEnhanceInfo::~AudioSpeechEnhanceInfo()
{
    ALOGD("AudioSpeechEnhanceInfo destructor");
    mHdRecScene = -1;
}


void AudioSpeechEnhanceInfo::SetRecordLRChannelSwitch(bool bIsLRSwitch)
{
    Mutex::Autolock lock(mHDRInfoLock);
    ALOGD("SetRecordLRChannelSwitch=%x", bIsLRSwitch);
    mIsLRSwitch = bIsLRSwitch;
}

bool AudioSpeechEnhanceInfo::GetRecordLRChannelSwitch(void)
{
    Mutex::Autolock lock(mHDRInfoLock);
    ALOGD("GetRecordLRChannelSwitch=%x", mIsLRSwitch);
    return mIsLRSwitch;
}

void AudioSpeechEnhanceInfo::SetUseSpecificMIC(int32 UseSpecificMic)
{
    Mutex::Autolock lock(mHDRInfoLock);
    ALOGD("SetUseSpecificMIC=%x", UseSpecificMic);
    mUseSpecificMic = UseSpecificMic;
}

int AudioSpeechEnhanceInfo::GetUseSpecificMIC(void)
{
    Mutex::Autolock lock(mHDRInfoLock);
    ALOGD("GetUseSpecificMIC=%x", mUseSpecificMic);
    return mUseSpecificMic;
}

bool AudioSpeechEnhanceInfo::SetForceMagiASR(bool enable)
{
    ALOGD("%s, %d", __FUNCTION__, enable);
    mForceMagiASR = enable;
    return true;
}

status_t AudioSpeechEnhanceInfo::GetForceMagiASRState()
{
    status_t ret = 0;
    uint32_t feature_support = QueryFeatureSupportInfo();

    ALOGD("%s(), feature_support=%x, %x, mForceMagiASR=%d", __FUNCTION__, feature_support, (feature_support & SUPPORT_ASR), mForceMagiASR);

    if (feature_support & SUPPORT_ASR)
    {
        if (mForceMagiASR)
        {
            ret = 1;
        }
        else
        {
            ret = -1;
        }
    }
    else
    {
        ret = 0;
    }

    return ret;
}

bool AudioSpeechEnhanceInfo::SetForceAECRec(bool enable)
{
    ALOGD("%s, %d", __FUNCTION__, enable);
    mForceAECRec = enable;
    return true;
}

bool AudioSpeechEnhanceInfo::GetForceAECRecState()
{
    status_t ret = false;

    ALOGD("%s(), mForceAECRec=%d", __FUNCTION__, mForceAECRec);

    if (mForceAECRec)
    {
        ret = true;
    }
    return ret;
}


//----------------for HD Record Preprocess-----------------------------
void AudioSpeechEnhanceInfo::SetHDRecScene(int32 HDRecScene)
{
    Mutex::Autolock lock(mHDRInfoLock);
    ALOGD("AudioSpeechEnhanceInfo SetHDRecScene=%d", HDRecScene);
    mHdRecScene = HDRecScene;
}

int32 AudioSpeechEnhanceInfo::GetHDRecScene()
{
    Mutex::Autolock lock(mHDRInfoLock);
    ALOGD("AudioSpeechEnhanceInfo GetHDRecScene=%d", mHdRecScene);
    return mHdRecScene;
}

void AudioSpeechEnhanceInfo::ResetHDRecScene()
{
    Mutex::Autolock lock(mHDRInfoLock);
    ALOGD("AudioSpeechEnhanceInfo ResetHDRecScene");
    mHdRecScene = -1;
}

//----------------for HDRec tunning --------------------------------
void AudioSpeechEnhanceInfo::SetHDRecTunningEnable(bool bEnable)
{
    Mutex::Autolock lock(mHDRInfoLock);
    ALOGD("SetHDRecTunningEnable=%d", bEnable);
    mHDRecTunningEnable = bEnable;
}

bool AudioSpeechEnhanceInfo::IsHDRecTunningEnable(void)
{
    Mutex::Autolock lock(mHDRInfoLock);
    ALOGD("IsHDRecTunningEnable=%d", mHDRecTunningEnable);
    return mHDRecTunningEnable;
}

status_t AudioSpeechEnhanceInfo::SetHDRecVMFileName(const char *fileName)
{
    Mutex::Autolock lock(mHDRInfoLock);
    if (fileName != NULL && strlen(fileName) < 128 - 1)
    {
        ALOGD("SetHDRecVMFileName file name:%s", fileName);
        memset(mVMFileName, 0, 128);
        strcpy(mVMFileName, fileName);
    }
    else
    {
        ALOGD("input file name NULL or too long!");
        return BAD_VALUE;
    }
    return NO_ERROR;
}
void AudioSpeechEnhanceInfo::GetHDRecVMFileName(char *VMFileName)
{
    Mutex::Autolock lock(mHDRInfoLock);
    memset(VMFileName, 0, 128);
    strcpy(VMFileName, mVMFileName);
    ALOGD("GetHDRecVMFileName mVMFileName=%s, VMFileName=%s", mVMFileName, VMFileName);
}

#ifndef DMNR_TUNNING_AT_MODEMSIDE
//----------------for AP DMNR tunning --------------------------------
void AudioSpeechEnhanceInfo::SetAPDMNRTuningEnable(bool bEnable)
{
#ifdef MTK_DUAL_MIC_SUPPORT
    Mutex::Autolock lock(mHDRInfoLock);
    ALOGD("SetAPDMNRTuningEnable=%d", bEnable);
    mAPDMNRTuningEnable = bEnable;
#else
    ALOGD("SetAPDMNRTuningEnable not Dual MIC, not set");
#endif
}

bool AudioSpeechEnhanceInfo::IsAPDMNRTuningEnable(void)
{
#ifdef MTK_DUAL_MIC_SUPPORT
    Mutex::Autolock lock(mHDRInfoLock);
    //ALOGD("IsAPDMNRTuningEnable=%d",mAPDMNRTuningEnable);
    return mAPDMNRTuningEnable;
#else
    return false;
#endif
}

bool AudioSpeechEnhanceInfo::SetAPTuningMode(const TOOL_TUNING_MODE mode)
{
    bool bRet = false;
    ALOGD("SetAPTuningMode mAPDMNRTuningEnable=%d, mode=%d", mAPDMNRTuningEnable, mode);
    if (mAPDMNRTuningEnable)
    {
        mAPTuningMode = mode;
        bRet = true;
    }
    return bRet;
}

int AudioSpeechEnhanceInfo::GetAPTuningMode()
{
    ALOGD("GetAPTuningMode, mAPTuningMode=%d", mAPTuningMode);
    return mAPTuningMode;
}

#endif

//----------------Get MMI info for AP Speech Enhancement --------------------------------
bool AudioSpeechEnhanceInfo::GetDynamicSpeechEnhancementMaskOnOff(const voip_sph_enh_dynamic_mask_t dynamic_mask_type)
{
    bool bret = false;
    voip_sph_enh_mask_struct_t mask;

    mask = GetDynamicVoIPSpeechEnhancementMask();

    if ((mask.dynamic_func & dynamic_mask_type) > 0)
    {
        bret = true;
    }

    ALOGD("%s(), %x, %x, bret=%d", __FUNCTION__, mask.dynamic_func, dynamic_mask_type, bret);
    return bret;
}

void AudioSpeechEnhanceInfo::UpdateDynamicSpeechEnhancementMask(const voip_sph_enh_mask_struct_t &mask)
{
    uint32_t feature_support = QueryFeatureSupportInfo();

    ALOGD("%s(), mask = %x, feature_support=%x, %x", __FUNCTION__, mask, feature_support, (feature_support & (SUPPORT_DMNR_3_0 | SUPPORT_VOIP_ENHANCE)));

    if (feature_support & (SUPPORT_DMNR_3_0 | SUPPORT_VOIP_ENHANCE))
    {

        char property_value[PROPERTY_VALUE_MAX];
        sprintf(property_value, "0x%x", mask);
        property_set(PROPERTY_KEY_VOIP_SPH_ENH_MASKS, property_value);

        mVoIPSpeechEnhancementMask = mask;

        if (mSPELayerVector.size())
        {
            for (size_t i = 0; i < mSPELayerVector.size() ; i++)
            {
                AudioMTKStreamIn *pTempMTKStreamIn = (AudioMTKStreamIn *)mSPELayerVector.keyAt(i);
                pTempMTKStreamIn->UpdateDynamicFunction();
            }
        }
    }
    else
    {
        ALOGD("%s(), not support", __FUNCTION__);
    }

}

status_t AudioSpeechEnhanceInfo::SetDynamicVoIPSpeechEnhancementMask(const voip_sph_enh_dynamic_mask_t dynamic_mask_type, const bool new_flag_on)
{
    //Mutex::Autolock lock(mHDRInfoLock);
    uint32_t feature_support = QueryFeatureSupportInfo();

    ALOGD("%s(), feature_support=%x, %x", __FUNCTION__, feature_support, (feature_support & (SUPPORT_DMNR_3_0 | SUPPORT_VOIP_ENHANCE)));

    if (feature_support & (SUPPORT_DMNR_3_0 | SUPPORT_VOIP_ENHANCE))
    {
        voip_sph_enh_mask_struct_t mask = GetDynamicVoIPSpeechEnhancementMask();

        ALOGW("%s(), dynamic_mask_type(%x), %x",
              __FUNCTION__, dynamic_mask_type, mask.dynamic_func);
        const bool current_flag_on = ((mask.dynamic_func & dynamic_mask_type) > 0);
        if (new_flag_on == current_flag_on)
        {
            ALOGW("%s(), dynamic_mask_type(%x), new_flag_on(%d) == current_flag_on(%d), return",
                  __FUNCTION__, dynamic_mask_type, new_flag_on, current_flag_on);
            return NO_ERROR;
        }

        if (new_flag_on == false)
        {
            mask.dynamic_func &= (~dynamic_mask_type);
        }
        else
        {
            mask.dynamic_func |= dynamic_mask_type;
        }

        UpdateDynamicSpeechEnhancementMask(mask);
    }
    else
    {
        ALOGW("%s(), not support", __FUNCTION__);
    }

    return NO_ERROR;
}


//----------------for Android Native Preprocess-----------------------------
void AudioSpeechEnhanceInfo::SetStreamOutPointer(void *pStreamOut)
{
    if (pStreamOut == NULL)
    {
        ALOGW(" SetStreamOutPointer pStreamOut = NULL");
    }
    else
    {
        mStreamOut = (AudioMTKStreamOut *)pStreamOut;
        ALOGW("SetStreamOutPointer mStreamOut=%p", mStreamOut);
    }
}

int AudioSpeechEnhanceInfo::GetOutputSampleRateInfo(void)
{
    int samplerate = 48000;
    if (mStreamOut != NULL)
    {
        samplerate = mStreamOut->GetSampleRate();
    }
    ALOGD("AudioSpeechEnhanceInfo GetOutputSampleRateInfo=%d", samplerate);
    return samplerate;
}

int AudioSpeechEnhanceInfo::GetOutputChannelInfo(void)
{
    int chn = 1;
    if (mStreamOut != NULL)
    {
        chn = mStreamOut->GetChannel();
    }
    ALOGD("AudioSpeechEnhanceInfo GetOutputChannelInfo=%d", chn);
    return chn;
}

bool AudioSpeechEnhanceInfo::IsOutputRunning(void)
{
    if (mStreamOut == NULL)
    {
        return false;
    }
    return mStreamOut->GetStreamRunning();
}

void AudioSpeechEnhanceInfo::add_echo_reference(struct echo_reference_itfe *reference)
{
    ALOGD("AudioSpeechEnhanceInfo add_echo_reference=%p", reference);
    if (mStreamOut != NULL)
    {
        mStreamOut->add_echo_reference(reference);
    }
}
void AudioSpeechEnhanceInfo::remove_echo_reference(struct echo_reference_itfe *reference)
{
    ALOGD("AudioSpeechEnhanceInfo remove_echo_reference=%p", reference);
    if (mStreamOut != NULL)
    {
        mStreamOut->remove_echo_reference(reference);
    }
}

void AudioSpeechEnhanceInfo::SetOutputStreamRunning(bool bRun)
{

    Mutex::Autolock lock(mHDRInfoLock);
    ALOGD("SetOutputStreamRunning %d, SPELayer %d", bRun, mSPELayerVector.size());

    if (mSPELayerVector.size())
    {
        for (size_t i = 0; i < mSPELayerVector.size() ; i++)
        {
            SPELayer *pTempSPELayer = (SPELayer *)mSPELayerVector.valueAt(i);
            pTempSPELayer->SetOutputStreamRunning(bRun, true);
        }
    }
}
void AudioSpeechEnhanceInfo::SetSPEPointer(AudioMTKStreamIn *pMTKStreamIn, SPELayer *pSPE)
{
    Mutex::Autolock lock(mHDRInfoLock);
    ALOGD("AudioSpeechEnhanceInfo SetSPEPointer %p, %p", pMTKStreamIn, pSPE);
    //mStreamOut->SetSPEPointer(pSPE);
    if (mSPELayerVector.size())
    {
        for (size_t i = 0; i < mSPELayerVector.size() ; i++)
        {
            if (pMTKStreamIn == mSPELayerVector.keyAt(i))
            {
                ALOGD("SetSPEPointer already add this before, not add it again");
                return;
            }
        }
    }
    if (mStreamOut != NULL)
    {
        pSPE->SetDownLinkLatencyTime(mStreamOut->latency());
    }
    mSPELayerVector.add(pMTKStreamIn, pSPE);
    ALOGD("SetSPEPointer size %d", mSPELayerVector.size());
}

void AudioSpeechEnhanceInfo::ClearSPEPointer(AudioMTKStreamIn *pMTKStreamIn)
{
    Mutex::Autolock lock(mHDRInfoLock);
    ALOGD("ClearSPEPointer %p, size=%d", pMTKStreamIn, mSPELayerVector.size());
    //mStreamOut->ClearSPEPointer();
    if (mSPELayerVector.size())
    {
        for (size_t i = 0; i < mSPELayerVector.size() ; i++)
        {
            if (pMTKStreamIn == mSPELayerVector.keyAt(i))
            {
                ALOGD("find and remove it ++");
                mSPELayerVector.removeItem(pMTKStreamIn);
                ALOGD("find and remove it --");
            }
        }
    }
}

bool AudioSpeechEnhanceInfo::IsInputStreamAlive(void)
{
    Mutex::Autolock lock(mHDRInfoLock);
    if (mSPELayerVector.size())
    {
        return true;
    }
    return false;
}

//no argument, check if there is VoIP running input stream
//MTKStreamIn argument, check if the dedicated MTKStreamIn is VoIP running stream
bool AudioSpeechEnhanceInfo::IsVoIPActive(AudioMTKStreamIn *pMTKStreamIn)
{
    Mutex::Autolock lock(mHDRInfoLock);
    if (mSPELayerVector.size())
    {
        if (pMTKStreamIn == NULL)
        {
            //ALOGD("IsVoIPActive!");
            for (size_t i = 0; i < mSPELayerVector.size() ; i++)
            {
                AudioMTKStreamIn *pTempMTKStreamIn = (AudioMTKStreamIn *)mSPELayerVector.keyAt(i);
                if (pTempMTKStreamIn->GetVoIPRunningState())
                {
                    return true;
                }
            }
            return false;
        }
        else
        {
            for (size_t i = 0; i < mSPELayerVector.size() ; i++)
            {
                if (pMTKStreamIn == mSPELayerVector.keyAt(i))
                {
                    if (pMTKStreamIn->GetVoIPRunningState())
                    {
                        return true;
                    }
                }
            }
            return false;
        }
    }
    return false;
}

void AudioSpeechEnhanceInfo::GetDownlinkIntrStartTime(void)
{
    Mutex::Autolock lock(mHDRInfoLock);
    ALOGD("GetDownlinkIntrStartTime %d", mSPELayerVector.size());
    if (mSPELayerVector.size())
    {
        for (size_t i = 0; i < mSPELayerVector.size() ; i++)
        {
            SPELayer *pTempSPELayer = (SPELayer *)mSPELayerVector.valueAt(i);
            pTempSPELayer->GetDownlinkIntrStartTime();
        }
    }
}

void AudioSpeechEnhanceInfo::WriteReferenceBuffer(struct InBufferInfo *Binfo)
{
    Mutex::Autolock lock(mHDRInfoLock);
    ALOGD("WriteReferenceBuffer %d", mSPELayerVector.size());
    if (mSPELayerVector.size())
    {
        for (size_t i = 0; i < mSPELayerVector.size() ; i++)
        {
            SPELayer *pTempSPELayer = (SPELayer *)mSPELayerVector.valueAt(i);
            pTempSPELayer->WriteReferenceBuffer(Binfo);
        }
    }
}

void AudioSpeechEnhanceInfo::NeedUpdateVoIPParams(void)
{
    Mutex::Autolock lock(mHDRInfoLock);
    ALOGD("NeedUpdateVoIPParams %d", mSPELayerVector.size());
    if (mSPELayerVector.size())
    {
        for (size_t i = 0; i < mSPELayerVector.size() ; i++)
        {
            AudioMTKStreamIn *pTempMTKStreamIn = (AudioMTKStreamIn *)mSPELayerVector.keyAt(i);
            pTempMTKStreamIn->NeedUpdateVoIPParams();
        }
    }
}

int AudioSpeechEnhanceInfo::GetOutputBufferSize(void)
{
    ALOGD("%s()", __FUNCTION__);
    int BufferSize = 4096;
    int format = AUDIO_FORMAT_PCM_16_BIT;
    if (mStreamOut != NULL)
    {
        format = mStreamOut->format();
        BufferSize = mStreamOut->bufferSize();
        if (format == AUDIO_FORMAT_PCM_32_BIT)
        {
            BufferSize >>= 1;
        }
    }
    ALOGD("%s(),BufferSize=%d,format=%d", __FUNCTION__, BufferSize, format);
    return BufferSize;
}

void AudioSpeechEnhanceInfo::SetEnableNormalModeVoIP(bool bEnable)
{
    Mutex::Autolock lock(mHDRInfoLock);
    ALOGD("SetEnableNormalModeVoIP=%d", bEnable);
    mEnableNormalModeVoIP = bEnable;
}

bool AudioSpeechEnhanceInfo::GetEnableNormalModeVoIP(void)
{
    Mutex::Autolock lock(mHDRInfoLock);
    ALOGD("GetEnableNormalModeVoIP=%x", mEnableNormalModeVoIP);
    return mEnableNormalModeVoIP;
}

}

