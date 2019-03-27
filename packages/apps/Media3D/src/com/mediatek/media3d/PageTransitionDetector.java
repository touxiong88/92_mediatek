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

import android.util.Log;

/**
 *  Handle the up and down fling, tells page when should scroll to next, previous, or reverse
 *  only handles up / down fling, does not handle horizontal flings (left and right).
 */
public class PageTransitionDetector {

    private static final String TAG = "PTD";

    public enum Action {
        NONE, UP, DOWN, REVERSE, ACCELERATE
    }

    public enum Direction {
        NONE, UP, DOWN
    }

    private final State mState = new State();
    private Direction mCurrentDirection = Direction.NONE;

    public PageTransitionDetector(ActionListener lis) {
        mListener = lis;
    };

    /**
     *   Reset the detector to idle state.
     */
    public void reset() {
        mState.set(State.IDLE);
        mCurrentDirection = Direction.NONE;
    }


    private boolean doReverse() {
        if (doAction(Action.REVERSE, false)) {
            mState.set(State.REVERSE);
            return true;
        }
        return false;
    }

    private boolean sameDirection(Direction direction) {
        return getDirection() == direction;
    }

    private boolean handleFling(Direction direction) {
        boolean result = false;
        switch (mState.get()) {
        case State.IDLE:
            result = doAction(getAction(direction), false);
            if (result) {
                setDirection(direction);
                mState.set(State.FLING);
            }
            break;

        case State.FLING:
            // reversing or double flinging
            if (sameDirection(direction)) {
                result = true;      // acc fling may be ignored, so the result is always true.
                doAction(Action.ACCELERATE, true);
                mState.set(State.ACC_FLING);
            } else {
                result = doReverse();
            }
            break;

        case State.ACC_FLING:
            if (sameDirection(direction)) {
                result = true;
            } else {
                result = doReverse();
            }
            break;

        case State.REVERSE:
            if (sameDirection(direction)) {
                result = doAction(Action.REVERSE, false);
                if (result) {
                    mState.set(State.FLING);
                }
            } else {
                setDirection(direction);
                doAction(Action.ACCELERATE, true);
                mState.set(State.ACC_REVERSE);
                result = true;
            }
            break;

        case State.ACC_REVERSE:
            if (sameDirection(direction)) {
                result = true;
            } else {
                setDirection(direction);
                result = doAction(Action.REVERSE, false);
                if (result) {
                    mState.set(State.FLING);
                }
            }
            break;

        default:
            // ignore
            break;
        }

        return result;
    }

    /**
     *  For Page to pass fling event to PageTransitionDetector
     *
     *  @param  the fling type defined in {@link <FlingEvent>}
     *  @return true if the fling is handled, otherwise false
     */
    public boolean onFling(int type) {
        if (type == FlingEvent.UP || type == FlingEvent.DOWN) {
            Log.v(TAG, "onFling(): " + type);
            return handleFling(type == FlingEvent.UP ? Direction.UP : Direction.DOWN);
        }

        return false;
    }

    /**
     *  For page to tell PageTransitionDetector that an transition animation is done.
     *  The PageTransitionDetector will see if another action is required. If yes,
     *  another {@link #doAction} will be invoked.
     *
     */
    public boolean onAnimationFinish() {
        boolean result = false;
        Log.v(TAG, "onAnimationFinish(): " + mState);
        switch (mState.get()) {
        case State.FLING:
            // fall through
        case State.REVERSE:
            result = true;
            setDirection(Direction.NONE);
            mState.set(State.IDLE);
            break;

        case State.ACC_REVERSE:
            // fall through
        case State.ACC_FLING:
            Action action = getAction();
            if (action != Action.NONE) {
                result = doAction(action, true);
                if (result) {
                    mState.set(State.FLING);
                } else {
                    // fling failed, means edge reached, reset to idle.
                    mState.set(State.IDLE);
                }
            }
            break;

        case State.IDLE:
            // fall through
        default:
            // ignore
            break;
        }

        return result;
    }

    /**
     *  Query current direction, generally it is the current on-going direction. If it is
     *  in REVERSE_FLING state, it will return the NEXT direction after reverse is done.
     *
     *  @return direction
     */
    private Direction getDirection() {
        return mCurrentDirection;
    }


    private void setDirection(Direction direction) {
        Log.v(TAG, "setDirection(): "  + direction);
        mCurrentDirection = direction;
    }

    private Action getAction() {
        return getAction(mCurrentDirection);
    }

    private Action getAction(Direction direction) {
        if (direction == Direction.DOWN) {
            return Action.DOWN;
        } else if (direction == Direction.UP) {
            return Action.UP;
        }

        return Action.NONE;
    }

    private boolean doAction(Action action, boolean accelerated) {
        Log.v(TAG, "doAction() " + action + " , accelerated:" + accelerated);
        boolean result = false;

        if (mListener != null) {
            result = mListener.onAction(action, accelerated);
        }
        return result;
    }


    public interface ActionListener  {
        boolean onAction(Action action, boolean accelerated);
    }

    private ActionListener mListener;

    private static final String [] STATE_STRINGS =
    { "Idle", "Flinging", "Accelearted Flinging", "Reversing", "Accelearted Rversing"};

    private static class State {
        public static final int IDLE = 0;
        public static final int FLING = 1;              // flinging
        public static final int ACC_FLING = 2;          // Accelerated fling
        public static final int REVERSE = 3;            // reversing
        public static final int ACC_REVERSE = 4;        // Accelerated reversing
        private static final int STATE_MAX = 5;

        private int mState = IDLE;

        public void set(int state) {
            mState = state;
            Log.v(TAG, "State.set(): "  + this.toString());
        }

        public String toString() {
            return STATE_STRINGS[mState];
        }

        public int get() {
            return mState;
        }
    }
}
