package com.marverenic.music.instances;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

public class Playlist implements Parcelable, Comparable<Playlist> {

    public static final Parcelable.Creator<Playlist> CREATOR = new Parcelable.Creator<Playlist>() {
        public Playlist createFromParcel(Parcel in) {
            return new Playlist(in);
        }

        public Playlist[] newArray(int size) {
            return new Playlist[size];
        }
    };

    @SerializedName("playlistId")
    public int playlistId;
    @SerializedName("playlistName")
    public String playlistName;

    public Playlist(final int playlistId, final String playlistName) {
        this.playlistId = playlistId;
        this.playlistName = playlistName;
    }

    public Playlist(Parcel in) {
        playlistId = in.readInt();
        playlistName = in.readString();
    }

    @Override
    public int hashCode() {
        return playlistId;
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj ||
                (obj != null && obj instanceof Playlist && playlistId == ((Playlist) obj).playlistId);
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
        dest.writeInt(playlistId);
        dest.writeString(playlistName);
    }

    @Override
    public int compareTo(@NonNull Playlist another) {
        if (!getClass().equals(another.getClass())) {
            if (this instanceof AutoPlaylist) {
                return -1;
            }
            if (this instanceof AutoPlaylist) {
                return 1;
            }
        }
        return playlistName.compareToIgnoreCase(another.playlistName);
    }
}
