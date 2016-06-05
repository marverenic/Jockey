package com.marverenic.music.lastfm2.model;

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

    public String getSize() {
        return mSize;
    }

    /**
     * Class of enumerated values that may be used as image sizes returned by the Last.fm server.
     * @see Image#mSize
     */
    public static final class Size {

        public static final String SMALL = "small";
        public static final String MEDIUM = "medium";
        public static final String LARGE = "large";
        public static final String X_LARGE = "extralarge";
        public static final String MEGA = "mega";

        /**
         * This class is never instantiated
         */
        private Size() {

        }

    }

}
