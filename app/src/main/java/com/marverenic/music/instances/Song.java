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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.marverenic.music.instances.Util.compareLong;
import static com.marverenic.music.instances.Util.compareTitle;
import static com.marverenic.music.instances.Util.hashLong;
import static com.marverenic.music.instances.Util.parseUnknown;

public class Song implements Parcelable, Comparable<Song> {

    public static final Parcelable.Creator<Song> CREATOR = new Parcelable.Creator<Song>() {
        public Song createFromParcel(Parcel in) {
            return new Song(in);
        }

        public Song[] newArray(int size) {
            return new Song[size];
        }
    };

    protected String songName;
    protected long songId;
    protected String artistName;
    protected String albumName;
    protected long songDuration;
    protected String location;
    protected int year;
    protected long dateAdded; // seconds since Jan 1, 1970
    protected long albumId;
    protected long artistId;
    protected long genreId;
    protected int trackNumber;

    private Song() {

    }

    private Song(Parcel in) {
        songName = in.readString();
        songId = in.readLong();
        albumName = in.readString();
        artistName = in.readString();
        songDuration = in.readLong();
        location = in.readString();
        year = in.readInt();
        dateAdded = in.readLong();
        albumId = in.readLong();
        artistId = in.readLong();
        genreId = in.readLong();
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

    /**
     * Builds a {@link List} of Songs from a Cursor
     * @param cur A {@link Cursor} to use when reading the {@link MediaStore}. This Cursor may have
     *            any filters and sorting, but MUST have AT LEAST the columns in
     *            {@link MediaStoreUtil#SONG_PROJECTION}. The caller is responsible for closing
     *            this Cursor.
     * @param res A {@link Resources} Object from {@link Context#getResources()} used to get the
     *            default values if an unknown value is encountered
     * @return A List of songs populated by entries in the Cursor
     */
    public static List<Song> buildSongList(Cursor cur, Resources res) {
        List<Song> songs = new ArrayList<>(cur.getCount());

        int titleIndex = cur.getColumnIndex(MediaStore.Audio.Media.TITLE);
        int idIndex = cur.getColumnIndex(MediaStore.Audio.Media._ID);
        int artistIndex = cur.getColumnIndex(MediaStore.Audio.Media.ARTIST);
        int albumIndex = cur.getColumnIndex(MediaStore.Audio.Media.ALBUM);
        int durationIndex = cur.getColumnIndex(MediaStore.Audio.Media.DURATION);
        int dataIndex = cur.getColumnIndex(MediaStore.Audio.Media.DATA);
        int yearIndex = cur.getColumnIndex(MediaStore.Audio.Media.YEAR);
        int dateIndex = cur.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED);
        int albumIdIndex = cur.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
        int artistIdIndex = cur.getColumnIndex(MediaStore.Audio.Media.ARTIST_ID);
        int trackIndex = cur.getColumnIndex(MediaStore.Audio.Media.TRACK);

        if (idIndex == -1) {
            idIndex = cur.getColumnIndex(MediaStore.Audio.Playlists.Members.AUDIO_ID);
        }

        final String unknownSong = res.getString(R.string.unknown);
        final String unknownArtist = res.getString(R.string.unknown_artist);
        final String unknownAlbum = res.getString(R.string.unknown_album);
        final String unknownData = "";

        for (int i = 0; i < cur.getCount(); i++) {
            cur.moveToPosition(i);
            Song next = new Song();
            next.songName = parseUnknown(cur.getString(titleIndex), unknownSong);
            next.songId = cur.getLong(idIndex);
            next.artistName = parseUnknown(cur.getString(artistIndex), unknownArtist);
            next.albumName = parseUnknown(cur.getString(albumIndex), unknownAlbum);
            next.songDuration = cur.getLong(durationIndex);
            next.location = parseUnknown(cur.getString(dataIndex), unknownData);
            next.year = cur.getInt(yearIndex);
            next.dateAdded = cur.getLong(dateIndex);
            next.albumId = cur.getLong(albumIdIndex);
            next.artistId = cur.getLong(artistIdIndex);
            next.trackNumber = cur.getInt(trackIndex);

            songs.add(next);
        }

        return songs;
    }

    public String getSongName() {
        return songName;
    }

    public long getSongId() {
        return songId;
    }

    public String getArtistName() {
        return artistName;
    }

    public String getAlbumName() {
        return albumName;
    }

    public long getSongDuration() {
        return songDuration;
    }

    public String getLocation() {
        return location;
    }

    public int getYear() {
        return year;
    }

    public long getDateAdded() {
        return dateAdded;
    }

    public long getAlbumId() {
        return albumId;
    }

    public long getArtistId() {
        return artistId;
    }

    public long getGenreId() {
        return genreId;
    }

    public int getTrackNumber() {
        return trackNumber;
    }

    public int getSkipCount() {
        return Library.getSkipCount(songId);
    }

    public int getPlayCount() {
        return Library.getPlayCount(songId);
    }

    public int getPlayDate() {
        return Library.getPlayDate(songId);
    }

    @Override
    public int hashCode() {
        return hashLong(songId);
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj
                || (obj != null && obj instanceof Song && songId == ((Song) obj).songId);
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
        dest.writeLong(songId);
        dest.writeString(albumName);
        dest.writeString(artistName);
        dest.writeLong(songDuration);
        dest.writeString(location);
        dest.writeInt(year);
        dest.writeLong(dateAdded);
        dest.writeLong(albumId);
        dest.writeLong(artistId);
        dest.writeLong(genreId);
    }

    @Override
    public int compareTo(@NonNull Song another) {
        return compareTitle(getSongName(), another.getSongName());
    }

    public static final Comparator<Song> ARTIST_COMPARATOR = (s1, s2) ->
            compareTitle(s1.getArtistName(), s2.getArtistName());

    public static final Comparator<Song> ALBUM_COMPARATOR = (o1, o2) ->
            compareTitle(o1.getAlbumName(), o2.getAlbumName());

    public static final Comparator<Song> PLAY_COUNT_COMPARATOR = (s1, s2) ->
            s2.getPlayCount() - s1.getPlayCount();

    public static final Comparator<Song> SKIP_COUNT_COMPARATOR = (s1, s2) ->
            s2.getSkipCount() - s1.getSkipCount();

    public static final Comparator<Song> DATE_ADDED_COMPARATOR = (s1, s2) ->
            compareLong(s1.getDateAdded(), s2.getDateAdded());

    public static final Comparator<Song> DATE_PLAYED_COMPARATOR = (s1, s2) ->
            s2.getPlayDate() - s1.getPlayDate();

    public static final Comparator<Song> YEAR_COMPARATOR = (s1, s2) ->
            s2.getYear() - s1.getYear();
}
