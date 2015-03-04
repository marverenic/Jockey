package com.marverenic.music.instances;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;

public class Song implements Parcelable {

    @SerializedName("songName")
    public String songName;
    @SerializedName("songId")
    public long songId;
    @SerializedName("artistName")
    public String artistName;
    @SerializedName("albumName")
    public String albumName;
    @SerializedName("songDuration")
    public int songDuration;
    @SerializedName("location")
    public String location;
    @SerializedName("albumId")
    public long albumId;
    @SerializedName("artistId")
    public long artistId;
    @SerializedName("genreId")
    public long genreId = -1;

    public Song(final String songName, final long songId, final String artistName,
                final String albumName, final int songDuration, final String location,
                final long albumId, final long artistId) {

        this.songName = songName;
        this.songId = songId;
        this.artistName = artistName;
        this.albumName = albumName;
        this.songDuration = songDuration;
        this.location = location;
        this.albumId = albumId;
        this.artistId = artistId;
    }

    private Song(Parcel in) {
        albumName = in.readString();
        artistName = in.readString();
        songDuration = in.readInt();
        location = in.readString();
        albumId = in.readLong();
        artistId = in.readLong();
        genreId = in.readLong();
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
        final Song other = (Song) obj;
        return  artistId == other.artistId && albumId == other.albumId &&
                songDuration == other.songDuration &&
                TextUtils.equals(albumName, other.albumName) &&
                TextUtils.equals(artistName, other.artistName) &&
                TextUtils.equals(songName, other.songName) &&
                TextUtils.equals(location, other.location);
    }

    public String toString() {
        return songName;
    }

    public static final Parcelable.Creator<Song> CREATOR = new Parcelable.Creator<Song>() {
        public Song createFromParcel(Parcel in) {
            return new Song(in);
        }

        public Song[] newArray(int size) {
            return new Song[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(albumName);
        dest.writeString(artistName);
        dest.writeInt(songDuration);
        dest.writeString(location);
        dest.writeLong(albumId);
        dest.writeLong(artistId);
        dest.writeLong(genreId);
    }
}
