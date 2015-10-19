package com.marverenic.music.instances;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.marverenic.music.Library;

import java.util.Comparator;
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

    public String songName;
    public int songId;
    public String artistName;
    public String albumName;
    public int songDuration;
    public String location;
    public int year;
    public int dateAdded; // seconds since Jan 1, 1970
    public int albumId;
    public int artistId;
    public int genreId = -1;
    public int trackNumber = 0;

    public Song(final String songName, final int songId, final String artistName,
                final String albumName, final int songDuration, final String location,
                final int year, final int dateAdded, final int albumId, final int artistId) {

        this.songName = songName;
        this.songId = songId;
        this.artistName = artistName;
        this.albumName = albumName;
        this.songDuration = songDuration;
        this.location = location;
        this.year = year;
        this.dateAdded = dateAdded;
        this.albumId = albumId;
        this.artistId = artistId;
    }

    private Song(Parcel in) {
        songName = in.readString();
        songId = in.readInt();
        albumName = in.readString();
        artistName = in.readString();
        songDuration = in.readInt();
        location = in.readString();
        year = in.readInt();
        dateAdded = in.readInt();
        albumId = in.readInt();
        artistId = in.readInt();
        genreId = in.readInt();
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

    public int skipCount(){
        return Library.getSkipCount(songId);
    }

    public int playCount(){
        return Library.getPlayCount(songId);
    }

    public int playDate() {
        return Library.getPlayDate(songId);
    }

    @Override
    public int hashCode() {
        return songId;
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj ||
                (obj != null && obj instanceof Song && songId == ((Song) obj).songId);
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
        dest.writeInt(songId);
        dest.writeString(albumName);
        dest.writeString(artistName);
        dest.writeInt(songDuration);
        dest.writeString(location);
        dest.writeInt(year);
        dest.writeInt(dateAdded);
        dest.writeInt(albumId);
        dest.writeInt(artistId);
        dest.writeInt(genreId);
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
            return s2.playCount() - s1.playCount();
        }
    };

    public static final Comparator<Song> SKIP_COUNT_COMPARATOR = new Comparator<Song>() {
        @Override
        public int compare(Song s1, Song s2) {
            return s2.skipCount() - s1.skipCount();
        }
    };

    public static final Comparator<Song> DATE_ADDED_COMPARATOR = new Comparator<Song>() {
        @Override
        public int compare(Song s1, Song s2) {
            return s2.dateAdded - s1.dateAdded;
        }
    };

    public static final Comparator<Song> DATE_PLAYED_COMPARATOR = new Comparator<Song>() {
        @Override
        public int compare(Song s1, Song s2) {
            return s2.playDate() - s1.playDate();
        }
    };

    public static final Comparator<Song> YEAR_COMPARATOR = new Comparator<Song>() {
        @Override
        public int compare(Song s1, Song s2) {
            return s2.year - s1.year;
        }
    };
}
