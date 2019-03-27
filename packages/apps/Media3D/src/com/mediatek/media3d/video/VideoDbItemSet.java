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
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.MediaStore.Video.Media;

import com.mediatek.media3d.MediaDbItem;
import com.mediatek.media3d.MediaDbItemSet;

public class VideoDbItemSet extends MediaDbItemSet {
    // the URI of video
    private static final Uri CONTENT_URI = Media.EXTERNAL_CONTENT_URI;
    private static final String [] PROJECTION = new String [] {
        Media._ID,
        Media.DATA,
        Media.DURATION,
        Media.MINI_THUMB_MAGIC,
        Media.DISPLAY_NAME,
        MediaStore.Video.Media.STEREO_TYPE
    };
    private static final int INDEX_ID = 0;
    private static final int INDEX_DATA = 1;
    private static final int INDEX_DURATION = 2;
    private static final int INDEX_MIMI_THUMB_MAGIC = 3;
    private static final int INDEX_DISPLAY_NAME = 4;
    private static final int INDEX_STEREO_TYPE = 5;


    VideoDbItemSet(ContentResolver cr, String bucketId) {
        super(cr, CONTENT_URI, bucketId);
    }

    public MediaDbItem getItemAtCursor(Cursor c) {
        Long id = c.getLong(INDEX_ID);
        String dataPath = c.getString(INDEX_DATA);
        Long duration = c.getLong(INDEX_DURATION);
        String displayName = c.getString(INDEX_DISPLAY_NAME);
        int stereoType = c.getInt(INDEX_STEREO_TYPE);
        MediaDbItem mi  = (MediaDbItem) new VideoDbItem(mContentResolver, id, getCurrentUri(id),
                          dataPath, displayName, duration, stereoType);
        return mi;
    }

    @Override
    protected String selection() {
        if (mBucketId == null) {
            return null;
        } else if (mBucketId.equalsIgnoreCase("0")) {
            return "stereo_type != 0";
        } else {
            return Media.BUCKET_ID + " = '" + mBucketId + "'";
        }
    }

    // private static final String SORT_ASCENDING = " ASC";
    private static final String SORT_DESCENDING = " DESC";

    // TODO: sort by date.
    @Override
    protected String sortOrder() {
        return Media.DATE_TAKEN + SORT_DESCENDING;
    }

    @Override
    protected String [] projection() {
        return PROJECTION;
    }


    public boolean isEmpty() {
        return (getItemCount() == 0);
    }
}