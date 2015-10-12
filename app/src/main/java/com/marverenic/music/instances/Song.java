package com.marverenic.music.instances;

import android.os.Parcel;
import android.os.Parcelable;

import com.marverenic.music.Library;

public class Song implements Parcelable {

    public static final Parcelable.Creator<Song> CREATOR = new Parcelable.Creator<Song>() {
        public Song createFromParcel(Parcel in) {
            return new Song(in);
        }

        public Song[] newArray(int size) {
            return new Song[size];
        }
    };

    public String songName;
    public int songId;
    public String artistName;
    public String albumName;
    public int songDuration;
    public String location;
    public int year;
    public int dateAdded; // seconds since Jan 1, 1970
    public int albumId;
    public int artistId;
    public int genreId = -1;
    public int trackNumber = 0;

    public Song(final String songName, final int songId, final String artistName,
                final String albumName, final int songDuration, final String location,
                final int year, final int dateAdded, final int albumId, final int artistId) {

        this.songName = songName;
        this.songId = songId;
        this.artistName = artistName;
        this.albumName = albumName;
        this.songDuration = songDuration;
        this.location = location;
        this.year = year;
        this.dateAdded = dateAdded;
        this.albumId = albumId;
        this.artistId = artistId;
    }

    private Song(Parcel in) {
        songName = in.readString();
        songId = in.readInt();
        albumName = in.readString();
        artistName = in.readString();
        songDuration = in.readInt();
        location = in.readString();
        year = in.readInt();
        dateAdded = in.readInt();
        albumId = in.readInt();
        artistId = in.readInt();
        genreId = in.readInt();
    }

    public Song(Song s) {
        this.songName = s.songName;
        this.songId = s.songId;
        this.artistName = s.artistName;
        this.albumName = s.albumName;
        this.songDuration = s.songDuration;
        this.location = s.location;
        this.year = s.year;
        this.dateAdded = s.dateAdded;
        this.albumId = s.albumId;
        this.artistId = s.artistId;
        this.genreId = s.genreId;
        this.trackNumber = s.trackNumber;
    }

    public int skipCount(){
        return Library.getSkipCount(songId);
    }

    public int playCount(){
        return Library.getPlayCount(songId);
    }

    public int playDate() {
        return Library.getPlayDate(songId);
    }

    @Override
    public int hashCode() {
        return songId;
    }

    @Override
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
        dest.writeInt(songId);
        dest.writeString(albumName);
        dest.writeString(artistName);
        dest.writeInt(songDuration);
        dest.writeString(location);
        dest.writeInt(year);
        dest.writeInt(dateAdded);
        dest.writeInt(albumId);
        dest.writeInt(artistId);
        dest.writeInt(genreId);
    }
}
