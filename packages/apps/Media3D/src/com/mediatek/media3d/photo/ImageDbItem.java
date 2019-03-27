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

package com.mediatek.media3d.photo;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory.Options;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.util.Log;

import com.mediatek.media3d.Media3D;
import com.mediatek.media3d.MediaDbItem;
import com.mediatek.media3d.MediaUtils;

public final class ImageDbItem extends MediaDbItem {
    private static final String TAG = "ImageDbItem";

    public static final int MEDIA_ID_INDEX = 0;
    public static final int MEDIA_CAPTION_INDEX = 1;
    public static final int MEDIA_MIME_TYPE_INDEX = 2;
    public static final int MEDIA_LATITUDE_INDEX = 3;
    public static final int MEDIA_LONGITUDE_INDEX = 4;
    public static final int MEDIA_DATE_TAKEN_INDEX = 5;
    public static final int MEDIA_DATE_ADDED_INDEX = 6;
    public static final int MEDIA_DATE_MODIFIED_INDEX = 7;
    public static final int MEDIA_DATA_INDEX = 8;
    public static final int MEDIA_ORIENTATION_OR_DURATION_INDEX = 9;
    public static final int MEDIA_BUCKET_ID_INDEX = 10;
    public static final int MEDIA_STEREO_TYPE_INDEX = 11;

    public String mMimeType;
    public double mLatitude;
    public double mLongitude;
    public long mDateTakenInMs;
    public long mDateAddedInSec;
    public long mDateModifiedInSec;
    public float mRotation;
    public int mStereoType;

    public ImageDbItem(ContentResolver cr, long id, Uri uri, String dataPath, String displayName) {
        super(cr, id, uri, dataPath, displayName);
    }

    public Bitmap getThumbnail() {
        return Images.Thumbnails.getThumbnail(mContentResolver, mId, Images.Thumbnails.MINI_KIND, null);
    }

    public String getPath() {
        return mDataPath;
    }

    public Bitmap getThumbnail(int maxWidth, int maxHeight) {
        if (Media3D.DEBUG) {
            Log.v(TAG, "getThumbnail (dst): " + maxWidth + "x" + maxHeight);
        }
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

        Bitmap miniThumb = MediaStore.Images.Thumbnails.getThumbnail(mContentResolver, mId,
            Images.Thumbnails.MINI_KIND, option);
        if (miniThumb == null) {
            if (Media3D.DEBUG) {
                Log.v(TAG, "bitmap create failed!!");
            }
            return null;
        }

        if (Media3D.DEBUG) {
            Log.v(TAG, "getThumbnail (src): " + miniThumb.getWidth() + "x" + miniThumb.getHeight());
        }

        Bitmap b;
        if (mStereoType == 2) {
            b = MediaUtils.cropHalfAndFit(maxWidth, maxHeight, miniThumb, true);
        } else {
            b = MediaUtils.resizeBitmapFitTarget(maxWidth, maxHeight, miniThumb);
            miniThumb.recycle();
        }
        return b;
    }

    public long getDuration() {
        return 0;
    }
}
