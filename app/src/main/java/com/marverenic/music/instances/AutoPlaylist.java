package com.marverenic.music.instances;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.data.store.PlayCountStore;
import com.marverenic.music.data.store.PlaylistStore;
import com.marverenic.music.instances.playlistrules.AutoPlaylistRule;
import com.marverenic.music.instances.playlistrules.AutoPlaylistRule.Field;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import rx.Observable;

public class AutoPlaylist extends Playlist implements Parcelable {

    /**
     * Value representing an unlimited amount of song entries
     */
    public static final int UNLIMITED_ENTRIES = -1;

    /**
     * How many items can be stored in this playlist. Default is unlimited
     */
    @SerializedName("maximumEntries")
    private final int mMaximumEntries;

    /**
     * The field to look at when truncating the playlist. Must be a member of {@link Field}.
     * {@link Field#ID} will yield a random trim
     */
    @SerializedName("truncateMethod")
    private final int mTruncateMethod;

    /**
     * Whether to trim the playlist ascending (A-Z, oldest to newest, or 0-infinity).
     * If false, sort descending (Z-A, newest to oldest, or infinity-0).
     */
    @SerializedName("truncateAscending")
    private final boolean mTruncateAscending;

    /**
     * Whether or not a song has to match all rules in order to appear in the playlist.
     */
    @SerializedName("matchAllRules")
    private final boolean mMatchAllRules;

    /**
     * The rules to match when building the playlist
     */
    @SerializedName("rules")
    private final List<AutoPlaylistRule> mRules;

    /**
     * The field to look at when sorting the playlist. Must be a member of {@link Field} and
     * cannot be {@link Field#ID}
     */
    @SerializedName("sortMethod")
    private final int mSortMethod;

    /**
     * Whether to sort the playlist ascending (A-Z, oldest to newest, or 0-infinity).
     * If false, sort descending (Z-A, newest to oldest, or infinity-0).
     * Default is true.
     */
    @SerializedName("sortAscending")
    private final boolean mSortAscending;

    /**
     * AutoPlaylist Creator
     * @param playlistId A unique ID for the Auto Playlist. Must be unique and not conflict with the
     *                   MediaStore
     * @param playlistName The name given to this playlist by the user
     * @param maximumEntries The maximum number of songs this playlist should have. Use
     *                       {@link AutoPlaylist#UNLIMITED_ENTRIES} for no limit. This limit will
     *                       be applied after the list has been sorted. Any extra entries will be
     *                       truncated.
     * @param sortMethod The order the songs will be sorted (Must be one of
     *                   {@link Field} and can't be ID
     * @param sortAscending Whether to sort this playlist ascending (A-Z or 0-infinity) or not
     * @param matchAllRules Whether or not all rules have to be matched for a song to appear in this
     *                      playlist
     * @param rules The rules that songs must follow in order to appear in this playlist
     */
    private AutoPlaylist(long playlistId, String playlistName, int maximumEntries, int sortMethod,
                         int truncateMethod, boolean truncateAscending, boolean sortAscending,
                         boolean matchAllRules, List<AutoPlaylistRule> rules) {
        super(playlistId, playlistName);
        mMaximumEntries = maximumEntries;
        mMatchAllRules = matchAllRules;
        mRules = Collections.unmodifiableList(rules);
        mTruncateMethod = truncateMethod;
        mTruncateAscending = truncateAscending;
        mSortMethod = sortMethod;
        mSortAscending = sortAscending;
    }

    public Observable<List<Song>> generatePlaylist(MusicStore musicStore,
                                                   PlaylistStore playlistStore,
                                                   PlayCountStore playCountStore) {

        if (getRules().isEmpty()) {
            return Observable.just(Collections.emptyList());
        }

        Observable<List<Song>> filtered = null;

        for (AutoPlaylistRule rule : getRules()) {
            if (filtered == null) {
                filtered = rule.applyFilter(playlistStore, musicStore, playCountStore);
            } else {
                combineRules(filtered, rule.applyFilter(playlistStore, musicStore, playCountStore));
            }
        }

        return filtered;
    }

    private Observable<List<Song>> combineRules(Observable<List<Song>> result1,
                                                Observable<List<Song>> result2) {

        if (isMatchAllRules()) { // AND
            return Observable.combineLatest(result1, result2, (songs, songs2) -> {
                List<Song> merged = new ArrayList<>(songs);
                merged.retainAll(songs2);
                return merged;
            });
        } else { // OR
            return Observable.combineLatest(result1, result2, (songs, songs2) -> {
                Set<Song> mergedSet = new HashSet<>(songs);
                mergedSet.addAll(songs2);

                return new ArrayList<>(mergedSet);
            });
        }
    }

    public static final Parcelable.Creator<Parcelable> CREATOR =
            new Parcelable.Creator<Parcelable>() {
                public AutoPlaylist createFromParcel(Parcel in) {
                    return new AutoPlaylist(in);
                }

                public AutoPlaylist[] newArray(int size) {
                    return new AutoPlaylist[size];
                }
            };

    private AutoPlaylist(Parcel in) {
        super(in);
        mMaximumEntries = in.readInt();
        mMatchAllRules = in.readByte() == 1;
        mRules = Collections.unmodifiableList(in.createTypedArrayList(AutoPlaylistRule.CREATOR));
        mSortMethod = in.readInt();
        mTruncateMethod = in.readInt();
        mTruncateAscending= in.readByte() == 1;
        mSortAscending = in.readByte() == 1;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(mMaximumEntries);
        dest.writeByte((byte) ((mMatchAllRules) ? 1 : 0));
        dest.writeTypedList(mRules);
        dest.writeInt(mSortMethod);
        dest.writeInt(mTruncateMethod);
        dest.writeByte((byte) ((mTruncateAscending) ? 1 : 0));
        dest.writeByte((byte) ((mSortAscending) ? 1 : 0));
    }

    public int getMaximumEntries() {
        return mMaximumEntries;
    }

    public int getTruncateMethod() {
        return mTruncateMethod;
    }

    public boolean isTruncateAscending() {
        return mTruncateAscending;
    }

    public boolean isMatchAllRules() {
        return mMatchAllRules;
    }

    public List<AutoPlaylistRule> getRules() {
        return mRules;
    }

    public int getSortMethod() {
        return mSortMethod;
    }

    public boolean isSortAscending() {
        return mSortAscending;
    }

    public static class Builder implements Parcelable {

        public static final long NO_ID = -1;

        private long mId;
        private String mName;

        private int mMaximumEntries;
        private int mTruncateMethod;
        private boolean mTruncateAscending;
        private boolean mMatchAllRules;
        private List<AutoPlaylistRule> mRules;
        private int mSortMethod;
        private boolean mSortAscending;

        public Builder() {
            mId = NO_ID;
        }

        public Builder(AutoPlaylist from) {
            mId = from.getPlaylistId();
            mName = from.getPlaylistName();
            mMaximumEntries = from.getMaximumEntries();
            mTruncateMethod = from.getTruncateMethod();
            mTruncateAscending = from.isTruncateAscending();
            mMatchAllRules = from.isMatchAllRules();
            mRules = new ArrayList<>(from.getRules());
            mSortMethod = from.getSortMethod();
            mSortAscending = from.isSortAscending();
        }

        protected Builder(Parcel in) {
            mId = in.readLong();
            mName = in.readString();
            mMaximumEntries = in.readInt();
            mTruncateMethod = in.readInt();
            mTruncateAscending = in.readByte() != 0;
            mMatchAllRules = in.readByte() != 0;
            mRules = in.createTypedArrayList(AutoPlaylistRule.CREATOR);
            mSortMethod = in.readInt();
            mSortAscending = in.readByte() != 0;
        }

        public static final Creator<Builder> CREATOR = new Creator<Builder>() {
            @Override
            public Builder createFromParcel(Parcel in) {
                return new Builder(in);
            }

            @Override
            public Builder[] newArray(int size) {
                return new Builder[size];
            }
        };

        public long getId() {
            return mId;
        }

        public Builder setId(long id) {
            mId = id;
            return this;
        }

        public String getName() {
            return mName;
        }

        public Builder setName(String name) {
            mName = name;
            return this;
        }

        public int getMaximumEntries() {
            return mMaximumEntries;
        }

        public Builder setMaximumEntries(int maximumEntries) {
            mMaximumEntries = maximumEntries;
            return this;
        }

        public int getTruncateMethod() {
            return mTruncateMethod;
        }

        public Builder setTruncateMethod(int truncateMethod) {
            mTruncateMethod = truncateMethod;
            return this;
        }

        public boolean isTruncateAscending() {
            return mTruncateAscending;
        }

        public Builder setTruncateAscending(boolean truncateAscending) {
            mTruncateAscending = truncateAscending;
            return this;
        }

        public boolean isMatchAllRules() {
            return mMatchAllRules;
        }

        public Builder setMatchAllRules(boolean matchAllRules) {
            mMatchAllRules = matchAllRules;
            return this;
        }

        public List<AutoPlaylistRule> getRules() {
            return mRules;
        }

        public Builder setRules(AutoPlaylistRule... rules) {
            return setRules(new ArrayList<>(Arrays.asList(rules)));
        }

        public Builder setRules(List<AutoPlaylistRule> rules) {
            mRules = rules;
            return this;
        }

        public int getSortMethod() {
            return mSortMethod;
        }

        public Builder setSortMethod(int sortMethod) {
            mSortMethod = sortMethod;
            return this;
        }

        public boolean isSortAscending() {
            return mSortAscending;
        }

        public Builder setSortAscending(boolean sortAscending) {
            mSortAscending = sortAscending;
            return this;
        }

        public boolean isEqual(AutoPlaylist reference) {
            return getId() == reference.getPlaylistId()
                    && getName().equals(reference.getPlaylistName())
                    && getMaximumEntries() == reference.getMaximumEntries()
                    && getTruncateMethod() == reference.getTruncateMethod()
                    && isTruncateAscending() == reference.isTruncateAscending()
                    && isMatchAllRules() == reference.isMatchAllRules()
                    && getRules().equals(reference.getRules())
                    && getSortMethod() == reference.getSortMethod()
                    && isSortAscending() == reference.isSortAscending();
        }

        public AutoPlaylist build() {
            return new AutoPlaylist(mId, mName, mMaximumEntries, mSortMethod, mTruncateMethod,
                    mTruncateAscending, mSortAscending, mMatchAllRules, mRules);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeLong(mId);
            parcel.writeString(mName);
            parcel.writeInt(mMaximumEntries);
            parcel.writeInt(mTruncateMethod);
            parcel.writeByte((byte) (mTruncateAscending ? 1 : 0));
            parcel.writeByte((byte) (mMatchAllRules ? 1 : 0));
            parcel.writeTypedList(mRules);
            parcel.writeInt(mSortMethod);
            parcel.writeByte((byte) (mSortAscending ? 1 : 0));
        }
    }

}
