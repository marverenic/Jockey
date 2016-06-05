package com.marverenic.music.lastfm2.api.model;

import com.google.gson.annotations.SerializedName;
import com.marverenic.music.lastfm2.api.LastFmService;
import com.marverenic.music.lastfm2.model.LfmArtist;

/**
 * Wrapper class for calls to {@link LastFmService#getArtistInfo(String)} to allow responses to be
 * easily serialized by GSON.
 */
public class LfmArtistResponse {

    @SerializedName("artist")
    private LfmArtist mArtist;

    /**
     * This class is instantiated by GSON with Reflection, and therefore deosn't have a public
     * constructor
     */
    private LfmArtistResponse() {

    }

    /**
     * Gets the Artist that was sent in the call to the server
     * @return The Last.fm Aritst encapsulated by this object
     */
    public LfmArtist getArtist() {
        return mArtist;
    }

}
