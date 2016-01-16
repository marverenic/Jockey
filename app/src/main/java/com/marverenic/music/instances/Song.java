package com.marverenic.music.instances;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.annotation.NonNull;

import com.marverenic.music.R;
import com.marverenic.music.utils.Util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

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
     *            {@link Library#songProjection}. The caller is responsible for closing this Cursor.
     * @param res A {@link Resources} Object from {@link Context#getResources()} used to get the
     *            default values if an unknown value is encountered
     * @return A List of songs populated by entries in the Cursor
     */
    protected static List<Song> buildSongList(Cursor cur, Resources res) {
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
            next.songName = Library.parseUnknown(cur.getString(titleIndex), unknownSong);
            next.songId = cur.getLong(idIndex);
            next.artistName = Library.parseUnknown(cur.getString(artistIndex), unknownArtist);
            next.albumName = Library.parseUnknown(cur.getString(albumIndex), unknownAlbum);
            next.songDuration = cur.getLong(durationIndex);
            next.location = Library.parseUnknown(cur.getString(dataIndex), unknownData);
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
        return Util.hashLong(songId);
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
        String o1c = songName.toLowerCase(Locale.ENGLISH);
        String o2c = another.songName.toLowerCase(Locale.ENGLISH);

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

    public static final Comparator<Song> ARTIST_COMPARATOR = new Comparator<Song>() {
        @Override
        public int compare(Song s1, Song s2) {
            String o1c = s1.artistName.toLowerCase(Locale.ENGLISH);
            String o2c = s2.artistName.toLowerCase(Locale.ENGLISH);
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
            if (!o1c.matches("[a-z]") && o2c.matches("[a-z]")) {
                return o2c.compareTo(o1c);
            }
            return o1c.compareTo(o2c);
        }
    };

    public static final Comparator<Song> ALBUM_COMPARATOR = new Comparator<Song>() {
        @Override
        public int compare(Song o1, Song o2) {
            String o1c = o1.albumName.toLowerCase(Locale.ENGLISH);
            String o2c = o2.albumName.toLowerCase(Locale.ENGLISH);
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
            if (!o1c.matches("[a-z]") && o2c.matches("[a-z]")) {
                return o2c.compareTo(o1c);
            }
            return o1c.compareTo(o2c);
        }
    };

    public static final Comparator<Song> PLAY_COUNT_COMPARATOR = new Comparator<Song>() {
        @Override
        public int compare(Song s1, Song s2) {
            return s2.getPlayCount() - s1.getPlayCount();
        }
    };

    public static final Comparator<Song> SKIP_COUNT_COMPARATOR = new Comparator<Song>() {
        @Override
        public int compare(Song s1, Song s2) {
            return s2.getSkipCount() - s1.getSkipCount();
        }
    };

    public static final Comparator<Song> DATE_ADDED_COMPARATOR = new Comparator<Song>() {
        @Override
        public int compare(Song s1, Song s2) {
            return s1.dateAdded < s2.dateAdded ? -1 : (s1.dateAdded == s2.dateAdded ? 0 : 1);
        }
    };

    public static final Comparator<Song> DATE_PLAYED_COMPARATOR = new Comparator<Song>() {
        @Override
        public int compare(Song s1, Song s2) {
            return s2.getPlayDate() - s1.getPlayDate();
        }
    };

    public static final Comparator<Song> YEAR_COMPARATOR = new Comparator<Song>() {
        @Override
        public int compare(Song s1, Song s2) {
            return s2.year - s1.year;
        }
    };
}
