package com.marverenic.music.data.api;

import com.google.gson.annotations.SerializedName;

@SuppressWarnings("unused")
public class PrivacyPolicyMetadata {

    // Milliseconds since January 1, 1970 in UTC
    @SerializedName("updated")
    private long lastUpdatedTimestamp;

    public PrivacyPolicyMetadata() {
        this(0);
    }

    public PrivacyPolicyMetadata(long lastUpdatedTimestamp) {
        this.lastUpdatedTimestamp = lastUpdatedTimestamp;
    }

    public long getLastUpdatedTimestamp() {
        return lastUpdatedTimestamp;
    }
}
