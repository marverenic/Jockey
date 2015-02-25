package com.marverenic.music.instances;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

public class Song implements Parcelable {
    public String songName;
    public String artistName;
    public String albumName;
    public int songDuration;
    public String location;
    public String albumId;

    public Song(final String songName, final String artistName, final String albumName, final int songDuration, final String location, final String albumId) {
        this.songName = songName;
        this.artistName = artistName;
        this.albumName = albumName;
        this.songDuration = songDuration;
        this.location = location;
        this.albumId = albumId;
    }

    private Song(Parcel in) {
        albumId = in.readString();
        albumName = in.readString();
        artistName = in.readString();
        songDuration = in.readInt();
        location = in.readString();
        albumId = in.readString();
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
        return TextUtils.equals(albumName, other.albumName) && TextUtils.equals(artistName, other.artistName) && songDuration != other.songDuration && TextUtils.equals(songName, other.songName);
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
        dest.writeString(albumId);
        dest.writeString(albumName);
        dest.writeString(artistName);
        dest.writeInt(songDuration);
        dest.writeString(location);
        dest.writeString(albumId);
    }
}
