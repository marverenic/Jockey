package com.marverenic.music.instances;

import android.os.Parcel;
import android.os.Parcelable;

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
    public int albumId;
    @SerializedName("albumName")
    public String albumName;
    @SerializedName("artistId")
    public int artistId;
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

    public Album(final int albumId, final String albumName, final int artistId, final String artistName, final String year, final String artUri) {
        this.albumId = albumId;
        this.albumName = albumName;
        this.artistId = artistId;
        this.artistName = artistName;
        this.year = year;
        this.artUri = artUri;
    }

    private Album(Parcel in) {
        albumId = in.readInt();
        albumName = in.readString();
        artistId = in.readInt();
        artistName = in.readString();
        year = in.readString();
        artUri = in.readString();
        artPrimaryPalette = in.readInt();
        artPrimaryTextPalette = in.readInt();
        artDetailTextPalette = in.readInt();
    }

    public boolean equals(final Object obj) {
        return this == obj ||
                (obj != null && obj instanceof Album && albumId == ((Album) obj).albumId);
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
        dest.writeInt(albumId);
        dest.writeString(albumName);
        dest.writeInt(artistId);
        dest.writeString(artistName);
        dest.writeString(year);
        dest.writeString(artUri);
        dest.writeInt(artPrimaryPalette);
        dest.writeInt(artPrimaryTextPalette);
        dest.writeInt(artDetailTextPalette);
    }
}
