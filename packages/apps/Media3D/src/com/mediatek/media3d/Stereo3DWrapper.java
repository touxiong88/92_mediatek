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

import android.view.SurfaceView;
import android.view.WindowManager;

import com.mediatek.common.featureoption.FeatureOption;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class Stereo3DWrapper {
    private static final String TAG = "Media3D.Stereo3DWrapper";
    private static Method sSet3DLayout;
    private static Field sDisabled3D;
    private static Field sSideBySide3D;
    private static boolean sS3DFeatureOption;

    private Stereo3DWrapper() {};

    static {
        initCompatibility();
    }

    private static void initCompatibility() {
        try {
            sSet3DLayout = SurfaceView.class.getMethod(
                "set3DLayout", new Class[] { int.class });
            sDisabled3D = WindowManager.LayoutParams.class.getField("LAYOUT3D_DISABLED");
            sSideBySide3D = WindowManager.LayoutParams.class.getField("LAYOUT3D_SIDE_BY_SIDE");
            sS3DFeatureOption = FeatureOption.MTK_S3D_SUPPORT;
        } catch (NoSuchMethodException ex) {
            LogUtil.v(TAG, "exception : " + ex);
        } catch (NoSuchFieldException ex) {
            LogUtil.v(TAG, "exception : " + ex);
        }
    }

    public static final int INVALID_PARAM = -1;
    public static final int LAYOUT3D_DISABLED = 0;
    public static final int LAYOUT3D_SIDE_BY_SIDE = 1;

    private static int convert3DLayoutParam(int param) {
        try {
            if (param == LAYOUT3D_DISABLED && sDisabled3D != null) {
                return sDisabled3D.getInt(null);
            } else if (param == LAYOUT3D_SIDE_BY_SIDE && sSideBySide3D != null) {
                return sSideBySide3D.getInt(null);
            }
        } catch (IllegalAccessException ex) {
            LogUtil.v(TAG, "exception : " + ex);
        }
        return INVALID_PARAM;
    }

    public static void set3DLayout(SurfaceView view, int param) {
        try {
            if (sSet3DLayout != null) {
                int convertParam = convert3DLayoutParam(param);
                if (convertParam != INVALID_PARAM) {
                    sSet3DLayout.invoke(view, convertParam);
                }
            }
        } catch (InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            } else {
                throw new RuntimeException(ite);
            }
        } catch (IllegalAccessException ex) {
            LogUtil.v(TAG, "exception : " + ex);
        }
    }

    public static boolean isStereo3DSupported() {
        return sS3DFeatureOption && (sSet3DLayout != null) &&
            (sDisabled3D != null) && (sSideBySide3D != null);
    }
}
