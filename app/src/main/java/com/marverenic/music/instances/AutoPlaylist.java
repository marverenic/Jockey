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
     *                   {@link AutoPlaylist.Rule.Field} and can't be ID
     * @param sortAscending Whether to sort this playlist ascending (A-Z or 0-infinity) or not
     * @param matchAllRules Whether or not all rules have to be matched for a song to appear in this
     *                      playlist
     * @param rules The rules that songs must follow in order to appear in this playlist
     */
    public AutoPlaylist(long playlistId, String playlistName, int maximumEntries, int sortMethod,
                         int truncateMethod, boolean truncateAscending, boolean sortAscending,
                         boolean matchAllRules, Rule... rules) {
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

        this.rules = new Rule[playlist.rules.length];
        for (int i = 0; i < this.rules.length; i++) {
            this.rules[i] = new Rule(playlist.rules[i]);
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
        rules = in.createTypedArray(Rule.CREATOR);
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

    public Rule[] getRules() {
        return rules.clone();
    }

    public static class Rule implements Parcelable {

        public static final class Type {
            public static final int PLAYLIST = 0;
            public static final int SONG = 1;
            public static final int ARTIST = 2;
            public static final int ALBUM = 3;
            public static final int GENRE = 4;
        }
        public static final class Field {
            public static final int ID = 5;
            public static final int NAME = 6;
            public static final int PLAY_COUNT = 7;
            public static final int SKIP_COUNT = 8;
            public static final int YEAR = 9;
            public static final int DATE_ADDED = 10;
            public static final int DATE_PLAYED = 11;
        }
        public static final class Match {
            public static final int EQUALS = 12;
            public static final int NOT_EQUALS = 13;
            public static final int CONTAINS = 14;
            public static final int NOT_CONTAINS = 15;
            public static final int LESS_THAN = 16;
            public static final int GREATER_THAN = 17;
        }

        public static final Rule EMPTY = new Rule(Type.SONG, Field.NAME, Match.CONTAINS, "");

        public int type;
        public int match;
        public int field;
        public String value;

        public Rule(int type, int field, int match, String value) {
            validate(type, field, match, value);
            this.type = type;
            this.field = field;
            this.match = match;
            this.value = value;
        }

        public Rule(Rule rule) {
            this(rule.type, rule.field, rule.match, rule.value);
        }

        private Rule(Parcel in) {
            type = in.readInt();
            match = in.readInt();
            field = in.readInt();
            value = in.readString();
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof  Rule)) {
                return false;
            }
            if (this == o) {
                return true;
            }
            Rule other = (Rule) o;
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

        public static final Parcelable.Creator<Rule> CREATOR = new Parcelable.Creator<Rule>() {
            public Rule createFromParcel(Parcel in) {
                return new Rule(in);
            }

            public Rule[] newArray(int size) {
                return new Rule[size];
            }
        };

        private static void validate(int type, int field, int match, String value) {
            // Only Songs have play counts and skip counts
            if ((type != Type.SONG) && (field == Field.PLAY_COUNT || field == Field.SKIP_COUNT)) {
                throw new IllegalArgumentException(type + " type does not have field " + field);
            }
            // Only Songs have years
            if (type != Type.SONG && field == Field.YEAR) {
                throw new IllegalArgumentException(type + " type does not have field " + field);
            }
            // Only Songs have dates added
            if (type != Type.SONG && field == Field.DATE_ADDED) {
                throw new IllegalArgumentException(type + " type does not have field " + field);
            }

            if (field == Field.ID) {
                // IDs can only be compared by equals or !equals
                if (match == Match.CONTAINS || match == Match.NOT_CONTAINS
                        || match == Match.LESS_THAN || match == Match.GREATER_THAN) {
                    throw new IllegalArgumentException("ID cannot be compared by method " + match);
                }
                // Make sure the value is actually a number
                try {
                    //noinspection ResultOfMethodCallIgnored
                    Long.parseLong(value);
                } catch (NumberFormatException e) {
                    Crashlytics.logException(e);
                    throw new IllegalArgumentException("ID cannot be compared to value " + value);
                }
            } else if (field == Field.NAME) {
                // Names can't be compared by < or >... that doesn't even make sense...
                if (match == Match.GREATER_THAN || match == Match.LESS_THAN) {
                    throw new IllegalArgumentException("Name cannot be compared by method "
                            + match);
                }
            } else if (field == Field.SKIP_COUNT || field == Field.PLAY_COUNT
                    || field == Field.YEAR || field == Field.DATE_ADDED) {
                // Numeric values can't be compared by contains or !contains
                if (match == Match.CONTAINS || match == Match.NOT_CONTAINS) {
                    throw new IllegalArgumentException(field + " cannot be compared by method "
                            + match);
                }
                // Make sure the value is actually a number
                try {
                    //noinspection ResultOfMethodCallIgnored
                    Long.parseLong(value);
                } catch (NumberFormatException e) {
                    Crashlytics.logException(e);
                    throw new IllegalArgumentException("ID cannot be compared to value " + value);
                }
            }
        }
    }
}
