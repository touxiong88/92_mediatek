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
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.mediatek.internal.R;

/**
 * @hide
 */
public class ColorPickerDialog extends Dialog implements
        ColorPickerView.OnColorChangedListener,
        View.OnClickListener {

    private ColorPickerView mColorPicker;

    private ColorPickerPanelView mOldColor;
    private ColorPickerPanelView mNewColor;

    private OnColorChangedListener mListener;

    public interface OnColorChangedListener {
        public void onColorChanged(int color);
    }

    public ColorPickerDialog(Context context, int initialColor) {
        super(context);

        init(initialColor);
    }

    public void show() {
        super.show();
    }

    private void init(int color) {
        // To fight color banding.
        getWindow().setFormat(PixelFormat.RGBA_8888);

        setUp(color);
    }

    /// M: set old and new colors to the same color if color picker sets up for the first time @{
    private void setUp(int color) {
        setUp(color, color);
    }
    /// @}

    /// M: support setting both old and new colors after orientation changes
    private void setUp(int oldColor, int newColor) {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.color_picker_dialog, null);

        setContentView(layout);
        setTitle(R.string.dialog_color_picker);

        mColorPicker = (ColorPickerView) layout.findViewById(R.id.color_picker_view);
        mOldColor = (ColorPickerPanelView) layout.findViewById(R.id.old_color_panel);
        mNewColor = (ColorPickerPanelView) layout.findViewById(R.id.new_color_panel);

        ((LinearLayout) mOldColor.getParent()).setPadding(
            Math.round(mColorPicker.getDrawingOffset()),
            0,
            Math.round(mColorPicker.getDrawingOffset()),
            0
        );

        mOldColor.setOnClickListener(this);
        mNewColor.setOnClickListener(this);
        mColorPicker.setOnColorChangedListener(this);

        /// M: set saved colors to old, new color, and color picker @{
        mOldColor.setColor(oldColor);
        mNewColor.setColor(newColor);
        mColorPicker.setColor(newColor, true);
        /// @}
    }

    @Override
    public void onColorChanged(int color) {
        mNewColor.setColor(color);

        /*
        if (mListener != null) {
            mListener.onColorChanged(color);
        }
        */
    }

    public void setAlphaSliderVisible(boolean visible) {
        mColorPicker.setAlphaSliderVisible(visible);
    }

    /**
     * Set a OnColorChangedListener to get notified when the color
     * selected by the user has changed.
     * @param listener
     */
    public void setOnColorChangedListener(OnColorChangedListener listener){
        mListener = listener;
    }

    public int getColor() {
        return mColorPicker.getColor();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.new_color_panel) {
            if (mListener != null) {
                mListener.onColorChanged(mNewColor.getColor());
            }
        }
        dismiss();
    }

    @Override
    public Bundle onSaveInstanceState() {
        Bundle state = super.onSaveInstanceState();
        state.putInt("old_color", mOldColor.getColor());
        state.putInt("new_color", mNewColor.getColor());
        return state;
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mOldColor.setColor(savedInstanceState.getInt("old_color"));
        mColorPicker.setColor(savedInstanceState.getInt("new_color"), true);
    }

    /// M: callback to color picker when the device configuration changes @{
    public void onConfigurationChanged() {
        if (mOldColor != null && mNewColor != null)
            setUp(mOldColor.getColor(), mNewColor.getColor());
    }
    /// @}
}