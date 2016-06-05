package com.marverenic.music.lastfm2.model;

import com.google.gson.annotations.SerializedName;
import com.marverenic.music.instances.Artist;

public class SimilarArtists {

    @SerializedName("artist")
    private Artist[] mArtists;

    /**
     * This class is instantiated by GSON with reflection, and therefore doesn't have a public
     * constructor
     */
    private SimilarArtists() {

    }

    public Artist[] getArtists() {
        return mArtists;
    }

}