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

package com.mediatek.media3d.weather;

import android.content.Context;

import com.mediatek.media3d.FormatUtil;
import com.mediatek.media3d.LayoutManager;
import com.mediatek.media3d.LogUtil;
import com.mediatek.media3d.R;
import com.mediatek.ngin3d.Box;
import com.mediatek.ngin3d.Container;
import com.mediatek.ngin3d.Dimension;
import com.mediatek.ngin3d.Point;
import com.mediatek.ngin3d.Image;
import com.mediatek.ngin3d.Text;

import java.util.Calendar;
import java.util.TimeZone;

public class DigitClock extends Container {

    static final String TAG = "Media3D.DigitClock";

    private final Image mH1;
    private final Image mH2;
    private final Image mM1;
    private final Image mM2;
    private final Image mComma;
    private final Text mHighLowTemp;
    private final Text mDate;

    /* digit: 116x163 */
    static final int DIGIT_WIDTH = 105;
    static final int DIGIT_HEIGHT = 147;
    /* comma, ":" : 49x163 , it is the last position */
    static final int COMMA_WIDTH = 45;
    static final int COMMA_HEIGHT = 147;

    private final int[] mDigitOffset = new int[11];
    private Dimension mDigitDim;
    private Dimension mCommaDim;

    private LocationWeather mLocation;
    private int mHour = -1;
    private int mMinute = -1;

    public DigitClock(final String name, final Context c, final LocationWeather location, LayoutManager lm) {
        mLocation = location;

//        mDigitOffset = new int[11];
        mDigitDim = lm.getDimension("weather_clock_digit_size");
        int dx = 0;
        for (int i = 0; i < mDigitOffset.length; i++) {
            mDigitOffset[i] = dx;
            dx += mDigitDim.width;
        }

        mH1 = Image.createFromResource(c.getResources(), R.drawable.digi_numbers);
        mH2 = Image.createFromResource(c.getResources(), R.drawable.digi_numbers);
        mM1 = Image.createFromResource(c.getResources(), R.drawable.digi_numbers);
        mM2 = Image.createFromResource(c.getResources(), R.drawable.digi_numbers);
        mComma = Image.createFromResource(c.getResources(), R.drawable.digi_numbers);

        int subSize = lm.getInteger("weather_panel_sub_date_size");
        mHighLowTemp = mLocation.getTempHighLowText(c);
        FormatUtil.applyTextAttributes(mHighLowTemp, subSize, true);
        mDate = mLocation.getDateText();
        FormatUtil.applyTextAttributes(mDate, subSize, false);


        mH1.setSize(mDigitDim);
        mH2.setSize(mDigitDim);
        mM1.setSize(mDigitDim);
        mM2.setSize(mDigitDim);

        mCommaDim = lm.getDimension("weather_clock_comma_size");
        mComma.setSize(mCommaDim);

        mH1.setPosition(lm.getPoint("weather_clock_hour_d1_pos"));
        mH2.setPosition(lm.getPoint("weather_clock_hour_d2_pos"));
        mM1.setPosition(lm.getPoint("weather_clock_min_d1_pos"));
        mM2.setPosition(lm.getPoint("weather_clock_min_d2_pos"));
        mComma.setPosition(lm.getPoint("weather_clock_comma_pos"));
        mHighLowTemp.setPosition(lm.getPoint("weather_clock_high_low_temperature_pos"));
        mDate.setPosition(lm.getPoint("weather_clock_date_pos"));
        final Image panel = Image.createFromResource(c.getResources(), R.drawable.timepanel_blank);
        panel.setPosition(lm.getPoint("weather_clock_panel_pos"));
        Container root = new Container();
        root.setPosition(new Point(0.5025f, 0.21f, true));
        root.add(panel, mH1, mH2, mM1, mM2, mComma, mHighLowTemp, mDate);
        this.add(root);
    }

    private void setDigit(final Image digit, int n) {
        LogUtil.v(TAG, "digit :" + n);
        final int offset = (n >= 0) ? (n % 10) : (-n) % 10; /* only 0~9 */
        final int left = mDigitOffset[offset];
        digit.setSourceRect(new Box(left, 0, left + mDigitDim.width, mDigitDim.height));
    }

    public void update(final LocationWeather location, final Context context) {
        mLocation = location;
        final TimeZone timeZone = TimeZone.getTimeZone(mLocation.getTimeZone());
        final Calendar now = Calendar.getInstance(timeZone);
        final int hourNow = now.get(Calendar.HOUR_OF_DAY);
        final int minuteNow = now.get(Calendar.MINUTE);
        boolean updated = false;

        LogUtil.v(TAG, "At time :" + timeZone + ", Hour :" + hourNow + ", Minute :" + minuteNow);
        if (hourNow != mHour) {
            mHour = hourNow;
            setDigit(mH1, mHour / 10);
            setDigit(mH2, mHour % 10);
            updated = true;
        }
        if (minuteNow != mMinute) {
            mMinute = minuteNow;
            setDigit(mM1, mMinute / 10);
            setDigit(mM2, mMinute % 10);
            updated = true;
        }

        if (updated) {
            mComma.setSourceRect(new Box(mDigitOffset[10], 0, mDigitOffset[10] + mCommaDim.width, mCommaDim.height));
        }

        mHighLowTemp.setText(mLocation.getTempHighLowString(context));
        mDate.setText(mLocation.getDateString());
    }
}