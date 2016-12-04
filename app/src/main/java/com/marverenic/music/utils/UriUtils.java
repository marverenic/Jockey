package com.marverenic.music.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import java.util.List;

public class UriUtils {

    private static final String[] NAME_PROJECTION = {OpenableColumns.DISPLAY_NAME};

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
}
