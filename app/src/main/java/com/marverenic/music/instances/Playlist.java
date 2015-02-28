package com.marverenic.music.instances;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

public class Playlist implements Parcelable {

    public static final Parcelable.Creator<Playlist> CREATOR = new Parcelable.Creator<Playlist>() {
        public Playlist createFromParcel(Parcel in) {
            return new Playlist(in);
        }

        public Playlist[] newArray(int size) {
            return new Playlist[size];
        }
    };

    public long playlistId;
    public String playlistName;

    public Playlist(final long playlistId, final String playlistName) {
        this.playlistId = playlistId;
        this.playlistName = playlistName;
    }

    private Playlist(Parcel in) {
        playlistId = in.readLong();
        playlistName = in.readString();
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
        final Playlist other = (Playlist) obj;
        return this.playlistId != other.playlistId && TextUtils.equals(playlistName, other.playlistName);
    }

    public String toString() {
        return playlistName;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(playlistId);
        dest.writeString(playlistName);
    }
}
