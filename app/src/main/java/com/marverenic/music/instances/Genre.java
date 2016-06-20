package com.marverenic.music.instances;

import android.content.Context;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.annotation.NonNull;

import com.marverenic.music.R;
import com.marverenic.music.data.store.MediaStoreUtil;

import java.util.ArrayList;
import java.util.List;

import static com.marverenic.music.instances.Util.compareTitle;
import static com.marverenic.music.instances.Util.hashLong;
import static com.marverenic.music.instances.Util.parseUnknown;

public final class Genre implements Parcelable, Comparable<Genre> {

    public static final Parcelable.Creator<Genre> CREATOR = new Parcelable.Creator<Genre>() {
        public Genre createFromParcel(Parcel in) {
            return new Genre(in);
        }

        public Genre[] newArray(int size) {
            return new Genre[size];
        }
    };

    protected long genreId;
    protected String genreName;

    private Genre() {

    }

    private Genre(Parcel in) {
        genreId = in.readLong();
        genreName = in.readString();
    }

    /**
     * Builds a {@link List} of Genres from a Cursor
     * @param context A Context used to get default unknown values and to associate songs with
     *                genres.
     * @param cur A {@link Cursor} to use when reading the {@link MediaStore}. This Cursor may have
     *            any filters and sorting, but MUST have AT LEAST the columns in
     *            {@link MediaStoreUtil#GENRE_PROJECTION}. The caller is responsible for closing
     *            this Cursor.
     * @return A List of songs populated by entries in the Cursor
     */
    public static List<Genre> buildGenreList(Context context, Cursor cur) {
        List<Genre> genres = new ArrayList<>();

        final int idIndex = cur.getColumnIndex(MediaStore.Audio.Genres._ID);
        final int nameIndex = cur.getColumnIndex(MediaStore.Audio.Genres.NAME);

        final String unknownName = context.getResources().getString(R.string.unknown);

        for (int i = 0; i < cur.getCount(); i++) {
            cur.moveToPosition(i);

            Genre next = new Genre();
            next.genreId = cur.getLong(idIndex);
            next.genreName = parseUnknown(cur.getString(nameIndex), unknownName);

            genres.add(next);
        }
        return genres;
    }

    public long getGenreId() {
        return genreId;
    }

    public String getGenreName() {
        return genreName;
    }

    @Override
    public int hashCode() {
        return hashLong(genreId);
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj
                || (obj != null && obj instanceof Genre && genreId == ((Genre) obj).genreId);
    }

    public String toString() {
        return genreName;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(genreId);
        dest.writeString(genreName);
    }

    @Override
    public int compareTo(@NonNull Genre another) {
        return compareTitle(getGenreName(), another.getGenreName());
    }
}
