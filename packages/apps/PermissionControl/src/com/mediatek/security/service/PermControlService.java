package com.mediatek.security.service;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.StatusBarManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.TextView;

import static com.mediatek.security.service.PermControlUtils.DEFAULT_OFF;
import com.mediatek.common.mom.IMobileConnectionCallback;
import com.mediatek.common.mom.IMobileManager;
import com.mediatek.common.mom.IPermissionListener;
import com.mediatek.common.mom.Permission;
import com.mediatek.common.mom.PermissionRecord;
import com.mediatek.security.R;
import com.mediatek.security.datamanager.DatabaseManager;
import com.mediatek.xlog.Xlog;

import java.util.ArrayList;
import java.util.List;

public class PermControlService extends AsyncService implements OnClickListener, OnDismissListener {
   
    private static final String TAG = "PermControlService";
    private static final int DELAY_TIME = 1000;
    private static final int MSG_RESET = 101;
    private static final int MSG_SHOW_TOAST = MSG_RESET + 1;
    private static final int MSG_SHOW_CONF_DLG = MSG_RESET + 2;
    private static final int MSG_COUNT_DOWN = MSG_RESET + 3;
    private static final int COUNT_DOWN_TIMER = PermControlUtils.MAX_WATI_TIME / 1000;
    private static final int EXTRA_TIMER = 5000;
    
    //for block the thread to wait user confirm
    private Object mUserConfirmLock = new Object();
    
    private List<PermissionRecord> mPermRecordList = new ArrayList<PermissionRecord>();
    private IMobileManager mMoMService;
    private boolean mIsGranted;
    private boolean mIsPermit;
    private boolean mIsAttached;
    
    private PermissionRecord mCurrentPermRecord;
    private CheckBox mCheckBox;
    private TextView mTimeCountDown;
    private AlertDialog mAlertDlg;
    
    public static final int NOTIFY_ID = 1200;
    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_SHOW_TOAST) {
                Bundle data = msg.getData();
                if (data != null) {
                    Xlog.d(TAG,"Always deny show the toast");
                    String pkgName = data.getString(PermControlUtils.PACKAGE_NAME);
                    String permName = data.getString(PermControlUtils.PERMISSION_NAME);
                    PermControlUtils.showDenyToast(getApplicationContext(), pkgName, permName);
                }
            } else if (msg.what == MSG_SHOW_CONF_DLG) {
                Xlog.d(TAG,"Show confirm dialog");
                showDialog(mCurrentPermRecord,msg.arg1);
            } else if (msg.what == MSG_COUNT_DOWN) {
                int timer = msg.arg1 - 1;
                Xlog.d(TAG,"timer is = " + timer);
                if (timer > 0) {
                    updateCount(timer);
                } else {
                    Xlog.d(TAG,"time out and deny the permission");
                    // time out dismiss dialog and return false onPermissionCheck
                    PermControlUtils.showDenyToast(getApplicationContext(),mCurrentPermRecord.mPackageName,
                                                mCurrentPermRecord.mPermissionName);
                    mIsPermit = false;
                    if (mAlertDlg != null) {
                        mAlertDlg.dismiss();
                    }   
                }
            }
        }
    };
    
    public PermControlService() {
        super("PermControlService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Xlog.d(TAG,"onCreate()");
        initService();
    }
    
    /**
     * Must call before access any further moms apis
     */
    private void initService() {
        Xlog.d(TAG,"initService()");
        PermControlUtils.initUtil(getApplicationContext());
        if (mMoMService == null) {
            mMoMService = (IMobileManager)getSystemService(Context.MOBILE_SERVICE);
        }
        mIsAttached = attachMoMS(mMoMService);
        Settings.System.putInt(getContentResolver(), 
                PermControlUtils.PERMISSION_CONTROL_ATTACH, mIsAttached ? 1 : 0);
        Xlog.d(TAG,"mIsAttached = " + mIsAttached);
    }
    
    private boolean attachMoMS(IMobileManager service) {
        boolean isAttach = service.attach(new IMobileConnectionCallback.Stub() {
            @Override
            public void onConnectionEnded() throws RemoteException {
                mIsAttached = false;
                Xlog.d(TAG,"onConnectionEnded() with mIsAttached = " + mIsAttached);
                Settings.System.putInt(getContentResolver(), 
                        PermControlUtils.PERMISSION_CONTROL_ATTACH,mIsAttached ? 1 : 0);
                //In case tencent attached after in house attach need to remove the notification
                NotificationManager notifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                notifyManager.cancel(NOTIFY_ID);
            }
            @Override
            public void onConnectionResume() {
                Xlog.d(TAG,"onConnectionResume()");
                initService();
                registerMoMS();
            }
        });
        return isAttach;
    }
    
    private void registerMoMS() {
        Xlog.d(TAG,"registerMoMS() mIsAttached = " + mIsAttached);
        //init database and get all cache data ready
        DatabaseManager.initPermControlData(this);
        if (mIsAttached) {
            showNotification();
            List<PermissionRecord> permRecordList = DatabaseManager.getAllPermRecordList();
            printRecordList(permRecordList);
            try {
                mMoMService.setPermissionRecords(permRecordList);
                mMoMService.registerPermissionListener(new PermissionListener());
                mMoMService.enablePermissionController(
                        Settings.System.getInt(getContentResolver(), 
                        PermControlUtils.PERMISSION_CONTROL_STATE,DEFAULT_OFF) > 0);
            } catch (SecurityException e) {
                Xlog.d(TAG,"is detached so no permission to use api with " + e);
            }
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            String action = intent.getAction();
            Log.d(TAG,"onHandleIntent() action = " + action);
            if (Intent.ACTION_PACKAGE_ADDED.equals(action)) {
                String pkgName = intent.getStringExtra(PermControlUtils.PACKAGE_NAME);
                handleInstall(pkgName);
            } else if (Intent.ACTION_PACKAGE_FULLY_REMOVED.equals(action)) {
                String pkgName = intent.getStringExtra(PermControlUtils.PACKAGE_NAME);
                handleRemove(pkgName);
            } else if (action == null) {
                //Launch from MoMS
                registerMoMS();
            }
        } else {
            //Handle if service is killed
            Xlog.d(TAG,"intent = null servie is killed and relaunched by system");
            registerMoMS();
        }
    }
    
    private void handleRemove(String pkgName) {
        Xlog.d(TAG,"handleRemove() with pkgName = " + pkgName);
        if (pkgName != null) {
            DatabaseManager.delete(pkgName);
            sendCacheUpdateBroadcast(pkgName);
        }
    }

    private void handleInstall(String pkgName) {
        Xlog.d(TAG,"handleInstall() with pkgName = " + pkgName);
        if (pkgName != null) {
            if (PermControlUtils.isPkgInstalled(this, pkgName)) {
                List<Permission> permList = mMoMService.getPackageGrantedPermissions(pkgName);
                List<PermissionRecord> permRecordList = DatabaseManager.add(pkgName,permList);
                if (permRecordList != null && mIsAttached) {
                    mMoMService.setPermissionRecords(permRecordList);
                    sendCacheUpdateBroadcast(pkgName);
                }
            } else {
                Xlog.e(TAG,"Receive add broadcast but can not query appinfo, internal app removed case");
                handleRemove(pkgName);
            }
            
        }
    }
    
    /**
     * Show a system confirm dialog from service 
     * @param record the PermissionRecord data type
     * @param flag the flag of the PermissionRecord
     */
    private void showDialog(PermissionRecord record, int flag) {
        Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.notify_dialog_title);
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setPositiveButton(R.string.accept_perm, this);
        builder.setNegativeButton(R.string.deny_perm, this);
        builder.setCancelable(false);
        
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.notify_dialog_customview, null);
        builder.setView(view);
        
        TextView messageText = (TextView)view.findViewById(R.id.message);
        mTimeCountDown = (TextView) view.findViewById(R.id.count_timer);
        mCheckBox = (CheckBox)view.findViewById(R.id.checkbox);
        if ((flag & IMobileManager.PERMISSION_FLAG_USERCONFIRM) > 0) {
            mCheckBox.setVisibility(View.GONE);
        }
        String label = PermControlUtils.getApplicationName(this, record.mPackageName);
        String msg = getString(R.string.notify_dialog_msg_body,label,
                                PermControlUtils.getMessageBody(this,record.mPermissionName));
        messageText.setText(msg);
        
        mAlertDlg = builder.create();
        mAlertDlg.setOnDismissListener(this);
        mAlertDlg.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        //Disable the home key
        Window win = mAlertDlg.getWindow();
        WindowManager.LayoutParams lp = win.getAttributes();
        lp.flags |= WindowManager.LayoutParams.FLAG_HOMEKEY_DISPATCHED;
        win.setAttributes(lp);
        setStatusBarEnableStatus(false);
        
        mAlertDlg.show();
        updateCount(COUNT_DOWN_TIMER);
    }
    
    private void updateCount(int timer) {
        setCountText(timer);
        Message msg = Message.obtain();
        msg.what = MSG_COUNT_DOWN;
        msg.arg1 = timer;
        mHandler.sendMessageDelayed(msg, DELAY_TIME);
    }
    
    private void setCountText(int timer) {
        String msg = getString(R.string.time_count_down_hint,String.valueOf(timer));
        mTimeCountDown.setText(msg);
    }
    private void printRecordList(List<PermissionRecord> permRecordList) {
        if (permRecordList != null) {
            for (PermissionRecord permrecord : permRecordList) {
                Xlog.d(TAG,"pkg = " + permrecord.mPackageName + 
                           "permName = " + permrecord.mPermissionName + 
                           "status = " + permrecord.getStatus());
            }
        }
    }
    
    private void showNotification() {
        int state = Settings.System.getInt(getContentResolver(), 
                PermControlUtils.PERMISSION_CONTROL_STATE,DEFAULT_OFF);
        Xlog.d(TAG,"state = " + state);
        if (state == DEFAULT_OFF) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            Notification notification = new Notification();
            String titleStr = getResources().getString(
                        R.string.notification_control_state_title);
            String summaryStr = getResources().getString(
                    R.string.notification_control_state_summary);
            notification.icon = android.R.drawable.stat_notify_error;
            notification.tickerText = titleStr;
            notification.flags = Notification.FLAG_AUTO_CANCEL;
            Intent intent = new Intent();
            intent.setAction(PermControlUtils.PERM_UI_ACTION);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                    intent, 0);
            notification.setLatestEventInfo(this, titleStr, summaryStr,
                    pendingIntent);
            notificationManager.notify(NOTIFY_ID, notification);
        }
    }
    
    /*
     * Show a notification to user if permission control never on
     */
    private void showDialogActivity(PermissionRecord record, int flag) {
        Intent intent = new Intent();
        intent.setAction(PermControlUtils.PERM_CONFIRM_DIALOG);
        intent.putExtra(PermControlUtils.PACKAGE_NAME, record.mPackageName);
        intent.putExtra(PermControlUtils.PERMISSION_NAME, record.mPermissionName);
        intent.putExtra(PermControlUtils.PERMISSION_FLAG, flag);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
    
    class PermissionListener extends IPermissionListener.Stub {
        @Override
        public boolean onPermissionCheck(PermissionRecord record, int flag, int uid, Bundle data) {
            Xlog.d(TAG,"onPermissionCheck pkg = " + record.mPackageName + " " + 
                        record.mPermissionName + " " + 
                        record.getStatus() + " " + flag);
            if (record.getStatus() == IMobileManager.PERMISSION_STATUS_CHECK) {
                return handleCheckCase(record,flag);
            } else if (record.getStatus() == IMobileManager.PERMISSION_STATUS_DENIED) {
                showDenyToast(record);
                return false;
            } else if (record.getStatus() == IMobileManager.PERMISSION_STATUS_GRANTED) {
                return true;
            } else {
                Xlog.e(TAG,"Not correct status");
                return false;
            }
        }
        @Override
        public void onPermissionChange(PermissionRecord record) {
            Xlog.d(TAG,"onPermissionChange");
        }
    }
    
    /*
     * Synchronized the function of handleCheckCase, whenever one permission confirm thread hold the lock
     * other permission thread need to wait previous release otherwise wait
     * 
     */
    private synchronized boolean handleCheckCase(PermissionRecord record, int flag) {
        Xlog.d(TAG,"handleCheckCase()");
        synchronized (mUserConfirmLock) {
            try {      
                mCurrentPermRecord = record;
                callShowDlg(flag);
                // add extra timer as the time counter is not accurate, in some case the lock 
                // may wake up before time counter up to 20s
                mUserConfirmLock.wait(PermControlUtils.MAX_WATI_TIME + EXTRA_TIMER);
                Xlog.d(TAG,"release the lock");
            } catch (InterruptedException e) {
                Xlog.d(TAG,"error");
            }
        }
        Xlog.d(TAG,"mIsGranted " + mIsGranted);
        return mIsGranted;
    }
    
    /*
     * Because the system dialog need to show in main thread of service so show the dialog via a handler
     */
    private void callShowDlg(int flag) {
        Message msg = Message.obtain();
        msg.arg1 = flag;
        msg.what = MSG_SHOW_CONF_DLG;
        mHandler.sendMessage(msg);
    }
    
    /*
     * The toast have to show in a main thread so show the toast via a handler
     */
    private void showDenyToast(PermissionRecord record) {
        Message msg = Message.obtain();
        Bundle data = new Bundle();
        data.putCharSequence(PermControlUtils.PACKAGE_NAME, record.mPackageName);
        data.putCharSequence(PermControlUtils.PERMISSION_NAME, record.mPermissionName);
        msg.setData(data);
        msg.what = MSG_SHOW_TOAST;
        mHandler.sendMessage(msg);
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
    
    
    /*
     * Whenever the data cache modified need to call this function to notify 
     */
    private void sendCacheUpdateBroadcast(String pkgName) {
        Intent intent = new Intent();
        intent.putExtra(PermControlUtils.PACKAGE_NAME, pkgName);
        intent.setAction(PermControlUtils.PERM_CONTROL_DATA_UPDATE);
        sendBroadcast(intent);
    }

    /**
     * true for grant and false for deny
     * @param status enable or not the permission
     */
    public void setPermissionStatus(boolean isEnable) {
        mIsGranted = isEnable;
        synchronized (mUserConfirmLock) {
            mUserConfirmLock.notifyAll();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Xlog.e(TAG,"onDestroy");
        if (mMoMService != null) {
            mMoMService.enablePermissionController(false);
        }
        setStatusBarEnableStatus(true);
    }
    
    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder {
        public PermControlService getService() {
            return PermControlService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
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
            mCurrentPermRecord.setStatus(status);
            PermControlUtils.changePermission(mCurrentPermRecord, this);
        }
        Xlog.d(TAG,"Click dialog button with check box " + mCheckBox.isChecked() + " enable = " + enable);
        mIsPermit = enable;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        Xlog.d(TAG,"Dialog dimissed");
        setStatusBarEnableStatus(true);
        mHandler.removeMessages(MSG_COUNT_DOWN);
        Xlog.d(TAG,"mIsPermit = " + mIsPermit);
        setPermissionStatus(mIsPermit);
    }
}
