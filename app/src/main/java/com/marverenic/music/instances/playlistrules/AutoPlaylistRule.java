package com.marverenic.music.instances.playlistrules;

import android.os.Parcelable;
import android.support.annotation.IntDef;

import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.instances.Song;

import java.util.List;

public abstract class AutoPlaylistRule implements Parcelable {

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

    @Type
    private final int mType;

    @Field
    private final int mField;

    @Match
    private final int mMatch;

    protected AutoPlaylistRule(@Type int type, @Field int field, @Match int match) {
        mType = type;
        mField = field;
        mMatch = match;
    }

    @Type
    public int getType() {
        return mType;
    }

    @Field
    public int getField() {
        return mField;
    }

    @Match
    public int getMatch() {
        return mMatch;
    }

    public abstract String getValue();

    public abstract List<Song> applyFilter(MusicStore musicStore);

    public static class Factory {

        @Type private int mType;
        @Field private int mField;
        @Match private int mMatch;

        private String mValue;

        public Factory() {

        }

        public Factory(AutoPlaylistRule from) {
            mType = from.getType();
            mField = from.getField();
            mMatch = from.getMatch();
        }

        public void setType(@Type int type) {
            mType = type;
        }

        public void setField(@Field int field) {
            mField = field;
        }

        public void setMatch(@Match int match) {
            mMatch = match;
        }

        public void setValue(String value) {
            mValue = value;
        }

        public AutoPlaylistRule build() {
            // TODO implement this once AutoPlaylistRule has been extended
            return null;
        }

    }

}
