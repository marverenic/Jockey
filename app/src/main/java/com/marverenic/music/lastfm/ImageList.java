package com.marverenic.music.lastfm;

import com.google.gson.annotations.SerializedName;

public class ImageList {

    public static final byte SIZE_SMALL = 0;
    public static final byte SIZE_MEDIUM = 1;
    public static final byte SIZE_LARGE = 2;
    public static final byte SIZE_XLARGE = 3;
    public static final byte SIZE_MEGA = 4;

    @SerializedName("small")
    protected String smallUrl;
    @SerializedName("medium")
    protected String mediumUrl;
    @SerializedName("large")
    protected String largeUrl;
    @SerializedName("xlarge")
    protected String xlargeUrl;
    @SerializedName("mega")
    protected String megaUrl;

    public String getUrl(byte size) {
        switch (size) {
            case SIZE_MEGA:
                if (megaUrl != null) return megaUrl;
            case SIZE_XLARGE:
                if (xlargeUrl != null) return xlargeUrl;
            case SIZE_LARGE:
                if (largeUrl != null) return largeUrl;
            case SIZE_MEDIUM:
                if (mediumUrl != null) return mediumUrl;
            case SIZE_SMALL:
                if (smallUrl != null) return smallUrl;
        }
        return null;
    }

}
