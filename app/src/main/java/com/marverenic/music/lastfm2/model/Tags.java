package com.marverenic.music.lastfm2.model;

import com.google.gson.annotations.SerializedName;

public class Tags {

    @SerializedName("tag")
    private Tag[] mTags;

    /**
     * This class is instantiated by GSON with reflection, and therefore doesn't have a public
     * constructor
     */
    public Tags() {

    }

    public Tag[] getTags() {
        return mTags;
    }

}
