package com.marverenic.music.instances;

import android.provider.MediaStore;

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
}
