package com.marverenic.music.lastfm.model;

import android.support.annotation.Nullable;
import android.support.annotation.StringDef;

import com.google.gson.annotations.SerializedName;

public class Image {

    @SerializedName("#text")
    private String mUrl;

    @SerializedName("size")
    private String mSize;

    /**
     * This class is instantiated by GSON with reflection, and therefore doesn't have a public
     * constructor
     */
    private Image() {

    }

    public String getUrl() {
        return mUrl;
    }

    @Size
    public String getSize() {
        return mSize;
    }

    @Size
    @Nullable
    protected static String smaller(@Size String imageSize) {
        switch (imageSize) {
            case Size.MEGA:
                return Size.X_LARGE;
            case Size.X_LARGE:
                return Size.LARGE;
            case Size.LARGE:
                return Size.MEDIUM;
            case Size.MEDIUM:
                return Size.SMALL;
            case Size.SMALL:
            default:
                return null;
        }
    }

    /**
     * Class of enumerated values that may be used as image sizes returned by the Last.fm server.
     * @see Image#mSize
     */
    @StringDef(value = {Size.SMALL, Size.MEDIUM, Size.LARGE, Size.X_LARGE, Size.MEGA})
    public @interface Size {

        String SMALL = "small";
        String MEDIUM = "medium";
        String LARGE = "large";
        String X_LARGE = "extralarge";
        String MEGA = "mega";

    }

}
