package com.marverenic.music.lastfm2.model;

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

}
