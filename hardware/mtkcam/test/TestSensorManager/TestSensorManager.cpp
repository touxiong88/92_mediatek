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
#include <gui/SensorManager.h>
#include <android/looper.h>
#include <android/sensor.h>
#include <android_native_app_glue.h>
//
#include <TestSensorManager.h>
//-----------------------------------------------------------------------------
ALooper*            gpLooper;
ASensorManager*     gpSensorManager;
ASensorEventQueue*  gpSensorEventQueue;
ASensorRef          gpGyroSensor;
ASensorRef          gpAccSensor;
ASensorRef          gpMagSensor;
ASensorRef          gpLightSensor;
ASensorRef          gpProxiSensor;
//-----------------------------------------------------------------------------
void TestSM_enableSensor(bool bEn)
{
    CAM_LOGD("bEn(%d)",bEn);
    //
    if(bEn)
    {
        ASensorEventQueue_enableSensor(
            gpSensorEventQueue,
            gpAccSensor);
        ASensorEventQueue_enableSensor(
            gpSensorEventQueue,
            gpMagSensor);
        ASensorEventQueue_enableSensor(
            gpSensorEventQueue,
            gpGyroSensor);
        ASensorEventQueue_enableSensor(
            gpSensorEventQueue,
            gpLightSensor);
        ASensorEventQueue_enableSensor(
            gpSensorEventQueue,
            gpProxiSensor);
    }
    else
    {
        ASensorEventQueue_disableSensor(
            gpSensorEventQueue,
            gpAccSensor);
        ASensorEventQueue_disableSensor(
            gpSensorEventQueue,
            gpMagSensor);
        ASensorEventQueue_disableSensor(
            gpSensorEventQueue,
            gpGyroSensor);
        ASensorEventQueue_disableSensor(
            gpSensorEventQueue,
            gpLightSensor);
        ASensorEventQueue_disableSensor(
            gpSensorEventQueue,
            gpProxiSensor);
    }
}
//-----------------------------------------------------------------------------
void TestSM_setEventRate(int sampPerSec)
{
    int usec = (1000L/sampPerSec)*1000;
    //
    CAM_LOGD("sampPerSec(%d)",sampPerSec);
    //
    ASensorEventQueue_setEventRate(
        gpSensorEventQueue,
        gpAccSensor,
        usec);
    ASensorEventQueue_setEventRate(
        gpSensorEventQueue,
        gpMagSensor,
        usec);
    ASensorEventQueue_setEventRate(
        gpSensorEventQueue,
        gpGyroSensor,
        usec);
    ASensorEventQueue_setEventRate(
        gpSensorEventQueue,
        gpLightSensor,
        usec);
    ASensorEventQueue_setEventRate(
        gpSensorEventQueue,
        gpProxiSensor,
        usec);
}
//-----------------------------------------------------------------------------
void TestSM_getEvents(void)
{
    ASensorEvent event;
    while (ASensorEventQueue_getEvents(gpSensorEventQueue, &event, 1) > 0)
    {
        switch(event.type)
        {
            case ASENSOR_TYPE_ACCELEROMETER:
            {
                CAM_LOGD("Acc(%f,%f,%f,%lld)",
                    event.acceleration.x,
                    event.acceleration.y,
                    event.acceleration.z,
                    event.timestamp);
                break;
            }
            case ASENSOR_TYPE_MAGNETIC_FIELD:
            {
                CAM_LOGD("Mag");
                break;
            }
            case ASENSOR_TYPE_GYROSCOPE:
            {
                CAM_LOGD("Gyro(%f,%f,%f,%lld)",
                    event.vector.x,
                    event.vector.y,
                    event.vector.z,
                    event.timestamp);
                break;
            }
            case ASENSOR_TYPE_LIGHT:
            {
                CAM_LOGD("Light");
                break;
            }
            case ASENSOR_TYPE_PROXIMITY:
            {
                CAM_LOGD("Proxi");
                break;
            }
            default:
            {
                CAM_LOGE("unknown type(%d)",event.type);
                break;
            }
        }
    }
}
//-----------------------------------------------------------------------------
void TestSM_poll(
    int     timeount,
    bool    bGetEvent)
{
    int ident, events;
    //
    if((ident = ALooper_pollOnce(timeount, NULL, &events, NULL) >= 0))
    {
        if(bGetEvent)
        {
            switch(ident)
            {
                case LOOPER_ID_MAIN:
                {
                    CAM_LOGD("LOOPER_ID_MAIN");
                    TestSM_getEvents();
                    break;
                }
                case LOOPER_ID_INPUT:
                {
                    CAM_LOGD("LOOPER_ID_INPUT");
                    TestSM_getEvents();
                    break;
                }
                case LOOPER_ID_USER:
                {
                    CAM_LOGD("LOOPER_ID_USER");
                    TestSM_getEvents();
                    break;
                }
                default:
                {
                    CAM_LOGE("unknown ident(%d)",ident);
                    break;
                }
            }
        }
    }
    else
    {
        //You should disable these log in real case to avoid "log lost".
        if(bGetEvent)
        {
            CAM_LOGD("No event!");   
        }
        else
        {
            CAM_LOGD("Done");
        }
    }
}
//-----------------------------------------------------------------------------
int TestSM_callback(
    int     fd,
    int     events,
    void*   pData)
{
    CAM_LOGD("+");
    TestSM_getEvents();
    CAM_LOGD("-");
    //should return 1 to continue receiving callbacks, or 0 to unregister
    return 1;
}
//-----------------------------------------------------------------------------
void TestSM_init(bool bUseCB)
{
    ALooper_callbackFunc cbFunc = NULL;
    //
    CAM_LOGD("+ bUseCB(%d)",bUseCB);
    //
    if(bUseCB)
    {
        cbFunc = TestSM_callback;
    }
    //
    gpLooper = ALooper_forThread();
    if(gpLooper == NULL)
    {
        gpLooper = ALooper_prepare(ALOOPER_PREPARE_ALLOW_NON_CALLBACKS);
    }
    gpSensorManager = ASensorManager_getInstance();
    gpAccSensor = ASensorManager_getDefaultSensor(
                    gpSensorManager,
                    ASENSOR_TYPE_ACCELEROMETER);
    gpMagSensor = ASensorManager_getDefaultSensor(
                    gpSensorManager,
                    ASENSOR_TYPE_MAGNETIC_FIELD);
    gpGyroSensor = ASensorManager_getDefaultSensor(
                    gpSensorManager,
                    ASENSOR_TYPE_GYROSCOPE);
    gpLightSensor = ASensorManager_getDefaultSensor(
                    gpSensorManager,
                    ASENSOR_TYPE_LIGHT);
    gpProxiSensor = ASensorManager_getDefaultSensor(
                    gpSensorManager,
                    ASENSOR_TYPE_PROXIMITY);
    gpSensorEventQueue = ASensorManager_createEventQueue(
                            gpSensorManager,
                            gpLooper,
                            LOOPER_ID_MAIN,
                            cbFunc,
                            NULL);
    //
    CAM_LOGD("-");
}
//-----------------------------------------------------------------------------
void TestSM_uninit(void)
{
    CAM_LOGD("+");
    //
    ASensorManager_destroyEventQueue(
        gpSensorManager,
        gpSensorEventQueue);
    gpLooper = NULL;
    gpSensorManager = NULL;
    gpGyroSensor = NULL;
    gpSensorEventQueue = NULL;
    //
    CAM_LOGD("-");
}

