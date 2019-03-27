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

import android.app.Activity;
import android.app.Instrumentation;
import android.graphics.Point;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.view.MotionEvent;

import java.util.ArrayList;
import junit.framework.Assert;

/**
 * This is a simple common testing function set.
 */

public class CommonTestUtil {
    public static final int DEFAULT_PAGE_SWITCH_TIMEOUT_IN_MS = 10000;
    public static final int DEFAULT_MENU_BAR_TIMEOUT_IN_MS = 3000;
    public static final int DEFAULT_ONE_TESTING_CASE_TIMEOUT_IN_MS = 10000;
    public static final int WAIT_FOR_ANIM_FINISHED_TIME = 1000;
    public static final int WAIT_FOR_STATE_IDLE_TIME_IN_MS = 1000;
    public static final int WAIT_FOR_MENU_BAR_TIME_IN_MS = 200;
    public static final int WAIT_FOR_INNER_PAGE_SWITCH_TIME_IN_SECS = 1;
    public static final int ACTION_SHORT_WAITING_TIME_IN_SECS = 2;
    public static final int WAIT_FOR_MENU_BAR_TIME_IN_SECS = 1;
    public static final int WAIT_FOR_PAGE_SWITCH_TIME_IN_SECS = 8;
    public static final Point WEATHER_ICON_IN_PORTALPAGE = new Point(183, 237);
    public static final Point PHOTO_ICON_IN_PORTALPAGE = new Point(385, 287);
    public static final Point VIDEO_ICON_IN_PORTALPAGE = new Point(586, 247);
    public static final Point FIRST_ITEM_IN_PHOTOPAGE = new Point(178, 298);
    public static final Point FIRST_ITEM_IN_VIDEOPAGE = new Point(178, 298);
    public static final Point NON_ACTOR_POINT = new Point(99, 117);
    public static final Point SLIDESHOW_ICON_IN_TOP_MENU = new Point(654, 50);
    public static final Point SETTING_ICON_IN_TOP_MENU = new Point(654, 50);
    public static final Point FOLDER_ICON_IN_TOP_MENU = new Point(774, 32);
    public static final Point REFRESH_ICON_IN_TOP_MENU = new Point(774, 32);
    public static final Point BACK_ICON_IN_TOP_MENU = new Point(20, 20);
    public static final Point WEATHER_ICON_IN_NAVI_BAR = new Point(310, 439);
    public static final Point PHOTO_ICON_IN_NAVI_BAR = new Point(400, 439);
    public static final Point VIDEO_ICON_IN_NAVI_BAR = new Point(500, 439);
    public static final Point BASE = new Point(800, 480);
    public static final Point CENTER = new Point(400, 240);
    private static final DisplayMetrics sDm = new DisplayMetrics();

    public static MotionEvent genEvent(Activity activity, Point pt, int action) {
        activity.getWindowManager().getDefaultDisplay().getMetrics(sDm);
        float x = ((float)pt.x / (float) BASE.x) * sDm.widthPixels;
        float y = ((float)pt.y / (float) BASE.y) * sDm.heightPixels;
        return MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), action, x, y, 0); // ignore pressure.
    }

    public static void singleTapView(Activity a, Media3DView v, Point pt) {
        v.onSingleTapConfirmed(genEvent(a, pt, MotionEvent.ACTION_DOWN));
    }

    public static void touchView(Activity a, Media3DView v, Point pt) {
        v.onTouchEvent(genEvent(a, pt, MotionEvent.ACTION_DOWN));
        v.onTouchEvent(genEvent(a, pt, MotionEvent.ACTION_UP));
    }

    private static final int MOVEMENT_POINTS_COUNT = 20;
    public static ArrayList<Point> genMovements(Point start, Point end) {
        ArrayList<Point> l = new ArrayList<Point>();
        float deltaX = Math.abs(end.x - start.x) / MOVEMENT_POINTS_COUNT;
        float delatY = Math.abs(end.y - start.y) / MOVEMENT_POINTS_COUNT;
        if(end.x < start.x) {
            deltaX = -deltaX;
        }
        if(end.y < start.y) {
            delatY = -delatY;
        }
        for(int i = 0; i<MOVEMENT_POINTS_COUNT; ++i) {
            l.add(new Point( (int)(start.x + i * deltaX), (int)(start.y + i * delatY)));
        }
        return l;
    }

    public static void sendScrollOnUiThread(Instrumentation inst, Activity a, Point start, Point end) {
        ArrayList<Point> list = genMovements(start, end);
        if(list.size() != MOVEMENT_POINTS_COUNT) return;

        Point p;
        for(int i=0; i<MOVEMENT_POINTS_COUNT; ++i) {
            p = list.get(i);
            if(i == 0){
                inst.sendPointerSync(genEvent(a, p, MotionEvent.ACTION_DOWN));
            } else if(i == (MOVEMENT_POINTS_COUNT - 1)) {
                inst.sendPointerSync(genEvent(a, p, MotionEvent.ACTION_UP));
            } else {
                inst.sendPointerSync(genEvent(a, p, MotionEvent.ACTION_MOVE));
            }
        }
    }

    public static void sendTouchEventsOnUiThread(final Activity a, final Media3DView v, final Point pt) {
        a.runOnUiThread(new Runnable() {
            public void run() {
                touchView(a, v, pt);
            }
        });
    }

    public static void sendSingleTapConfirmedEventOnUiThread(final Activity a, final Media3DView v, final Point pt) {
        a.runOnUiThread(new Runnable() {
            public void run() {
                singleTapView(a, v, pt);
            }
        });
    }

    public static void callOnPauseOnUiThread(final Activity a, final Instrumentation instr) {
        a.runOnUiThread(new Runnable() {
            public void run() {
                instr.callActivityOnPause(a);
            }
        });
    }

    public static void callOnResumeOnUiThread(final Activity a, final Instrumentation instr) {
        a.runOnUiThread(new Runnable() {
            public void run() {
                instr.callActivityOnResume(a);
            }
        });
    }

    public static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            // just end it.
        }
    }

    public static void wait(Instrumentation inst, int times) {
        inst.waitForIdleSync();
        sleep(WAIT_FOR_ANIM_FINISHED_TIME * times);
    }

    public static boolean isTimeOut(long startTime, long timeOutInMs) {
       long endTime = SystemClock.elapsedRealtime();
        return (endTime-startTime) > timeOutInMs;
    }

    public static void waitLoadForIdleSync(Instrumentation inst, Page expectedPage, long timeOutInMs) {
        long startTime = SystemClock.elapsedRealtime();
        inst.waitForIdleSync();
        while(!expectedPage.isLoaded()) {
            if (isTimeOut(startTime, timeOutInMs)) {
                Assert.fail("waitPageForIdleSync is timing out for (ms) :" + timeOutInMs);
                break;
            }
            sleep(WAIT_FOR_STATE_IDLE_TIME_IN_MS);
        }
    }

    public static void waitPageForIdleSync(Instrumentation inst, Media3DView view, Page expectedPage, long timeOutInMs) {
        long startTime = SystemClock.elapsedRealtime();
        inst.waitForIdleSync();
        Page currentPage = view.getCurrentPage();
        while((currentPage != expectedPage) || (expectedPage.getState() != Page.IDLE) || (view.getBarState() != Media3DView.BAR_STATE_HIDDEN)) {
            if (isTimeOut(startTime, timeOutInMs)) {
                Assert.fail("waitPageForIdleSync is timing out for (ms) :" + timeOutInMs);
                break;
            }
            sleep(WAIT_FOR_STATE_IDLE_TIME_IN_MS);
            currentPage = view.getCurrentPage();
        }
    }

    public static void waitMenuBarForActionSync(Instrumentation inst, Media3DView view, long timeOutInMs) {
        long startTime = SystemClock.elapsedRealtime();
        inst.waitForIdleSync();
        while(view.getBarState()!= Media3DView.BAR_STATE_ENTERED) {
            if (isTimeOut(startTime, timeOutInMs)) {
                Assert.fail("waitMenuBarForActionSync is timing out for (ms) :" + timeOutInMs);
                break;
            }
            sleep(WAIT_FOR_MENU_BAR_TIME_IN_MS);
        }
    }

    public static void finishLaunchedActivity(Activity origActivity, int requestCode) {
        origActivity.finishActivity(requestCode);
    }

    // Dragging utility
    public enum DragDirection {
        RIGHT, LEFT, UP, DOWN
    }

    public static boolean isUp(DragDirection d) {
        return (d == DragDirection.UP);
    }

    public static boolean isDown(DragDirection d) {
        return (d == DragDirection.DOWN);
    }

    public static boolean isLeft(DragDirection d) {
        return (d == DragDirection.LEFT);
    }

    public static boolean isRight(DragDirection d) {
        return (d == DragDirection.RIGHT);
    }

    public static boolean isHorizontal(DragDirection d) {
        return (isLeft(d) || isRight(d));
    }

    public static boolean isVertical(DragDirection d) {
        return (isUp(d) || isDown(d));
    }
    public static void sendDragSync(Instrumentation inst, Main activity, DragDirection direction) {
        // calculate delta distance
        int dx = 0;
        int dy = 0;
        final int MOVE_DISTANCE = 200; // 150 pixel.
        if (isHorizontal(direction)) {
            dx = isRight(direction) ? MOVE_DISTANCE : -MOVE_DISTANCE;
        } else if (isVertical(direction)) {
            dy = isUp(direction) ? -MOVE_DISTANCE : MOVE_DISTANCE;
        }

        Point startPt = new Point(400, 240); // 800 /2, 480/2
        Point endPt = new Point(startPt.x + dx, startPt.y + dy);
        sendScrollOnUiThread(inst, activity, startPt, endPt);
        wait(inst, ACTION_SHORT_WAITING_TIME_IN_SECS);
    }
}