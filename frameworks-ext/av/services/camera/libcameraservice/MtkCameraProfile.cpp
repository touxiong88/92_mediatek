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

#define LOG_TAG "MtkCameraProfile"

#include <camera/MtkCameraProfile.h>

static bool gbInit = false; 

// Event, parent , name 
static CPT_Event_Info gCPTEventInfo[] =
{
    {Event_Camera_Framework_Root, EVENT_CAMERA_ROOT, "CameraFramework"}
    // Define the event info used in Cameraservice
    ,{Event_CameraService, Event_Camera_Framework_Root, "CameraService"}
    ,{Event_CS_connect, Event_CameraService, "CS_connect"}
    ,{Event_CS_newMediaPlayer, Event_CS_connect, "newMediaPlayer"}
    ,{Event_CS_newCamHwIF, Event_CS_connect, "newCamHwIF"}
    ,{Event_CS_newClient, Event_CS_connect, "newClient"}
    ,{Event_CS_getParameters, Event_CameraService, "getParameters"}
    ,{Event_CS_setParameters, Event_CameraService, "setParameters"}
    ,{Event_CS_setPreviewDisplay, Event_CameraService, "setPreviewDisplay"}
    ,{Event_CS_sendCommand, Event_CameraService, "sendCommand"}
    ,{Event_CS_startPreview, Event_CameraService, "startPreview"}
    ,{Event_CS_takePicture, Event_CameraService, "takePicture"}
    ,{Event_CS_stopPreview, Event_CameraService, "stopPreview"}
    ,{Event_CS_startRecording, Event_CameraService, "startRecording"}
    ,{Event_CS_releaseRecordingFrame, Event_CameraService, "releaseRecordingFrame"}
    ,{Event_CS_dataCallbackTimestamp, Event_CameraService, "dataCallbackTimestamp"}
    ,{Event_CS_stopRecording, Event_CameraService, "stopRecording"}
    ,{Event_CS_playSound, Event_CameraService, "playSound"}
    ,{Event_CS_disconnect, Event_CameraService, "disconnect"}
    ,{Event_CS_disconnectWindow, Event_CameraService, "disconnectWindow"}

}; 

bool initCameraProfile()
{
    bool ret = false; 
    if(!gbInit)
    {
        ret = CPTRegisterEvents(gCPTEventInfo, sizeof(gCPTEventInfo) / sizeof(gCPTEventInfo[0]));     
        gbInit = ret;
    }
    return ret;
}

