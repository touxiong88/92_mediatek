package com.mediatek.deskclock.plugin;

import android.content.Context;
import android.content.ContextWrapper;

import com.mediatek.deskclock.ext.IRepeatPreferenceExtension;

/**
 * M: Default implementation of Plug-in definition of Desk Clock.
 */
public class Op01RepeatPreferenceExtension extends ContextWrapper implements IRepeatPreferenceExtension {

    public Op01RepeatPreferenceExtension(Context ctx) {
        super(ctx);
    }
    
    /**
     * @return Return if use mtk repeat pref ..
     */
    public boolean shouldUseMTKRepeatPref() {
        return true;
    }
}
