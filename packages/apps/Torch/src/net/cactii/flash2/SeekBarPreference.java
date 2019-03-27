/* The following code was written by Matthew Wiggins
 * and is released under the APACHE 2.0 license
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package net.cactii.flash2;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class SeekBarPreference extends DialogPreference implements SeekBar.OnSeekBarChangeListener {

    private static final String androidns = "http://schemas.android.com/apk/res/android";

    private SeekBar mSeekBar;

    private TextView mSplashText, mValueText;

    private Context mContext;

    private String mDialogMessage, mSuffix;

    private int mDefault, mMax, mValue = 0;

    public SeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;

/* Vanzo:huangchaojun on: Sun, 09 Oct 2011 12:33:39 +0800
        mDialogMessage = attrs.getAttributeValue(androidns, "dialogMessage");
        mSuffix = attrs.getAttributeValue(androidns, "text");
 */
        mDialogMessage = context.getString(Integer.parseInt(attrs.getAttributeValue(androidns,
                "dialogMessage").replace("@", "")));
        mSuffix = context.getString(Integer.parseInt(attrs.getAttributeValue(androidns, "text")
                .replace("@", "")));
// End of Vanzo:huangchaojun
        mDefault = attrs.getAttributeIntValue(androidns, "defaultValue", 0);
        mMax = attrs.getAttributeIntValue(androidns, "max", 100);

    }

    @Override
    protected View onCreateDialogView() {
        LinearLayout.LayoutParams params;
        LinearLayout layout = new LinearLayout(mContext);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(6, 6, 6, 6);

        mSplashText = new TextView(mContext);
        if (mDialogMessage != null)
            mSplashText.setText(mDialogMessage);
        layout.addView(mSplashText);

        mValueText = new TextView(mContext);
        mValueText.setGravity(Gravity.CENTER_HORIZONTAL);
/* Vanzo:huangchaojun on: Sun, 09 Oct 2011 12:34:58 +0800
        mValueText.setTextSize(32);
 */
        mValueText.setTextSize(25);
// End of Vanzo:huangchaojun
        params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        layout.addView(mValueText, params);

        mSeekBar = new SeekBar(mContext);
        mSeekBar.setOnSeekBarChangeListener(this);
        layout.addView(mSeekBar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        if (shouldPersist())
            mValue = getPersistedInt(mDefault);

        mSeekBar.setMax(mMax);
        mSeekBar.setProgress(mValue);
        return layout;
    }

    @Override
    protected void onBindDialogView(View v) {
        super.onBindDialogView(v);
        mSeekBar.setMax(mMax);
        mSeekBar.setProgress(mValue);
    }

    @Override
    protected void onSetInitialValue(boolean restore, Object defaultValue) {
        super.onSetInitialValue(restore, defaultValue);
        if (restore)
            mValue = shouldPersist() ? getPersistedInt(mDefault) : 0;
        else
            mValue = (Integer) defaultValue;
    }

    public void onProgressChanged(SeekBar seek, int value, boolean fromTouch) {
/* Vanzo:wangyi on: Wed, 13 Nov 2013 10:28:04 +0800
 * bugfix #55890
        String t = String.valueOf(value);
 */
        String t = (value == mMax ? String.valueOf(value) : String.valueOf(value + 1));
// End of Vanzo: wangyi
        mValueText.setText(mSuffix == null ? t : t.concat(mSuffix));
        if (shouldPersist())
            persistInt(value);
        callChangeListener(new Integer(value));
    }

    public void onStartTrackingTouch(SeekBar seek) {
    }

    public void onStopTrackingTouch(SeekBar seek) {
    }

    public void setMax(int max) {
        mMax = max;
    }

    public int getMax() {
        return mMax;
    }

    public void setProgress(int progress) {
        mValue = progress;
        if (mSeekBar != null)
            mSeekBar.setProgress(progress);
    }

    public int getProgress() {
        return mValue;
    }
}
