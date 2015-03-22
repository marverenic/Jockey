package com.marverenic.music.instances;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

public class Song implements Parcelable {

    public static final Parcelable.Creator<Song> CREATOR = new Parcelable.Creator<Song>() {
        public Song createFromParcel(Parcel in) {
            return new Song(in);
        }

        public Song[] newArray(int size) {
            return new Song[size];
        }
    };

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
    @SerializedName("trackNumber")
    public long trackNumber = 0;
    @SerializedName("playCount")
    public int playCount = 0;
    @SerializedName("skipCount")
    public int skipCount = 0;

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
        songName = in.readString();
        albumName = in.readString();
        artistName = in.readString();
        songDuration = in.readInt();
        location = in.readString();
        albumId = in.readLong();
        artistId = in.readLong();
        genreId = in.readLong();
        playCount = in.readInt();
        skipCount = in.readInt();
    }

    public boolean equals(final Object obj) {
        return this == obj ||
                (obj != null && obj instanceof Song && songId == ((Song) obj).songId);
    }

    public String toString() {
        return songName;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(songName);
        dest.writeString(albumName);
        dest.writeString(artistName);
        dest.writeInt(songDuration);
        dest.writeString(location);
        dest.writeLong(albumId);
        dest.writeLong(artistId);
        dest.writeLong(genreId);
        dest.writeInt(playCount);
        dest.writeInt(skipCount);
    }
}
