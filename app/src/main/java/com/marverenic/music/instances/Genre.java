package com.marverenic.music.instances;

import android.content.Context;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.annotation.NonNull;

import com.marverenic.music.R;
import com.marverenic.music.utils.Util;

import java.util.ArrayList;
import java.util.List;

public class Genre implements Parcelable, Comparable<Genre> {

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
     *                genres. Since a song's genre isn't accessible from the MediaStore but the
     *                songs in a genre are, a new Cursor will be opened for all genres and each
     *                song in {@link Library#songLib} will have its genre id set to the proper
     *                value. For this reason, when loading Jockey it's important to set the Song
     *                library before calling this method.
     * @param cur A {@link Cursor} to use when reading the {@link MediaStore}. This Cursor may have
     *            any filters and sorting, but MUST have AT LEAST the columns in
     *            {@link Library#genreProjection}. The caller is responsible for closing this
     *            Cursor.
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
            next.genreName = Library.parseUnknown(cur.getString(nameIndex), unknownName);

            // Comparing these two strings using == instead of .equals() is fine here, because
            // if an unknown genre is encountered, the call to Library.parseUnknown(...) will return
            // the same reference to unknownName that was passed in. Because these references are
            // the same and Strings are immutable, using == to compare references is just as
            // reliable as .equals() while being faster. (And in the rare case that a user actually
            // has a genre called "Unknown", it won't be included in this if statement).

            //noinspection StringEquality
            if (next.genreName.isEmpty() || next.genreName == unknownName){
                next.genreId = -1;
            } else {
                associateGenre(context, next);
            }
            
            genres.add(next);
        }
        return genres;
    }

    private static void associateGenre(Context context, Genre genre) {
        // Associate all songs in this genre by setting the genreID field of each song in the genre
        Cursor genreCur = context.getContentResolver().query(
                MediaStore.Audio.Genres.Members.getContentUri("external", genre.genreId),
                new String[]{MediaStore.Audio.Media._ID},
                MediaStore.Audio.Media.IS_MUSIC + " != 0 ", null, null);

        if (genreCur != null) {
            genreCur.moveToFirst();

            final int ID_INDEX = genreCur.getColumnIndex(MediaStore.Audio.Media._ID);
            for (int j = 0; j < genreCur.getCount(); j++) {
                genreCur.moveToPosition(j);
                final Song s = Library.findSongById(genreCur.getInt(ID_INDEX));
                if (s != null) s.genreId = genre.genreId;
            }
            genreCur.close();
        }
    }

    public long getGenreId() {
        return genreId;
    }

    public String getGenreName() {
        return genreName;
    }

    @Override
    public int hashCode() {
        return Util.hashLong(genreId);
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj ||
                (obj != null && obj instanceof Genre && genreId == ((Genre) obj).genreId);
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
        return genreName.compareTo(another.genreName);
    }
}
