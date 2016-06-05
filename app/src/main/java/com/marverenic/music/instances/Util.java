package com.marverenic.music.instances;

import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.text.Collator;

class Util {

    /**
     * Checks Strings from ContentResolvers and replaces the default unknown value of
     * {@link MediaStore#UNKNOWN_STRING} with another String if needed
     * @param value The value returned from the ContentResolver
     * @param convertValue The value to replace unknown Strings with
     * @return A String with localized unknown values if needed, otherwise the original value
     */
    protected static String parseUnknown(String value, String convertValue) {
        if (value == null || value.equals(MediaStore.UNKNOWN_STRING)) {
            return convertValue;
        } else {
            return value;
        }
    }

    protected static int compareLong(long lhs, long rhs) {
        return lhs < rhs ? -1 : (lhs == rhs ? 0 : 1);
    }

    protected static int compareTitle(@Nullable String left, @Nullable String right) {
        return Collator.getInstance().compare(sortableTitle(left), sortableTitle(right));
    }

    /**
     * Creates a sortable String from a title, so that leading "the"s and "a"s can be removed. This
     * method will also strip the title's original case.
     * @param title The title to create a sortable String from
     * @return A new String with the same contents of {@code title}, but with any leading articles
     *         removed to conform to English standards.
     */
    @NonNull
    protected static String sortableTitle(@Nullable String title) {
        if (title == null) {
            return "";
        }

        title = title.toLowerCase();

        if (title.startsWith("the ")) {
            return title.substring(4);
        } else if (title.startsWith("a ")) {
            return title.substring(2);
        } else {
            return title;
        }
    }
}
