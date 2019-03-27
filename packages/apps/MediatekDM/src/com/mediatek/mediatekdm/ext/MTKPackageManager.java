
package com.mediatek.mediatekdm.ext;

import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageInstallObserver;
import android.content.pm.PackageManager;
import android.content.pm.PackageParser;
import android.content.pm.PackageParser.Package;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.util.Log;

import com.mediatek.mediatekdm.DmConst;

import java.io.File;

public final class MTKPackageManager {

    public static final int INSTALL_SUCCEEDED = PackageManager.INSTALL_SUCCEEDED;
    public static final int INSTALL_ALLOW_DOWNGRADE = PackageManager.INSTALL_ALLOW_DOWNGRADE;
    public static final int INSTALL_REPLACE_EXISTING = PackageManager.INSTALL_REPLACE_EXISTING;
    public static final int INSTALL_FAILED_ALREADY_EXISTS = PackageManager.INSTALL_FAILED_ALREADY_EXISTS;

    public interface InstallListener {
        void packageInstalled(final String name, final int status);
    }

    public static final class PackageInfo {
        public String name;
        public String label;
        public String version;
        public String description;
        public Drawable icon;
        public Package pkg;
    }

    public static PackageInfo getPackageInfo(PackageManager pm, Resources resources,
            String archiveFilePath) {
        PackageInfo ret = new PackageInfo();

        PackageParser packageParser = new PackageParser(archiveFilePath);
        File sourceFile = new File(archiveFilePath);
        DisplayMetrics metrics = new DisplayMetrics();
        metrics.setToDefaults();
        PackageParser.Package pkg = packageParser.parsePackage(sourceFile, archiveFilePath,
                metrics, 0);
        if (pkg == null) {
            Log.w(DmConst.TAG.SCOMO, "package Parser get package is null");
            return null;
        }
        packageParser = null;

        ret.name = pkg.packageName;
        ret.version = pkg.mVersionName;
        ret.pkg = pkg;
        // get icon and label from archive file
        ApplicationInfo appInfo = pkg.applicationInfo;
        AssetManager assmgr = new AssetManager();
        assmgr.addAssetPath(archiveFilePath);
        Resources res = new Resources(assmgr, resources.getDisplayMetrics(),
                resources.getConfiguration());
        CharSequence label = null;
        if (appInfo.labelRes != 0) {
            try {
                label = res.getText(appInfo.labelRes);
            } catch (Resources.NotFoundException e) {
                e.printStackTrace();
            }
        }
        if (label == null) {
            label = (appInfo.nonLocalizedLabel != null) ? appInfo.nonLocalizedLabel
                    : appInfo.packageName;
        }
        Drawable icon = null;
        if (appInfo.icon != 0) {
            try {
                icon = res.getDrawable(appInfo.icon);
            } catch (Resources.NotFoundException e) {
                e.printStackTrace();
            }
        }
        if (icon == null) {
            icon = pm.getDefaultActivityIcon();
        }
        ret.label = label.toString();
        ret.icon = icon;

        return ret;
    }

    public static void installPackage(PackageManager pm, Uri packUri, int flag,
            final InstallListener listener) {
        pm.installPackage(packUri, new IPackageInstallObserver.Stub() {
            public void packageInstalled(final String name, final int status) {
                listener.packageInstalled(name, status);
            }
        }, flag, null);
    }

}
