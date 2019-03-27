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

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;

public abstract class MediaDbItemSet implements MediaItemSet {

    protected Cursor mCursor;
    protected ContentResolver mContentResolver;
    protected ContentObserver mContentObserver;
    protected Uri mContentUri;
    protected final String mBucketId;

    protected MediaDbItemSet(ContentResolver cr, Uri contentUri, String bucketId) {
        mContentResolver = cr;
        mContentUri = contentUri;
        mBucketId = bucketId;
    }

    protected abstract MediaDbItem getItemAtCursor(Cursor c);

    // put selection string here. e.g. bucket id
    @SuppressWarnings("PMD.EmptyMethodInAbstractClassShouldBeAbstract")
    protected String selection() {
        return null;
    }

    @SuppressWarnings({"PMD.ReturnEmptyArrayRatherThanNull", "PMD.EmptyMethodInAbstractClassShouldBeAbstract"})
    protected String[] selectionArgs() {
        return null;
    }

    // put sort order here. e.g. by date
    @SuppressWarnings("PMD.EmptyMethodInAbstractClassShouldBeAbstract")
    protected String sortOrder() {
        return null;
    }

    // put projection here. the columns
    @SuppressWarnings({"PMD.ReturnEmptyArrayRatherThanNull", "PMD.EmptyMethodInAbstractClassShouldBeAbstract"})
    protected String[] projection() {
        return null;
    }

    private Cursor getCursor() {
        if (mCursor == null) {
            mCursor = query();
        }
        return mCursor;
    }

    public int getItemCount() {
        Cursor c = getCursor();
        if (c == null) {
            return 0;
        }

        synchronized (this) {
            return c.getCount();
        }
    }

    public boolean isEmpty() {
        return getItemCount() == 0;
    }

    public MediaDbItem getItem(int i) {
        Cursor c = getCursor();
        MediaDbItem ret = null;
        if (c != null) {
            ret = c.moveToPosition(i) ? getItemAtCursor(c) : null;
        }
        return ret;
    }

    protected Uri getCurrentUri(long id) {
        return ContentUris.withAppendedId(mContentUri, id);
    }

    public void registerObserver(ContentObserver observer) {
        synchronized (this) {
            if (observer != null) {
                Cursor cursor = getCursor();
                if (cursor != null) {
                    cursor.registerContentObserver(observer);
                    mContentObserver = observer;
                }
            }
        }
    }

    public void unregisterObserver() {
        synchronized (this) {
            if (mContentObserver != null) {
                Cursor cursor = getCursor();
                if (cursor != null) {
                    cursor.unregisterContentObserver(mContentObserver);
                    mContentObserver = null;
                }
            }
        }
    }

    public Cursor query() {
       return mContentResolver.query(
               mContentUri, projection(), selection(), selectionArgs(), sortOrder());
    }

    /**
     * close the list before discard it.
     */
    public void close() {
        unregisterObserver();
        if (mCursor != null) {
            mCursor.close();
            mCursor = null;
        }
    }

    /**
     * Dump the media item set.
     *
     * @return dump string
     */
    public String toString() {
        if (mCursor == null || isEmpty()) {
            return "empty Cursor";
        } else {
            StringBuilder sb = new StringBuilder();
            final int size = mCursor.getCount();
            mCursor.moveToFirst();
            for (int i = 0; i < size; i++) {
                sb.append(String.format("%s\n", getItemAtCursor(mCursor).toString()));
                mCursor.moveToNext();
            }
            return sb.toString();
        }
    }
}
