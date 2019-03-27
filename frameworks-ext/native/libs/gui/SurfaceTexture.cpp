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

#include <gui/SurfaceTexture.h>
#include <GLES2/gl2ext.h>
#include <cutils/xlog.h>
#include <math.h>

// Macros for including the SurfaceTexture name in log messages
#define ST_LOGI(x, ...) ALOGI("[%s] "x, mName.string(), ##__VA_ARGS__)
#define ST_LOGE(x, ...) ALOGE("[%s] "x, mName.string(), ##__VA_ARGS__)

namespace android {

#define LOCK_FOR_SW (GRALLOC_USAGE_SW_READ_RARELY | GRALLOC_USAGE_SW_WRITE_RARELY | GRALLOC_USAGE_HW_TEXTURE)
#define ALIGN(x,a)  (((x) + (a) - 1L) & ~((a) - 1L))


static int computeSize(int width, int height, PixelFormat pixelFormat)
{
    float bpp;
    switch (pixelFormat) {
        case HAL_PIXEL_FORMAT_I420:
        case HAL_PIXEL_FORMAT_YV12:
        case HAL_PIXEL_FORMAT_NV12_BLK:
        case HAL_PIXEL_FORMAT_NV12_BLK_FCM:
            bpp = 1.5;
            break;

        case HAL_PIXEL_FORMAT_RGB_888:
            bpp = 3;
            break;

        case HAL_PIXEL_FORMAT_RGBA_8888:
            bpp = 4;
            break;

        default:
            XLOGD("Not avaliable color format");
            return -1;
    }

    return (width * height * bpp);
}

static int computeBpp(PixelFormat pixelFormat)
{
    float bpp;
    switch (pixelFormat) {
        case HAL_PIXEL_FORMAT_I420:
        case HAL_PIXEL_FORMAT_YV12:
            bpp = 1.5;
            break;

        case HAL_PIXEL_FORMAT_RGB_888:
            bpp = 3;
            break;

        case HAL_PIXEL_FORMAT_RGBA_8888:
            bpp = 4;
            break;

        default:
            XLOGD("Not avaliable color format");
            return -1;
    }

    return bpp;
}

// CAUTION: 658x version MTKYUV to YV12 SW conversion
static void MTKYUVtoYV12(uint8_t *i, uint8_t *o, int w, int h) {
    const int ysize = w * h;
    const int usize = (w / 2) * (h / 2);

    uint8_t *y = o;
    uint8_t *u = y + ysize + usize;
    uint8_t *v = y + ysize;                   //  Y V U order 3 plane

    int gw;         // grid unit width
    int r, c;       // row, column
    int gr, gc;     // grid
    int fr, fc;     // fraction
    int iaddr;      // input addr
    int oaddr;      // output addr

    gw = w / 16;                            // handle y plane
    for (r = 0; r < h; r++) {
        for (c = 0; c < w; c++) {
            gr = r / 32;
            fr = r % 32;

            gc = c / 16;
            fc = c % 16;

            oaddr = r * w + c;
            iaddr = ((gr * gw + gc) * (16 * 32)) + (fr * 16 + fc);

            y[oaddr] = i[iaddr];
        }
    }

    w /= 2;
    h /= 2;
    i += ysize;
    gw = w / 8;
    for (r = 0; r < h; r++) {
        for (c = 0; c < w; c++) {
            gr = r / 16;
            fr = r % 16;

            gc = c / 8;
            fc = c % 8;

            oaddr = r * w + c;
            iaddr = (((gr * gw + gc) * (8 * 16)) + (fr * 8 + fc)) * 2;

            u[oaddr] = i[iaddr];
            v[oaddr] = i[iaddr + 1];
        }
    }
}

static inline double floatRound(double f) {
    return (f >= 0.0f) ? floor(f + 0.5f) : ceil(f - 0.5f);
}

static void YUV444toRGBA8888(uint8_t y, uint8_t cb, uint8_t cr,
                      uint8_t *r, uint8_t *g, uint8_t *b, uint8_t *a) {
    *r = floatRound(y + 1.402 * (cr - 128));
    *g = floatRound(y - 0.344 * (cb - 128) - 0.714 * (cr - 128));
    *b = floatRound(y + 1.772 * (cb - 128));
    *a = 0xFF;
}

static void YV12toRGBA8888(uint8_t *yuv, uint8_t *rgba, int w, int h) {
    int r, c;               // row, column
    uint8_t y, u, v;          // y, u, v data

    for (r = 0; r < h; r++) {
        for (c = 0; c < w; c++) {
            y = yuv[r * w + c];
            u = yuv[(r / 2) * (w / 2) + (c / 2) + ((w * h) * 5 / 4)];
            v = yuv[(r / 2) * (w / 2) + (c / 2) + (w * h)];
            YUV444toRGBA8888(y, u, v, rgba, rgba + 1, rgba + 2, rgba + 3);
            rgba = rgba + 4;
        }
    }
}

static void I420toRGBA8888(uint8_t *yuv, uint8_t *rgba, int w, int h) {
    int r, c;               // row, column
    uint8_t y, u, v;          // y, u, v data

    for (r = 0; r < h; r++) {
        for (c = 0; c < w; c++) {
            y = yuv[r * w + c];
            u = yuv[(r / 2) * (w / 2) + (c / 2) + (w * h)];
            v = yuv[(r / 2) * (w / 2) + (c / 2) + ((w * h) * 5 / 4)];
            YUV444toRGBA8888(y, u, v, rgba, rgba + 1, rgba + 2, rgba + 3);
            rgba = rgba + 4;
        }
    }
}

static void I420toYV12(uint8_t *src_yp, uint8_t *dst_yp, int sg_w, int sg_h, int dg_s) {
    uint32_t yheight  = sg_h;
    uint32_t uvheight = yheight / 2;

    // row padding required
    uint32_t src_ywidth  = sg_w;
    uint32_t src_uvwidth = src_ywidth / 2;

    // to 16 align
    uint32_t src_ystride = ALIGN(sg_w, 16);
    // already 32 align
    uint32_t dst_ystride = dg_s;
    uint32_t src_ysize = src_ystride * yheight;
    uint32_t dst_ysize = dst_ystride * yheight;

    // as 8 align
    uint32_t src_uvstride = src_ystride / 2;
    // as 16 align
    uint32_t dst_uvstride = dst_ystride / 2;
    uint32_t src_uvsize = src_uvstride * uvheight;
    uint32_t dst_uvsize = dst_uvstride * uvheight;

    uint32_t i;
    uint8_t *src_p, *dst_p;

    src_p = src_yp;
    dst_p = dst_yp;
    for (i = 0; i < yheight; i++) {         // y plane
        memcpy(dst_p, src_p, src_ywidth);
        src_p += src_ystride;
        dst_p += dst_ystride;
    }

    src_p = src_yp + src_ysize;
    dst_p = dst_yp + dst_ysize + dst_uvsize;
    for (i = 0; i < uvheight; i++) {         // u plane
        memcpy(dst_p, src_p, src_uvwidth);
        src_p += src_uvstride;
        dst_p += dst_uvstride;
    }

    src_p = src_yp + src_ysize + src_uvsize;
    dst_p = dst_yp + dst_ysize;
    for (i = 0; i < uvheight; i++) {         // v plane
        memcpy(dst_p, src_p, src_uvwidth);
        src_p += src_uvstride;
        dst_p += dst_uvstride;
    }
}

// support src I420/NV12/YV12 to dst RGBA8888
status_t SurfaceTexture::swConversionLocked(Slot &src, AuxSlot &dst) {
    // source graphic buffer
    sp<GraphicBuffer> sg = src.mGraphicBuffer;

    // destination graphic buffer
    sp<GraphicBuffer> dg = dst.slot.mGraphicBuffer;

    if (!(HAL_PIXEL_FORMAT_I420 == sg->format ||
        HAL_PIXEL_FORMAT_NV12_BLK == sg->format ||
        HAL_PIXEL_FORMAT_NV12_BLK_FCM == sg->format)) {
        XLOGE("[%s] SW convert not for format:%d now", __func__, sg->format);
        return INVALID_OPERATION;
    }

    //uint32_t hal_out_fmt = HAL_PIXEL_FORMAT_RGBA_8888;
    uint32_t hal_out_fmt = HAL_PIXEL_FORMAT_YV12;

    // free aux buffer
    if ((EGL_NO_IMAGE_KHR != dst.eglSlot.mEglImage && dg != NULL) &&
        ((sg->width != dg->width) || (sg->height != dg->height) ||
         (hal_out_fmt != (uint32_t)dg->format))) {

        XLOGI("[%s] free old aux slot", __func__);
        XLOGI("src[w:%d, h:%d, f:0x%x], dst[w:%d, h:%d, f:0x%x], required format:0x%x",
            sg->width, sg->height, sg->format,
            dg->width, dg->height, dg->format,
            hal_out_fmt);

        freeAuxSlotLocked(dst);
    }

    // create aux buffer slot if not exist
    if ((EGL_NO_IMAGE_KHR == dst.eglSlot.mEglImage) && (dst.slot.mGraphicBuffer == NULL)) {
        XLOGI("[%s] create dst buffer and image ", __func__);

        // always convert for IMG_YV12 for SW path
        dg = dst.slot.mGraphicBuffer = new GraphicBuffer(
            sg->width,
            sg->height,
            hal_out_fmt,
            sg->usage);

        if (dg == NULL) {
            XLOGE("    create aux GraphicBuffer FAILED");
            freeAuxSlotLocked(dst);
            return BAD_VALUE;
        } else {
            XLOGI("    create aux GraphicBuffer: %p", dg.get());
        }

        dst.eglSlot.mEglImage = createImage(eglGetCurrentDisplay(), dg);
        if (EGL_NO_IMAGE_KHR == dst.eglSlot.mEglImage) {
            XLOGE("    create aux eglImage FAILED");
            freeAuxSlotLocked(dst);
            return BAD_VALUE;
        }

        XLOGI("[%s] create aux slot success", __func__);
        XLOGI("src[s:%d, w:%d, h:%d, f:0x%x], dst[s:%d, w:%d, h:%d, f:0x%x]",
            sg->stride, sg->width, sg->height, sg->format,
            dg->stride, dg->width, dg->height, dg->format);
    }

    // start copy for alignment adjust
    uint8_t *src_yp, *dst_yp;
    status_t lockret;

    lockret = sg->lock(LOCK_FOR_SW, (void**)&src_yp);
    if (NO_ERROR != lockret) {
        XLOGE("[%s] buffer lock fail: %s", __func__, strerror(lockret));
        return INVALID_OPERATION;
    }

    lockret = dg->lock(LOCK_FOR_SW, (void**)&dst_yp);
    if (NO_ERROR != lockret) {
        XLOGE("[%s] buffer lock fail: %s", __func__, strerror(lockret));
        return INVALID_OPERATION;
    }

    XLOGD("SW convertToAuxSlot +");

    XLOGD("source format:%d", sg->format);

    switch(sg->format) {
        case HAL_PIXEL_FORMAT_I420:
        {
            // I420 (for SW 3D)
            //I420toRGBA8888(src_yp, dst_yp, sg->width, sg->height);
            //XLOGD("I420toRGBA8888");
            I420toYV12(src_yp, dst_yp, sg->width, sg->height, dg->stride);
            XLOGD("I420toYV12");
        }
        break;

        case HAL_PIXEL_FORMAT_NV12_BLK:
        case HAL_PIXEL_FORMAT_NV12_BLK_FCM:
        {
            // MTKYUV
            //uint8_t *temp_yp = (uint8_t *)malloc(computeSize(sg->width, sg->height, sg->format));
            MTKYUVtoYV12(src_yp, dst_yp, sg->width, sg->height);
            XLOGD("MTKYUVtoYV12");

            //YV12toRGBA8888(temp_yp, dst_yp, sg->width, sg->height);
            //free((void*)temp_yp);
            //XLOGD("YV12toRGBA8888");
        }
        break;
/*
        case HAL_PIXEL_FORMAT_YV12:
        {
            YV12toRGBA8888(src_yp, dst_yp, sg->width, sg->height);
            XLOGD("YV12toRGBA8888");
        }
        break;
*/
        default:
        {
            XLOGE("not support format:%d", sg->format);
            dg->unlock();
            sg->unlock();
            return INVALID_OPERATION;
        }
    }
    XLOGD("SW convertToAuxSlot -");

    dg->unlock();
    sg->unlock();

    return NO_ERROR;
}


// CAUTION: bind texture should in context thread only
status_t SurfaceTexture::bindToAuxSlotLocked() {
    if (EGL_NO_IMAGE_KHR != mBackAuxSlot->eglSlot.mEglImage) {
        AuxSlot *tmp = mBackAuxSlot;
        mBackAuxSlot = mFrontAuxSlot;
        mFrontAuxSlot = tmp;

        glBindTexture(mTexTarget, mTexName);
        glEGLImageTargetTexture2DOES(mTexTarget, (GLeglImageOES)mFrontAuxSlot->eglSlot.mEglImage);

        // insert fence sync object just after new front texture applied
        EGLSyncKHR eglFence = mFrontAuxSlot->eglSlot.mEglFence;
        if (eglFence != EGL_NO_SYNC_KHR) {
            XLOGI("[%s] fence sync already exists in mFrontAuxSlot:%p, destoryed it", __func__, mFrontAuxSlot);
            eglDestroySyncKHR(mEglDisplay, eglFence);
        }

        eglFence = eglCreateSyncKHR(mEglDisplay, EGL_SYNC_FENCE_KHR, NULL);
        if (eglFence == EGL_NO_SYNC_KHR) {
            XLOGE("[%s] error creating fence: %#x", __func__, eglGetError());
        }
        glFlush();
        mFrontAuxSlot->eglSlot.mEglFence = eglFence;
    }
    mAuxSlotDirty = false;

    return NO_ERROR;
}


status_t SurfaceTexture::convertToAuxSlot(bool isForce) {
    Mutex::Autolock l(mMutex);
    return convertToAuxSlotLocked(isForce);
}

status_t SurfaceTexture::bindToAuxSlot() {
    Mutex::Autolock l(mMutex);
    return bindToAuxSlotLocked();
}

status_t SurfaceTexture::forceAuxConversionLocked() {
    status_t err = NO_ERROR;
    ST_LOGI("[%s] mCurrentTexture:%d, mCurrentBuf:%p",
        __func__, mCurrentTexture, mCurrentTextureBuf.get());

    if ((mCurrentTextureBuf != NULL) &&
        (checkPixelFormatSupported(mCurrentTextureBuf) == OK)) {
        err = convertToAuxSlotLocked(true);
    }

    return err;
}

status_t SurfaceTexture::dumpAux() const {
    Mutex::Autolock l(mMutex);

    sp<GraphicBuffer> gb = mFrontAuxSlot->slot.mGraphicBuffer;
    if (gb != NULL) {
        String8 filename = String8::format("/data/SF_dump/AUX%d.RGBA", mTexName);
        
        status_t lockret;
        uint8_t *ptr;

        lockret = gb->lock(LOCK_FOR_SW, (void**)&ptr);        
        if (NO_ERROR != lockret) {
            ST_LOGE("[%s] buffer lock fail: %s (gb:%p, handle:%p)",
                __func__, strerror(lockret), gb.get(), gb->handle);
			return INVALID_OPERATION;
        } else {
            FILE *f = fopen(filename.string(), "wb");
            fwrite(ptr, gb->stride * gb->height * 4, 1, f);
            fclose(f);
        }
        gb->unlock();
    }

    return NO_ERROR;
}

}

