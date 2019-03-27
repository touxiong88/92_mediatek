/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.mediatek.phone.plugin;

import android.app.Activity;

import android.content.res.Resources;

import android.graphics.Color;

import android.util.Config;
import android.util.FloatMath;
import android.util.Log;

import android.view.KeyEvent;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.android.internal.telephony.CallManager;

import com.mediatek.phone.ext.VTInCallScreenExtension;

public class VTInCallScreenOP01Extension extends VTInCallScreenExtension {

    private static final String LOG_TAG = "VTInCallScreenOP01Extension";

    private static final int NONEPOINT = 0;
    private static final int DRAGPOINT = 1; // 1 point
    private static final int ZOOMPOINT = 2; // 2 point

    private ViewGroup mVTInCallCanvas;
    private SurfaceView mVTHighVideo;
    private SurfaceView mVTLowVideo;
    private CompoundButton mVTMute;
    private CompoundButton mVTAudio;
    private CompoundButton mVTDialpad;
    private CompoundButton mVTSwapVideo;
    private ImageButton mVTOverflowMenu;
    private ViewGroup mCallBanner;
    private LinearLayout mVTHangUpWrapper;

    private Activity mInCallScreen;

    private int mMode = NONEPOINT;
    private float mOldDist;
    private float mChangeThreshold;

    public boolean initVTInCallScreen(ViewGroup vtInCallScreen, View.OnTouchListener touchListener,
                                      Activity inCallScreen) {

        mInCallScreen = inCallScreen;

        Resources resource = mInCallScreen.getResources();
        String packageName = mInCallScreen.getPackageName();
        mCallBanner = (ViewGroup) vtInCallScreen.findViewById(resource.getIdentifier("callBanner", "id", packageName));
        mVTMute = (CompoundButton) vtInCallScreen.findViewById(resource.getIdentifier("VTMute", "id", packageName));
        mVTHangUpWrapper = (LinearLayout) vtInCallScreen. 
                               findViewById(resource.getIdentifier("VTHangUpWrapper", "id", packageName));
        mVTDialpad = (CompoundButton) vtInCallScreen.findViewById(resource.getIdentifier("VTDialpad", "id", packageName));
        mVTAudio = (CompoundButton) vtInCallScreen.findViewById(resource.getIdentifier("VTSpeaker", "id", packageName));
        mVTOverflowMenu = (ImageButton) vtInCallScreen. 
                              findViewById(resource.getIdentifier("VTOverflowMenu", "id", packageName));
        mVTSwapVideo = (CompoundButton) vtInCallScreen. 
                           findViewById(resource.getIdentifier("VTSwapVideo", "id", packageName));
        mVTHighVideo = (SurfaceView) vtInCallScreen.findViewById(resource.getIdentifier("VTHighVideo", "id", packageName));
        mVTLowVideo = (SurfaceView) vtInCallScreen.findViewById(resource.getIdentifier("VTLowVideo", "id", packageName));
        mVTInCallCanvas = (ViewGroup) vtInCallScreen. 
                              findViewById(resource.getIdentifier("VTInCallCanvas", "id", packageName));

        if (null != mVTHighVideo) {
            mVTHighVideo.setOnTouchListener(touchListener);
        }
       
        if (null != mVTInCallCanvas) {
            mVTInCallCanvas.setOnTouchListener(touchListener);
        }

        mChangeThreshold = mInCallScreen.getResources().getDisplayMetrics().density * 20;
        return false;
    }

    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                log("MotionEvent.ACTION_DOWN");
                mMode = DRAGPOINT;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                log("MotionEvent.ACTION_UP");
                log("MotionEvent.ACTION_POINTER_UP");
                mMode = NONEPOINT;
                mOldDist = 0;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                mOldDist = spacing(event);
                mMode = ZOOMPOINT;
                log("MotionEvent.ACTION_POINTER_DOWN, mOldDist is" + mOldDist);
                break;
            case MotionEvent.ACTION_MOVE:
                log("MotionEvent.ACTION_MOVE, mode is " + mMode);
                if (mMode == ZOOMPOINT) {
                    // moving first point
                    float newDist = spacing(event);
                    log("MotionEvent.ACTION_MOVE, new dist is " + newDist + 
                            ", old dist is " + mOldDist + " threshold is " + mChangeThreshold);
                    if ((newDist - mOldDist > mChangeThreshold)
                            && (!VTInCallScreenFlagsOP01Extension.getInstance().getVTFullScreenFlag())) {
                        setVTDisplayScreenMode(true);
                        mMode = NONEPOINT;
                        mOldDist = 0;
                    } else if ((mOldDist - newDist > mChangeThreshold)
                            && (VTInCallScreenFlagsOP01Extension.getInstance().getVTFullScreenFlag())) {
                        setVTDisplayScreenMode(false);
                        mMode = NONEPOINT;
                    }
                }
                break;
            default:
                break;
            }
        return false;
    }

    /**
     * Compute two point distance
     */
    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return FloatMath.sqrt(x * x + y * y);
    }

    private void setVTDisplayScreenMode(final boolean isFullScreenMode) {
        log("setVTDisplayScreenMode(), isFullScreenMode is " + isFullScreenMode);
        if (isFullScreenMode) {
            VTInCallScreenFlagsOP01Extension.getInstance().setVTFullScreenFlag(true);
            if (null != mCallBanner) {
                mCallBanner.setVisibility(View.INVISIBLE);
            }
            if (null != mVTMute) {
                mVTMute.setVisibility(View.INVISIBLE);
            }
            if (null != mVTHangUpWrapper) {
                mVTHangUpWrapper.setVisibility(View.INVISIBLE);
            }
            if (null != mVTDialpad) {
                mVTDialpad.setVisibility(View.INVISIBLE);
            }
            if (null != mVTAudio) {
                mVTAudio.setVisibility(View.INVISIBLE);
            }
            if (null != mVTOverflowMenu) {
                mVTOverflowMenu.setVisibility(View.INVISIBLE);
            }
            if (null != mVTSwapVideo) {
                mVTSwapVideo.setVisibility(View.INVISIBLE);
            }
            if (null != mVTLowVideo) {
                mVTLowVideo.setBackgroundColor(Color.BLACK);
            }
        } else {
            VTInCallScreenFlagsOP01Extension.getInstance().setVTFullScreenFlag(false);
            if (null != mCallBanner) {
                mCallBanner.setVisibility(View.VISIBLE);
            }
            if (null != mVTMute) {
                mVTMute.setVisibility(View.VISIBLE);
            }
            if (null != mVTHangUpWrapper) {
                mVTHangUpWrapper.setVisibility(View.VISIBLE);
            }
            if (null != mVTDialpad) {
                mVTDialpad.setVisibility(View.VISIBLE);
            }
            if (null != mVTAudio) {
                mVTAudio.setVisibility(View.VISIBLE);
            }
            if (ViewConfiguration.get(mInCallScreen).hasPermanentMenuKey()) {
                if (null != mVTSwapVideo) {
                    mVTSwapVideo.setVisibility(View.VISIBLE);
                }
            } else {
                if (null != mVTOverflowMenu) {
                    mVTOverflowMenu.setVisibility(View.VISIBLE);
                }
            }
            if (null != mVTLowVideo) {
                mVTLowVideo.setBackgroundDrawable(null);
            }
        }
    }

    public boolean internalAnswerVTCallPre() {
        setVTDisplayScreenMode(false);
        return false;
    }

    public boolean initDialingSuccessVTState() {
        setVTDisplayScreenMode(false);
        return false;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (VTInCallScreenFlagsOP01Extension.getInstance().getVTFullScreenFlag()) {
            setVTDisplayScreenMode(false);
            return false;
        }
        return false;
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        if (VTInCallScreenFlagsOP01Extension.getInstance().getVTFullScreenFlag()) {
            return true;
        }
        return false;
    }

    public boolean onReceiveVTManagerStartCounter(CallManager cm) {
        if (VTInCallScreenFlagsOP01Extension.getInstance().getCallStartDate() < 0) {
            if (null != cm.getActiveFgCall()) {
                if (cm.getActiveFgCall().getLatestConnection() != null) {
                    VTInCallScreenFlagsOP01Extension.getInstance().setCallStartDate(System.currentTimeMillis());
                }
            }
        }
        return false;
    }

    private static void log(String msg) {
        if (Config.LOGD) {
            Log.d(LOG_TAG, msg);
        }
    }
}
