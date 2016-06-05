package com.marverenic.music.lastfm.model;

import com.google.gson.annotations.SerializedName;

public class SimilarArtists {

    @SerializedName("artist")
    private LfmArtist[] mArtists;

    /**
     * This class is instantiated by GSON with reflection, and therefore doesn't have a public
     * constructor
     */
    private SimilarArtists() {

    }

    public LfmArtist[] getArtists() {
        return mArtists;
    }

}
