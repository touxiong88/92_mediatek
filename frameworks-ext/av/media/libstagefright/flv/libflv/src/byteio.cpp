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

#include "byteio.h"
#include <math.h>

uint8_t  flv_byteio_get_byte(uint8_t  *data)
{
    if (data){
        return (*data);
    }
    else {
        ALOGE("flv_byteio_get_byte error\n");
        return FLV_ERROR;
    }
}
uint16_t flv_byteio_get_2byte(uint8_t  *data)
{
        uint16_t out;
        out = flv_byteio_get_byte(data) << 8;
        out |= flv_byteio_get_byte(data+1);
        return out;
}
uint32_t flv_byteio_get_3byte(uint8_t *data)
{
        uint32_t out;
        out &=0x00000000;
        out = flv_byteio_get_2byte(data) << 8;
        out |= flv_byteio_get_byte(data+2);
        return out;
}
uint32_t flv_byteio_get_4byte(uint8_t *data)
{
        uint32_t out;
        out &=0x00000000;
        out = flv_byteio_get_2byte(data) << 16;
        out |= flv_byteio_get_2byte(data+2);
        return out;
}
uint64_t flv_byteio_get_8byte(uint8_t *data)
{
        uint64_t out;
        out &=0x0000000000000000;
        out = flv_byteio_get_4byte(data);
        out= out << 32;
        out |= flv_byteio_get_4byte(data+4);
        return out;
}
void flv_byteio_get_string(uint8_t *string, uint32_t strlen, uint8_t *data)
{
    uint i = 0;
    char c;
    if(strlen>=256)
    {
      ALOGE("flv_byteio_get_string:  error strlen=%d\n",strlen);
      return;
    }

    while ((c = flv_byteio_get_byte(data++))) {
        if (i < (strlen-1))
            string[i++] = c;
    }

    string[i] = 0; /* Ensure null terminated, but may be truncated */
}
int32_t flv_byteio_read(uint8_t *Out,uint32_t size,flv_iostream_str *iostream)
{
    int32_t tmp;

    if (!iostream || !Out || !iostream->read || !iostream->source || (size < 0)) {
            ALOGE("flv_byteio_read error\n");
            return FLV_ERROR;
    }

    tmp = iostream->read(iostream->source, Out, size);

    if (tmp != size) {
        ALOGE("flv_byteio_read error: read %d,need read %d\n",tmp, size);
    }
    return tmp;
}

double  flv_amf_number2double(uint64_t number)
{
     if((number+number) > 0xFFEULL<<52){
        return 0.0/0.0;
     }
        
     return ldexp(((number&((1LL<<52)-1)) + (1LL<<52)) * (number>>63|1), (number>>52&0x7FF)-1075);
}



