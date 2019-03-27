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

package com.mediatek.media3d.video;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Video;
import android.util.Log;

import com.mediatek.media3d.MediaDbItem;
import com.mediatek.media3d.MediaUtils;

public class VideoDbItem extends MediaDbItem {
    private static final String TAG = "VDOOBJECT";
    protected long mDuration;
    protected int mStereoType;

    VideoDbItem(ContentResolver cr, long id, Uri uri, String dataPath,
                String displayName, long videoDuration, int stereoType) {
        super(cr, id, uri, dataPath, displayName);
        mDuration = videoDuration;
        mStereoType = stereoType;
    }

    public long getDuration() {
        return mDuration;
    }

    public Bitmap getThumbnail() {
        return Video.Thumbnails.getThumbnail(mContentResolver, mId, Images.Thumbnails.MINI_KIND, null);
    }

    private Rect getTargetRect(int srcWidth, int srcHeight, int maxWidth, int maxHeight) {
        float r = (float)srcHeight / srcWidth;

        int height = (int)(maxWidth * r);
        int margin = (maxHeight - height) / 2;
        return new Rect(0 , margin, maxWidth, margin + height);
    }

    public Bitmap getThumbnail(int maxWidth, int maxHeight) {
        Log.v(TAG, "getThumbnail: " + maxWidth + "x" + maxHeight);
        if (maxWidth == 0 && maxHeight == 0) {
            return getThumbnail();
        }

        Options option = new Options();
        if (mStereoType == 2) {
            option.inSampleSize = calculateSubSampleSize(
                    Images.Thumbnails.MINI_KIND, maxWidth, maxHeight, true);
        } else {
            option.inSampleSize = calculateSubSampleSize(
                    Images.Thumbnails.MINI_KIND, maxWidth, maxHeight, false);
        }

        Bitmap videoThumb = Video.Thumbnails.getThumbnail(mContentResolver, mId,
                            Images.Thumbnails.MINI_KIND, option);

        if (videoThumb == null) {
            Log.v(TAG, "bitmap create failed!!");
            return null;
        }

        Bitmap b;
        if (mStereoType == 2) {
            b = MediaUtils.cropHalfAndFit(maxWidth, maxHeight, videoThumb, true);
        } else {
            int srcWidth = videoThumb.getWidth();
            int srcHeight = videoThumb.getHeight();

            Rect srcRect = new Rect(0, 0, srcWidth, srcHeight);
            Rect dstRect = getTargetRect(srcWidth, srcHeight, maxWidth, maxHeight);
            b = Bitmap.createBitmap(maxWidth, maxHeight, videoThumb.getConfig());
            if (b == null) {
                Log.v(TAG, "bitmap create failed!!");
                return null;
            }
            b.eraseColor(Color.BLACK);
            Canvas canvas = new Canvas(b);
            canvas.drawBitmap(videoThumb, srcRect, dstRect, null);
            videoThumb.recycle();
        }
        return b;
    }
}
