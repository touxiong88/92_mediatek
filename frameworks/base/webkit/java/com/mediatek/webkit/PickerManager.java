/*
 * Copyright (C) 2010 Daniel Nilsson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mediatek.webkit;

import android.app.Dialog;
import android.content.Context;
import com.mediatek.common.webkit.IPicker;
import com.mediatek.common.webkit.IOnChangedListener;

/// M: import dialog related packages @{
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
/// @}

/**
 * @hide
 */
public class PickerManager implements IPicker {
    private String mPickerType = IPicker.COLOR_PICKER;
    private IOnChangedListener mListener;
    /// M: keep a color picker dialog object
    private ColorPickerDialog mDialog;
    /// M: define an invalid color value
    private int INVALID_COLOR = 0;

    public static PickerManager getInstance(String type) {
        if (isValid(type)) {
            return new PickerManager(type);
        }
        return null;
    }

    private static boolean isValid(String type) {
        return type.equals(IPicker.COLOR_PICKER) ||
               type.equals(IPicker.MONTH_PICKER) ||
               type.equals(IPicker.WEEK_PICKER);
    }

    public String getType() {
        return mPickerType;
    }

    public void setOnChangedListener(IOnChangedListener listener) {
        mListener = listener;
    }

    public void show(Context context, int initialValue1, int initialValue2, Object initialObj) {
        if (mPickerType.equals(IPicker.COLOR_PICKER)) {
            if (context != null) {
                /// M: create a new color picker dialog and save this object for later use
                mDialog = new ColorPickerDialog(context, initialValue1);
                if (mListener != null) {
                    mDialog.setOnColorChangedListener(new ColorChangedListener(mListener));

                    /// M: add onDismiss callback to notify native WebViewCore that color picker
                    /// is closed so that WebViewCore can release m_colorChooser object.
                    /// INVALID_COLOR = 0 is set because this value is not a valid value in the color picker
                    /// as it is defined in WebViewCore::ColorChooserReply @{
                    mDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        public void onDismiss(DialogInterface dialog) {
                            mListener.onChanged(IPicker.COLOR_PICKER, INVALID_COLOR, 0, null);
                        }
                    });
                    /// @}
                }
                mDialog.show();
            }
        }
    }

    /// M: notify the color picker when the device configuration changes @{
    public void onConfigurationChanged() {
        if (mDialog != null) {
            mDialog.onConfigurationChanged();
        }
    }
    /// @}

    // Only allow create via getInstance()
    private PickerManager(String type) {
        mPickerType = type;
    }

    class ColorChangedListener implements ColorPickerDialog.OnColorChangedListener {
        private IOnChangedListener mListener;
        public ColorChangedListener(IOnChangedListener listener) {
            mListener = listener;
        }

        public void onColorChanged(int color) {
            if (mListener != null) {
                // Only value1 is valid for color picker.
                mListener.onChanged(IPicker.COLOR_PICKER, color, 0, null);
            }
        }
    }
}
