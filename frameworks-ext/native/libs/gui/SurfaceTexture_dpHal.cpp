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

#include "ui/gralloc_extra.h"

#include "ui/GraphicBufferExtra.h"
#include <gui/SurfaceTexture.h>

#include <utils/Trace.h>

#include <cutils/xlog.h>
#include <cutils/properties.h>

#include <DpBlitStream.h>

#define ALIGN_CEIL(x,a) (((x) + (a) - 1L) & ~((a) - 1L))
#define LOCK_FOR_DP (GRALLOC_USAGE_SW_READ_RARELY | GRALLOC_USAGE_SW_WRITE_NEVER | GRALLOC_USAGE_HW_TEXTURE)

namespace android {

status_t SurfaceTexture::checkPixelFormatSupported(
    sp<GraphicBuffer> graphicBuffer) const {

    if (graphicBuffer != NULL) {
        PixelFormat format = graphicBuffer->format;
        if ((HAL_PIXEL_FORMAT_I420 == format) ||
            (HAL_PIXEL_FORMAT_NV12_BLK == format) ||
            (HAL_PIXEL_FORMAT_NV12_BLK_FCM == format) ||
            (HAL_PIXEL_FORMAT_YUV_PRIVATE == format)) {
            return OK;
        }
    }
    return INVALID_OPERATION;
}

status_t SurfaceTexture::freeAuxSlotLocked(AuxSlot &bs) {
    if (EGL_NO_IMAGE_KHR != bs.eglSlot.mEglImage || bs.slot.mGraphicBuffer != NULL) {

        // destroying fence sync
        if (EGL_NO_SYNC_KHR != bs.eglSlot.mEglFence) {
            eglDestroySyncKHR(mEglDisplay, bs.eglSlot.mEglFence);
            bs.eglSlot.mEglFence = EGL_NO_SYNC_KHR;
        }

        XLOGI("[%s] this:%p", __func__, this);
        XLOGD("    GraphicBuffer: gb=%p handle=%p", bs.slot.mGraphicBuffer.get(), bs.slot.mGraphicBuffer->handle);
        XLOGD("    EGLImage: dpy=%p, img=%p", mEglDisplay, bs.eglSlot.mEglImage);

        bs.slot.mGraphicBuffer = NULL;
        eglDestroyImageKHR(mEglDisplay, bs.eglSlot.mEglImage);
        bs.eglSlot.mEglImage = EGL_NO_IMAGE_KHR;
    }

    return NO_ERROR;
}

// conversion function should format by format, chip by chip
// currently the input is I420, YV12, and MTKYUV; the output is ABGR
status_t SurfaceTexture::convertToAuxSlotLocked(bool isForce) {
    // check invalid buffer
    if (BufferQueue::INVALID_BUFFER_SLOT == mCurrentTexture) {
        mAuxSlotConvert = false;
        return INVALID_OPERATION;
    }

    ATRACE_CALL();

    // 1) normal BufferQueue needs conversion now
    // 2) SurfaceTextureLayer neesd conversion aftern HWC
    bool isNeedConversionNow =
        (BufferQueue::TYPE_BufferQueue == mBufferQueue->getType()) ||
        ((true == isForce) && (BufferQueue::TYPE_SurfaceTextureLayer == mBufferQueue->getType()));

    if (true == isNeedConversionNow) {
        XLOGI("do convertToAuxSlot...");

        Slot &src = mSlots[mCurrentTexture];
        AuxSlot &dst = *mBackAuxSlot;

        // wait fence sync here to make sure buffer is released from G3D
        EGLSyncKHR fence = mFrontAuxSlot->eglSlot.mEglFence;
        if (fence != EGL_NO_SYNC_KHR) {
            EGLint result = eglClientWaitSyncKHR(mEglDisplay, fence, 0, 1000000000);
            if (result == EGL_FALSE) {
                XLOGW("[%s] FAILED waiting for front fence: %#x, tearing risk", __func__, eglGetError());
            } else if (result == EGL_TIMEOUT_EXPIRED_KHR) {
                XLOGW("[%s] TIMEOUT waiting for front fence, tearing risk", __func__);
            }
            eglDestroySyncKHR(mEglDisplay, fence);
            mFrontAuxSlot->eglSlot.mEglFence = EGL_NO_SYNC_KHR;
        }

#if 1
        // source graphic buffer
        sp<GraphicBuffer> sg = src.mGraphicBuffer;

        // destination graphic buffer
        sp<GraphicBuffer> dg = dst.slot.mGraphicBuffer;

        // force to convert to RGBA format (X for const alpha)
        uint32_t hal_out_fmt = HAL_PIXEL_FORMAT_RGBA_8888;
        DP_COLOR_ENUM dp_out_fmt = DP_COLOR_RGBX8888;
        int dp_out_bpp = 4;

        // free if current aux slot exist and not fit
        if ((EGL_NO_IMAGE_KHR != dst.eglSlot.mEglImage && dg != NULL) &&
            ((sg->width != dg->width) || (sg->height != dg->height) || (hal_out_fmt != (uint32_t)dg->format))) {

            XLOGI("[%s] free old aux slot ", __func__);
            XLOGI("    src[w:%d, h:%d, f:0x%x] dst[w:%d, h:%d, f:0x%x]",
                sg->width, sg->height, sg->format,
                dg->width, dg->height, dg->format);
            XLOGI("    required format:0x%x", hal_out_fmt);

            freeAuxSlotLocked(dst);
        }

        // create aux buffer if current is NULL
        if ((EGL_NO_IMAGE_KHR == dst.eglSlot.mEglImage) && (dst.slot.mGraphicBuffer == NULL)) {
            XLOGI("[%s] create dst buffer and image", __func__);

            XLOGI("    before create new aux buffer: %p", dg.get());
            dg = dst.slot.mGraphicBuffer = new GraphicBuffer(sg->width,
                                                        sg->height,
                                                        hal_out_fmt,
                                                        sg->usage);
            if ((dg == NULL) || (dg->handle == NULL)) {
                XLOGE("    create aux GraphicBuffer FAILED");
                freeAuxSlotLocked(dst);
                return BAD_VALUE;
            } else {
                XLOGI("    [NEW AUX] gb=%p, handle=%p, w=%d, h=%d, s=%d, fmt=%d",
                    dg.get(), dg->handle,
                    dg->width, dg->height, dg->stride,
                    dg->format);
            }

            dst.eglSlot.mEglImage = createImage(mEglDisplay, dg);
            if (EGL_NO_IMAGE_KHR == dst.eglSlot.mEglImage) {
                XLOGE("[%s] create aux eglImage FAILED", __func__);
                freeAuxSlotLocked(dst);
                return BAD_VALUE;
            }

            XLOGI("[%s] create aux slot success", __func__);
            XLOGI("    src[w:%d, h:%d, f:0x%x], dst[w:%d, h:%d, f:0x%x]",
                sg->width, sg->height, sg->format,
                dg->width, dg->height, dg->format);
        }

        // validate buffer ion
        int sg_ion_idx, dg_ion_idx;
        int num;
        GraphicBufferExtra::get().getIonFd(sg->handle, &sg_ion_idx, &num);
        if ((num <= 0) || (sg->handle->data[sg_ion_idx] == -1)) {
            XLOGE("[%s] validate src ion failed", __func__);
            return BAD_VALUE;
        }
        GraphicBufferExtra::get().getIonFd(dg->handle, &dg_ion_idx, &num);
        if ((num <= 0) || (dg->handle->data[dg_ion_idx] == -1)) {
            XLOGE("[%s] validate dst ion failed", __func__);
            return BAD_VALUE;
        }

        // start buffer conversion with blitter
        {
            DpBlitStream bltStream;

            // buffer data layout desc for dp
            // all these following vars should be se correctly case by case
            int src_y_stride;
            int src_uv_stride;
            int src_y_size;
            int src_uv_size;

            // for planes
            unsigned int src_size[3];
            unsigned int dst_size[1];

            // for dp color format
            DP_COLOR_ENUM dp_in_fmt;
            int dp_in_plane_num;

            int inputFormat = sg->format;
            int sourceHeight = sg->height;
            if (inputFormat == HAL_PIXEL_FORMAT_YUV_PRIVATE) {
                gralloc_buffer_info_t buffInfo;
                GraphicBufferExtra::get().getBufInfo(sg->handle, &buffInfo);
                int fillFormat = (buffInfo.status & GRALLOC_EXTRA_MASK_CM);
                switch (fillFormat) {
                    case GRALLOC_EXTRA_BIT_CM_YV12:
                        inputFormat = HAL_PIXEL_FORMAT_YV12;
                        break;
                    case GRALLOC_EXTRA_BIT_CM_NV12_BLK:
                        inputFormat = HAL_PIXEL_FORMAT_NV12_BLK;
                        sourceHeight = ALIGN_CEIL(sg->height, 32);
                        break;
                    case GRALLOC_EXTRA_BIT_CM_NV12_BLK_FCM:
                        inputFormat = HAL_PIXEL_FORMAT_NV12_BLK_FCM;
                        sourceHeight = ALIGN_CEIL(sg->height, 32);
                        break;
                    default:
                        XLOGD("unexpected format for clear motion: 0x%x", fillFormat);
                        return INVALID_OPERATION;
                }
            }

            // set src buffer
            switch (inputFormat) {
                case HAL_PIXEL_FORMAT_I420:
                    src_y_stride  = sg->stride;
                    src_uv_stride = src_y_stride / 2;
                    src_y_size    = sg->stride * sourceHeight;
                    src_uv_size   = src_y_size / 4;

                    src_size[0] = src_y_size;
                    src_size[1] = src_uv_size;
                    src_size[2] = src_uv_size;

                    dp_in_fmt = DP_COLOR_I420;
                    dp_in_plane_num = 3;
                    break;

                case HAL_PIXEL_FORMAT_NV12_BLK:
                    // for MTK internal HW decode format
                    src_y_stride  = sg->width * 32;
                    src_uv_stride = src_y_stride / 2;
                    src_y_size    = sg->width * sourceHeight;
                    src_uv_size   = src_y_size  / 4;

                    src_size[0] = src_y_size;
                    src_size[1] = src_uv_size * 2;

                    dp_in_fmt = DP_COLOR_420_BLKP;
                    dp_in_plane_num = 2;
                    break;

                case HAL_PIXEL_FORMAT_NV12_BLK_FCM:
                    // for MTK internal HW decode format
                    src_y_stride  = sg->width * 32;
                    src_uv_stride = src_y_stride / 2;
                    src_y_size    = sg->width * sourceHeight;
                    src_uv_size   = src_y_size / 4;

                    src_size[0] = src_y_size;
                    src_size[1] = src_uv_size * 2;

                    dp_in_fmt = DP_COLOR_420_BLKI;
                    dp_in_plane_num = 2;
                    break;

                case HAL_PIXEL_FORMAT_YV12:
                    src_y_stride  = sg->width;
                    src_uv_stride = ALIGN_CEIL((sg->width / 2), 16);
                    src_y_size    = sg->width * sourceHeight;
                    src_uv_size   = src_uv_stride * (sourceHeight / 2);

                    src_size[0] = src_y_size;
                    src_size[1] = src_uv_size;
                    src_size[2] = src_uv_size;

                    dp_in_fmt = DP_COLOR_YV12;
                    dp_in_plane_num = 3;
                    break;

                default:
                    XLOGD("unexpected format for dp in:%d", sg->format);
                    return INVALID_OPERATION;
            }
            bltStream.setSrcBuffer(
                sg->handle->data[sg_ion_idx],
                src_size,
                dp_in_plane_num);

            DpRect src_roi;
            src_roi.x = 0;
            src_roi.y = 0;
            src_roi.w = sg->width;
            src_roi.h = sg->height;
            bltStream.setSrcConfig(
                sg->width, sourceHeight,
                src_y_stride, src_uv_stride,
                dp_in_fmt, DP_PROFILE_BT601, eInterlace_None,
                &src_roi, DP_SECURE_NONE, false);

            // set dst buffer
            dst_size[0] = (dg->stride * dg->height) * dp_out_bpp;
            bltStream.setDstBuffer(
                dg->handle->data[dg_ion_idx],
                dst_size,
                1);

            DpRect dst_roi;
            dst_roi.x = 0;
            dst_roi.y = 0;
            dst_roi.w = dg->width;
            dst_roi.h = dg->height;

            bltStream.setDstConfig(
                dg->width, dg->height,
                dg->stride * 4, 0,
                dp_out_fmt, DP_PROFILE_BT601, eInterlace_None,
                &dst_roi, DP_SECURE_NONE, false);

#ifdef MTK_PQ_SUPPORT
            bltStream.setTdshp(1);
#endif

            if (DP_STATUS_RETURN_SUCCESS != bltStream.invalidate()) {
                XLOGE("DpBlitStream invalidate failed");
                return INVALID_OPERATION;
            }
        }

#else // TODO: backup path to use SW conversion, have to remove before MP
        status_t err = swConversionLocked(src, dst);
        if (NO_ERROR != err) return err;
#endif // USE_DP

        mAuxSlotConvert = false;
        mAuxSlotDirty = true;

        // draw grey debug line to aux
        if (true == mLine) {
            BufferQueue::DrawDebugLineToGraphicBuffer(dg, mLineCnt, 0x80);
            mLineCnt += 1;
        }
    }

    return NO_ERROR;
}


}

