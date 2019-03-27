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

package com.mediatek.media3d;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.os.Environment;
import android.os.SystemProperties;
import android.util.Log;

import java.util.Locale;

public final class MediaUtils {
    public static final String TAG = "MediaUtils";
    public static final String CAMERA_IMAGE_BUCKET_NAME  =
            Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera";
    public static final String CAMERA_IMAGE_BUCKET_ID = getBucketId(CAMERA_IMAGE_BUCKET_NAME);

    private MediaUtils() {}

    public static String getBucketId(String path) {
        return String.valueOf(path.toLowerCase(Locale.ENGLISH).hashCode());
    }

    public static Rect getTargetRect(int srcWidth, int srcHeight, int maxWidth, int maxHeight) {
        float r = (float)srcHeight / srcWidth;

        int height = (int)(maxWidth * r);
        int margin = (maxHeight - height) / 2;
        return new Rect(0 , margin, maxWidth, margin + height);
    }

    public static Bitmap resizeBitmap(int maxWidth, int maxHeight, Bitmap bitmap) {
        int srcWidth = bitmap.getWidth();
        int srcHeight = bitmap.getHeight();

        Rect srcRect = new Rect(0, 0, srcWidth, srcHeight);
        Rect dstRect = getTargetRect(srcWidth, srcHeight, maxWidth, maxHeight);
        Bitmap b = Bitmap.createBitmap(maxWidth, maxHeight, bitmap.getConfig());
        if (b != null) {
            Canvas canvas = new Canvas(b);
            canvas.drawColor(Color.BLACK, PorterDuff.Mode.SRC);
            canvas.drawBitmap(bitmap, srcRect, dstRect, null);
        }

        return b;
    }

    public static Rect getSourceRect(int srcWidth, int srcHeight, int dstWidth, int dstHeight) {
        float dstRatio = (float) dstWidth / dstHeight;
        float srcRatio = (float) srcWidth / srcHeight;

        if (srcRatio >= dstRatio) {
            int width = (int)(dstRatio * srcHeight);
            int margin = (srcWidth - width) / 2;
            return new Rect(margin, 0, margin + width, srcHeight);
        } else {
            int height = (int)(srcWidth / dstRatio);
            int margin = (srcHeight - height) / 2;
            return new Rect(0, margin, srcWidth, margin + height);
        }
    }

    public static Bitmap resizeBitmapFitTarget(int maxWidth, int maxHeight, Bitmap bitmap) {
        int srcWidth = bitmap.getWidth();
        int srcHeight = bitmap.getHeight();

        Rect srcRect = getSourceRect(srcWidth, srcHeight, maxWidth, maxHeight);
        Rect dstRect = new Rect(0, 0, maxWidth, maxHeight);
        Bitmap b = Bitmap.createBitmap(maxWidth, maxHeight, bitmap.getConfig());
        if (b != null) {
            Canvas canvas = new Canvas(b);
            canvas.drawBitmap(bitmap, srcRect, dstRect, null);
        }

        return b;
    }

    public static Bitmap decodeResourceBitmap(Resources resources, int resId, int width, int height) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inScaled = false;
        Bitmap bitmap = BitmapFactory.decodeResource(resources, resId, opts);
        if ((width == 0 && height == 0) || (width == bitmap.getWidth() && height == bitmap.getHeight())) {
            return bitmap;
        } else {
            Bitmap resizedBitmap = resizeBitmap(width, height, bitmap);
            bitmap.recycle();
            return resizedBitmap;
        }
    }

    public static int calculateSubSampleSize(int srcWidth, int srcHeight, int dstWidth, int dstHeight) {
        if (Media3D.DEBUG) {
            Log.v(TAG, "calculateSubSampleSize - src: " +
                    srcWidth + "x" + srcHeight + ", dst: " + dstWidth + "x" + dstHeight);
        }
        int subSampleSize;
        int width = srcWidth;
        int height = srcHeight;
        // any side is smaller than target size is ok.
        for (subSampleSize = 0; width > 0 && height > 0; subSampleSize++) {
            if (dstWidth > width || dstHeight > height) {
                if (dstWidth > width && dstHeight > height) {
                    if (subSampleSize > 0) {
                        subSampleSize--;
                    }
                }
                break;
            }
            width >>= 1;
            height >>= 1;
        }

        return subSampleSize;
    }

    public static Bitmap cropHalfAndFit(int maxWidth, int maxHeight, Bitmap bitmap, boolean recycle) {
        int srcWidth = bitmap.getWidth();
        int srcHeight = bitmap.getHeight();

        Rect srcRect = new Rect(0, 0, srcWidth / 2, srcHeight);
        Rect dstRect = new Rect(0, 0, maxWidth, maxHeight);
        Bitmap b = Bitmap.createBitmap(maxWidth, maxHeight, getConfig(bitmap));
        if (b != null) {
            Canvas canvas = new Canvas(b);
            canvas.drawBitmap(bitmap, srcRect, dstRect, null);
        }
        if (recycle) {
            bitmap.recycle();
        }
        return b;
    }

    private static Bitmap.Config getConfig(Bitmap bitmap) {
        Bitmap.Config config = bitmap.getConfig();
        if (config == null) {
            config = Bitmap.Config.ARGB_8888;
        }
        return config;
    }

    public static String getDefaultBucketId() {
        return SystemProperties.getBoolean("media3d.defaultbucket", false) ?
               CAMERA_IMAGE_BUCKET_ID : "";
    }
}