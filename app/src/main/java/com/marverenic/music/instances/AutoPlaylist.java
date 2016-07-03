package com.marverenic.music.instances;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;
import com.marverenic.music.instances.playlistrules.AutoPlaylistRule;
import com.marverenic.music.instances.playlistrules.AutoPlaylistRule.Field;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
                         boolean matchAllRules, AutoPlaylistRule... rules) {
        super(playlistId, playlistName);
        mMaximumEntries = maximumEntries;
        mMatchAllRules = matchAllRules;
        mRules = Collections.unmodifiableList(Arrays.asList(rules));
        mTruncateMethod = truncateMethod;
        mTruncateAscending = truncateAscending;
        mSortMethod = sortMethod;
        mSortAscending = sortAscending;
    }

    /**
     * Generate the list of songs that match all rules for this playlist.
     * @param context A {@link Context} used for various operations like reading play counts and
     *                checking playlist rules
     * @return An {@link ArrayList} of Songs that contains all songs in the library which match
     *         the rules of this playlist
     */
    public List<Song> generatePlaylist(Context context) {
        return null;
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

    public static class Builder {

        private long mId;
        private String mName;

        private int mMaximumEntries;
        private int mTruncateMethod;
        private boolean mTruncateAscending;
        private boolean mMatchAllRules;
        private AutoPlaylistRule[] mRules;
        private int mSortMethod;
        private boolean mSortAscending;

        public Builder() {

        }

        public Builder(AutoPlaylist from) {
            mId = from.getPlaylistId();
            mName = from.getPlaylistName();
            mMaximumEntries = from.getMaximumEntries();
            mTruncateMethod = from.getTruncateMethod();
            mTruncateAscending = from.isTruncateAscending();
            mMatchAllRules = from.isMatchAllRules();
            mRules = from.getRules().toArray(new AutoPlaylistRule[from.getRules().size()]);
            mSortMethod = from.getSortMethod();
            mSortAscending = from.isSortAscending();
        }

        public long getId() {
            return mId;
        }

        public void setId(long id) {
            mId = id;
        }

        public String getName() {
            return mName;
        }

        public void setName(String name) {
            mName = name;
        }

        public int getMaximumEntries() {
            return mMaximumEntries;
        }

        public void setMaximumEntries(int maximumEntries) {
            mMaximumEntries = maximumEntries;
        }

        public int getTruncateMethod() {
            return mTruncateMethod;
        }

        public void setTruncateMethod(int truncateMethod) {
            mTruncateMethod = truncateMethod;
        }

        public boolean isTruncateAscending() {
            return mTruncateAscending;
        }

        public void setTruncateAscending(boolean truncateAscending) {
            mTruncateAscending = truncateAscending;
        }

        public boolean isMatchAllRules() {
            return mMatchAllRules;
        }

        public void setMatchAllRules(boolean matchAllRules) {
            mMatchAllRules = matchAllRules;
        }

        public AutoPlaylistRule[] getRules() {
            return mRules;
        }

        public void setRules(AutoPlaylistRule[] rules) {
            mRules = rules;
        }

        public int getSortMethod() {
            return mSortMethod;
        }

        public void setSortMethod(int sortMethod) {
            mSortMethod = sortMethod;
        }

        public boolean isSortAscending() {
            return mSortAscending;
        }

        public void setSortAscending(boolean sortAscending) {
            mSortAscending = sortAscending;
        }

        public AutoPlaylist build() {
            return new AutoPlaylist(mId, mName, mMaximumEntries, mSortMethod, mTruncateMethod,
                    mTruncateAscending, mSortAscending, mMatchAllRules, mRules);
        }

    }

}
