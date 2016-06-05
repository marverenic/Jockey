package com.marverenic.music.lastfm.model;

import com.google.gson.annotations.SerializedName;

public class Tag {

    @SerializedName("name")
    private String mName;

    @SerializedName("url")
    private String mUrl;

    /**
     * This class is instantiated by GSON with reflection, and therefore doesn't have a public
     * constructor
     */
    private Tag() {

    }

    public String getName() {
        return mName;
    }

    public String getUrl() {
        return mUrl;
    }
}
