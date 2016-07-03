package com.marverenic.music.instances;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.IntDef;

public class PlaylistRule implements Parcelable {

    public static final int PLAYLIST = 0;
    public static final int SONG = 1;
    public static final int ARTIST = 2;
    public static final int ALBUM = 3;
    public static final int GENRE = 4;

    public static final int ID = 5;
    public static final int NAME = 6;
    public static final int PLAY_COUNT = 7;
    public static final int SKIP_COUNT = 8;
    public static final int YEAR = 9;
    public static final int DATE_ADDED = 10;
    public static final int DATE_PLAYED = 11;

    public static final int EQUALS = 12;
    public static final int NOT_EQUALS = 13;
    public static final int CONTAINS = 14;
    public static final int NOT_CONTAINS = 15;
    public static final int LESS_THAN = 16;
    public static final int GREATER_THAN = 17;

    @IntDef(value = {PLAYLIST, SONG, ARTIST, ALBUM, GENRE})
    public @interface Type {
    }

    @IntDef(value = {ID, NAME, PLAY_COUNT, SKIP_COUNT, YEAR, DATE_ADDED, DATE_PLAYED})
    public @interface Field {
    }

    @IntDef(value = {EQUALS, NOT_EQUALS, CONTAINS, NOT_CONTAINS, LESS_THAN, GREATER_THAN})
    public @interface Match {
    }

    private int type;
    private int match;
    private int field;
    private String value;

    private PlaylistRule(int type, int field, int match, String value) {
        this.type = type;
        this.field = field;
        this.match = match;
        this.value = value;
    }

    public PlaylistRule(PlaylistRule rule) {
        this(rule.type, rule.field, rule.match, rule.value);
    }

    private PlaylistRule(Parcel in) {
        type = in.readInt();
        match = in.readInt();
        field = in.readInt();
        value = in.readString();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PlaylistRule)) {
            return false;
        }
        if (this == o) {
            return true;
        }
        PlaylistRule other = (PlaylistRule) o;
        return this.type == other.type && this.field == other.field && this.match == other.match
                && this.value.equals(other.value);
    }

    @Override
    public int hashCode() {
        int result = type;
        result = 31 * result + match;
        result = 31 * result + field;
        result = 31 * result + value.hashCode();
        return result;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(type);
        dest.writeInt(match);
        dest.writeInt(field);
        dest.writeString(value);
    }

    public static final Creator<PlaylistRule> CREATOR = new Creator<PlaylistRule>() {
        public PlaylistRule createFromParcel(Parcel in) {
            return new PlaylistRule(in);
        }

        public PlaylistRule[] newArray(int size) {
            return new PlaylistRule[size];
        }
    };
}
