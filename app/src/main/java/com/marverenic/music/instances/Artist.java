package com.marverenic.music.instances;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.annotation.NonNull;

import com.marverenic.music.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Artist implements Parcelable, Comparable<Artist> {

    public static final Parcelable.Creator<Artist> CREATOR = new Parcelable.Creator<Artist>() {
        public Artist createFromParcel(Parcel in) {
            return new Artist(in);
        }

        public Artist[] newArray(int size) {
            return new Artist[size];
        }
    };

    protected int artistId;
    protected String artistName;

    private Artist() {

    }

    public Artist(final int artistId, final String artistName) {
        this.artistId = artistId;
        this.artistName = artistName;
    }

    private Artist(Parcel in) {
        artistId = in.readInt();
        artistName = in.readString();
    }

    /**
     * Builds a {@link List} of Artists from a Cursor
     * @param cur A {@link Cursor} to use when reading the {@link MediaStore}. This Cursor may have
     *            any filters and sorting, but MUST have AT LEAST the columns in
     *            {@link Library#artistProjection}. The caller is responsible for closing this
     *            Cursor.
     * @param res A {@link Resources} Object from {@link Context#getResources()} used to get the
     *            default values if an unknown value is encountered
     * @return A List of artists populated by entries in the Cursor
     */
    public static List<Artist> buildArtistList(Cursor cur, Resources res) {
        List<Artist> artists = new ArrayList<>(cur.getCount());

        final int idIndex = cur.getColumnIndex(MediaStore.Audio.Artists._ID);
        final int artistIndex = cur.getColumnIndex(MediaStore.Audio.Artists.ARTIST);

        final String unknownName = res.getString(R.string.unknown_artist);

        for (int i = 0; i < cur.getCount(); i++) {
            cur.moveToPosition(i);
            Artist next = new Artist();
            next.artistId = cur.getInt(idIndex);
            next.artistName = Library.parseUnknown(cur.getString(artistIndex), unknownName);

            artists.add(next);
        }

        return artists;
    }

    public int getArtistId() {
        return artistId;
    }

    public String getArtistName() {
        return artistName;
    }

    @Override
    public int hashCode() {
        return artistId;
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj ||
                (obj != null && obj instanceof Artist && artistId == ((Artist) obj).artistId);
    }

    public String toString() {
        return artistName;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(artistId);
        dest.writeString(artistName);
    }

    @Override
    public int compareTo(@NonNull Artist another) {
        String o1c = (artistName == null)
                ? ""
                : artistName.toLowerCase(Locale.ENGLISH);
        String o2c = (another.artistName == null)
                ? ""
                : another.artistName.toLowerCase(Locale.ENGLISH);
        if (o1c.startsWith("the ")) {
            o1c = o1c.substring(4);
        } else if (o1c.startsWith("a ")) {
            o1c = o1c.substring(2);
        }
        if (o2c.startsWith("the ")) {
            o2c = o2c.substring(4);
        } else if (o2c.startsWith("a ")) {
            o2c = o2c.substring(2);
        }
        return o1c.compareTo(o2c);
    }
}
