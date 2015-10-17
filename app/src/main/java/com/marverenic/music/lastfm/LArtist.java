package com.marverenic.music.lastfm;

import com.google.gson.annotations.SerializedName;

public class LArtist {

    @SerializedName("name")
    protected String name;
    @SerializedName("mbid")
    protected String mbid;
    @SerializedName("url")
    protected String url;
    @SerializedName("related")
    protected LArtist[] relatedArtists;
    @SerializedName("tags")
    protected Tag[] tags;
    @SerializedName("bio")
    protected Bio bio;
    @SerializedName("images")
    protected ImageList images;

    protected LArtist() {
        images = new ImageList();
    }

    public String getName() {
        return name;
    }

    public Tag[] getTags() {
        return tags;
    }

    public LArtist[] getRelatedArtists() {
        return relatedArtists;
    }

    public Bio getBio() {
        return bio;
    }

    public String getImageURL(byte size) {
        return images.getUrl(size);
    }

    public String getUrl() {
        return url;
    }
}
