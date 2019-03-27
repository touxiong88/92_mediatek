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

public class ToolBar extends Bar {
    private static final String TAG = "ToolBar";

    private final Resources mResources;
    private final NavigationBarMenu mNaviMenu;
    private final HashMap<Integer, Button> mButtons = new HashMap<Integer, Button>();
    private int mHitActorTag;

    public static final int TAG_HOME = 1;
    public static final int TAG_MENU_ITEM_1 = 2;
    public static final int TAG_MENU_ITEM_2 = 3;
    public static final int TAG_HOME_PRESSED = 4;
    public static final int TAG_MENU_ITEM_1_PRESSED = 5;
    public static final int TAG_MENU_ITEM_2_PRESSED = 6;
    public static final int TAG_STEREO3D = 7;
    public static final int TAG_STEREO3D_PRESSED = 8;
    public static final int TAG_NORMAL2D = 9;
    public static final int TAG_NORMAL2D_PRESSED = 10;

    public interface Listener {
        void onButtonPressed(int buttonTag);
        void onMenuItemSelected(NavigationBarMenuItem item);
        void onTimerStopped();
        void onTimerStarted();
    }

    private final Listener mListener;

    static class Button {
        private final int mDefaultState;
        private final int mPressedTag;
        private final float mXPosition;
        private static final int PRESSED_ICON = R.drawable.tool_bar_button_pressed;

        Button(int defaultState, int pressedTag, float xPosition) {
            mDefaultState = defaultState;
            mPressedTag = pressedTag;
            mXPosition = xPosition;
        }

        int getDefaultIcon() {
            return mDefaultState;
        }

        int getPressedTag() {
            return mPressedTag;
        }

        int getPressedIcon() {
            return PRESSED_ICON;
        }

        float getXPosition() {
            return mXPosition;
        }
    }

    public ToolBar(Resources resources, Listener listener) {
        mResources = resources;
        mListener = listener;

        mNaviMenu = new NavigationBarMenu();

        int zBase = -20;
        Image mBackground = Image.createFromResource(resources, R.drawable.top_bar_background);
        mBackground.setPosition(new Point(0, 0, zBase));
        mBackground.setColor(new Color(128, 128, 128, 128));
        add(mBackground);

        mHeight = mBackground.getSize().height;
    }

    private float mHeight;
    public float getHeight() {
        return mHeight;
    }

    public void adjustLayout(int width) {
        if (mButtons.isEmpty()) {
            int zBase = -20;
            float iconWidth = BitmapUtil.getImageWidth(mResources, R.drawable.top_menu);
            // Icon size is 99 pixel
            mButtons.put(TAG_HOME,
                    new Button(R.drawable.top_menu, TAG_HOME_PRESSED,  0.5f * (iconWidth / width) - 0.5f));
            mButtons.put(TAG_STEREO3D,
                    new Button(R.drawable.top_stereo3d, TAG_STEREO3D_PRESSED, 1.5f * (iconWidth / width) - 0.5f));
            mButtons.put(TAG_NORMAL2D,
                    new Button(R.drawable.top_normal2d, TAG_NORMAL2D_PRESSED, 1.5f * (iconWidth / width) - 0.5f));
            mButtons.put(TAG_MENU_ITEM_1,
                    new Button(R.drawable.top_menu, TAG_MENU_ITEM_1_PRESSED, -0.5f * (iconWidth / width) + 0.5f));
            mButtons.put(TAG_MENU_ITEM_2,
                    new Button(R.drawable.top_menu, TAG_MENU_ITEM_2_PRESSED, -1.5f * (iconWidth / width) + 0.5f));

            for (int key : mButtons.keySet()) {
                if ((key == TAG_STEREO3D || key == TAG_NORMAL2D) && !Stereo3DWrapper.isStereo3DSupported()) {
                    continue;
                }

                Button btnActor = mButtons.get(key);

                Image pressedActor = Image.createFromResource(mResources, btnActor.getPressedIcon());
                pressedActor.setPosition(new Point(btnActor.getXPosition(), 0, -0.01f + zBase, true));
                pressedActor.setReactive(false);
                pressedActor.setVisible(false);
                pressedActor.setTag(btnActor.getPressedTag());
                add(pressedActor);

                Image defaultActor = Image.createFromResource(mResources, btnActor.getDefaultIcon());
                defaultActor.setPosition(new Point(btnActor.getXPosition(), 0, -0.02f + zBase, true));
                defaultActor.setTag(key);
                add(defaultActor);

                if (key == TAG_NORMAL2D) {
                    defaultActor.setVisible(false);
                }
            }
        }
    }

    public NavigationBarMenu getNavigationMenu() {
        return mNaviMenu;
    }

    public void fillMenuIcon() {
        Image menu1 = (Image) (findChildByTag(TAG_MENU_ITEM_1));
        Image menu2 = (Image) (findChildByTag(TAG_MENU_ITEM_2));
        if (menu1 == null || menu2 == null) {
            return;
        }

        menu1.setReactive(false);
        menu1.setVisible(false);
        menu2.setReactive(false);
        menu2.setVisible(false);

        if (mNaviMenu.getItemCount() > 0 && mNaviMenu.getItem(0) != null) {
            menu1.setImageFromResource(mResources, mNaviMenu.getItem(0).getIconId());
            menu1.setReactive(true);
            menu1.setVisible(true);
        }
        if (mNaviMenu.getItemCount() > 1 && mNaviMenu.getItem(1) != null) {
            menu2.setImageFromResource(mResources, mNaviMenu.getItem(1).getIconId());
            menu2.setReactive(true);
            menu2.setVisible(true);
        }
    }

    private void onHitAction(int action, int hittingTag) {
        if (Media3D.DEBUG) {
            Log.v(TAG, "action = " + action + " , hittingTag = " + hittingTag);
        }

        if (action == MotionEvent.ACTION_DOWN) {
            if (mHitActorTag == 0 && mButtons.get(hittingTag) != null) {
                Actor icon = findChildByTag(mButtons.get(hittingTag).getPressedTag());
                if (icon != null) {
                    icon.setVisible(true);
                }
                mHitActorTag = hittingTag;
            }
            mListener.onTimerStopped();
        } else if (action == MotionEvent.ACTION_MOVE) {
            if (mHitActorTag != 0 && mHitActorTag != hittingTag &&
                    mButtons.get(mHitActorTag) != null) {
                Actor icon = findChildByTag(mButtons.get(mHitActorTag).getPressedTag());
                if (icon != null) {
                    icon.setVisible(false);
                }
            }
        } else {
            if (mButtons.get(hittingTag) != null) {
                Actor icon = findChildByTag(mButtons.get(hittingTag).getPressedTag());
                if (icon != null) {
                    icon.setVisible(false);
                }
            }
            if (mHitActorTag == hittingTag) {
                if (mHitActorTag == TAG_HOME ||
                    mHitActorTag == TAG_STEREO3D ||
                    mHitActorTag == TAG_NORMAL2D) {
                    mListener.onButtonPressed(mHitActorTag);
                } else if (mHitActorTag == TAG_MENU_ITEM_1) {
                    mListener.onMenuItemSelected(mNaviMenu.getItem(0));
                } else if (mHitActorTag == TAG_MENU_ITEM_2) {
                    mListener.onMenuItemSelected(mNaviMenu.getItem(1));
                }
                if (Media3D.DEBUG) {
                    Log.v(TAG, "onHitAction, " + hittingTag);
                }
            }
            mListener.onTimerStarted();
            mHitActorTag = 0;
        }
    }

    @Override
    protected boolean onHit(Actor hit, int action) {
        if (hit.getTag() == TAG_HOME ||
            hit.getTag() == TAG_STEREO3D || hit.getTag() == TAG_NORMAL2D ||
            hit.getTag() == TAG_MENU_ITEM_1 || hit.getTag() == TAG_MENU_ITEM_2) {
            onHitAction(action, hit.getTag());
            return true;
        } else {
            if (mHitActorTag != 0 && mButtons.get(mHitActorTag) != null) {
                Actor icon = findChildByTag(mButtons.get(mHitActorTag).getPressedTag());
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
        if (Media3D.DEBUG) {
            Log.v(TAG, "onHitNothing");
        }

        if (mHitActorTag != 0 && mButtons.get(mHitActorTag) != null) {
            Actor icon = findChildByTag(mButtons.get(mHitActorTag).getPressedTag());
            if (icon != null) {
                icon.setVisible(false);
            }
            mListener.onTimerStarted();
            mHitActorTag = 0;
        }
        return false;
    }

    public void show3DIcon(boolean is3D) {
        Actor icon3d = findChildByTag(TAG_STEREO3D);
        if (icon3d != null) {
            icon3d.setVisible(is3D);
        }
        Actor icon2d = findChildByTag(TAG_NORMAL2D);
        if (icon2d != null) {
            icon2d.setVisible(!is3D);
        }
    }
}
