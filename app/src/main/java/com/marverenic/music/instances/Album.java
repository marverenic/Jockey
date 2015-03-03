package com.marverenic.music.instances;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;

public class Album implements Parcelable {

    public static final Parcelable.Creator<Album> CREATOR = new Parcelable.Creator<Album>() {
        public Album createFromParcel(Parcel in) {
            return new Album(in);
        }

        public Album[] newArray(int size) {
            return new Album[size];
        }
    };

    @SerializedName("albumId")
    public long albumId;
    @SerializedName("albumName")
    public String albumName;
    @SerializedName("artistId")
    public long artistId;
    @SerializedName("artistName")
    public String artistName;
    @SerializedName("year")
    public String year;
    @SerializedName("artUri")
    public String artUri;
    // Transparent colors are uninitialized
    @SerializedName("artPrimaryPalette")
    public int artPrimaryPalette = 0;
    @SerializedName("artPrimaryTextPalette")
    public int artPrimaryTextPalette = 0;
    @SerializedName("artDetailTextPalette")
    public int artDetailTextPalette = 0;

    public Album(final long albumId, final String albumName, final long artistId, final String artistName, final String year, final String artUri) {
        this.albumId = albumId;
        this.albumName = albumName;
        this.artistId = artistId;
        this.artistName = artistName;
        this.year = year;
        this.artUri = artUri;
    }

    private Album(Parcel in) {
        albumId = in.readLong();
        albumName = in.readString();
        artistName = in.readString();
        year = in.readString();
        artUri = in.readString();
    }

    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Album other = (Album) obj;
        return albumId == other.albumId
                && TextUtils.equals(albumName, other.albumName)
                && TextUtils.equals(artistName, other.artistName)
                && TextUtils.equals(year, other.year)
                && TextUtils.equals(artUri, other.artUri);
    }

    public String toString() {
        return albumName;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(albumId);
        dest.writeString(albumName);
        dest.writeString(artistName);
        dest.writeString(year);
        dest.writeString(artUri);
    }
}
