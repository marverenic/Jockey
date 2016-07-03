package com.marverenic.music.instances;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import com.crashlytics.android.Crashlytics;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class AutoPlaylist extends Playlist implements Parcelable {

    /**
     * Value representing an unlimited amount of song entries
     */
    public static final int UNLIMITED_ENTRIES = -1;

    /**
     * An empty auto playlist instance
     */
    public static final AutoPlaylist EMPTY =
            new AutoPlaylist(-1, "", UNLIMITED_ENTRIES, Rule.Field.NAME, Rule.Field.NAME,
                    true, true, true, Rule.EMPTY);

    /**
     * How many items can be stored in this playlist. Default is unlimited
     */
    @SerializedName("maximumEntries")
    protected int maximumEntries;

    /**
     * The field to look at when truncating the playlist. Must be a member of {@link Rule.Field}.
     * {@link Rule.Field#ID} will yield a random trim
     */
    @SerializedName("truncateMethod")
    protected int truncateMethod;

    /**
     * Whether to trim the playlist ascending (A-Z, oldest to newest, or 0-infinity).
     * If false, sort descending (Z-A, newest to oldest, or infinity-0).
     */
    @SerializedName("truncateAscending")
    protected boolean truncateAscending;

    /**
     * Whether or not a song has to match all rules in order to appear in the playlist.
     */
    @SerializedName("matchAllRules")
    protected boolean matchAllRules;

    /**
     * The rules to match when building the playlist
     */
    @SerializedName("rules")
    protected Rule[] rules;

    /**
     * The field to look at when sorting the playlist. Must be a member of {@link Rule.Field} and
     * cannot be {@link Rule.Field#ID}
     */
    @SerializedName("sortMethod")
    protected int sortMethod;

    /**
     * Whether to sort the playlist ascending (A-Z, oldest to newest, or 0-infinity).
     * If false, sort descending (Z-A, newest to oldest, or infinity-0).
     * Default is true.
     */
    @SerializedName("sortAscending")
    protected boolean sortAscending;


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
     *                   {@link PlaylistRule.Field} and can't be ID
     * @param sortAscending Whether to sort this playlist ascending (A-Z or 0-infinity) or not
     * @param matchAllRules Whether or not all rules have to be matched for a song to appear in this
     *                      playlist
     * @param rules The rules that songs must follow in order to appear in this playlist
     */
    public AutoPlaylist(long playlistId, String playlistName, int maximumEntries, int sortMethod,
                         int truncateMethod, boolean truncateAscending, boolean sortAscending,
                         boolean matchAllRules, PlaylistRule... rules) {
        super(playlistId, playlistName);
        this.playlistId = playlistId;
        this.playlistName = playlistName;
        this.maximumEntries = maximumEntries;
        this.matchAllRules = matchAllRules;
        this.rules = rules;
        this.truncateMethod = truncateMethod;
        this.truncateAscending = truncateAscending;
        this.sortMethod = sortMethod;
        this.sortAscending = sortAscending;
    }

    /**
     * Duplicate a playlist. The instantiated playlist will be completely independent of its parent
     * @param playlist The AutoPlaylist to become a copy of
     */
    public AutoPlaylist(AutoPlaylist playlist) {
        this(
                playlist.playlistId,
                playlist.playlistName,
                playlist.maximumEntries,
                playlist.sortMethod,
                playlist.truncateMethod,
                playlist.truncateAscending,
                playlist.sortAscending,
                playlist.matchAllRules);

        this.rules = new PlaylistRule[playlist.rules.length];
        for (int i = 0; i < this.rules.length; i++) {
            this.rules[i] = new PlaylistRule(playlist.rules[i]);
        }
    }

    public int getMaximumEntries() {
        return maximumEntries;
    }

    public int getTruncateMethod() {
        return truncateMethod;
    }

    public boolean isTruncateAscending() {
        return truncateAscending;
    }

    public boolean isMatchAllRules() {
        return matchAllRules;
    }

    public int getSortMethod() {
        return sortMethod;
    }

    public boolean isSortAscending() {
        return sortAscending;
    }

    public void setPlaylistName(String name) {
        this.playlistName = name;
    }

    public void setMaximumEntries(int maximumEntries) {
        this.maximumEntries = maximumEntries;
    }

    public void setTruncateMethod(int truncateMethod) {
        this.truncateMethod = truncateMethod;
    }

    public void setTruncateAscending(boolean truncateAscending) {
        this.truncateAscending = truncateAscending;
    }

    public void setMatchAllRules(boolean matchAllRules) {
        this.matchAllRules = matchAllRules;
    }

    public void setRules(Rule[] rules) {
        this.rules = rules;
    }

    public void setSortMethod(int sortMethod) {
        this.sortMethod = sortMethod;
    }

    public void setSortAscending(boolean sortAscending) {
        this.sortAscending = sortAscending;
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

    /**
     * Used to determine if the rules of this AutoPlaylist are equal to that of another playlist.
     * This is different from .equals() because .equals() looks at ID's only which is required
     * behavior in other areas of the app.
     * @param other The AutoPlaylist to compare to
     * @return true if these AutoPlaylists have the same rules
     */
    public boolean isEqual(AutoPlaylist other) {
        return other == this || other != null
                && other.matchAllRules == this.matchAllRules
                && other.sortAscending == this.sortAscending
                && other.truncateAscending == this.truncateAscending
                && other.maximumEntries == this.maximumEntries
                && other.playlistName.equals(this.playlistName);
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
        maximumEntries = in.readInt();
        matchAllRules = in.readByte() == 1;
        rules = in.createTypedArray(PlaylistRule.CREATOR);
        sortMethod = in.readInt();
        truncateMethod = in.readInt();
        truncateAscending = in.readByte() == 1;
        sortAscending = in.readByte() == 1;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(maximumEntries);
        dest.writeByte((byte) ((matchAllRules) ? 1 : 0));
        dest.writeTypedArray(rules, 0);
        dest.writeInt(sortMethod);
        dest.writeInt(truncateMethod);
        dest.writeByte((byte) ((truncateAscending) ? 1 : 0));
        dest.writeByte((byte) ((sortAscending) ? 1 : 0));
    }

    public PlaylistRule[] getRules() {
        return rules.clone();
    }

}
