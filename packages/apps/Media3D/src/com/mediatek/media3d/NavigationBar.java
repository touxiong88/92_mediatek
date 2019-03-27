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
import android.util.Log;
import android.view.MotionEvent;
import com.mediatek.ngin3d.Actor;
import com.mediatek.ngin3d.Color;
import com.mediatek.ngin3d.Image;
import com.mediatek.ngin3d.Point;

import java.util.HashMap;

public class NavigationBar extends Bar {
    private static final String TAG = "NavigationBar";

    private final Listener mListener;
    private int mHitActorTag;
    private final HashMap<Integer, Button> mButtons = new HashMap<Integer, Button>();

    public interface Listener {
        void onPageSelected(String page);

        void onTimerStopped();

        void onTimerStarted();
    }

    static class Button {
        private final int mDefaultState;
        private final int mPressedState;
        private final String mPageName;

        Button(int defaultState, int pressedState, String pageName) {
            mDefaultState = defaultState;
            mPressedState = pressedState;
            mPageName = pageName;
        }

        int getDefaultIcon() {
            return mDefaultState;
        }

        int getPressedIcon() {
            return mPressedState;
        }

        String getPageName() {
            return mPageName;
        }
    }

    private static final int TAG_WEATHER = 1;
    private static final int TAG_PHOTO = 2;
    private static final int TAG_VIDEO = 3;

    public NavigationBar(Resources resources, Listener listener) {
        int zBase = -20;
        mListener = listener;

        mButtons.put(TAG_WEATHER,
            new Button(R.drawable.bottom_tab_weather, R.drawable.bottom_tab_weather_pressed, PageHost.WEATHER));
        mButtons.put(TAG_PHOTO,
            new Button(R.drawable.bottom_tab_photo, R.drawable.bottom_tab_photo_pressed, PageHost.PHOTO));
        mButtons.put(TAG_VIDEO,
            new Button(R.drawable.bottom_tab_video, R.drawable.bottom_tab_video_pressed, PageHost.VIDEO));

        Image mBackground = Image.createFromResource(resources, R.drawable.bottom_bar_background);
        mBackground.setPosition(new Point(0, 0, zBase));
        mBackground.setColor(new Color(64, 64, 64, 128));
        add(mBackground);
        mHeight = mBackground.getSize().height;

        float iconWidth = BitmapUtil.getImageWidth(resources, R.drawable.bottom_tab_weather);
        int xStartPos = -(int)iconWidth;
        int xPosInterval = (int)iconWidth;
        int index = 0;
        for (int key : mButtons.keySet()) {
            Button btnActor = mButtons.get(key);
            if (btnActor == null) {
                continue;
            }
            Image pressedActor = Image.createFromResource(resources, btnActor.getPressedIcon());
            pressedActor.setPosition(new Point((xStartPos + xPosInterval * index), 0, -0.01f + zBase));
            pressedActor.setTag(btnActor.getPressedIcon());
            pressedActor.setReactive(false);
            pressedActor.setVisible(false);
            add(pressedActor);

            Image defaultActor = Image.createFromResource(resources, btnActor.getDefaultIcon());
            defaultActor.setPosition(new Point((xStartPos + xPosInterval * index++), 0, -0.02f + zBase));
            defaultActor.setTag(key);
            add(defaultActor);
        }
    }

    private float mHeight;
    public float getHeight() {
        return mHeight;
    }

    private void onHitAction(int action, int hittingTag) {
        if (Media3D.DEBUG) {
            Log.v(TAG, "action = " + action + " , hittingTag = " + hittingTag);
        }

        if (action == MotionEvent.ACTION_DOWN) {
            if (mHitActorTag == 0 && mButtons.get(hittingTag) != null) {
                Actor icon = findChildByTag(mButtons.get(hittingTag).getPressedIcon());
                if (icon != null) {
                    icon.setVisible(true);
                }
                mHitActorTag = hittingTag;
            }
            mListener.onTimerStopped();
        } else if (action == MotionEvent.ACTION_MOVE) {
            if (mHitActorTag != 0 &&
                    mHitActorTag != hittingTag &&
                    mButtons.get(mHitActorTag) != null) {
                Actor icon = findChildByTag(mButtons.get(mHitActorTag).getPressedIcon());
                if (icon != null) {
                    icon.setVisible(false);
                }
            }
        } else {
            if (mButtons.get(hittingTag) != null) {
                Actor icon = findChildByTag(mButtons.get(hittingTag).getPressedIcon());
                if (icon != null) {
                    icon.setVisible(false);
                }
            }

            if (mHitActorTag == hittingTag && mButtons.get(hittingTag) != null) {
                String pageName = mButtons.get(hittingTag).getPageName();
                mListener.onPageSelected(pageName);
                if (Media3D.DEBUG) {
                    Log.v(TAG, "onTouchEvent, " + pageName);
                }
            }
            mListener.onTimerStarted();
            mHitActorTag = 0;
        }
    }

    @Override
    protected boolean onHit(Actor hit, int action) {
        if (hit.getTag() == TAG_WEATHER || hit.getTag() == TAG_PHOTO || hit.getTag() == TAG_VIDEO) {
            onHitAction(action, hit.getTag());
            return true;
        } else {
            if (mHitActorTag != 0 && mButtons.get(mHitActorTag) != null) {
                Actor icon = findChildByTag(mButtons.get(mHitActorTag).getPressedIcon());
                if (icon != null) {
                    icon.setVisible(false);
                }
                mListener.onTimerStarted();
                mHitActorTag = 0;
            }
            return false;
        }
    }

    @Override
    protected boolean onHitNothing() {
        Log.v(TAG, "onHitNothing");
        return false;
    }
}
