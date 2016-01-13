package com.marverenic.music.instances;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import com.crashlytics.android.Crashlytics;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
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
        // In the event that the play counts in this process have gone stale, make
        // sure they're current
        Library.loadPlayCounts(context);

        List<Song> songs;
        if (matchAllRules) {
            songs = new ArrayList<>(Library.getSongs());
            for (Rule r : rules) {
                if (r != null) {
                    songs = r.evaluate(songs, context);
                }
            }
        } else {
            HashSet<Song> songSet = new HashSet<>(); // Use a Set to prevent duplicates
            final List<Song> allSongs = new ArrayList<>(Library.getSongs());
            for (Rule r : rules) {
                songSet.addAll(r.evaluate(allSongs, context));
            }
            songs = new ArrayList<>(songSet);
        }
        return sort(
                trim(sort(songs, truncateMethod, truncateAscending)),
                sortMethod, sortAscending);
    }

    /**
     * Sorts an {@link ArrayList} of songs as specified by {@link AutoPlaylist#sortMethod} and
     * {@link AutoPlaylist#sortAscending}. Used in {@link AutoPlaylist#generatePlaylist(Context)}
     * @param in The {@link ArrayList} to be sorted
     * @return The original, sorted, {@link ArrayList} for convenience
     */
    private static List<Song> sort(List<Song> in, int sortMethod,
                                        final boolean sortAscending) {
        switch (sortMethod) {
            case Rule.Field.ID:
                Collections.shuffle(in);
                break;
            case Rule.Field.PLAY_COUNT:
                Collections.sort(in, Song.PLAY_COUNT_COMPARATOR);
                break;
            case Rule.Field.SKIP_COUNT:
                Collections.sort(in, Song.SKIP_COUNT_COMPARATOR);
                break;
            case Rule.Field.DATE_ADDED:
                Collections.sort(in, Song.DATE_ADDED_COMPARATOR);
                break;
            case Rule.Field.DATE_PLAYED:
                Collections.sort(in, Song.DATE_PLAYED_COMPARATOR);
                break;
            case Rule.Field.YEAR:
                Collections.sort(in, Song.YEAR_COMPARATOR);
                break;
            case Rule.Field.NAME:
            default:
                Collections.sort(in);
                break;
        }

        if (sortAscending) {
            Collections.reverse(in);
        }
        return in;
    }

    /**
     * Trims an {@link ArrayList} of Songs so that it contains no more songs than
     * {@link AutoPlaylist#maximumEntries}. Extra songs are truncated out of the list.
     * Used in {@link AutoPlaylist#generatePlaylist(Context)}
     * @param in The {@link ArrayList} to be trimmed
     * @return A new {@link ArrayList} that satisfies the maximum entry size
     */
    private List<Song> trim(List<Song> in) {
        if (in.size() <= maximumEntries || maximumEntries == UNLIMITED_ENTRIES) {
            return in;
        }

        List<Song> trimmed = new ArrayList<>(maximumEntries);
        for (int i = 0; i < maximumEntries; i++) {
            trimmed.add(in.get(i));
        }

        return trimmed;
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

        /**
         * Evaluate this rule on a data set
         * @param in The data to evaluate this rule on. Items in this list will be returned by this
         *           method if they match the query. Nothing is removed from the original list.
         * @return The filtered data as a {@link List<Song>} that only contains songs that
         *         match this rule that were in the original data set
         */
        public List<Song> evaluate(List<Song> in, Context context) {
            switch (type) {
                case Type.PLAYLIST:
                    return evaluatePlaylist(in, context);
                case Type.SONG:
                    return evaluateSong(in, context);
                case Type.ARTIST:
                    return evaluateArtist(in);
                case Type.ALBUM:
                    return evaluateAlbum(in);
                case Type.GENRE:
                    return evaluateGenre(in);
                default:
                    return null;
            }
        }

        /**
         * Logic to evaluate playlist rules.
         * @param in The data to evaluate this rule on. Items in this list will be returned by this
         *           method if they match the query. Nothing is removed from the original list.
         * @return The filtered data as a {@link List<Song>} that only contains songs that
         *         match this rule that were in the original data set
         * @see AutoPlaylist.Rule#evaluate(List, Context)
         */
        private List<Song> evaluatePlaylist(List<Song> in, Context context) {
            List<Song> filteredSongs = new ArrayList<>();
            switch (field) {
                case Field.ID:
                    final long id = Long.parseLong(value);
                    for (Playlist p : Library.getPlaylists()) {
                        if (p.playlistId == id ^ match == Match.NOT_EQUALS) {
                            for (Song s : Library.getPlaylistEntries(context, p)) {
                                if (in.contains(s) && !filteredSongs.contains(s)) {
                                    filteredSongs.add(s);
                                }
                            }
                        }
                    }
                    break;
                case Field.NAME:
                default:
                    if (match == Match.EQUALS || match == Match.NOT_EQUALS) {
                        for (Playlist p : Library.getPlaylists()) {
                            if (p.playlistName.equalsIgnoreCase(value)
                                    ^ match == Match.NOT_EQUALS) {
                                for (Song s : Library.getPlaylistEntries(context, p)) {
                                    if (in.contains(s) && !filteredSongs.contains(s)) {
                                        filteredSongs.add(s);
                                    }
                                }
                            }
                        }
                    } else if (match == Match.CONTAINS || match == Match.NOT_CONTAINS) {
                        for (Playlist p : Library.getPlaylists()) {
                            if (p.playlistName.contains(value) ^ match == Match.NOT_EQUALS) {
                                for (Song s : Library.getPlaylistEntries(context, p)) {
                                    if (in.contains(s) && !filteredSongs.contains(s)) {
                                        filteredSongs.add(s);
                                    }
                                }
                            }
                        }
                    }
                    break;
            }
            return filteredSongs;
        }

        /**
         * Logic to evaluate song rules. See {@link AutoPlaylist.Rule#evaluate(List, Context)}
         * @param in The data to evaluate this rule on. Items in this list will be returned by this
         *           method if they match the query. Nothing is removed from the original list
         * @param context A Context used to scan play and skip counts if necessary
         * @return The filtered data as a {@link List<Song>} that only contains songs that
         *         match this rule that were in the original data set
         */
        private List<Song> evaluateSong(List<Song> in, Context context) {
            List<Song> filteredSongs = new ArrayList<>();
            switch (field) {
                case Field.ID:
                    final long id = Long.parseLong(value);
                    for (Song s : in) {
                        if (s.songId == id ^ match == Match.NOT_EQUALS) {
                            filteredSongs.add(s);
                        }
                    }
                    break;
                case Field.NAME:
                    if (match == Match.EQUALS || match == Match.NOT_EQUALS) {
                        for (Song s : in) {
                            if (s.songName.equals(value) ^ match == Match.NOT_EQUALS) {
                                filteredSongs.add(s);
                            }
                        }
                    } else if (match == Match.CONTAINS || match == Match.NOT_CONTAINS) {
                        for (Song s : in) {
                            if (s.songName.contains(value) ^ match == Match.NOT_CONTAINS) {
                                filteredSongs.add(s);
                            }
                        }
                    }
                    break;
                case Field.PLAY_COUNT:
                    final long playCount = Long.parseLong(value);
                    if (match == Match.EQUALS || match == Match.NOT_EQUALS) {
                        for (Song s : in) {
                            if (s.getPlayCount() == playCount ^ match == Match.NOT_EQUALS) {
                                filteredSongs.add(s);
                            }
                        }
                    } else if (match == Match.LESS_THAN || match == Match.GREATER_THAN) {
                        for (Song s : in) {
                            if (s.getPlayCount() != playCount && (s.getPlayCount() < playCount
                                    ^ match == Match.GREATER_THAN)) {
                                filteredSongs.add(s);
                            }
                        }
                    }
                    break;
                case Field.SKIP_COUNT:
                    final long skipCount = Long.parseLong(value);
                    if (match == Match.EQUALS || match == Match.NOT_EQUALS) {
                        for (Song s : in) {
                            if (s.getSkipCount() == skipCount ^ match == Match.NOT_EQUALS) {
                                filteredSongs.add(s);
                            }
                        }
                    } else if (match == Match.LESS_THAN || match == Match.GREATER_THAN) {
                        for (Song s : in) {
                            if (s.getSkipCount() != skipCount && (s.getSkipCount() < skipCount
                                    ^ match == Match.GREATER_THAN)) {
                                filteredSongs.add(s);
                            }
                        }
                    }
                    break;
                case Field.YEAR:
                    final int year = Integer.parseInt(value);
                    if (match == Match.EQUALS || match == Match.NOT_EQUALS) {
                        for (Song s : in) {
                            if (s.year == year ^ match == Match.NOT_EQUALS) {
                                filteredSongs.add(s);
                            }
                        }
                    } else if (match == Match.LESS_THAN || match == Match.GREATER_THAN) {
                        for (Song s : in) {
                            if (s.year < year ^ match == Match.GREATER_THAN) {
                                filteredSongs.add(s);
                            }
                        }
                    }
                    break;
                case Field.DATE_ADDED:
                    final int date = Integer.parseInt(value);
                    if (match == Match.EQUALS || match == Match.NOT_EQUALS) {
                        for (Song s : in) {
                            // Look at the day the song was added, not the time
                            // (This value is still in seconds)
                            int dayAdded = (int) (s.dateAdded - s.dateAdded % 86400);
                            if (dayAdded == date ^ match == Match.NOT_EQUALS) {
                                filteredSongs.add(s);
                            }
                        }
                    } else if (match == Match.LESS_THAN || match == Match.GREATER_THAN) {
                        for (Song s : in) {
                            // Look at the day the song was added, not the time
                            // (This value is still in seconds)
                            int dayAdded = (int) (s.dateAdded - s.dateAdded % 86400);
                            if (dayAdded < date ^ match == Match.GREATER_THAN) {
                                filteredSongs.add(s);
                            }
                        }
                    }
                    break;
                case Field.DATE_PLAYED:
                    final int playDate = Integer.parseInt(value);
                    if (match == Match.EQUALS || match == Match.NOT_EQUALS) {
                        for (Song s : in) {
                            // Look at the day the song was added, not the time
                            // (This value is still in seconds)
                            int dayAdded = s.getPlayDate();
                            dayAdded -= dayAdded % 86400; // 24 * 60 * 60
                            if (dayAdded == playDate ^ match == Match.NOT_EQUALS) {
                                filteredSongs.add(s);
                            }
                        }
                    } else if (match == Match.LESS_THAN || match == Match.GREATER_THAN) {
                        for (Song s : in) {
                            // Look at the day the song was added, not the time
                            // (This value is still in seconds)
                            int dayAdded = s.getPlayDate();
                            dayAdded -= dayAdded % 86400; // 24 * 60 * 60
                            if (dayAdded < playDate ^ match == Match.GREATER_THAN) {
                                filteredSongs.add(s);
                            }
                        }
                    }
                    break;
            }
            return filteredSongs;
        }

        /**
         * Logic to evaluate artist rules
         * @param in The data to evaluate this rule on. Items in this list will be returned by this
         *           method if they match the query. Nothing is removed from the original list.
         * @return The filtered data as a {@link List<Song>} that only contains songs that
         *         match this rule that were in the original data set
         * @see AutoPlaylist.Rule#evaluate(List, Context)
         */
        private List<Song> evaluateArtist(List<Song> in) {
            List<Song> filteredSongs = new ArrayList<>();
            switch (field) {
                case Field.ID:
                    final long id = Long.parseLong(value);
                    for (Song s : in) {
                        if (s.artistId == id ^ match == Match.NOT_EQUALS) {
                            filteredSongs.add(s);
                        }
                    }
                    break;
                case Field.NAME:
                default:
                    if (match == Match.EQUALS || match == Match.NOT_EQUALS) {
                        for (Song s : in) {
                            if (s.artistName.equals(value) ^ match == Match.NOT_EQUALS) {
                                filteredSongs.add(s);
                            }
                        }
                    } else if (match == Match.CONTAINS || match == Match.NOT_CONTAINS) {
                        for (Song s : in) {
                            if (s.artistName.contains(value) ^ match == Match.NOT_CONTAINS) {
                                filteredSongs.add(s);
                            }
                        }
                    }
                    break;
            }
            return filteredSongs;
        }

        /**
         * Logic to evaluate album rules
         * @param in The data to evaluate this rule on. Items in this list will be returned by this
         *           method if they match the query. Nothing is removed from the original list.
         * @return The filtered data as a {@link List<Song>} that only contains songs that
         *         match this rule that were in the original data set
         * @see AutoPlaylist.Rule#evaluate(List, Context)
         */
        private List<Song> evaluateAlbum(List<Song> in) {
            List<Song> filteredSongs = new ArrayList<>();
            switch (field) {
                case Field.ID:
                    final long id = Long.parseLong(value);
                    for (Song s : in) {
                        if (s.albumId == id ^ match == Match.NOT_EQUALS) {
                            filteredSongs.add(s);
                        }
                    }
                    break;
                case Field.NAME:
                default:
                    if (match == Match.EQUALS || match == Match.NOT_EQUALS) {
                        for (Song s : in) {
                            if (s.albumName.equals(value) ^ match == Match.NOT_EQUALS) {
                                filteredSongs.add(s);
                            }
                        }
                    } else if (match == Match.CONTAINS || match == Match.NOT_CONTAINS) {
                        for (Song s : in) {
                            if (s.albumName.contains(value) ^ match == Match.NOT_CONTAINS) {
                                filteredSongs.add(s);
                            }
                        }
                    }
                    break;
            }
            return filteredSongs;
        }

        /**
         * Logic to evaluate genre rules
         * @param in The data to evaluate this rule on. Items in this list will be returned by this
         *           method if they match the query. Nothing is removed from the original list.
         * @return The filtered data as a {@link List<Song>} that only contains songs that
         *         match this rule that were in the original data set
         * @see AutoPlaylist.Rule#evaluate(List, Context)
         */
        private List<Song> evaluateGenre(List<Song> in) {
            List<Song> filteredSongs = new ArrayList<>();
            switch (field) {
                case Field.ID:
                    final Long id = Long.parseLong(value);
                    for (Song s : in) {
                        if (s.genreId == id ^ match == Match.NOT_EQUALS) {
                            filteredSongs.add(s);
                        }
                    }
                    break;
                case Field.NAME:
                default:
                    if (match == Match.EQUALS || match == Match.NOT_EQUALS) {
                        for (Genre g : Library.getGenres()) {
                            if (g.genreName.equals(value) ^ match == Match.NOT_EQUALS) {
                                for (Song s : in) {
                                    if (s.genreId == g.genreId) {
                                        filteredSongs.add(s);
                                    }
                                }
                            }
                        }
                    } else if (match == Match.CONTAINS || match == Match.NOT_CONTAINS) {
                        for (Genre g : Library.getGenres()) {
                            if (g.genreName.contains(value) ^ match == Match.NOT_CONTAINS) {
                                for (Song s : in) {
                                    if (s.genreId == g.genreId) {
                                        filteredSongs.add(s);
                                    }
                                }
                            }
                        }
                    }
                    break;
            }
            return filteredSongs;
        }
    }
}
