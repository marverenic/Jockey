package com.marverenic.music.instances;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

public class Album implements Parcelable {
    public static final Parcelable.Creator<Album> CREATOR = new Parcelable.Creator<Album>() {
        public Album createFromParcel(Parcel in) {
            return new Album(in);
        }

        public Album[] newArray(int size) {
            return new Album[size];
        }
    };
    public String albumId;
    public String albumName;
    public String artistName;
    public String year;
    public String artUri;

    public Album(final String albumId, final String albumName, final String artistName, final String year, final String artUri) {
        this.albumId = albumId;
        this.albumName = albumName;
        this.artistName = artistName;
        this.year = year;
        this.artUri = artUri;
    }

    private Album(Parcel in) {
        albumId = in.readString();
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
        return TextUtils.equals(albumId, other.albumId)
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
        dest.writeString(albumId);
        dest.writeString(albumName);
        dest.writeString(artistName);
        dest.writeString(year);
        dest.writeString(artUri);
    }
}
