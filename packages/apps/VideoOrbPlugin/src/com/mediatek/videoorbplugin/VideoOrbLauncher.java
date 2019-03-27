package com.mediatek.videoorbplugin;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.view.View;

import com.mediatek.common.policy.IKeyguardLayer;
import com.mediatek.common.policy.KeyguardLayerInfo;

public class VideoOrbLauncher implements IKeyguardLayer  {
    private static final String TAG = "vo.Launcher";
    private static final String PACKAGE_NAME = "com.mediatek.videoorbplugin";
    private static int mTotalRunInstance = 0;
    private VideoOrbView mStageView = null;
    private Context mContext;
    private int mID;

    private void log(String msg) {
        Log.d(TAG, "<" + mID + "> " + msg);
    }

    public VideoOrbLauncher(Context context) {
        mContext = context;
        mID = mTotalRunInstance;
        mTotalRunInstance++;
        log("VideoOrbLauncher() " + context);
    }
    
    @Override
    public View create() {
        View ret = null;
        boolean isShow = checkIfCurrentLayer();
        if (isShow) {
            mStageView = new VideoOrbView(mContext);
            log("create() " + mStageView);
            ret = mStageView.create();
        } else {
            log("create() " + mStageView);
        }
        return ret;
    }

    @Override
    public void destroy() {
        log("destroy() " + mStageView);
        if (null != mStageView) {
            mStageView.destroy();
            mStageView = null;
        }
    }

    /**
     * Called by LockScreen to get VideoOrbView layer information.
     * Requested in IKeyguardLayer
     */
    @Override
    public KeyguardLayerInfo getKeyguardLayerInfo() {
        KeyguardLayerInfo info = new KeyguardLayerInfo();
        info.layerPackage = PACKAGE_NAME; // package name
        info.nameResId = R.string.plugin_name;
        info.descResId = R.string.plugin_description;
        info.previewResId = R.drawable.preview;
        info.configIntent = new Intent("com.mediatek.action.VIDEOORB_TRANSCODE_VIDEO");
        return info;
    }

    /**
     * Dynamically detect whether videoOrbPlugin is set as lockscreen layer.
     * The detection method is from "Setting" application.
     */
    public static final String CURRENT_KEYGURAD_LAYER_KEY = "mtk_current_keyguard_layer";
    private boolean checkIfCurrentLayer() {
        String currentLayer = android.provider.Settings.System.getString(
                mContext.getContentResolver(), CURRENT_KEYGURAD_LAYER_KEY);
        Log.v(TAG, "current layer : " + currentLayer);
        return (currentLayer != null) && currentLayer.contains(PACKAGE_NAME);
    }
}
