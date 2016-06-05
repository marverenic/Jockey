package com.marverenic.music.instances;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.annotation.NonNull;

import com.marverenic.music.R;
import com.marverenic.music.data.store.MediaStoreUtil;
import com.marverenic.music.utils.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.marverenic.music.instances.Util.parseUnknown;

public final class Album implements Parcelable, Comparable<Album> {

    public static final Parcelable.Creator<Album> CREATOR = new Parcelable.Creator<Album>() {
        public Album createFromParcel(Parcel in) {
            return new Album(in);
        }

        public Album[] newArray(int size) {
            return new Album[size];
        }
    };

    protected long albumId;
    protected String albumName;
    protected long artistId;
    protected String artistName;
    protected int year;
    protected String artUri;

    private Album() {

    }

    private Album(Parcel in) {
        albumId = in.readLong();
        albumName = in.readString();
        artistId = in.readLong();
        artistName = in.readString();
        year = in.readInt();
        artUri = in.readString();
    }

    /**
     * Builds a {@link List} of Albums from a Cursor
     * @param cur A {@link Cursor} to use when reading the {@link MediaStore}. This Cursor may have
     *            any filters and sorting, but MUST have AT LEAST the columns in
     *            {@link MediaStoreUtil#ALBUM_PROJECTION}. The caller is responsible for closing
     *            this Cursor.
     * @param res A {@link Resources} Object from {@link Context#getResources()} used to get the
     *            default values if an unknown value is encountered
     * @return A List of albums populated by entries in the Cursor
     */
    public static List<Album> buildAlbumList(Cursor cur, Resources res) {
        List<Album> albums = new ArrayList<>(cur.getCount());

        final int idIndex = cur.getColumnIndex(MediaStore.Audio.Albums._ID);
        final int albumIndex = cur.getColumnIndex(MediaStore.Audio.Albums.ALBUM);
        final int artistIndex = cur.getColumnIndex(MediaStore.Audio.Albums.ARTIST);
        final int artistIdIndex = cur.getColumnIndex(MediaStore.Audio.Media.ARTIST_ID);
        final int yearIndex = cur.getColumnIndex(MediaStore.Audio.Albums.LAST_YEAR);
        final int artIndex = cur.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART);

        final String unknownAlbum = res.getString(R.string.unknown_album);
        final String unknownArtist = res.getString(R.string.unknown_artist);

        for (int i = 0; i < cur.getCount(); i++) {
            cur.moveToPosition(i);
            Album next = new Album();
            next.albumId = cur.getLong(idIndex);
            next.albumName = parseUnknown(cur.getString(albumIndex), unknownAlbum);
            next.artistName = parseUnknown(cur.getString(artistIndex), unknownArtist);
            next.artistId = cur.getLong(artistIdIndex);
            next.year = cur.getInt(yearIndex);
            next.artUri = cur.getString(artIndex);

            albums.add(next);
        }

        return albums;
    }

    public long getAlbumId() {
        return albumId;
    }

    public String getAlbumName() {
        return albumName;
    }

    public long getArtistId() {
        return artistId;
    }

    public String getArtistName() {
        return artistName;
    }

    public int getYear() {
        return year;
    }

    public String getArtUri() {
        return artUri;
    }

    @Override
    public int hashCode() {
        return Util.hashLong(albumId);
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj
                || (obj != null && obj instanceof Album && albumId == ((Album) obj).albumId);
    }

    public String toString() {
        return albumName;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(albumId);
        dest.writeString(albumName);
        dest.writeLong(artistId);
        dest.writeString(artistName);
        dest.writeInt(year);
        dest.writeString(artUri);
    }

    @Override
    public int compareTo(@NonNull Album another) {
        String o1c = (albumName == null)
                ? ""
                : albumName.toLowerCase(Locale.ENGLISH);
        String o2c = (another.albumName == null)
                ? ""
                : another.albumName.toLowerCase(Locale.ENGLISH);
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
