package com.marverenic.music.lastfm;

import com.google.gson.annotations.SerializedName;

public class Bio {

    @SerializedName("date")
    protected String date;
    @SerializedName("summary")
    protected String summary;
    @SerializedName("content")
    protected String content;

    protected Bio() {

    }

    public String getDate() {
        return date;
    }

    public String getSummary() {
        return summary;
    }

    public String getContent() {
        return content;
    }
}
