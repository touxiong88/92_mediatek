package com.mediatek.security.service;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.provider.Settings;
import android.widget.Toast;

import com.mediatek.common.mom.IMobileManager;
import com.mediatek.common.mom.Permission;
import com.mediatek.common.mom.PermissionRecord;
import com.mediatek.common.mom.SubPermissions;
import com.mediatek.security.R;
import com.mediatek.security.datamanager.DatabaseManager;
import com.mediatek.xlog.Xlog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



public class PermControlUtils {
    private static final String TAG = "PmUtils";
    private static Map<String,Integer> sPermControlMap;
    
    public static final String PACKAGE_NAME = "exta_package_name";
    public static final String PERMISSION_NAME = "extra_permission_name";
    public static final String PERMISSION_FLAG = "extra_permission_flag";
    public static final String PERMISSION_CONTROL_STATE = "permission_control_state";
    public static final String PERMISSION_CONTROL_ATTACH = "permission_control_attached";
    public static final String PERMISSION_SWITCH_OFF_DLG_STATE = "permission_switch_off_dlg_state";
    public static final int MAX_WATI_TIME = 20000;//in mill-seconds
    public static final String PERM_CONFIRM_DIALOG = "com.mediatek.security.action.PERM_NOTIFY";
    public static final String PERM_UI_ACTION = "com.mediatek.security.PERMISSION_CONTROL";
    public static final String PERM_CONTROL_DATA_UPDATE = "com.mediatek.security.action.DATA_UPDATE";
    public static final int  DEFAULT_OFF = -1;
    public static final String PKG_NAME = "package_name";
    // when adding one of these:
    //  - increment _NUM_OP
    public static final int PERM_NONE = -1;
    public static final int PERM_MAKE_CALL = 0;
    public static final int PERM_SEND_SMS = 1;
    public static final int PERM_SEND_MMS = 2;
    public static final int PERM_RECORD_VOICE = 3;
    public static final int PERM_READ_SMS = 4;
    public static final int PERM_READ_MMS = 5;
    public static final int PERM_READ_CONTACT = 6;
    public static final int PERM_READ_CALL_LOG = 7;
    public static final int PERM_ACCESS_LOCATION = 8;
    public static final int PERM_OPEN_CAMERA = 9;
    public static final int PERM_CHANGE_NETWORK_STATE_ON = 10;
    public static final int PERM_WIFI_STATE_ON = 11;
    public static final int PERM_BT_STATE_ON = 12;
    /** @hide */
    public static final int NUM_OP = 13;
    
    /**
     * This maps each operation to the operation that serves as the
     * switch to determine whether it is allowed.  
     */
    private static final int[] PERM_CODE = new int[] {
        PERM_MAKE_CALL,
        PERM_SEND_SMS,
        PERM_SEND_MMS,
        PERM_RECORD_VOICE,
        PERM_READ_SMS,
        PERM_READ_MMS,
        PERM_READ_CONTACT,
        PERM_READ_CALL_LOG,
        PERM_ACCESS_LOCATION,
        PERM_OPEN_CAMERA,
        PERM_CHANGE_NETWORK_STATE_ON,
        PERM_WIFI_STATE_ON,
        PERM_BT_STATE_ON
    };
    
    /**
     * This optionally maps a permission to an operation.  If there
     * is no permission associated with an operation, it is null.
     */
    private static final String[] PERM_NAME = new String[] {
        SubPermissions.MAKE_CALL,
        SubPermissions.SEND_SMS,
        SubPermissions.SEND_MMS,
        SubPermissions.RECORD_MIC,
        SubPermissions.QUERY_SMS,
        SubPermissions.QUERY_MMS,
        SubPermissions.QUERY_CONTACTS,
        SubPermissions.QUERY_CALL_LOG,
        SubPermissions.ACCESS_LOCATION,
        SubPermissions.OPEN_CAMERA,
        SubPermissions.CHANGE_NETWORK_STATE_ON,
        SubPermissions.CHANGE_WIFI_STATE_ON,
        SubPermissions.CHANGE_BT_STATE_ON
    };
    
    private static final int[] PERM_ICON = {
        R.drawable.perm_group_phone_calls,
        R.drawable.perm_group_messages,
        R.drawable.perm_group_sent_mms,
        R.drawable.perm_group_start_sound_recording,
        R.drawable.perm_group_read_message,
        R.drawable.perm_group_read_mms,
        R.drawable.perm_group_read_contacts,
        R.drawable.perm_group_read_calllog,
        R.drawable.perm_group_location,
        R.drawable.perm_group_camera,
        R.drawable.perm_group_turn_on_data_connection,
        R.drawable.perm_group_network,
        R.drawable.perm_group_bluetooth
    };
    
    public static void initUtil(Context context) {
        if (sPermControlMap == null) {
            sPermControlMap = new HashMap<String, Integer>();
            int i = 0;
            for (String key : PERM_NAME) {
                sPermControlMap.put(key, PERM_CODE[i++]);
            }
        }
    }
    
    /**
     * Search the permission name is in the control list
     * @param permName input permission name
     * @return true for control
     */
    public static boolean isPermNeedControl(String permName) {
        return sPermControlMap.containsKey(permName); 
    }
    
    /**
     * Get the label of permission
     * @param permName permission name
     * @param permLabelArray the array of permission name
     * @return the label of permission
     */
    public static String getPermissionLabel(String permName, String[] permLabelArray) {
        Integer code = sPermControlMap.get(permName);
        return code != null ? permLabelArray[code] : null;
    }
    
    /**
     * Get the icon of permission
     * @param permName permission name
     * @return icon of the permisssion
     */
    public static int getPermissionIcon(String permName) {
        Integer code = sPermControlMap.get(permName);
        return code != null ? PERM_ICON[code] : null;
    }
    
    /**
     * Get the permission index in permission array such as PERM_MAKE_CALL = 0
     * @param permName permission name
     * @return the index of permission
     */
    public static int getPermissionIndex(String permName) {
        Integer code = sPermControlMap.get(permName);
        return code;
    }
    
    /**
     * By using the index to get permission name
     * @param index the permission index
     * @return permission name
     */
    public static String getPermissionName(int index) {
        if (index < 0 || index >= PERM_NAME.length) {
            return null;
        }
        return PERM_NAME[index];
    }
    
    /**
     * The controlled permission name array
     * @return the array
     */
    public static String[] getControlPermArray() {
        return PERM_NAME;
    }
    
    /**
     * Permission control in some case need to be disabled, to get the state by calling this function
     * @param context Context
     * @return true for enable
     */
    public static boolean isInHouseEnabled(Context context) {
        int state = Settings.System.getInt(context.getContentResolver(),
                            PERMISSION_CONTROL_ATTACH,0);
        return state > 0;
    }
    
    /**
     * Get application name by passing the  package name
     * @param context Context
     * @param pkgName package name
     * @return the application name
     */
    public static String getApplicationName(Context context, String pkgName) {
        if (pkgName == null) {
            return null;
        }
        String appName = null;
        try {
            PackageManager pkgManager = context.getPackageManager();
            ApplicationInfo info = pkgManager.getApplicationInfo(pkgName, 0);
            appName = pkgManager.getApplicationLabel(info).toString();
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return appName;
    }
    
    /**
     * Get whether the pkg is under installed, false not installed.
     * @param context
     * @param pkgName
     * @return
     */
    public static boolean isPkgInstalled(Context context, String pkgName) {
        boolean isPkgInstalled = true;
        try {
            PackageManager pkgManager = context.getPackageManager();
            ApplicationInfo info = pkgManager.getApplicationInfo(pkgName, 0);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
            Xlog.e(TAG,"Package is not installed");
            isPkgInstalled = false;
        }
        return isPkgInstalled;
    }
    
    
    /**
     * Get application icon by passing the  package name
     * @param context Context
     * @param pkgName package name
     * @return the application icon
     */
    public static Drawable getApplicationIcon(Context context, String pkgName) {
        Drawable appIcon = null;
        try {
            PackageManager pkgManager = context.getPackageManager();
            ApplicationInfo info = pkgManager.getApplicationInfo(pkgName, 0);
            appIcon = pkgManager.getApplicationIcon(info);
        } catch (NameNotFoundException ex) {
            Xlog.w(TAG, "get icon is null");
        }
        return appIcon;
    }
    
    /**
     * Modify the permission status of the package
     * @param permRecord PermissionRecord data type
     * @param context Context
     */
    public static void changePermission(PermissionRecord permRecord, Context context) {
        IMobileManager moMService = (IMobileManager)context.getSystemService(Context.MOBILE_SERVICE);
        moMService.setPermissionRecord(permRecord);
        DatabaseManager.modify(permRecord);
    }
    
    /**
     * Switch on/off permission control monitor, only on the permission control will work 
     * @param state true for turn on monitor
     * @param context Context
     */
    public static void enablePermissionControl(boolean state, Context context) {
        if (Settings.System.getInt(context.getContentResolver(), 
                PERMISSION_CONTROL_STATE,DEFAULT_OFF) == DEFAULT_OFF) {
            NotificationManager notifyManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
            notifyManager.cancel(PermControlService.NOTIFY_ID);
        }
        Settings.System.putInt(context.getContentResolver(), PERMISSION_CONTROL_STATE, state ? 1 : 0);
        IMobileManager moMService = (IMobileManager)context.getSystemService(Context.MOBILE_SERVICE);
        moMService.enablePermissionController(state);
    }
    
    /**
     * Get PermissionRecord List by passing packge name and corresponding Permission List,
     * because not every permission need to control, so only the control permission package 
     * return list
     * 
     * @param pkgName package name
     * @param permList Permission List of the package
     * @return the List of PermissionRecord of the package, if permList is null then return null
     */
    public static List<PermissionRecord> getPermRecordListByPkg(String pkgName,List<Permission> permList) {
        Xlog.d(TAG,"getPermRecordListByPkg() pkgName = " + pkgName);
        if (permList == null) {
            Xlog.e(TAG,"permList null");
            return null;
        }
        List<PermissionRecord> permRecordList = new ArrayList<PermissionRecord>();
        for (Permission permission : permList) {
            List<Permission> subPermList = permission.mSubPermissions;
            //If there is no sub permission and parent perm under monitor add into list
            if (subPermList == null) {
                if (sPermControlMap.containsKey(permission.mPermissionName)) {
                    PermissionRecord record = new PermissionRecord(pkgName,permission.mPermissionName,
                                                            IMobileManager.PERMISSION_STATUS_CHECK);
                    permRecordList.add(record);    
                }
            } else {
                List<PermissionRecord> subPermRecordList = getSubPermRecordList(pkgName,subPermList);
                if (subPermRecordList != null) {
                    permRecordList.addAll(subPermRecordList);    
                }
            }
        }
        return permRecordList;
    }

    private static List<PermissionRecord> getSubPermRecordList(String pkgName, List<Permission> subPermList) {
        List<PermissionRecord> subPermRecordList = null;
        for (Permission subPerm : subPermList) {
            if (sPermControlMap.containsKey(subPerm.mPermissionName)) {
                if (subPermRecordList == null) {
                    subPermRecordList = new ArrayList<PermissionRecord>();
                }
                PermissionRecord record = new PermissionRecord(pkgName,subPerm.mPermissionName,
                                                                IMobileManager.PERMISSION_STATUS_CHECK);
                subPermRecordList.add(record);
            }
        }
        return subPermRecordList;
    }
    
    /**
     * show a toast with deny hint msg body
     * @param context the context
     * @param pkgName the package name
     * @param permissionName the related permissin name
     */
    public static void showDenyToast(Context context, String pkgName, String permissionName) {
        String label = getApplicationName(context,pkgName);
        Xlog.d(TAG,"showDenyToast() pkgName = " + pkgName + " label = " + label);
        if (label != null) {
            String msg = context.getString(R.string.toast_deny_msg_body,label,getMessageBody(context,permissionName));
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Get the permission name related dialog message body
     * @param context Context
     * @param permName Permission name
     * @return String of the message body
     */
    public static String getMessageBody(Context context, String permName) {
        String msgArray[] = context.getResources().getStringArray(R.array.confirm_msg_body);
        int permIndex = getPermissionIndex(permName);
        String msg = msgArray[permIndex];
        return msg;
    }
}
