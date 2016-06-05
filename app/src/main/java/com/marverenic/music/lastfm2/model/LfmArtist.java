package com.marverenic.music.lastfm2.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;
import com.marverenic.music.instances.Artist;

public class LfmArtist {

    @SerializedName("name")
    private String mName;

    @SerializedName("mbid")
    private String mMbid;

    @SerializedName("url")
    private String mUrl;

    @SerializedName("image")
    private Image[] mImageList;

    @SerializedName("similar")
    private SimilarArtists mSimilarArtists;

    @SerializedName("tags")
    private Tags mTags;

    @SerializedName("bio")
    private Bio mBio;

    /**
     * This class is instantiated by GSON with reflection, and therefore doesn't have a public
     * constructor
     */
    private LfmArtist() {

    }

    public String getName() {
        return mName;
    }

    public String getMbid() {
        return mMbid;
    }

    public String getUrl() {
        return mUrl;
    }

    public Image[] getImageList() {
        return mImageList;
    }

    @Nullable
    public Image getImageBySize(@NonNull @Image.Size String imageSize) {
        for (Image image : mImageList) {
            if (imageSize.equals(image.getSize())) {
                return image;
            }
        }

        String smaller = Image.smaller(imageSize);
        if (smaller != null) {
            return getImageBySize(smaller);
        } else {
            return null;
        }
    }

    public Artist[] getSimilarArtists() {
        return mSimilarArtists.getArtists();
    }

    public Tag[] getTags() {
        return mTags.getTags();
    }

    public Bio getBio() {
        return mBio;
    }
}
