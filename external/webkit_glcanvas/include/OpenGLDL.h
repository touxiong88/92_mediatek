/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef WEBKIT_GLCANVAS_OPENGL_DL_H
#define WEBKIT_GLCANVAS_OPENGL_DL_H


#include "utils/Errors.h"
#include <cutils/compiler.h>

#ifndef EGL_EGLEXT_PROTOTYPES
#define EGL_EGLEXT_PROTOTYPES
#endif
#include <EGL/egl.h>
#include <EGL/eglext.h>
#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>

#include <SkBitmap.h>
#include <SkMatrix.h>
#include <SkPaint.h>
#include <SkRefCnt.h>
#include <SkRegion.h>
#include <SkShader.h>
#include <SkXfermode.h>

namespace WebCore {

class OpenGLTextureProxy;
class OpenGLDLWrapper;
class OpenGLTextureLayerWrapper;

/**
 * EGL Context info, run on CanvasTexGenerator thread ONLY
 *
 */
class HwuiContextInfo {
public:
    ~HwuiContextInfo();

    ANDROID_API bool needInit() { return m_needInit; }

    ANDROID_API bool init();

    ANDROID_API void makeContextCurrent();
    // NOT thread-safe, only on CanvasTexGenerator thread.
    ANDROID_API static HwuiContextInfo* getInstance();
    
    ANDROID_API void fontRenderCheckInit(SkPaint* paint);
private:
    HwuiContextInfo();
    bool initEGL();

    bool createContext();
    bool deleteContext();

    EGLDisplay m_dpy;
    EGLConfig m_config;
    EGLSurface m_surface;
    EGLContext m_context;

    bool m_needInit;
    static HwuiContextInfo* gHwuiContextInfo;
};

class OpenGLTextureHandle {
public:

    ANDROID_API OpenGLTextureHandle(int unqueId);
    ANDROID_API ~OpenGLTextureHandle();

    ANDROID_API android::status_t replay(OpenGLDLWrapper* dlWrapper);

    ANDROID_API OpenGLTextureLayerWrapper* lockBuffer();
    ANDROID_API void releaseBuffer(OpenGLTextureLayerWrapper* layer);
    ANDROID_API OpenGLTextureProxy* getProxy() { return m_proxy; }

private:

    OpenGLTextureProxy* m_proxy;
    int m_uniqueId;
};


/**
 * A wrapper for Layer
 *
 */
class OpenGLTextureLayerWrapper {
public:
    ANDROID_API OpenGLTextureLayerWrapper(int uniqueId, bool opaque);
    ANDROID_API ~OpenGLTextureLayerWrapper();

    ANDROID_API void requireTexture(int width, int height);
    ANDROID_API void setSize(int width, int height);
    ANDROID_API GLuint getTexture();
    ANDROID_API bool hasTexture() { return (m_layer != 0); }

    ANDROID_API int getLayerTexturePtr() { return m_layer; }

    ANDROID_API void createSync();
    ANDROID_API void waitSync();
    ANDROID_API EGLImageKHR getEGLImage();

    ANDROID_API void setCurSeq(unsigned seq) { m_currSeq = seq; }
    ANDROID_API int getCurSeq() { return m_currSeq; }

private:

    int m_layer;
    bool m_isOpaque;
    int m_uniqueId;
    int m_width;
    int m_height;
    unsigned m_currSeq;

    static GLuint m_savedProgram;
};

class ReadPixelFuncPtr {
public:
    ANDROID_API virtual ~ReadPixelFuncPtr() {}
    ANDROID_API virtual unsigned char* allocPixels() = 0;
    ANDROID_API virtual void postRead(void* payload) = 0;
private:
    void* fPayload;
};

class GLCanvasExternalResource {
public:
    ANDROID_API GLCanvasExternalResource() {}
    ANDROID_API virtual ~GLCanvasExternalResource() {}
};

/**
 * A wrapper class fro DisplayList
 */
class OpenGLDLWrapper : public SkRefCnt {
public:
    ANDROID_API OpenGLDLWrapper(int displayList, int width, int height, GLCanvasExternalResource* extRes, unsigned seq = 0);
    ANDROID_API virtual ~OpenGLDLWrapper();
    ANDROID_API void dumpDisplayList();

    ANDROID_API size_t getDisplayListSize();
    ANDROID_API int getWidth() { return m_width; }
    ANDROID_API int getHeight() { return m_height; }
    ANDROID_API unsigned getSeq() { return m_seq; }
    ANDROID_API int getDisplayListPtr() { return m_displayList; }
    ANDROID_API android::status_t replayToLayerTexture(OpenGLTextureProxy* proxy, SkRect& rect, bool opaque);
    ANDROID_API void registerPostReplayFuncPtr(ReadPixelFuncPtr* funcPtr) {
        m_readPixelFuncPtr = funcPtr;
    }

private:
    int m_displayList;
    int m_layer;
    SkRect m_dirtyRect;

    ReadPixelFuncPtr* m_readPixelFuncPtr;

    GLCanvasExternalResource* m_externalResource;

    int m_width;
    int m_height;
    unsigned m_seq;
};

/**
 * A wrapper for DisplayListRenderer
 *
 *
 */
class SkiaShader;
class OpenGLDLRWrapper {
public:
    ANDROID_API OpenGLDLRWrapper();
    ANDROID_API ~OpenGLDLRWrapper();

    //---------------------------------------------------------
    //  draw operation functions
    //---------------------------------------------------------
    ANDROID_API bool quickReject(float left, float top, float right, float bottom);
    ANDROID_API void getMatrix(SkMatrix* matrix);
    ANDROID_API int save(int flags);
    ANDROID_API void restore();
    ANDROID_API void restoreToCount(int saveCount);
    ANDROID_API int saveLayer(float left, float top, float right, float bottom,
            SkPaint* p, int flags);
    ANDROID_API int saveLayerAlpha(float left, float top, float right, float bottom,
                int alpha, int flags);

    ANDROID_API void translate(float dx, float dy);
    ANDROID_API void rotate(float degrees);
    ANDROID_API void scale(float sx, float sy);
    ANDROID_API void skew(float sx, float sy);

    ANDROID_API void setMatrix(SkMatrix* matrix);
    ANDROID_API void concatMatrix(SkMatrix* matrix);

    ANDROID_API bool clipRect(float left, float top, float right, float bottom, SkRegion::Op op);


    ANDROID_API android::status_t drawBitmap(SkBitmap* bitmap, float left, float top, SkPaint* paint);
    ANDROID_API android::status_t drawBitmap(SkBitmap* bitmap, SkMatrix* matrix, SkPaint* paint);
    ANDROID_API android::status_t drawBitmap(SkBitmap* bitmap, float srcLeft, float srcTop,
            float srcRight, float srcBottom, float dstLeft, float dstTop,
            float dstRight, float dstBottom, SkPaint* paint);
    ANDROID_API android::status_t drawBitmapData(SkBitmap* bitmap, float left, float top, SkPaint* paint);
    ANDROID_API android::status_t drawBitmapMesh(SkBitmap* bitmap, int meshWidth, int meshHeight,
            float* vertices, int* colors, SkPaint* paint);

    ANDROID_API android::status_t drawPatch(SkBitmap* bitmap, const int32_t* xDivs, const int32_t* yDivs,
            const uint32_t* colors, uint32_t width, uint32_t height, int8_t numColors,
            float left, float top, float right, float bottom, SkPaint* paint);

    //android::status_t drawColor(int color, SkXfermode::Mode mode);
    ANDROID_API android::status_t drawRect(float left, float top, float right, float bottom, SkPaint* paint);
    ANDROID_API android::status_t drawRoundRect(float left, float top, float right, float bottom,
            float rx, float ry, SkPaint* paint);
    ANDROID_API android::status_t drawCircle(float x, float y, float radius, SkPaint* paint);
    ANDROID_API android::status_t drawOval(float left, float top, float right, float bottom, SkPaint* paint);
    ANDROID_API android::status_t drawArc(float left, float top, float right, float bottom,
            float startAngle, float sweepAngle, bool useCenter, SkPaint* paint);
    ANDROID_API android::status_t drawPath(SkPath* path, SkPaint* paint);
    ANDROID_API android::status_t drawLines(float* points, int count, SkPaint* paint);
    ANDROID_API android::status_t drawPoints(float* points, int count, SkPaint* paint);
    ANDROID_API android::status_t drawTextOnPath(const char* text, int bytesCount, int count, SkPath* path,
            float hOffset, float vOffset, SkPaint* paint);
    ANDROID_API android::status_t drawPosText(const char* text, int bytesCount, int count,
            const float* positions, SkPaint* paint);
    ANDROID_API android::status_t drawText(const char* text, int bytesCount, int count,
            float x, float y, const float* positions, SkPaint* paint, float length);

    ANDROID_API android::status_t drawDisplayList(OpenGLDLWrapper* dlWrapper, float srcLeft, float srcTop
            , float srcRight, float srcBottom, float dstLeft, float dstTop, float dstRight, float dstBottom);

    // shader
    ANDROID_API void setupShader(SkiaShader* shader);
    ANDROID_API void resetShader();
    ANDROID_API SkiaShader* createSkiaLinearGradientShader(float* bounds, uint32_t* colors,
        float* positions, int count, SkShader* key, SkShader::TileMode tileMode,
        SkMatrix* matrix, bool blend);
    ANDROID_API SkiaShader* createSkiaCircularGradientShader(float x, float y, float radius, uint32_t* colors,
        float* positions, int count, SkShader* key,SkShader::TileMode tileMode,
        SkMatrix* matrix, bool blend);
    ANDROID_API SkiaShader* createSkiaBitmapShader(SkBitmap* bitmap, SkShader* key, SkShader::TileMode tileX,
        SkShader::TileMode tileY, SkMatrix* matrix, bool blend);
    ANDROID_API SkiaShader* createSkiaTwoPointRadialShader(int width, int height,
        SkShader* key, SkPaint* paint, SkMatrix* matrix, bool blend);

    //---------------------------------------------------------
    //
    //---------------------------------------------------------
    ANDROID_API void setViewport(int width, int height);
    ANDROID_API android::status_t prepareDirty(float left, float top, float right, float bottom, bool opaque);
    ANDROID_API android::status_t prepare(bool opaque);

    ANDROID_API void finish();
    ANDROID_API OpenGLDLWrapper* genDisplayList(GLCanvasExternalResource* extRes = NULL);
    ANDROID_API void reset();

private:
    int m_displayListRenderer;
    // for debug
    unsigned m_displayListSeq;
}; // class OpenGLDL

};

#endif // WEBKIT_GLCANVAS_OPENGL_DL_H
