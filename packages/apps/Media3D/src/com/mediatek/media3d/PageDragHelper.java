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

/**
 *  Help to handle and switch drag status: initial, start, dragging and finish.
 *  initial: prepare for dragging
 *  start: start dragging
 *  dragging: in the dragging
 *  finish: finish dragging
 */
public class PageDragHelper {
    private int mDragX;
    private int mDragY;
    private Direction mDrag2Fling = Direction.NONE;
    private static final int ON_FLING_THRESHOLD = 20;
    private static final int ON_DRAG_THRESHOLD = 2;

    public enum State {
        INITIAL, START, DRAGGING, FINISH
    }
    private State mState = State.INITIAL;

    public enum Direction {
        NONE, LEFT, RIGHT, DOWN, UP
    }

    public PageDragHelper() {
        // Do nothing now
    }

    public Direction handleActionUp(Page page, Media3DView m3d) {
        Direction dir = mDrag2Fling;
        updateState();
        if (mState == State.START || mState == State.DRAGGING) {
            setFinish();
            page.onDrag(State.FINISH, 0);
            m3d.onDrag(State.FINISH, 0);
        }
        mDrag2Fling = Direction.NONE;
        mDragX = 0;
        mDragY = 0;

        return dir;
    }

    public boolean onScroll(float disX, float disY, Page page, Media3DView m3d) {
        mDragX += disX;
        mDragY += disY;
        if (Math.abs(mDragY) > Math.abs(mDragX) && Math.abs(mDragY) > ON_DRAG_THRESHOLD) {
            updateState();
            page.onDrag(mState, mDragY);
            if (mDragY > 0) {
                mDrag2Fling = Direction.UP;
            } else {
                mDrag2Fling = Direction.DOWN;
            }
        } else if (Math.abs(mDragX) > ON_FLING_THRESHOLD) {
            updateState();
            m3d.onDrag(mState, mDragX);
            if (mDragX > 0) {
                mDrag2Fling = Direction.LEFT;
            } else if (mDragX < 0) {
                mDrag2Fling = Direction.RIGHT;
            }
        }

        return true;
    }

    private void updateState() {
        if (mState == State.FINISH) {
            mState = State.INITIAL;
        } else if (mState == State.INITIAL) {
            mState = State.START;
        } else {
            mState = State.DRAGGING;
        }
    }

    public void setFinish() {
        mState = State.FINISH;
    }
}
