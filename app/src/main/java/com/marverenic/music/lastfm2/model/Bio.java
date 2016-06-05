package com.marverenic.music.lastfm2.model;

import com.google.gson.annotations.SerializedName;

public class Bio {

    @SerializedName("published")
    private String mPublished;

    @SerializedName("summary")
    private String mSummary;

    @SerializedName("content")
    private String mContent;

    /**
     * This class is instantiated by GSON with reflection, and therefore doesn't have a public
     * constructor
     */
    private Bio() {

    }

    public String getPublished() {
        return mPublished;
    }

    public String getSummary() {
        return mSummary;
    }

    public String getContent() {
        return mContent;
    }
}
