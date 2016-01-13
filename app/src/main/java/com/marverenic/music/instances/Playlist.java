package com.marverenic.music.instances;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;
import com.marverenic.music.utils.Util;

import java.util.ArrayList;
import java.util.List;

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
    protected long playlistId;
    @SerializedName("playlistName")
    protected String playlistName;

    protected Playlist(long playlistId, String playlistName) {
        this.playlistId = playlistId;
        this.playlistName = playlistName;
    }

    public Playlist(Parcel in) {
        playlistId = in.readLong();
        playlistName = in.readString();
    }

    /**
     * Builds a {@link List} of Songs from a Cursor. Any {@link AutoPlaylist}s on the filesystem
     * are ignored by this scan and will be loaded into the List as a regular playlist.
     * @param cur A {@link Cursor} to use when reading the {@link MediaStore}. This Cursor may have
     *            any filters and sorting, but MUST have AT LEAST the columns in
     *            {@link Library#playlistProjection}. The caller is responsible for closing this
     *            Cursor.
     * @return A List of songs populated by entries in the Cursor
     */
    public static List<Playlist> buildPlaylistList(Cursor cur) {
        List<Playlist> playlists = new ArrayList<>(cur.getCount());

        final int idIndex = cur.getColumnIndex(MediaStore.Audio.Playlists._ID);
        final int nameIndex = cur.getColumnIndex(MediaStore.Audio.Playlists.NAME);

        for (int i = 0; i < cur.getCount(); i++) {
            cur.moveToPosition(i);
            Playlist next = new Playlist(cur.getLong(idIndex), cur.getString(nameIndex));

            playlists.add(next);
        }
        return playlists;
    }

    public long getPlaylistId() {
        return playlistId;
    }

    public String getPlaylistName() {
        return playlistName;
    }

    @Override
    public int hashCode() {
        return Util.hashLong(playlistId);
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj
                || (obj != null && obj instanceof Playlist
                && playlistId == ((Playlist) obj).playlistId);
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
