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

import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import java.io.IOException;

public class ResourceItemSet implements MediaItemSet {

    private MediaItem[] mItems;
    private Resources mResources;

    private static class AssetFileItem implements MediaItem {
        private final String mThumbnail;
        private final String mPhoto;

        AssetFileItem(String thumbnail, String photo) {
            mThumbnail = thumbnail;
            mPhoto = photo;
        }

        public Bitmap getThumbnail(int width, int height) {
            return BitmapFactory.decodeFile(mThumbnail);
        }

        public Uri getUri() {
            return Uri.parse("file://" + mPhoto);
        }

        public String getFilePath() {
            return mPhoto;
        }

        public long getDuration() {
            return 0;
        }
    }

    private class RawResourceItem implements MediaItem {
        private final int mThumbnailResId;
        private final int mDataResId;

        RawResourceItem(int thumbnailResId, int dataResId) {
            mThumbnailResId = thumbnailResId;
            mDataResId = dataResId;
        }

        public Bitmap getThumbnail(int width, int height) {
            return MediaUtils.decodeResourceBitmap(mResources, mThumbnailResId, width, height);
        }

        public Uri getUri() {
            return Uri.parse("android.resource://com.mediatek.media3d/" + mDataResId);
        }

        public String getFilePath() {
            return "";
        }

        public long getDuration() {
            return 1000;
        }
    }

    public ResourceItemSet(Resources resources, int[] thumbnails, int[] files) {
        mResources = resources;
        mItems = new RawResourceItem[thumbnails.length];
        for (int i = 0; i < thumbnails.length; ++i) {
            mItems[i] = new RawResourceItem(thumbnails[i], files[i]);
        }
    }

    public ResourceItemSet(AssetManager assetManager, String path) {
        try {
            String[] files = assetManager.list(path);
            mItems = new AssetFileItem[files.length];
            for (int i = 0; i < mItems.length; i++) {
                String image = path + "/" + files[i];
                mItems[i] = new AssetFileItem(image, image);
            }
        } catch (IOException e) {
            Log.e("ResourceItem", "Exception : " + e);
        }
    }

    public int getItemCount() {
        if (mItems == null) {
            return 0;
        }
        return mItems.length;
    }

    public MediaItem getItem(int index) {
        if (index < 0 || index >= getItemCount()) {
            return null;
        }
        return mItems[index];
    }

    public void close() {
        mItems = null;
    }
}
