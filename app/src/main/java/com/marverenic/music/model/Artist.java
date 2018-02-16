package com.marverenic.music.model;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.annotation.NonNull;

import com.marverenic.music.R;
import com.marverenic.music.data.store.MediaStoreUtil;

import java.util.ArrayList;
import java.util.List;

import static com.marverenic.music.model.ModelUtil.compareTitle;
import static com.marverenic.music.model.ModelUtil.parseUnknown;

public final class Artist implements Parcelable, Comparable<Artist> {

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

    public Artist(Context context, Cursor cur) {
        artistId = cur.getInt(cur.getColumnIndex(MediaStore.Audio.Artists._ID));
        artistName = parseUnknown(
                cur.getString(cur.getColumnIndex(MediaStore.Audio.Artists.ARTIST)),
                context.getResources().getString(R.string.unknown_artist));
    }

    private Artist(Parcel in) {
        artistId = in.readInt();
        artistName = in.readString();
    }

    /**
     * Builds a {@link List} of Artists from a Cursor
     * @param cur A {@link Cursor} to use when reading the {@link MediaStore}. This Cursor may have
     *            any filters and sorting, but MUST have AT LEAST the columns in
     *            {@link MediaStoreUtil#ARTIST_PROJECTION}. The caller is responsible for closing
     *            this Cursor.
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
            next.artistName = parseUnknown(cur.getString(artistIndex), unknownName);

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
        return this == obj
                || (obj != null && obj instanceof Artist && artistId == ((Artist) obj).artistId);
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
        return compareTitle(getArtistName(), another.getArtistName());
    }
}
