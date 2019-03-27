package com.mediatek.security.ui;

import android.Manifest;
import android.app.StatusBarManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;


import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import com.mediatek.common.mom.IMobileManager;
import com.mediatek.common.mom.PermissionRecord;
import com.mediatek.common.mom.SubPermissions;
import com.mediatek.security.R;
import com.mediatek.security.service.PermControlService;
import com.mediatek.security.service.PermControlUtils;
import com.mediatek.xlog.Xlog;

public class PermissionNotify extends AlertActivity implements OnClickListener {
    
    private static final String TAG = "PermissionNotify";
    
    private static final int DELAY_TIME = 1000;
    private static final int MSG_TIMER = 101;
    private CheckBox mCheckBox;
    private TextView mMessageText;
    private TextView mTimeCountDown;
    private String mPackageLable;
    private String mPackageName;
    private String mPermissionName;
    private int mFlag;
    private PermControlService mBoundService;
    // Need the time in seconds
    private static final int COUNT_DOWN_TIMER = PermControlUtils.MAX_WATI_TIME / 1000; 
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mBoundService = ((PermControlService.LocalBinder)service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBoundService = null;
        }
    };
    
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_TIMER) {
                int timer = msg.arg1 - 1;
                Xlog.d(TAG,"timer is = " + msg.arg1);
                if (timer > 0) {
                    msg = Message.obtain();
                    msg.what = MSG_TIMER;
                    msg.arg1 = timer;
                    setCountDownTimerText(timer);
                    mHandler.sendMessageDelayed(msg, DELAY_TIME);
                } else {
                    PermControlUtils.showDenyToast(getApplicationContext(),mPackageName,mPermissionName);
                    mBoundService.setPermissionStatus(false);
                    finish();
                }
            }
        }
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Xlog.d(TAG,"onCreate()");
        Intent intent = getIntent();
        if (intent == null) {
            finish();
        }
        mPackageName = intent.getStringExtra(PermControlUtils.PACKAGE_NAME);
        mPackageLable = PermControlUtils.getApplicationName(this, mPackageName);
        mPermissionName = intent.getStringExtra(PermControlUtils.PERMISSION_NAME);
        mFlag = intent.getIntExtra(PermControlUtils.PERMISSION_FLAG, IMobileManager.PERMISSION_FLAG_NORMAL);
        Xlog.d(TAG,"mPackageLable = " + mPackageLable + " mPermissionName = " + mPermissionName);
        doBindService();
        showDialog();
        Message msg = Message.obtain();
        msg.what = MSG_TIMER;
        msg.arg1 = COUNT_DOWN_TIMER;
        mHandler.sendMessageDelayed(msg, DELAY_TIME);
        setWindowProperty();
    }
    
    private void setWindowProperty() {
        getWindow().setCloseOnTouchOutside(false);
        Window win = getWindow();
        WindowManager.LayoutParams lp = win.getAttributes();
        lp.flags |= WindowManager.LayoutParams.FLAG_HOMEKEY_DISPATCHED;
        win.setAttributes(lp);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        Xlog.d(TAG,"onResume()");
        //disable status bar to prevent jump to other activity
        setStatusBarEnableStatus(false);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // restore the status bar onPause() to prevent some cases that
        // app crashed and no chance to restore the status bar onDestroy()
        setStatusBarEnableStatus(true);
    }

    private void showDialog() {
        final AlertController.AlertParams p = mAlertParams;
        p.mIconId = android.R.drawable.ic_dialog_alert;
        p.mTitle = getString(R.string.notify_dialog_title);
        p.mPositiveButtonText = getString(R.string.accept_perm);
        p.mNegativeButtonText = getString(R.string.deny_perm);
        p.mPositiveButtonListener = this;
        p.mNegativeButtonListener = this;
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.notify_dialog_customview, null);
        mCheckBox = (CheckBox)view.findViewById(R.id.checkbox);
        if ((mFlag & IMobileManager.PERMISSION_FLAG_USERCONFIRM) > 0) {
            mCheckBox.setVisibility(View.GONE);
        }
        mMessageText = (TextView)view.findViewById(R.id.message);
        mTimeCountDown = (TextView) view.findViewById(R.id.count_timer);
        String msg = getString(R.string.notify_dialog_msg_body,mPackageLable,
                                PermControlUtils.getMessageBody(this,mPermissionName));
        Xlog.d(TAG,"msg = " + msg);
        mMessageText.setText(msg);
        setCountDownTimerText(COUNT_DOWN_TIMER);
        p.mView = view;
        setupAlert();
    }

    private void setCountDownTimerText(int timer) {
        String msg = getString(R.string.time_count_down_hint,String.valueOf(timer));
        mTimeCountDown.setText(msg);
    }
   

    @Override
    public void onClick(DialogInterface dialog, int which) {
        boolean enable = false;
        int status = IMobileManager.PERMISSION_STATUS_CHECK;
        if (which == DialogInterface.BUTTON_POSITIVE) {
            status = IMobileManager.PERMISSION_STATUS_GRANTED;
            enable = true;
        } else if (which == DialogInterface.BUTTON_NEGATIVE) {
            status = IMobileManager.PERMISSION_STATUS_DENIED;
            enable = false;
        }
        if (mCheckBox.isChecked()) {
            PermissionRecord permRecord = new PermissionRecord(mPackageName, mPermissionName, status);
            PermControlUtils.changePermission(permRecord, this);
        }
        mBoundService.setPermissionStatus(enable);
        mHandler.removeCallbacksAndMessages(null);
        Xlog.d(TAG,"onClick which = " + which + " status = " + status + "enable = " + enable);
    }
    
    void doBindService() {
        // Establish a connection with the service.  We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
        bindService(new Intent(PermissionNotify.this, 
                PermControlService.class), mConnection, Context.BIND_AUTO_CREATE);
    }
    
    void doUnbindService() {
        unbindService(mConnection);
    }
    
    //Disable back key so press back won't finish activity
    @Override
    public void onBackPressed() {
        
    }
    
    private void setStatusBarEnableStatus(boolean enabled) {
        Xlog.i(TAG, "setStatusBarEnableStatus(" + enabled + ")");
        StatusBarManager statusBarManager;
        statusBarManager = (StatusBarManager)getSystemService(Context.STATUS_BAR_SERVICE);
        if (statusBarManager != null) {
            if (enabled) {
                statusBarManager.disable(StatusBarManager.DISABLE_NONE);
            } else {
                statusBarManager.disable(StatusBarManager.DISABLE_EXPAND |
                                         StatusBarManager.DISABLE_RECENT |
                                         StatusBarManager.DISABLE_HOME);
            }
        } else {
            Xlog.e(TAG, "Fail to get status bar instance");
        }
    }
    
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        doUnbindService();
    }
}