package com.marverenic.music.instances;

import android.os.Parcel;
import android.os.Parcelable;

public class Album implements Parcelable {

    public static final Parcelable.Creator<Album> CREATOR = new Parcelable.Creator<Album>() {
        public Album createFromParcel(Parcel in) {
            return new Album(in);
        }

        public Album[] newArray(int size) {
            return new Album[size];
        }
    };

    public int albumId;
    public String albumName;
    public int artistId;
    public String artistName;
    public String year;
    public String artUri;

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
    }
}
