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

#include <cutils/xlog.h>
#include <cutils/properties.h>

#include <SkImageEncoder.h>
#include <SkBitmap.h>

#include <gui/ISurfaceComposer.h>
#include <gui/BufferQueue.h>

#include "SurfaceFlinger.h"
#include "Layer.h"

#ifndef EMULATOR_SUPPORT
#include "ui/gralloc_extra.h"
#include "ui/GraphicBufferExtra.h"
#endif

#define ALIGN_CEIL(x,a) (((x) + (a) - 1L) & ~((a) - 1L))

namespace android {

void Layer::drawProtectedImage(const sp<const DisplayDevice>& hw, const Region& clip) const {
    const State& s(drawingState());

    glColor4f(3, 3, 3, 1);
    glEnable(GL_BLEND);
    glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
    glTexEnvx(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);

    Transform trans(s.transform);
    uint32_t len;
    Rect win(s.active.w, s.active.h);
    if (!s.active.crop.isEmpty()) {
        win.intersect(s.active.crop, &win);
    }
    int w = win.getWidth();
    int h = win.getHeight();
    if (w > h) {
        win.left += ((w - h) / 2);
        win.right = win.left + h;
    } else {
        win.top += ((h - w) / 2);
        win.bottom = win.top + w;
    }

    const Transform tr(hw->getTransform() * trans);
    const uint32_t hw_h = hw->getHeight();
    LayerMesh mesh;
    tr.transform(mesh.mVertices[0], win.left,  win.top);
    tr.transform(mesh.mVertices[1], win.left,  win.bottom);
    tr.transform(mesh.mVertices[2], win.right, win.bottom);
    tr.transform(mesh.mVertices[3], win.right, win.top);
    for (size_t i=0 ; i<4 ; i++) {
        mesh.mVertices[i][1] = hw_h - mesh.mVertices[i][1];
    }

    GLfloat texCoords[8];
    texCoords[0] = 0;  texCoords[1] = 0;
    texCoords[2] = 0;  texCoords[3] = 1;
    texCoords[4] = 1;  texCoords[5] = 1;
    texCoords[6] = 1;  texCoords[7] = 0;

    glBindTexture(GL_TEXTURE_2D, mFlinger->getProtectedImageTexName());
    glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

    glDisable(GL_TEXTURE_EXTERNAL_OES);
    glEnable(GL_TEXTURE_2D);

    glMatrixMode(GL_TEXTURE);
    glLoadIdentity();
    glMatrixMode(GL_MODELVIEW);

    glEnableClientState(GL_TEXTURE_COORD_ARRAY);
    glTexCoordPointer(2, GL_FLOAT, 0, texCoords);
    glVertexPointer(2, GL_FLOAT, 0, mesh.getVertices());
    glDrawArrays(GL_TRIANGLE_FAN, 0, mesh.getVertexCount());

    glDisable(GL_BLEND);
    glDisableClientState(GL_TEXTURE_COORD_ARRAY);

    glDisable(GL_TEXTURE_2D);
}

// dump current using buffer in Layer
void Layer::dumpActiveBuffer() const {
    XLOGV("[dumpActiveBuffer] + id=%d", getIdentity());

    if (mActiveBuffer != NULL) {
        char     value[PROPERTY_VALUE_MAX];
        bool     raw;
        uint32_t identity;

        property_get("debug.sf.layerdump.raw", value, "0");
        raw = (0 != atoi(value));
        identity = getIdentity();

        char             fname[128];
        void*            ptr;
        float            bpp;
        SkBitmap         b;
        SkBitmap::Config c;

        int inputFormat = mActiveBuffer->format;
        int dumpHeight = mActiveBuffer->height;

#ifndef EMULATOR_SUPPORT
        // check private format
        if (inputFormat == HAL_PIXEL_FORMAT_YUV_PRIVATE) {
            gralloc_buffer_info_t buffInfo;
            GraphicBufferExtra::get().getBufInfo(mActiveBuffer->handle, &buffInfo);
            int fillFormat = (buffInfo.status & GRALLOC_EXTRA_MASK_CM);
            switch (fillFormat) {
                case GRALLOC_EXTRA_BIT_CM_YV12:
                    inputFormat = HAL_PIXEL_FORMAT_YV12;
                    break;
                case GRALLOC_EXTRA_BIT_CM_NV12_BLK:
                    inputFormat = HAL_PIXEL_FORMAT_NV12_BLK;
                    dumpHeight = ALIGN_CEIL(mActiveBuffer->height, 32);
                    break;
                case GRALLOC_EXTRA_BIT_CM_NV12_BLK_FCM:
                    inputFormat = HAL_PIXEL_FORMAT_NV12_BLK_FCM;
                    dumpHeight = ALIGN_CEIL(mActiveBuffer->height, 32);
                    break;
                default:
                    XLOGD("unexpected format for dumpping clear motion: 0x%x", fillFormat);
                    return;
            }
        }
#endif

        bpp = 1.0f;
        c = SkBitmap::kNo_Config;
        switch (inputFormat) {
            case PIXEL_FORMAT_RGBA_8888:
            case PIXEL_FORMAT_RGBX_8888:
                if (false == raw) {
                    c = SkBitmap::kARGB_8888_Config;
                    sprintf(fname, "/data/SF_dump/%d.png", identity);
                } else {
                    bpp = 4.0;
                    sprintf(fname, "/data/SF_dump/%d.RGBA", identity);
                }
                break;
            case PIXEL_FORMAT_BGRA_8888:
            case 0x1ff:                     // tricky format for SGX_COLOR_FORMAT_BGRX_8888 in fact
                if (false == raw) {
                    c = SkBitmap::kARGB_8888_Config;
                    sprintf(fname, "/data/SF_dump/%d(RBswapped).png", identity);
                } else {
                    bpp = 4.0;
                    sprintf(fname, "/data/SF_dump/%d.BGRA", identity);
                }
                break;
            case PIXEL_FORMAT_RGB_565:
                if (false == raw) {
                    c = SkBitmap::kRGB_565_Config;
                    sprintf(fname, "/data/SF_dump/%d.png", identity);
                } else {
                    bpp = 2.0;
                    sprintf(fname, "/data/SF_dump/%d.RGB565", identity);
                }
                break;
            case HAL_PIXEL_FORMAT_I420:
                bpp = 1.5;
                sprintf(fname, "/data/SF_dump/%d.i420", identity);
                break;
            case HAL_PIXEL_FORMAT_NV12_BLK:
                bpp = 1.5;
                sprintf(fname, "/data/SF_dump/%d.nv12_blk", identity);
                break;
            case HAL_PIXEL_FORMAT_NV12_BLK_FCM:
                bpp = 1.5;
                sprintf(fname, "/data/SF_dump/%d.nv12_blk_fcm", identity);
                break;
            case HAL_PIXEL_FORMAT_YV12:
                bpp = 1.5;
                sprintf(fname, "/data/SF_dump/%d.yv12", identity);
                break;
            default:
                XLOGE("[%s] cannot dump format:%d for identity:%d",
                      __func__, mActiveBuffer->format, identity);
                return;
        }

        {
            //Mutex::Autolock _l(mDumpLock);
            mActiveBuffer->lock(GraphicBuffer::USAGE_SW_READ_OFTEN, &ptr);
            {
                XLOGI("[%s] %s", __func__, getName().string());
                XLOGI("    %s (config:%d, stride:%d, height:%d, ptr:%p)",
                    fname, c, mActiveBuffer->stride, dumpHeight, ptr);

                if (SkBitmap::kNo_Config != c) {
                    b.setConfig(c, mActiveBuffer->stride, dumpHeight);
                    b.setPixels(ptr);
                    SkImageEncoder::EncodeFile(fname, b, SkImageEncoder::kPNG_Type,
                                               SkImageEncoder::kDefaultQuality);
                } else {
                    uint32_t size = mActiveBuffer->stride * dumpHeight * bpp;
                    FILE *f = fopen(fname, "wb");
                    fwrite(ptr, size, 1, f);
                    fclose(f);
                }
            }
            mActiveBuffer->unlock();
        }

        dumpContinuousBuffer();
    }
    XLOGV("[dumpActiveBuffer] - id=%d", getIdentity());
}

void Layer::setContBufsDumpById(int identity) {
    XLOGV("setContBufsDumpById, id=%d", identity);
    mContBufsDumpById = identity;
}

void Layer::activeBufferBackup() {
    if (mActiveBuffer == NULL) {
        XLOGW("[Layer::activeBufferBackup] mActiveBuffer=%p not initialized", mActiveBuffer.get());
        return;
    }
    
    if (mContBufsDumpById == (int)getIdentity() || mContBufsDumpById == -1) {
        XLOGV("[Layer::activeBufferBackup] +, req=%d, id=%d", mContBufsDumpById, getIdentity());
        // check bpp
        float bpp = 0.0f;
        uint32_t width  = mActiveBuffer->width;
        uint32_t height = mActiveBuffer->height;
        uint32_t format = mActiveBuffer->format;
        uint32_t usage  = mActiveBuffer->usage;
        uint32_t stride = mActiveBuffer->stride;
        status_t err;
        
        switch (mActiveBuffer->format) {
            case PIXEL_FORMAT_RGBA_8888:
            case PIXEL_FORMAT_BGRA_8888:
            case PIXEL_FORMAT_RGBX_8888:
            case 0x1ff:
                // tricky format for SGX_COLOR_FORMAT_BGRX_8888 in fact
                bpp = 4.0;
                break;
            case PIXEL_FORMAT_RGB_565:
                bpp = 2.0;
                break;
            case HAL_PIXEL_FORMAT_I420:
                bpp = 1.5;
                break;
            case HAL_PIXEL_FORMAT_YV12:
                bpp = 1.5;
                break;
            default:
                XLOGE("[%s] cannot dump format:%d for identity:%d", __func__, mActiveBuffer->format, getIdentity());
                break;
        }

#define MAX_DEFAULT_BUFFERS 10
        // initialize backup buffer max size
        char value[PROPERTY_VALUE_MAX];

        property_get("debug.sf.contbufsmax", value, "0");
        uint32_t max = atoi(value);
        if (max <= 0)
            max = MAX_DEFAULT_BUFFERS;

        if (mBackupBufsMax != max) {
            mBackupBufsMax = max;
            XLOGI("==>  ring buffer max size, max = %d", max);

            mBackBufs.clear();
            mBackupBufsIndex = 0;
        }

        // create new GraphicBuffer
        if (mBackBufs.size() < mBackupBufsMax) {
            sp<GraphicBuffer> buf;
            buf = new GraphicBuffer(width, height, format, usage);
            mBackBufs.push(buf);

            XLOGI("[id=%d] new buffer for cont. dump, size = %d", getIdentity(), mBackBufs.size());
        }
        
        // detect geometry changed
        if (mBackBufs[mBackupBufsIndex]->width != mActiveBuffer->width || 
            mBackBufs[mBackupBufsIndex]->height != mActiveBuffer->height ||
            mBackBufs[mBackupBufsIndex]->format != mActiveBuffer->format) {
            XLOGI("[id=%d] geometry changed, backup=(%d, %d, %d) ==> active=(%d, %d, %d)",
                getIdentity(),
                mBackBufs[mBackupBufsIndex]->width,
                mBackBufs[mBackupBufsIndex]->height,
                mBackBufs[mBackupBufsIndex]->format,
                mActiveBuffer->width,
                mActiveBuffer->height,
                mActiveBuffer->format);

            sp<GraphicBuffer> buf;
            buf = new GraphicBuffer(width, height, format, usage);
            mBackBufs.replaceAt(buf, mBackupBufsIndex);
        }

        if (mActiveBuffer.get() == NULL || mBackBufs[mBackupBufsIndex] == NULL) {
            XLOGW("[Layer::activeBufferBackup] backup fail, mActiveBuffer=%p, mBackBufs[%d]=%p",
                mActiveBuffer.get(), mBackupBufsIndex, mBackBufs[mBackupBufsIndex].get());
            return;
        }
        
        // backup
        nsecs_t now = systemTime();        

        void* src;
        void* dst;
        err = mActiveBuffer->lock(GraphicBuffer::USAGE_SW_READ_OFTEN, &src);
        if (err != NO_ERROR) {
            XLOGW("[Layer::activeBufferBackup] lock active buffer failed");
            return;
        }

        XLOGV("[Layer::activeBufferBackup] lock +, req=%d, id=%d", mContBufsDumpById, getIdentity());
        err = mBackBufs[mBackupBufsIndex]->lock(GraphicBuffer::USAGE_SW_READ_OFTEN | GraphicBuffer::USAGE_SW_WRITE_OFTEN, &dst);
        if (err != NO_ERROR) {
            XLOGW("[Layer::activeBufferBackup] lock backup buffer failed");
            return;
        }

        backupProcess(dst, src, stride*height*bpp);

        mBackBufs[mBackupBufsIndex]->unlock();
        mActiveBuffer->unlock();

        if (mContBufsDumpById == -1) {
            XLOGI("[Layer::activeBufferBackup] req=%d, id=%d, buf=%d, time=%lld", 
                mContBufsDumpById, getIdentity(), mBackupBufsIndex, ns2ms(systemTime() - now));
        } else {
            XLOGI("[Layer::activeBufferBackup] id=%d, buf=%d, time=%lld", 
                getIdentity(), mBackupBufsIndex, ns2ms(systemTime() - now));
        }
        mBackupBufsIndex ++;
        if (mBackupBufsIndex >= mBackupBufsMax)
            mBackupBufsIndex = 0;
    }
}

void Layer::backupProcess(void* dst, void* src, size_t size) const {
    XLOGV("[Layer::backupProcess] +, req=%d, id=%d", mContBufsDumpById, getIdentity());

    // backup 
    memcpy(dst, src, size);

    XLOGV("[Layer::backupProcess] -, req=%d, id=%d", mContBufsDumpById, getIdentity());
}

void Layer::dumpContinuousBuffer() const {
    char tmp[PROPERTY_VALUE_MAX];
    int  identity = getIdentity();

    if (mContBufsDumpById <= 0 && mContBufsDumpById != -1)
        return;
        
    if (mBackupBufsMax <= 0) {
        XLOGW("[Layer::dumpContinuousBuffer] mBackupBufsMax not updated");
        return;
    }

    XLOGD("[Layer::dumpContinuousBuffer] +, req=%d, id=%d, size=%d",
        mContBufsDumpById, getIdentity(), mBackBufs.size());

    if (mContBufsDumpById == (int)getIdentity() || mContBufsDumpById == -1) {
        int start = (mBackupBufsIndex + mBackupBufsMax - 1) % mBackupBufsMax;
        for (uint32_t i = 0; i < mBackupBufsMax; i++) {
            if (i >= mBackBufs.size()) {
                XLOGW("[Layer::dumpContinuousBuffer] overflow i=%d, max=%d", i, mBackBufs.size());
                return;
            }

            int index = (start + mBackupBufsMax - i) % mBackupBufsMax;
            XLOGD("[Layer::dumpContinuousBuffer] i=%d, index=%d", mBackupBufsMax - i, index);
            sp<GraphicBuffer> buf = mBackBufs[index];
            dumpGraphicBuffer(buf, mBackupBufsMax - i);
        }
    }

    XLOGD("[Layer::dumpContinuousBuffer] -");
}

void Layer::dumpGraphicBuffer(sp<GraphicBuffer> buf, int index) const {
    char             fname[128];
    void*            ptr;
    SkBitmap         b;
    SkBitmap::Config c;
    int              identity = getIdentity();
    float            bpp;
    
    
    c = SkBitmap::kNo_Config;
    switch (buf->format) {
        case PIXEL_FORMAT_RGBA_8888:
        case PIXEL_FORMAT_BGRA_8888:
        case PIXEL_FORMAT_RGBX_8888:
        case 0x1ff:                     // tricky format for SGX_COLOR_FORMAT_BGRX_8888 in fact
            c = SkBitmap::kARGB_8888_Config;
            sprintf(fname, "/data/SF_dump/%d_%03d.png", identity, index);
            break;
        case PIXEL_FORMAT_RGB_565:
            c = SkBitmap::kRGB_565_Config;
            sprintf(fname, "/data/SF_dump/%d_%03d.png", identity, index);
            break;
        case HAL_PIXEL_FORMAT_I420:
            bpp = 1.5;
            sprintf(fname, "/data/SF_dump/%d_%03d.i420", identity, index);
            break;
        case HAL_PIXEL_FORMAT_YV12:
            bpp = 1.5;
            sprintf(fname, "/data/SF_dump/%d_%03d.yv12", identity, index);
            break;
        default:
            XLOGE("[%s] cannot dump format:%d for identity:%d", __func__, buf->format, identity);
            return;
    }

    buf->lock(GraphicBuffer::USAGE_SW_READ_OFTEN, &ptr);
    {
        XLOGI("[Layer::dumpGraphicBuffer] %s", getName().string());
        XLOGI("    %s (config:%d, stride:%d, height:%d, ptr:%p)",
            fname, c, buf->stride, buf->height, ptr);

        if (SkBitmap::kNo_Config != c) {
            b.setConfig(c, buf->stride, buf->height);
            b.setPixels(ptr);
            SkImageEncoder::EncodeFile(
                fname, b, SkImageEncoder::kPNG_Type, SkImageEncoder::kDefaultQuality);
        } else {
            uint32_t size = buf->stride * buf->height * bpp;
            FILE *f = fopen(fname, "wb");
            fwrite(ptr, size, 1, f);
            fclose(f);
        }
    }
    buf->unlock();
}

};
