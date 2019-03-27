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
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;

public final class BitmapUtil {

    private static final String TAG = "BMPUTIL";

    private BitmapUtil() {} // make it singleton

    /**
     *  create a bit map with assigned width / height.
     *  the bitmap will be up/down sized and cropped if the sourcebmp is larger/smaller
     *  than target image
     *
     *  @param  sourceBmp   the original bitmap
     *  @param  dstWidth    desired width of bitmap
     *  @param  dstHeight   desired height of bitmap
     *  @return cropped bitmap
     *
     **/
    public static Bitmap autoCropBitmapBySize(Bitmap sourceBmp, int dstWidth, int dstHeight) {
        Rect dst = new Rect(0, 0, dstWidth, dstHeight);

        Rect src = measureRect(sourceBmp.getWidth(), sourceBmp.getHeight(), dstWidth, dstHeight);

        Log.v(TAG, "dstRect: " + dst + ", srcRect: " + src);
        Bitmap targetBmp = Bitmap.createBitmap(dstWidth, dstHeight, sourceBmp.getConfig());
        if (targetBmp == null) {
            Log.v(TAG, "bitmap create failed!!");
            return null;
        }
        Canvas c = new Canvas(targetBmp);
        c.drawBitmap(sourceBmp, src, dst, null);

        return targetBmp;
    }

    /**
     *  create a bit map with assigned width / height, and blur it
     *  the bitmap will be up/down sized and cropped if the sourcebmp is larger/smaller
     *  than target image
     *
     *  @param  sourceBmp   the original bitmap
     *  @param  dstWidth    desired width of bitmap
     *  @param  dstHeight   desired height of bitmap
     *  @return cropped bitmap
     *
     **/
    public static Bitmap autoCropBitmapBySizeBlur(Bitmap sourceBmp, int dstWidth, int dstHeight) {
        // get a smaller one, then enlarge.
        Bitmap thumbBitmap = autoCropBitmapBySize(sourceBmp, dstWidth / 4, dstHeight / 4);
        if (thumbBitmap == null) {
            return null;
        }
        Bitmap ret = Bitmap.createScaledBitmap(thumbBitmap, dstWidth, dstHeight, true);
        thumbBitmap.recycle();
        return ret;
    }


    private static Rect measureRect(int srcWidth, int srcHeight, int destWidth, int destHeight) {
        // got the src width / height
        float dstRatio = (float)destWidth / destHeight;

        Log.v(TAG, String.format("src: %dx%d", srcWidth, srcHeight));
        // use the dest ratio to get the biggest rect in src.
        // try width;
        Log.v(TAG, "dstRatio = " + dstRatio);
        int tempWidth = srcWidth;
        int tempHeight = (int)((float)srcWidth / dstRatio);
        Log.v(TAG, "tempHeight: " + tempHeight);
        if (tempHeight > srcHeight) {
            // use height
            tempHeight = srcHeight;
            tempWidth = (int)((float)srcHeight * dstRatio);
        }

        int x0 = (srcWidth - tempWidth) / 2;
        int y0 = (srcHeight - tempHeight) / 2;

        return new Rect(x0, y0, x0 + tempWidth, y0 + tempHeight);
    }

    public static float getImageWidth(Resources res, int id) {
        return ((BitmapDrawable)res.getDrawable(id)).getBitmap().getWidth();
    }
}
