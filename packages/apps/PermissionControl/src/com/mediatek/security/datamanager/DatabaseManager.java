package com.mediatek.security.datamanager;

import android.content.Context;

import com.mediatek.common.mom.IMobileManager;
import com.mediatek.common.mom.Permission;
import com.mediatek.common.mom.PermissionRecord;
import com.mediatek.security.service.PermControlUtils;
import com.mediatek.xlog.Xlog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;


public class DatabaseManager {
    private static final String TAG = "DatabaseManager";
    
    private static DatabaseHelper sDataBaseHelper;
    private static HashMap<String, List<PermissionRecord>> sPkgKeyCache;
    private static HashMap<String, List<PermissionRecord>> sPermKeyCache;
    
    /**
     * Init database and cache must be called at first and in async process
     * @param context Context of process
     */
    public static void initPermControlData(Context context) {
        if (sDataBaseHelper == null) {
            Xlog.d(TAG,"PermControlService constructor");
            Xlog.d(TAG,"new DatabaseHelper");
            sDataBaseHelper = new DatabaseHelper(context);
            sPkgKeyCache = sDataBaseHelper.getmPkgKeyCache();
            sPermKeyCache = sDataBaseHelper.getmPermKeyCache(); 
            printCache();
        }
    }
    
    private static void printCache() {
        Xlog.d(TAG,"****************Pkg Cache****************");
        Set<Entry<String, List<PermissionRecord>>> entry = sPkgKeyCache.entrySet();
        for (Entry<String, List<PermissionRecord>> e : entry) {
            Xlog.d(TAG,"key = " + e.getKey() + " value = " + e.getValue());
        }
        Xlog.d(TAG,"****************Permission Cache****************");
        entry = sPermKeyCache.entrySet();
        for (Entry<String, List<PermissionRecord>> e : entry) {
            Xlog.d(TAG,"key = " + e.getKey() + " value = " + e.getValue());
        }
    }
    
    /**
     * Get a list permissionRecord store in database. Load data from cache
     * @return List<PermissionRecord> from database 
     */
    public static List<PermissionRecord> getAllPermRecordList() {
        List<PermissionRecord> permRecordList = null;
        Set<Entry<String, List<PermissionRecord>>> entry = sPkgKeyCache.entrySet();
        for (Entry<String, List<PermissionRecord>> e : entry) {
            if (permRecordList == null) {
                permRecordList = new ArrayList<PermissionRecord>();
            }
            permRecordList.addAll(e.getValue());
        }
        return permRecordList;
    }
    
    /**
     * Get all package names store in database
     * @return a String list includes all packages in database
     */
    public static List<String> getPackageNames() {
        List<String> nameList = new ArrayList<String>();
        Set<String> keys = sPkgKeyCache.keySet();
        for (Iterator it = keys.iterator();it.hasNext();) {
            nameList.add((String)it.next());
        }
        return nameList;
    }
    
    /**
     * Get the specific permission record of the package
     * @param pkgName the package name of app
     * @return the list of permission record
     */
    public static List<PermissionRecord> getPermRecordListByPkgName(String pkgName) {
        List<PermissionRecord> permissionRecordList = sPkgKeyCache.get(pkgName);
        if (permissionRecordList != null) {
            return new ArrayList<PermissionRecord>(permissionRecordList);
        } else {
            return null;
        }
    }
    
    /**
     * Get a list permissin record contain the specific permission name
     * @param pkgName the package name of app
     * @return the list of permission record
     */
    public static List<PermissionRecord> getPermRecordListByPermName(String permName) {
        List<PermissionRecord> permissionRecordList = sPermKeyCache.get(permName);
        if (permissionRecordList != null) {
            return new ArrayList<PermissionRecord>(permissionRecordList);
        } else {
            return null;
        }
    }
    
    /**
     * Add a new install pkg permission into database. Since it will write database,call it in Asynchronous.
     * @param pkgName package name
     * @param permList package related Permission List
     * @return the package corresponding permission record list, if null the package no permission under monitor
     * or no permission registered
     */
    public static List<PermissionRecord> add(String pkgName, List<Permission> permList) {
        List<PermissionRecord> newPkgPermRecordList = getPermRecordListForNewPkg(pkgName,permList);
        if (newPkgPermRecordList != null) {
            // update cache and database
            delete(pkgName);
            for (PermissionRecord permRecord : newPkgPermRecordList) {
                addIntoCache(permRecord,sPkgKeyCache,KEY_TYPE.Pkg_Key);
                addIntoCache(permRecord,sPermKeyCache,KEY_TYPE.Perm_Key);
                sDataBaseHelper.add(permRecord);
            }
        }
        return newPkgPermRecordList;
    }
    
    /**
     * Get a PermissionRecord List for a new package
     * @param pkgName new package name
     * @param permList corresponding Permission in List
     * @return the new package's List in PermissionRecord type 
     */
    private static List<PermissionRecord> getPermRecordListForNewPkg(String pkgName, List<Permission> permList) {
        List<PermissionRecord> permRecordList = PermControlUtils.getPermRecordListByPkg(pkgName, permList);
        if (permRecordList == null) {
            Xlog.e(TAG,"permRecordList = null");
            return null;
        }
        if (sPkgKeyCache.containsKey(pkgName)) {
            List<PermissionRecord> origList = sPkgKeyCache.get(pkgName);
            Map<String,Integer> map1 = new HashMap<String,Integer>();
            for (PermissionRecord record : origList) {
                //Since default is check then if not check status need to record user old data
                if (record.getStatus() != IMobileManager.PERMISSION_STATUS_CHECK) {
                    map1.put(record.mPermissionName, record.getStatus());
                }
            }
            for (PermissionRecord record : permRecordList) {
                if(map1.get(record.mPermissionName) != null) {
                    record.setStatus(map1.get(record.mPermissionName));
                }
            }
        }
        return permRecordList;
    }
    
    private static void addIntoCache(PermissionRecord permRecord, Map<String,List<PermissionRecord>> map,
                                    KEY_TYPE type) {
        String key = getKeyValue(permRecord,type);
        if (map.containsKey(key)) {
            map.get(key).add(permRecord);
        } else {
            List<PermissionRecord> newPermRecordList = new ArrayList<PermissionRecord>();
            newPermRecordList.add(permRecord);
            map.put(key, newPermRecordList);
        }
    }
    
    /**
     * Delete the specific package from database. Since it will write database, 
     * call it in Asynchronous.
     * @param pkgName package name of apk
     */
    public static void delete(String pkgName) {
        if (pkgName != null) {
            sPkgKeyCache.remove(pkgName);
            deletePermKeyCache(pkgName);
            sDataBaseHelper.delete(pkgName);
        }  
    }
    
    private static void deletePermKeyCache(String pkgName) {
        Set<String> keys = sPermKeyCache.keySet();
        for (Iterator it = keys.iterator();it.hasNext();) {
            String key = (String)it.next();
            List<PermissionRecord> recordList = sPermKeyCache.get(key);
            int pos = 0;
            for (PermissionRecord record : recordList) {
                if (record.mPackageName.equals(pkgName)) {
                    recordList.remove(pos);
                    break;
                }
                pos ++;
            }
        }
    }
    
    /**
     * Modify a permission's status of package. Since it write db, call it in Asynchronous
     * @param permRecord the target permission
     */
    public static void modify(PermissionRecord permRecord) {
        modifyCache(permRecord);
        sDataBaseHelper.updatePermStatus(permRecord.mPackageName, permRecord.mPermissionName, permRecord.getStatus());
    }
    
    private static String getKeyValue(PermissionRecord permRecord, KEY_TYPE type) {
        String key;
        if (type == KEY_TYPE.Pkg_Key) {
            key = permRecord.mPackageName;
        } else {
            key = permRecord.mPermissionName;
        }
        return key;
    }
    
    private static void modifyCache(PermissionRecord permRecord) {
        List<PermissionRecord> permRecordList = sPkgKeyCache.get(permRecord.mPackageName);
        if (permRecordList != null) {
            for (PermissionRecord record : permRecordList) {
                if (record.mPermissionName.equals(permRecord.mPermissionName)) {
                    record.setStatus(permRecord.getStatus());
                }
            }
        } else {
            Xlog.e(TAG,"Something not right need to check mPackageName = " + permRecord.mPackageName);
        }
        permRecordList = sPermKeyCache.get(permRecord.mPermissionName);
        if (permRecordList != null) {
            for (PermissionRecord record : permRecordList) {
                if (record.mPackageName.equals(permRecord.mPackageName)) {
                    record.setStatus(permRecord.getStatus());
                }
            }
        } else {
            Xlog.e(TAG,"Something not right need to check mPermissionName = " + permRecord.mPermissionName);
        }
    }
    enum KEY_TYPE {
        Pkg_Key,
        Perm_Key
    }
}
