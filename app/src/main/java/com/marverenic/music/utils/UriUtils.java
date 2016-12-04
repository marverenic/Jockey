package com.marverenic.music.utils;

import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;

import java.util.List;

public class UriUtils {

    private static final String[] NAME_PROJECTION = {OpenableColumns.DISPLAY_NAME};
    private static final String DOCUMENTS_AUTHORITY = "com.android.externalstorage.documents";
    private static final String DOWNLOADS_AUTHORITY = "com.android.providers.downloads.documents";
    private static final String MEDIA_AUTHORITY = "com.android.providers.media.documents";

    public static String getDisplayName(Context context, Uri uri) {
        Cursor cursor = null;

        try {
            cursor = context.getContentResolver().query(uri, NAME_PROJECTION, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                int nameColumn = cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME);
                String name = cursor.getString(nameColumn);

                if (name.trim().isEmpty()) {
                    return getFileName(uri);
                } else {
                    return name;
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return getFileName(uri);
    }

    public static String getFileName(Uri uri) {
        List<String> segments = uri.getPathSegments();
        return (segments.isEmpty()) ? "" : segments.get(segments.size() - 1);
    }

    public static String getPathFromUri(Context context, Uri contentUri) {
        if ("file".equals(contentUri.getScheme())) {
            return contentUri.getPath();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            String docProviderPath = getPathFromDocumentProviderUri(context, contentUri);
            if (docProviderPath != null && !docProviderPath.trim().isEmpty()) {
                return docProviderPath;
            }
        }

        if ("content".equals(contentUri.getPath())) {
            return getPathFromGeneralUri(context, contentUri);
        }

        return null;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static String getPathFromDocumentProviderUri(Context context, Uri uri) {
        if (!DocumentsContract.isDocumentUri(context, uri)) {
            return null;
        }

        if (DOCUMENTS_AUTHORITY.equals(uri.getAuthority())) {
            // External storage provider
            String docId = DocumentsContract.getDocumentId(uri);
            String[] split = docId.split(":");
            String type = split[0];

            if ("primary".equalsIgnoreCase(type)) {
                return Environment.getExternalStorageDirectory() + "/" + split[1];
            } else {
                return null;
            }
        } else if (DOWNLOADS_AUTHORITY.equals(uri.getAuthority())) {
            // Downloads provider
            String id = DocumentsContract.getDocumentId(uri);
            Uri contentUri = ContentUris.withAppendedId(
                    Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

            return getPathFromGeneralUri(context, contentUri);
        } else if (MEDIA_AUTHORITY.equals(uri.getAuthority())) {
            // Media Provider – assumes that only music URIs will be passed in
            String id = DocumentsContract.getDocumentId(uri).split(":")[1];
            return getPathFromMediaStore(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
        } else {
            return null;
        }
    }

    private static String getPathFromMediaStore(Context context, Uri uri, String id) {
        String[] projection = {MediaStore.MediaColumns.DATA};
        String selection = MediaStore.MediaColumns._ID +  " = ?";
        String[] selectionArgs = {id};

        Cursor cur = context.getContentResolver().query(uri, projection, selection,
                selectionArgs, null);

        if (cur == null) {
            return null;
        }

        String path = null;
        if (cur.moveToFirst()) {
            path = cur.getString(cur.getColumnIndex(MediaStore.MediaColumns.DATA));
        }
        cur.close();

        return path;
    }

    private static String getPathFromGeneralUri(Context context, Uri uri) {
        String[] projection = {MediaStore.MediaColumns.DATA};
        Cursor cur = context.getContentResolver().query(uri, projection, null, null, null);

        if (cur != null) {
            cur.moveToFirst();
            String result = cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.DATA));
            cur.close();
            return result;
        } else {
            return null;
        }
    }
}
