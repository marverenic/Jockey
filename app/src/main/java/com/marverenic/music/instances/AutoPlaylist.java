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
    private final int maximumEntries;

    /**
     * The field to look at when truncating the playlist. Must be a member of {@link Field}.
     * {@link Field#ID} will yield a random trim
     */
    @SerializedName("truncateMethod")
    private final int truncateMethod;

    /**
     * Whether to trim the playlist ascending (A-Z, oldest to newest, or 0-infinity).
     * If false, sort descending (Z-A, newest to oldest, or infinity-0).
     */
    @SerializedName("truncateAscending")
    private final boolean truncateAscending;

    /**
     * Whether or not a song has to match all rules in order to appear in the playlist.
     */
    @SerializedName("matchAllRules")
    private final boolean matchAllRules;

    /**
     * The rules to match when building the playlist
     */
    @SerializedName("rules")
    private final List<AutoPlaylistRule> rules;

    /**
     * The field to look at when sorting the playlist. Must be a member of {@link Field} and
     * cannot be {@link Field#ID}
     */
    @SerializedName("sortMethod")
    private final int sortMethod;

    /**
     * Whether to sort the playlist ascending (A-Z, oldest to newest, or 0-infinity).
     * If false, sort descending (Z-A, newest to oldest, or infinity-0).
     * Default is true.
     */
    @SerializedName("sortAscending")
    private final boolean sortAscending;

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
        this.playlistId = playlistId;
        this.playlistName = playlistName;
        this.maximumEntries = maximumEntries;
        this.matchAllRules = matchAllRules;
        this.rules = Collections.unmodifiableList(Arrays.asList(rules));
        this.truncateMethod = truncateMethod;
        this.truncateAscending = truncateAscending;
        this.sortMethod = sortMethod;
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
        rules = Collections.unmodifiableList(in.createTypedArrayList(AutoPlaylistRule.CREATOR));
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
        dest.writeTypedList(rules);
        dest.writeInt(sortMethod);
        dest.writeInt(truncateMethod);
        dest.writeByte((byte) ((truncateAscending) ? 1 : 0));
        dest.writeByte((byte) ((sortAscending) ? 1 : 0));
    }

}
