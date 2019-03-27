
package com.mediatek.mediatekdm.ext;

import android.os.FileUtils;
import android.util.Log;

import com.android.internal.os.AtomicFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.StreamCorruptedException;

public final class MTKFileUtil {
    private static final String TAG = "MTKFileUtil";

    public static void openPermission(String dir) {
        FileUtils.setPermissions(dir, FileUtils.S_IRWXU | FileUtils.S_IRWXG | FileUtils.S_IXOTH,
                -1, -1);
    }

    public static void atomicWrite(File file, Object data) {
        AtomicFile atomicFile = new AtomicFile(file);
        FileOutputStream fos = null;
        try {
            fos = atomicFile.startWrite();
            ObjectOutputStream out = new ObjectOutputStream(fos);
            out.writeObject(data);
            atomicFile.finishWrite(fos);
            Log.i(TAG, "atomicWrite: state stored: " + data);
        } catch (IOException e) {
            atomicFile.failWrite(fos);
            Log.e(TAG, "atomicWrite: exception", e);
        }
    }

    public static Object atomicRead(File file) {
        Object obj = null;
        try {
            AtomicFile atomicFile = new AtomicFile(file);
            ObjectInputStream in = new ObjectInputStream(atomicFile.openRead());
            obj = in.readObject();
            in.close();
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "atomicRead: exception", e);
        } catch (StreamCorruptedException e) {
            Log.e(TAG, "atomicRead: exception", e);
        } catch (OptionalDataException e) {
            Log.e(TAG, "atomicRead: exception", e);
        } catch (IOException e) {
            Log.e(TAG, "atomicRead: exception", e);
        }
        return obj;
    }
}
