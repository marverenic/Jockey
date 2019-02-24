package com.marverenic.music.model;

import android.content.res.Resources;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.marverenic.music.library.R;

public class ModelUtil {

    /**
     * Checks Strings from ContentResolvers and replaces the default unknown value of
     * {@link MediaStore#UNKNOWN_STRING} with another String if needed
     * @param value The value returned from the ContentResolver
     * @param convertValue The value to replace unknown Strings with
     * @return A String with localized unknown values if needed, otherwise the original value
     */
    public static String parseUnknown(String value, String convertValue) {
        if (value == null || value.equals(MediaStore.UNKNOWN_STRING)) {
            return convertValue;
        } else {
            return value;
        }
    }

    public static int stringToInt(String string, int defaultValue) {
        try {
            return Integer.parseInt(string);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static long stringToLong(String string, long defaultValue) {
        try {
            return Long.parseLong(string);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static int compareLong(long lhs, long rhs) {
        return lhs < rhs ? -1 : (lhs == rhs ? 0 : 1);
    }

    @NonNull
    public static String sortableTitle(@Nullable String title, Resources res) {
        if (title == null) {
            return "";
        }

        String[] ignoredPrefixes = res.getStringArray(R.array.ignored_title_prefixes);
        String cmp = title.toLowerCase();

        for (String prefix : ignoredPrefixes) {
            if (cmp.startsWith(prefix.toLowerCase() + " ")) {
                return cmp.substring(prefix.length() + 1);
            }
        }

        return cmp;
    }

    public static int hashLong(long value) {
        return (int) (value ^ (value >>> 32));
    }
}
