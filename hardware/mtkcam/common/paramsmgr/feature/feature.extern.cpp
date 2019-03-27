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

#include "Local.h"
#include "Feature.h"
//
#if '1'==MTKCAM_HAVE_SENSOR_HAL
    #include <mtkcam/hal/sensor_hal.h>
    #include <camera_custom_sensor.h>
#endif
//
#include <dlfcn.h>


/******************************************************************************
*
*******************************************************************************/
#define MY_LOGV(fmt, arg...)        CAM_LOGV("[Feature::%s] "fmt, __FUNCTION__, ##arg)
#define MY_LOGD(fmt, arg...)        CAM_LOGD("[Feature::%s] "fmt, __FUNCTION__, ##arg)
#define MY_LOGI(fmt, arg...)        CAM_LOGI("[Feature::%s] "fmt, __FUNCTION__, ##arg)
#define MY_LOGW(fmt, arg...)        CAM_LOGW("[Feature::%s] "fmt, __FUNCTION__, ##arg)
#define MY_LOGE(fmt, arg...)        CAM_LOGE("[Feature::%s] "fmt, __FUNCTION__, ##arg)
#define MY_LOGA(fmt, arg...)        CAM_LOGA("[Feature::%s] "fmt, __FUNCTION__, ##arg)
#define MY_LOGF(fmt, arg...)        CAM_LOGF("[Feature::%s] "fmt, __FUNCTION__, ##arg)
//
#define MY_LOGV_IF(cond, ...)       do { if ( (cond) ) { MY_LOGV(__VA_ARGS__); } }while(0)
#define MY_LOGD_IF(cond, ...)       do { if ( (cond) ) { MY_LOGD(__VA_ARGS__); } }while(0)
#define MY_LOGI_IF(cond, ...)       do { if ( (cond) ) { MY_LOGI(__VA_ARGS__); } }while(0)
#define MY_LOGW_IF(cond, ...)       do { if ( (cond) ) { MY_LOGW(__VA_ARGS__); } }while(0)
#define MY_LOGE_IF(cond, ...)       do { if ( (cond) ) { MY_LOGE(__VA_ARGS__); } }while(0)
#define MY_LOGA_IF(cond, ...)       do { if ( (cond) ) { MY_LOGA(__VA_ARGS__); } }while(0)
#define MY_LOGF_IF(cond, ...)       do { if ( (cond) ) { MY_LOGF(__VA_ARGS__); } }while(0)


/******************************************************************************
 *
 ******************************************************************************/
bool
Feature::
setupSensorInfo_FromExModule()
{
    if  ( DevMetaInfo::queryDeviceId(mi4OpenId) == eDevId_AtvSensor )
    {
        ms8SensorName = DLSYM_MODULE_NAME_COMMON_SENSOR_ATV;
        mu4SensorType = NSSensorType::eSensorType_YUV;
        MY_LOGW("ATV sensor...return true");
        return  true;
    }

#if '1'!=MTKCAM_HAVE_SENSOR_HAL     //++++++++++++++++++++++++++++++++++++++++++
    //
    #warning "[FIXME] setupSensorInfo()"
    ms8SensorName = "No_Sensor_Hal";
    mu4SensorType = NSSensorType::eSensorType_RAW;
    return  true;
    //
#else   //MTKCAM_HAVE_SENSOR_HAL    //..........................................
    //
    using namespace NSFeature;
    SensorInfoBase* pSensorInfo = NULL;
    bool ret = false;
    halSensorDev_s halSensorDev = (halSensorDev_s)DevMetaInfo::queryHalSensorDev(mi4OpenId);
    SensorHal* pSensorHal = SensorHal::createInstance();
    if  ( ! pSensorHal ) {
        MY_LOGE("SensorHal::createInstance()");
        goto lbExit;
    }
    //
    if  ( 0 != pSensorHal->sendCommand(halSensorDev, SENSOR_CMD_GET_SENSOR_FEATURE_INFO, (int)&pSensorInfo) || ! pSensorInfo )
    {
        MY_LOGE("SensorHal::sendCommand(%x, SENSOR_CMD_GET_SENSOR_FEATURE_INFO), pSensorInfo(%p)", halSensorDev, pSensorInfo);
        goto lbExit;
    }
    //
    MY_LOGD("type:%d <%#x/%s/%s>", pSensorInfo->GetType(), pSensorInfo->GetID(), pSensorInfo->getDrvName(), pSensorInfo->getDrvMacroName());
    ms8SensorName = pSensorInfo->getDrvMacroName();
    switch  (pSensorInfo->GetType())
    {
    case SensorInfoBase::EType_RAW:
        mu4SensorType = NSSensorType::eSensorType_RAW;
        break;
    case SensorInfoBase::EType_YUV:
        mu4SensorType = NSSensorType::eSensorType_YUV;
        break;
    }
    //
    ret = true;
lbExit:
    if  ( pSensorHal ) {
        pSensorHal->destroyInstance();
        pSensorHal = NULL;
    }
    return ret;
    //
#endif  //MTKCAM_HAVE_SENSOR_HAL    //------------------------------------------
}


/******************************************************************************
 *
 ******************************************************************************/
bool
Feature::
queryCustomFeature(FeatureKeyedMap& rFMap, String8 const& rs8ModuleName)
{
    bool ret = false;
#if 1   //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
#define USE_DIFFERENT_LIBRARY   (0)
    //
    uint32_t u4SensorType   = mu4SensorType;
    String8 const s8Symbol  = String8::format("queryCustCamfeature_%s", rs8ModuleName.string());
    typedef bool (*PFN_CUSTOM_SENSOR_FEATURE_T)(FeatureKeyedMap& rFMap, uint32_t u4SensorType, int facing);
    PFN_CUSTOM_SENSOR_FEATURE_T pfnCustSensorFeature = NULL;
    //
#if USE_DIFFERENT_LIBRARY
    String8 const s8LibPath = String8::format("/system/lib/libcameracustom.so");
    void *handle = ::dlopen(s8LibPath.string(), RTLD_NOW);;
    if ( handle )
    {
        char const *err_str = ::dlerror();
        MY_LOGW("dlopen library=%s %s", s8LibPath.string(), err_str?err_str:"unknown");
        goto lbExit;
    }
    //
    pfnCustSensorFeature = (PFN_CUSTOM_SENSOR_FEATURE_T)::dlsym(handle, s8Symbol.string());
#else
    pfnCustSensorFeature = (PFN_CUSTOM_SENSOR_FEATURE_T)::dlsym(RTLD_DEFAULT, s8Symbol.string());
#endif
    if  ( ! pfnCustSensorFeature )
    {
        MY_LOGW("couldn't find symbol %s", s8Symbol.string());
        goto lbExit;
    }
    //
    if  ( ! pfnCustSensorFeature(rFMap, u4SensorType, DevMetaInfo::queryCameraInfo(mi4OpenId).facing) )
    {
        MY_LOGE("%s() return false", s8Symbol.string());
        goto lbExit;
    }
    //
    ret = true;
lbExit:
#if USE_DIFFERENT_LIBRARY
    if ( handle )
    {
        ::dlclose(handle);
        handle = NULL;
    }
#endif
#endif  //------------------------------------------------------------------------------------------
    MY_LOGD_IF(0, "- ret(%d)", ret);
    return  ret;
}

