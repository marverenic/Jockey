package com.marverenic.music.instances.playlistrules;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.IntDef;

import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.data.store.PlayCountStore;
import com.marverenic.music.data.store.PlaylistStore;
import com.marverenic.music.instances.Song;

import java.util.List;

import rx.Observable;

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

    private final String mValue;

    protected AutoPlaylistRule(@Type int type, @Field int field, @Match int match, String value) {
        mType = type;
        mField = field;
        mMatch = match;
        mValue = value;
    }

    @SuppressWarnings("WrongConstant")
    protected AutoPlaylistRule(Parcel in) {
        mType = in.readInt();
        mField = in.readInt();
        mMatch = in.readInt();
        mValue = in.readString();
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

    @SuppressLint("SwitchIntDef")
    protected boolean checkId(long actual) {
        switch (getMatch()) {
            case EQUALS:
                return actual == Long.parseLong(getValue());
            case NOT_EQUALS:
                return actual != Long.parseLong(getValue());
        }
        throw new IllegalArgumentException("Cannot compare ids with match type " + getMatch());
    }

    @SuppressLint("SwitchIntDef")
    protected boolean checkString(String actual) {
        switch (getMatch()) {
            case EQUALS:
                return actual.equalsIgnoreCase(getValue());
            case NOT_EQUALS:
                return !actual.equalsIgnoreCase(getValue());
            case CONTAINS:
                return actual.contains(getValue());
            case NOT_CONTAINS:
                return !actual.contains(getValue());
        }
        throw new IllegalArgumentException("Cannot compare Strings with match type " + getMatch());
    }

    @SuppressLint("SwitchIntDef")
    protected boolean checkInt(long actual) {
        switch (getMatch()) {
            case EQUALS:
                return actual == Long.parseLong(getValue());
            case NOT_EQUALS:
                return actual != Long.parseLong(getValue());
            case LESS_THAN:
                return actual < Long.parseLong(getValue());
            case GREATER_THAN:
                return actual > Long.parseLong(getValue());
        }
        throw new IllegalArgumentException("Cannot compare integers with match type" + getMatch());
    }

    public String getValue() {
        return mValue;
    }

    public abstract Observable<List<Song>> applyFilter(PlaylistStore playlistStore,
                                                       MusicStore musicStore,
                                                       PlayCountStore playCountStore);

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(mType);
        parcel.writeInt(mField);
        parcel.writeInt(mMatch);
        parcel.writeString(mValue);
    }

    public static final Creator<AutoPlaylistRule> CREATOR = new Creator<AutoPlaylistRule>() {
        @Override
        public AutoPlaylistRule createFromParcel(Parcel in) {
            //noinspection WrongConstant
            return new Factory()
                    .setType(in.readInt())
                    .setField(in.readInt())
                    .setMatch(in.readInt())
                    .setValue(in.readString())
                    .build();
        }

        @Override
        public AutoPlaylistRule[] newArray(int size) {
            return new AutoPlaylistRule[size];
        }
    };

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

        @Type
        public int getType() {
            return mType;
        }

        public Factory setType(@Type int type) {
            mType = type;
            return this;
        }

        @Field
        public int getField() {
            return mField;
        }

        public Factory setField(@Field int field) {
            mField = field;
            return this;
        }

        @Match
        public int getMatch() {
            return mMatch;
        }

        public Factory setMatch(@Match int match) {
            mMatch = match;
            return this;
        }

        public String getValue() {
            return mValue;
        }

        public Factory setValue(String value) {
            mValue = value;
            return this;
        }

        public AutoPlaylistRule build() {
            switch (mType) {
                case PLAYLIST:
                    return new PlaylistRule(mField, mMatch, mValue);
                case SONG:
                    return new SongRule(mField, mMatch, mValue);
                case ALBUM:
                    return new AlbumRule(mField, mMatch, mValue);
                case ARTIST:
                    return new ArtistRule(mField, mMatch, mValue);
                case GENRE:
                    return new GenreRule(mField, mMatch, mValue);
            }
            throw new IllegalArgumentException("Cannot construct rule over type " + mType);
        }

    }

}
