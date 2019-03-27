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
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.              0
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.mediatek.media3d;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.util.DisplayMetrics;

import android.util.Log;
import com.mediatek.ngin3d.Dimension;
import com.mediatek.ngin3d.Point;
import com.mediatek.ngin3d.Scale;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Stack;
import java.util.Vector;

public final class LayoutManager {
    private static final String TAG = "LM";
    public LayoutManager() {}

    public static Dimension getScreenResolution(final Resources res) {
        DisplayMetrics dm = res.getDisplayMetrics();
        int width = (dm.widthPixels > dm.heightPixels) ? dm.widthPixels : dm.heightPixels;
        int height = (dm.widthPixels > dm.heightPixels) ? dm.heightPixels : dm.widthPixels;
        return new Dimension(width, height);
    }

    private final Stack<String> mPrefix = new Stack<String>();
    private final HashMap<Integer, HashMap<String, Object>> mMap = new HashMap<Integer, HashMap<String, Object>>();
    private final Vector<Integer> mKeys = new Vector<Integer>();
    private int mMatchKey;

    private DisplayMetrics mDM;
    public void getMatch(final Resources res) {
        mDM = res.getDisplayMetrics();
        getMatchInteger();
        Log.v(TAG, "match : " + mMatchKey );
    }

    public Integer getMatchInteger() {
        int match = 0;
        int distance = mDM.widthPixels;
        for(int i=0; i<mKeys.size();++i) {
            int dis = Math.abs(mDM.widthPixels - mKeys.get(i) );
            Log.v(TAG, "distance : " + dis  + ", i " + i + "keys : " +  mKeys.get(i));
            if (dis <= distance) {
                distance = dis;
            } else {
                int index = (i - 1) >= 0 ? (i - 1) : 0;
                mMatchKey = mKeys.get(index);
                Log.v(TAG, "match key : " + mMatchKey  + ", index: " + index);
                return mMatchKey;
            }
        }
        Log.v(TAG, "match key : " + mMatchKey  + ", key size : " + mKeys.size());
        return mMatchKey = mKeys.lastElement();
    }

    public Object get(final String name) {
        // Find most match.
        HashMap<String, Object> map = mMap.get(mMatchKey);
        Log.v(TAG, "get map: " + map + ", match : " + mMatchKey);
        Object obj = map.get(name);
        if (obj != null) {
            Log.v(TAG, "get : " + obj + ", match : " + mMatchKey);
            return obj;
        }

        // Search others
        for (int i = 0; i < mKeys.size(); ++i) {
            if (mMatchKey == mKeys.get(i))
                continue;
            obj = mMap.get(mKeys.get(i)).get(name);
            if (obj != null) {
                Log.v(TAG, "get : " + obj + ", match i: " + i + ", key : " + mKeys.get(i));
                return obj;
            }
        }

        return null;
    }

    public Scale getScale(final String name) {
        return (Scale)get(name);
    }

    public Point getPoint(final String name) {
        return (Point)get(name);
    }

    public Dimension getDimension(final String name) {
        return (Dimension)get(name);
    }

    public Integer getInteger(final String name) {
        return (Integer)get(name);
    }

    public Float getFloat(final String name) {
        return (Float)get(name);
    }

    public static LayoutManager realize(Context context, int resId) {
        XmlResourceParser parser = context.getResources().getXml(resId);
        if (parser == null)
            return null;

        LayoutManager lm = new LayoutManager();
        int xmlEventType;
        try {
            while ((xmlEventType = parser.next()) != XmlResourceParser.END_DOCUMENT) {
                switch (xmlEventType) {
                    case XmlPullParser.START_DOCUMENT:
                        LogUtil.v(TAG, "Start Document.");
                        break;
                    case XmlPullParser.START_TAG:
                        lm.processStartElement(parser);
                        break;
                    case XmlPullParser.END_TAG:
                        lm.processEndElement(parser);
                        break;
                    case XmlPullParser.TEXT:
                        break;
                    default:
                        break;
                }
            }
        } catch (XmlPullParserException e) {
            LogUtil.v(TAG, "Exception :" + e);
        } catch (IOException e) {
            LogUtil.v(TAG, "Exception :" + e);
        }

        lm.getMatch(context.getResources());
        return lm;
    }

    private void processStartElement(XmlPullParser parser) {
        String tag = parser.getName();
        if (tag.equalsIgnoreCase("resolution")) {
            processResolution(parser);
        } else if (tag.equalsIgnoreCase("point")) {
            processPoint(parser);
        } else if (tag.equalsIgnoreCase("integer")) {
            processInteger(parser);
        } else if (tag.equalsIgnoreCase("float")) {
            processFloat(parser);
        } else if (tag.equalsIgnoreCase("dimension")) {
            processDimension(parser);
        } else if (tag.equalsIgnoreCase("scale")) {
            processScale(parser);
        } else if (tag.equalsIgnoreCase("application")) {
            LogUtil.v(TAG, "Start Element : application");
        } else {
            LogUtil.v(TAG, "Start Element : unknown");
        }
    }

    private void processResolution(XmlPullParser parser) {
        String key = parser.getAttributeValue(null, "wvalue");
        mPrefix.push(key);
        mKeys.add(Integer.valueOf(key));
        HashMap<String, Object> map = mMap.get(Integer.valueOf(key));
        if (map == null) {
            map = new HashMap<String, Object>();
            mMap.put(Integer.valueOf(key), map);
        }
        Log.v(TAG, "processResolution pre : " + key);
    }

    private void processPoint(XmlPullParser parser) {
        String prefix = mPrefix.peek();
        Log.v(TAG, "processPoint pre : " + prefix);
        HashMap<String, Object> map = mMap.get(Integer.valueOf(prefix));
        if (map == null) {
            map = new HashMap<String, Object>();
            mMap.put(Integer.valueOf(prefix), map);
        }
        String name = parser.getAttributeValue(null, "name");
        Float x = Float.valueOf(parser.getAttributeValue(null, "x"));
        Float y = Float.valueOf(parser.getAttributeValue(null, "y"));
        Float z = Float.valueOf(parser.getAttributeValue(null, "z"));
        Boolean abs = Boolean.valueOf(parser.getAttributeValue(null, "abs"));
        Log.v(TAG, "point abs : " + parser.getAttributeValue(null, "abs"));
        Log.v(TAG, "point abs : " + Boolean.valueOf(parser.getAttributeValue(null, "abs")));
        Point point = new Point(x, y, z, abs);
        map.put(name, point);
        mPrefix.push("point");
        Log.v(TAG, "name : " + name + ", point : " + point);
    }

    private void processInteger(XmlPullParser parser) {
        String prefix = mPrefix.peek();
        Log.v(TAG, "processInteger pre : " + prefix);
        HashMap<String, Object> map = mMap.get(Integer.valueOf(prefix));
        if (map == null) {
            map = new HashMap<String, Object>();
            mMap.put(Integer.valueOf(prefix), map);
        }
        String name = parser.getAttributeValue(null, "name");
        Integer value = Integer.valueOf(parser.getAttributeValue(null, "value"));
        map.put(name, value);
        mPrefix.push("integer");
        Log.v(TAG, "name : " + name + ", integer : " + value);
    }

    private void processFloat(XmlPullParser parser) {
        String prefix = mPrefix.peek();
        Log.v(TAG, "processFloat pre : " + prefix);
        HashMap<String, Object> map = mMap.get(Integer.valueOf(prefix));
        if (map == null) {
            map = new HashMap<String, Object>();
            mMap.put(Integer.valueOf(prefix), map);
        }
        String name = parser.getAttributeValue(null, "name");
        Float value = Float.valueOf(parser.getAttributeValue(null, "value"));
        map.put(name, value);
        mPrefix.push("float");
        Log.v(TAG, "name : " + name + ", float : " + value);
    }

    private void processDimension(XmlPullParser parser) {
        String prefix = mPrefix.peek();
        Log.v(TAG, "processDimension pre : " + prefix);
        HashMap<String, Object> map = mMap.get(Integer.valueOf(prefix));
        if (map == null) {
            map = new HashMap<String, Object>();
            mMap.put(Integer.valueOf(prefix), map);
        }
        String name = parser.getAttributeValue(null, "name");
        Float width = Float.valueOf(parser.getAttributeValue(null, "width"));
        Float height = Float.valueOf(parser.getAttributeValue(null, "height"));
        Dimension dim = new Dimension(width, height);
        map.put(name, dim);
        mPrefix.push("dimension");
        Log.v(TAG, "name : " + name + ", dimension : " + dim);
    }

    private void processScale(XmlPullParser parser) {
        String prefix = mPrefix.peek();
        Log.v(TAG, "processScale pre : " + prefix);
        HashMap<String, Object> map = mMap.get(Integer.valueOf(prefix));
        if (map == null) {
            map = new HashMap<String, Object>();
            mMap.put(Integer.valueOf(prefix), map);
        }
        String name = parser.getAttributeValue(null, "name");
        Float x = Float.valueOf(parser.getAttributeValue(null, "x"));
        Float y = Float.valueOf(parser.getAttributeValue(null, "y"));
        Float z = Float.valueOf(parser.getAttributeValue(null, "z"));
        Scale scale = new Scale(x, y, z);
        map.put(name, scale);
        mPrefix.push("scale");
        Log.v(TAG, "name : " + name + ", scale : " + scale);
    }

    public void processEndElement(XmlPullParser parser) {
        String name = parser.getName();
        String uri = parser.getNamespace();
        if ("".equals(uri)) {
            LogUtil.v(TAG, "End element : " + name);
        } else {
            LogUtil.v(TAG, "End element:   {" + uri + "}" + name);
        }
        if (!mPrefix.empty()) {
            mPrefix.pop();
        }
    }
}