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
#define LOG_TAG "MtkCam/TestSL"
//-----------------------------------------------------------------------------
#include <utils/Log.h>
#include <cutils/xlog.h>
#include <android/sensor.h>
#include <mtkcam/common.h>
#include <mtkcam/utils/SensorListener.h>
//-----------------------------------------------------------------------------
using namespace android;
//-----------------------------------------------------------------------------
#define CAM_LOGD(fmt, arg...)    printf("(%d)[%s]"          fmt "\r\n", ::gettid(), __FUNCTION__,           ##arg)
#define CAM_LOGW(fmt, arg...)    printf("(%d)[%s]WRN(%5d):" fmt "\r\n", ::gettid(), __FUNCTION__, __LINE__, ##arg)
#define CAM_LOGE(fmt, arg...)    printf("(%d)[%s]ERR(%5d):" fmt "\r\n", ::gettid(), __FUNCTION__, __LINE__, ##arg)
//-----------------------------------------------------------------------------
void myListener(ASensorEvent event)
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
//-----------------------------------------------------------------------------
int main(int argc, char** argv)
{
    CAM_LOGD("+");
    //
    SensorListener* pSensorListener = SensorListener::createInstance();
    pSensorListener->setListener(myListener);
    //
    pSensorListener->enableSensor(SensorListener::SensorType_Acc,33);
    usleep(5*1000*1000);
    //
    pSensorListener->enableSensor(SensorListener::SensorType_Gyro,33);
    usleep(5*1000*1000);
    //
    pSensorListener->disableSensor(SensorListener::SensorType_Acc);
    pSensorListener->disableSensor(SensorListener::SensorType_Gyro);
    pSensorListener->destroyInstance();
    pSensorListener = NULL;
    //
    CAM_LOGD("-");
    return 0;
}

