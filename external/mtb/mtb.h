/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2012. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */


#ifndef __MTB__
#define __MTB__

#ifdef __cplusplus
extern "C" {
#endif

#define MTB_TRACE_SYSTRACE        (1<<0)
#define MTB_TRACE_MET             (1<<1)
#define MTB_TRACE_MMP             (1<<2)
#define MTB_TRACE_LAST            (MTB_TRACE_MMP)
#define MTB_TRACE_TYPE_VALID_MASK ((MTB_TRACE_LAST -1) | MTB_TRACE_LAST)


#define MTB_TAG_NEVER            0       // The "never" tag is never enabled.
#define MTB_TAG_GRAPHICS         (1<<1)
#define MTB_TAG_INPUT            (1<<2)
#define MTB_TAG_VIEW             (1<<3)
#define MTB_TAG_WEBVIEW          (1<<4)
#define MTB_TAG_WINDOW_MANAGER   (1<<5)
#define MTB_TAG_ACTIVITY_MANAGER (1<<6)
#define MTB_TAG_SYNC_MANAGER     (1<<7)
#define MTB_TAG_AUDIO            (1<<8)
#define MTB_TAG_VIDEO            (1<<9)
#define MTB_TAG_CAMERA           (1<<10)
#define MTB_TAG_LAST             MTB_TAG_CAMERA

#ifndef MTB_SUPPORT

#define mtb_trace_init(type, tag) (0)
#define mtb_trace_begin(tag, name, pid) (0)
#define mtb_trace_end(tag) (0)
#define mtb_trace_oneshot() (0)

#else

int mtb_trace_init(uint32_t type, uint64_t tag);

int mtb_trace_begin(uint64_t tag, const char *name, uint32_t pid);

int mtb_trace_end(uint64_t tag, uint32_t pid);

int mtb_trace_oneshot(uint64_t tag, const char *type, const char *name, uint32_t pid);

#endif

#ifdef __cplusplus
}
#endif


#endif // __MTB__
