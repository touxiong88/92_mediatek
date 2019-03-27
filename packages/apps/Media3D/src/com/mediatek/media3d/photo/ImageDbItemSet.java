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
import android.database.Cursor;
import android.provider.MediaStore.Images;

import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.media3d.MediaDbItem;
import com.mediatek.media3d.MediaDbItemSet;

public class ImageDbItemSet extends MediaDbItemSet {
    private static final String TAG = "ImageDbItemSet";

    ImageDbItemSet(ContentResolver cr, String bucketId) {
        super(cr, Images.Media.EXTERNAL_CONTENT_URI, bucketId);
    }

    @Override
    protected String selection() {
        if (mBucketId.equalsIgnoreCase("0")) {
            return "stereo_type != 0";
        } else {
            String stereoFilter = "";
            if (!FeatureOption.MTK_S3D_SUPPORT) {
                stereoFilter = " AND stereo_type == 0";
            }
            return String.format("%s in (%s)%s", Images.ImageColumns.BUCKET_ID, mBucketId, stereoFilter);
        }
    }

    private static final boolean IS_DESCEND_SORT = true;
    private static final String DESCEND_SORT = " DESC";
    private static final String ASCEND_SORT = " ASC";

    private static final String DEFAULT_IMAGE_SORT_ORDER =
            Images.ImageColumns.DATE_TAKEN + (IS_DESCEND_SORT ? DESCEND_SORT : ASCEND_SORT);

    @Override
    protected String sortOrder() {
        return DEFAULT_IMAGE_SORT_ORDER;
    }

    private static final String[] PROJECTION_IMAGES = new String[] {
        Images.ImageColumns._ID, Images.ImageColumns.TITLE,
        Images.ImageColumns.MIME_TYPE, Images.ImageColumns.LATITUDE, Images.ImageColumns.LONGITUDE,
        Images.ImageColumns.DATE_TAKEN, Images.ImageColumns.DATE_ADDED, Images.ImageColumns.DATE_MODIFIED,
        Images.ImageColumns.DATA, Images.ImageColumns.ORIENTATION, Images.ImageColumns.BUCKET_ID, "stereo_type"};

    @Override
    protected String[] projection() {
        return PROJECTION_IMAGES;
    }

    private static final String BASE_CONTENT_STRING_IMAGES = (Images.Media.EXTERNAL_CONTENT_URI).toString() + "/";

    @Override
    protected MediaDbItem getItemAtCursor(Cursor c) {
        long id = c.getLong(ImageDbItem.MEDIA_ID_INDEX);
        String dataPath = c.getString(ImageDbItem.MEDIA_DATA_INDEX);
        String displayName = c.getString(ImageDbItem.MEDIA_CAPTION_INDEX);

        final ImageDbItem item = new ImageDbItem(mContentResolver, id, getCurrentUri(id), dataPath, displayName);
        populateMediaItemSetFromCursor(item, c, BASE_CONTENT_STRING_IMAGES);

        return item;
    }

    private static void populateMediaItemSetFromCursor(
            final ImageDbItem item, final Cursor cursor, final String baseUri) {
        item.mMimeType = cursor.getString(ImageDbItem.MEDIA_MIME_TYPE_INDEX);
        item.mLatitude = cursor.getDouble(ImageDbItem.MEDIA_LATITUDE_INDEX);
        item.mLongitude = cursor.getDouble(ImageDbItem.MEDIA_LONGITUDE_INDEX);
        item.mDateTakenInMs = cursor.getLong(ImageDbItem.MEDIA_DATE_TAKEN_INDEX);
        item.mDateAddedInSec = cursor.getLong(ImageDbItem.MEDIA_DATE_ADDED_INDEX);
        item.mDateModifiedInSec = cursor.getLong(ImageDbItem.MEDIA_DATE_MODIFIED_INDEX);
        if (item.mDateTakenInMs == item.mDateModifiedInSec) {
            item.mDateTakenInMs = item.mDateModifiedInSec * 1000;
        }

        item.mRotation = cursor.getInt(ImageDbItem.MEDIA_ORIENTATION_OR_DURATION_INDEX);
        item.mStereoType = cursor.getInt(ImageDbItem.MEDIA_STEREO_TYPE_INDEX);
    }
}