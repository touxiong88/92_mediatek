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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.FontMetricsInt;
import android.graphics.RectF;
import android.graphics.Typeface;

import com.mediatek.media3d.R;
import com.mediatek.ngin3d.Container;
import com.mediatek.ngin3d.Image;
import com.mediatek.ngin3d.Point;
import com.mediatek.ngin3d.android.StageView;
import com.mediatek.ngin3d.presentation.BitmapGenerator;

/**
 *   A Custom actor class, to represent the video by display the thumbnail of video.
 *   only the overlay is created during construction. you have to set the thumbnail image
 *   via setThumbImage.
 */
public class VideoThumbnailActor extends Container {
    static final String TAG = "VTHUMBACTOR";

    private final Context mContext;
    private Image mImage;
    private long mTime;

    public static final float DEFAULT_THUMBNAIL_OFFSET_Z = 0.1f;
    public static final float FRAME_OFFSET_Z = -0.1f;

    // TODO: calculate size dynamically after the new image is decided
    public static final int TIME_OFFSET_X = 62;
    public static final int TIME_OFFSET_Y = 37;
    public static final float TIME_OFFSET_Z = -0.2f;

    // paint, metric texture
    private Paint mPaint;

    private static final int TEXT_COLOR = Color.WHITE;
    private static final int TEXT_BACKGROUND_COLOR = Color.BLACK;
    private static final int TEXT_PADDING_X = 4;
    private static final int TEXT_PADDING_Y = 2;
    private static final int RECT_MARGIN = 6;
    private static final int FONT_SIZE = 16;

    public VideoThumbnailActor(Context c, long duration) {
        mTime = duration;
        mContext = c;

        Image frame = Image.createFromResource(c.getResources(), R.drawable.icon_video_frame);
        frame.setPosition(new Point(0, 0, FRAME_OFFSET_Z));
        frame.setReactive(false);
        this.add(frame);

        Image defaultThumbnail = Image.createFromResource(c.getResources(), R.drawable.video_frame_background);
        defaultThumbnail.setPosition(new Point(0, 0, DEFAULT_THUMBNAIL_OFFSET_Z));
        defaultThumbnail.setReactive(false);
        this.add(defaultThumbnail);
    }

    private Paint getPaint() {
        if (mPaint == null) {
            Paint p = new Paint();
            p.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
            p.setTextSize(StageView.dpToPixel(mContext, FONT_SIZE));
            p.setAntiAlias(true);
            mPaint = p;
        }
        return mPaint;
    }

    public static Bitmap drawDuration(Bitmap bmp, long duration, Paint p) {

        if (null == bmp) {
            return null;
        }

        Canvas c;
        if (bmp.isMutable()) {
            c = new Canvas(bmp);
        } else {
            Bitmap mutableBmp = bmp.copy(bmp.getConfig(), true);
            c = new Canvas(mutableBmp);
        }

        int canvasWidth = bmp.getWidth();
        int canvasHeight = bmp.getHeight();

        FontMetricsInt fm = p.getFontMetricsInt();

        String timeText = getTimeString(duration);
        int stringWidth = (int)(.5f + p.measureText(timeText)) + TEXT_PADDING_X * 2;
        int stringHeight = fm.descent - fm.ascent + TEXT_PADDING_Y * 2;
        int rectLeft = canvasWidth - RECT_MARGIN - stringWidth;
        int rectTop = canvasHeight - RECT_MARGIN - stringHeight;
        RectF r = new RectF((float) rectLeft,
                            (float) rectTop,
                            canvasWidth - RECT_MARGIN,
                            canvasHeight - RECT_MARGIN);
        p.setColor(TEXT_BACKGROUND_COLOR);
        c.drawRoundRect(r, TEXT_PADDING_X, TEXT_PADDING_Y, p);

        p.setColor(TEXT_COLOR);
        c.drawText(timeText, rectLeft + TEXT_PADDING_X,
                   rectTop + (-fm.ascent) + TEXT_PADDING_Y, p);

        return bmp;
    }

    // draw duration text to bottom left corner of bitmap.
    public void drawDuration(Bitmap bmp, long duration) {
        if (bmp == null) {
            return;
        }
        mTime = duration;
        drawDuration(bmp, mTime, getPaint());
    }

    public void setThumbImage(Bitmap bmp) {
        if (bmp == null) {
            return;
        }
        drawDuration(bmp, mTime);
        if (mImage == null) {
            mImage = Image.createFromBitmap(bmp);
            mImage.setReactive(false);
            this.add(mImage);
        } else {
            mImage.setImageFromBitmap(bmp);
        }
    }

    public void setThumbImage(BitmapGenerator bmpGenerator) {
        if (bmpGenerator == null) {
            return;
        }
        // NOTE: the duration of thumbnail should be drawn in bmpGenerator
        if (mImage == null) {
            mImage = Image.createFromBitmapGenerator(bmpGenerator);
            mImage.setReactive(false);
            this.add(mImage);
        } else {
            mImage.setImageFromBitmapGenerator(bmpGenerator);
        }
    }

    private static final String TIME_NOT_AVAILABLE = "--:--";
    private static final String TIME_H_MM_SS = "%d:%02d:%02d";
    private static final String TIME_MM_SS = "%02d:%02d";

    private static String getTimeString(long duration) {
        if (duration <= 0) {
            return TIME_NOT_AVAILABLE;
        }

        long totalSecond = duration / 1000;
        long second = totalSecond % 60;
        long minute = (totalSecond % 3600) / 60;
        long hour = totalSecond / 3600;

        if (totalSecond == 0) {
            second++;  // less than 1 sec, but not zero.
        }

        if (hour == 0) {
            return String.format(TIME_MM_SS, minute, second);
        } else {
            return String.format(TIME_H_MM_SS, hour, minute, second);
        }
    }


    public static final int getFontSize() {
        return FONT_SIZE;
    }

}


