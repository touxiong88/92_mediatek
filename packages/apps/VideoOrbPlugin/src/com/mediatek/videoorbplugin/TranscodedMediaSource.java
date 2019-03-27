package com.mediatek.videoorbplugin;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.SystemProperties;
import android.provider.MediaStore;
import android.util.Log;
import com.mediatek.ngin3d.Video;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Hashtable;

public class TranscodedMediaSource implements IMediaSource {
    private final static String TAG = "vo.transcoded";
    private static final String [] PROJECTION =
            new String [] { MediaStore.Video.Media._ID,
                    MediaStore.Video.Media.DATE_TAKEN };
    public static final int MAX_VIDEO_CONTENT = 8;

    private final ContentResolver mCr;
    private Cursor mCursor;
    private int mCounts = 0;

    public TranscodedMediaSource(ContentResolver cr) {
        mCr = cr;
    }

    private Cursor query() {
        if (mCursor == null) {
            try {
                mCursor = mCr.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        PROJECTION, selectionAll(), null, sortOrder());
            } catch (NullPointerException e) {
                e.printStackTrace();
                mCursor.close();
                mCursor = null;
            }
            Log.v(TAG, "query() : " + mCursor);
        }
        return mCursor;
    }

    private final String TRANSCODE_PATH0 = "/storage/sdcard0/.voplugin/";
    private File[] mFiles = null;

    public class VideoFileFilter implements FileFilter {
        Hashtable<String, String> ht;

        VideoFileFilter() {
            ht = new Hashtable<String, String>();
            ht.put("3gp", "1");
            ht.put("mp4", "2");
            ht.put("mkv", "3");
            ht.put("avi", "4");
            ht.put("wmv", "5");
            ht.put("flv", "6");
            ht.put("mpg", "7");
            ht.put("mpeg", "8");
        }

        public String getExtension(File f) {
            if (f != null) {
                String filename = f.getName();
                int i = filename.lastIndexOf('.');
                if (i > 0 && i < filename.length() - 1) {
                    return filename.substring(i + 1).toLowerCase();
                }
            }
            return null;
        }

        @Override
        public boolean accept(File f) {
            Log.d(TAG, "accept()? " + f);
            if (f != null) {
                if (f.isFile()) {
                    String extension = getExtension(f);
                    if (extension != null && ht.get(extension) != null) {
                        return true;
                    }
                }
            }
            return false;
        }
    }
    
    private final FileFilter videoFilter = new VideoFileFilter();
    
    public int getMediaCountFromFile() {
        Log.d(TAG, "getMediaCountFromFile()");

        // search on the file system directly
        File folder = null;

        String internalStorage = SystemProperties.get("internal_sd_path");
        Log.d(TAG, "internal_sd_path: " + internalStorage);
        if (internalStorage.length() == 0) {
            internalStorage = TRANSCODE_PATH0;
        } else {
            internalStorage += "/.voplugin/";
        }

        Log.d(TAG, "getMediaCountFromFile() check " + internalStorage);
        folder = new File(internalStorage);
        Log.d(TAG, "getMediaCountFromFile() folder: " + folder + " exists:" + folder.exists());
        mFiles = folder.listFiles(videoFilter);
        Log.d(TAG, "getMediaCountFromFile() mFiles: " + mFiles);
        if (mFiles != null && mFiles.length > 0) {
            mCounts = mFiles.length;
            for (int i = 0; i < mCounts; i++) {
                Log.d(TAG, "mFiles[" + i + "]: " + mFiles[i].getPath());
            }
        } else {
            mFiles = null;
            mCounts = 0;
        }

        return mCounts;
    }

    public int getMediaCount(int isTranscodeQuery) {
        if (isTranscodeQuery == -1) {
            return mCounts;
        }

        query();
        if (mCursor != null) {
            mCounts = mCursor.getCount();
        }
        Log.d(TAG, "media count: " + mCounts + ", isTranscodeQuery: " + isTranscodeQuery);
        
        if (mCounts == 0 && isTranscodeQuery == 0) {
            mCounts = getMediaCountFromFile();
            Log.w(TAG, "media_count: " + mCounts);
        }

        return mCounts > MAX_VIDEO_CONTENT ?
                (mCounts = MAX_VIDEO_CONTENT) : mCounts;
    }

    public Video getMedia(Context cts, int index, int width, int height) {
        if (index >= mCounts) {
            Log.w(TAG, "getMedia(): index:" + index + " mCounts:" + mCounts);
            return null;
        }
        query();
        if ((mCursor != null) && mCursor.moveToPosition(index)) {
            Uri uri = ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI, mCursor.getLong(0)); // _ID
            Log.w(TAG, "getMedia(u): " + uri + " (" + TranscoderActivity.getPathFromUri(mCr, uri) + ")");
            Video clip = Video.createFromVideo(cts, uri, width, height);
            clip.setDoubleSided(true);
            clip.setVolume(0, 0);
            return clip;
        } else if (mFiles != null && mCounts > 0) {
            Video clip = Video.createFromVideo(cts, Uri.fromFile(mFiles[index]), width, height);
            Log.w(TAG, "getMedia(f): " + Uri.fromFile(mFiles[index]) + " (" + mFiles[index] + ")");
            clip.setDoubleSided(true);
            clip.setVolume(0, 0);
            return clip;
        }
        return null;
    }

    public void close() {
        if (mCursor != null) {
            mCursor.close();
        }
    }

    public static final String TRANSCODED_TAG_ID = "VOTranscoded";
    public static final String INCLUDED_TAG_ID = "VOLink";
    private static String selectionAll() {
        String query = MediaStore.Video.Media.TAGS + "='" + TRANSCODED_TAG_ID
                + "' OR " + MediaStore.Video.Media.TAGS + "='" + INCLUDED_TAG_ID + "'";
        Log.v(TAG, "selectionAll() : " + query);
        return query;
    }

    private static String selectionNotCurVer(int ver) {
        String query = MediaStore.Video.Media.TAGS + " = '" + TRANSCODED_TAG_ID + "' AND " +
                       MediaStore.Video.Media.CATEGORY + " <> " + ver;
        Log.v(TAG, "selectionNotCurVer() : " + query);
        return query;
    }

    private static String selectionCurVer(int ver) {
        String query = MediaStore.Video.Media.TAGS + " = '" + TRANSCODED_TAG_ID + "' AND " +
                MediaStore.Video.Media.CATEGORY + " = " + ver;
        Log.v(TAG, "selectionNotCurVer() : " + query);
        return query;
    }

    private static String sortOrder() {
        return MediaStore.Video.Media.DATE_TAKEN + " DESC LIMIT 8";
    }

    public Uri getMediaUri(Context ctx, int index) {
        if (index >= mCounts) {
            return null;
        }
        query();
        if (mCursor == null || !mCursor.moveToPosition(index)) {
            return null;
        }

        return ContentUris.withAppendedId(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI, mCursor.getLong(0));
    }

    public static boolean hasTranscoded(ContentResolver cr, int ver) {
        if (cr == null)
            return false;

        // Query transcoded
        Cursor cursor = cr.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                PROJECTION, selectionCurVer(ver), null, sortOrder());

        Log.v(TAG, "cr.query result : " + cursor);
        if (null == cursor) {
            return false;
        }

        if (cursor.getCount() == 0) {
            cursor.close();
            Log.v(TAG, "hasTrascoded : no transcoded");
            return false;
        }

        // Get first transcoded taken date.
        ArrayList<Long> transcodeTime = new ArrayList<Long>();
        cursor.moveToFirst();
        do {
            String transcodedDateAdded = cursor.getString(1); // DateAdded
            transcodeTime.add(Long.valueOf(transcodedDateAdded));
            Log.v(TAG, "hasTrascoded : transcoded dateModified : " + transcodedDateAdded);
        } while (cursor.moveToNext());
        cursor.close();

        // Query external except transcoded
        cursor = ExternalMediaSource.queryLatest8(cr);
        if (cursor.getCount() == 0) {
            cursor.close();
            Log.v(TAG, "hasTrascoded : no external");
            // if there is no external sources,
            // which means we should re-config the transcoded videos.
            return false;
        }

        ArrayList<Long> externalTime = new ArrayList<Long>();
        cursor.moveToFirst();
        do {
            String externalDateTaken = cursor.getString(1); // DateTaken
            externalTime.add(Long.valueOf(externalDateTaken));
            Log.v(TAG, "hasTrascoded : external dateTaken : " + externalDateTaken);
        } while(cursor.moveToNext());
        cursor.close();

        boolean needTranscode = false;
        if (transcodeTime.size() != externalTime.size()) {
            needTranscode = true;
        } else {
            for (int i = 0; i < transcodeTime.size(); ++i) {
                if (!transcodeTime.get(i).equals(externalTime.get(i)))
                    needTranscode = true;
            }
        }

        Log.v(TAG, "Transcode count : " + transcodeTime.size() +
                ", external count : " + externalTime.size() + ", hasTranscoded : " + !needTranscode);
        return !needTranscode;
    }

    // Remove all invalid sources.
    public static void removeNonCurVersionData(ContentResolver cr, int ver) {
        ArrayList<Uri> invalidate = new ArrayList<Uri>();
        Cursor c = cr.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                PROJECTION, selectionNotCurVer(ver), null, null);
        if (c.getCount() > 0) {
            c.moveToFirst();
            do {
                Uri uri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI, c.getLong(0)); //_ID
                String path = TranscoderActivity.getPathFromUri(cr, uri);
                Log.v(TAG, "Validate : ver : " + ver +  "uri : " + uri + ", path : " + path);
                File f = new File(path);
                if (!f.exists()) {
                    Log.w(TAG, "Validate path doesn't exist : " + path);
                    invalidate.add(uri);
                }
            } while (c.moveToNext());
        }
        c.close();

        for(Uri uri : invalidate) {
            int deletedRow = cr.delete(uri, null, null);
            Log.v(TAG, "Validate : ver " + ver +  ", remove uri (deleted): " + uri + ", row : " + deletedRow);
        }
    }
}