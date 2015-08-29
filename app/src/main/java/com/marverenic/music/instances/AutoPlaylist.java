package com.marverenic.music.instances;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import com.crashlytics.android.Crashlytics;
import com.marverenic.music.Library;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Locale;

public class AutoPlaylist extends Playlist implements Parcelable {

    /**
     * Value representing an unlimited amount of song entries
     */
    public static final int UNLIMITED_ENTRIES = -1;

    /**
     * How many items can be stored in this playlist. Default is unlimited
     */

    private int maximumEntries = UNLIMITED_ENTRIES;

    /**
     * Whether or not a song has to match all rules in order to appear in the playlist.
     */
    private boolean matchAllRules;

    /**
     * The rules to match when building the playlist
     */
    private Rule[] rules;

    /**
     * The field to look at when sorting the playlist. Must be a member of {@link Rule.Field} and
     * cannot be {@link Rule.Field#ID}
     */
    private int sortMethod = Rule.Field.NAME;

    /**
     * Whether to sort the playlist ascending (A-Z, oldest to newest, or 0-infinity).
     * If false, sort descending (Z-A, newest to oldest, or infinity-0).
     * Default is true.
     */
    private boolean sortAscending = true;


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
    public AutoPlaylist (int playlistId, String playlistName, int maximumEntries, int sortMethod,
                         boolean sortAscending, boolean matchAllRules, Rule... rules){
        super(playlistId, playlistName);
        setRules(maximumEntries, matchAllRules, rules);
        this.sortMethod = sortMethod;
        this.sortAscending = sortAscending;
    }

    /**
     * Set the rules for this playlist
     * @param maximumEntries The maximum number of songs this playlist should have. Use
     *                       {@link AutoPlaylist#UNLIMITED_ENTRIES} for no limit. This limit will
     *                       be applied after the list has been sorted. Any extra entries will be
     *                       truncated.
     * @param matchAllRules Whether or not all rules have to be matched for a song to appear in this
     *                      playlist
     * @param rules The rules that songs must follow in order to appear in this playlist
     */
    public void setRules(int maximumEntries, boolean matchAllRules, Rule... rules){
        this.maximumEntries = maximumEntries;
        this.matchAllRules = matchAllRules;
        this.rules = rules;
    }

    /**
     * Generate the list of songs that match all rules for this playlist.
     * @param context A {@link Context} used for various operations like reading play counts and
     *                checking playlist rules
     * @return An {@link ArrayList} of Songs that contains all songs in the library which match
     *         the rules of this playlist
     */
    public ArrayList<Song> generatePlaylist(Context context){
        ArrayList<Song> songs;
        if (matchAllRules) {
            songs = Library.getSongs();
            for (Rule r : rules) {
                if (r != null) {
                    songs = r.evaluate(songs, context);
                }
            }
        }
        else{
            HashSet<Song> songSet = new HashSet<>(); // Use a Set to prevent duplicates
            final ArrayList<Song> allSongs = Library.getSongs();
            for (Rule r : rules){
                songSet.addAll(r.evaluate(allSongs, context));
            }
            songs = new ArrayList<>(songSet);
        }
        return trim(sort(songs));
    }

    /**
     * Sorts an {@link ArrayList} of songs as specified by {@link AutoPlaylist#sortMethod} and
     * {@link AutoPlaylist#sortAscending}. Used in {@link AutoPlaylist#generatePlaylist(Context)}
     * @param in The {@link ArrayList} to be sorted
     * @return The original, sorted, {@link ArrayList} for convenience
     */
    private ArrayList<Song> sort(ArrayList<Song> in){
        switch (sortMethod){
            case Rule.Field.NAME:
                Collections.sort(in, new Comparator<Song>() {
                    @Override
                    public int compare(Song o1, Song o2) {
                        String o1c = o1.songName.toLowerCase(Locale.ENGLISH);
                        String o2c = o2.songName.toLowerCase(Locale.ENGLISH);
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
                        if (sortAscending) return o1c.compareTo(o2c);
                        else return o2c.compareTo(o1c);
                    }
                });
                break;
            case Rule.Field.PLAY_COUNT:
                Collections.sort(in, new Comparator<Song>() {
                    @Override
                    public int compare(Song s1, Song s2) {
                        if (sortAscending) return s1.playCount() - s2.playCount();
                        else return s2.playCount() - s1.playCount();
                    }
                });
                break;
            case Rule.Field.SKIP_COUNT:
                Collections.sort(in, new Comparator<Song>() {
                    @Override
                    public int compare(Song s1, Song s2) {
                        if (sortAscending) return s1.skipCount() - s2.skipCount();
                        else return s2.skipCount() - s1.skipCount();
                    }
                });
                break;
            case Rule.Field.DATE_ADDED:
                Collections.sort(in, new Comparator<Song>() {
                    @Override
                    public int compare(Song s1, Song s2) {
                        if (sortAscending) return s1.dateAdded - s2.dateAdded;
                        else return s2.dateAdded - s1.dateAdded;
                    }
                });
                break;
            case Rule.Field.YEAR:
                Collections.sort(in, new Comparator<Song>() {
                    @Override
                    public int compare(Song s1, Song s2) {
                        if (sortAscending) return s1.year - s2.year;
                        else return s2.year - s1.year;
                    }
                });
                break;
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
    private ArrayList<Song> trim(ArrayList<Song> in){
        if (in.size() <= maximumEntries || maximumEntries == UNLIMITED_ENTRIES){
            return in;
        }

        ArrayList<Song> trimmed = new ArrayList<>(maximumEntries);
        for (int i = 0; i < maximumEntries; i++){
            trimmed.add(in.get(i));
        }

        return trimmed;
    }

    public static final Parcelable.Creator<Parcelable> CREATOR = new Parcelable.Creator<Parcelable>() {
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
        dest.writeByte((byte) ((sortAscending)? 1 : 0));
    }

    public static class Rule implements Parcelable {

        public static final class Type {
            public static final int PLAYLIST = 1;
            public static final int SONG = 2;
            public static final int ARTIST = 3;
            public static final int ALBUM = 8;
            public static final int GENRE = 16;
        }
        public static final class Field {
            public static final int ID = 32;
            public static final int NAME = 64;
            public static final int PLAY_COUNT = 128;
            public static final int SKIP_COUNT = 256;
            public static final int YEAR = 512;
            public static final int DATE_ADDED = 1024;
        }
        public static final class Match {
            public static final int EQUALS = 2048;
            public static final int NOT_EQUALS = 4096;
            public static final int CONTAINS = 8192;
            public static final int NOT_CONTAINS = 16384;
            public static final int LESS_THAN = 32768;
            public static final int GREATER_THAN = 65536;
        }

        private int type;
        private int match;
        private int field;
        private String value;

        public Rule(int type, int field, int match, String value){
            validate(type, field, match, value);
            this.type = type;
            this.field = field;
            this.match = match;
            this.value = value;
        }

        public static final Parcelable.Creator<Rule> CREATOR = new Parcelable.Creator<Rule>() {
            public Rule createFromParcel(Parcel in) {
                return new Rule(in);
            }

            public Rule[] newArray(int size) {
                return new Rule[size];
            }
        };

        private Rule(Parcel in) {
            type = in.readInt();
            match = in.readInt();
            field = in.readInt();
            value = in.readString();
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

        private void validate(int type, int field, int match, String value){
            // Only Songs have play counts and skip counts
            if ((type != Type.SONG) && (field == Field.PLAY_COUNT || field == Field.SKIP_COUNT)) {
                throw new IllegalArgumentException(type + " type does not have field " + field);
            }
            // Only Songs have years
            if (type != Type.SONG && field == Field.YEAR){
                throw new IllegalArgumentException(type + " type does not have field " + field);
            }
            // Only Songs have dates added
            if (type != Type.SONG && field == Field.DATE_ADDED){
                throw new IllegalArgumentException(type + " type does not have field " + field);
            }

            if (field == Field.ID){
                // IDs can only be compared by equals or !equals
                if (match == Match.CONTAINS || match == Match.NOT_CONTAINS || match == Match.LESS_THAN || match == Match.GREATER_THAN){
                    throw new IllegalArgumentException("ID cannot be compared by method " + match);
                }
                // Make sure the value is actually a number
                try{
                    //noinspection ResultOfMethodCallIgnored
                    Long.parseLong(value);
                }
                catch (NumberFormatException e){
                    Crashlytics.logException(e);
                    throw new IllegalArgumentException("ID cannot be compared to value " + value);
                }
            }
            else if (field == Field.NAME){
                // Names can't be compared by < or >... that doesn't even make sense...
                if (match == Match.GREATER_THAN || match == Match.LESS_THAN){
                    throw new IllegalArgumentException("Name cannot be compared by method " + match);
                }
            }
            else if (field == Field.SKIP_COUNT || field == Field.PLAY_COUNT || field == Field.YEAR || field == Field.DATE_ADDED){
                // Numeric values can't be compared by contains or !contains
                if (match == Match.CONTAINS || match == Match.NOT_CONTAINS){
                    throw new IllegalArgumentException(field + " cannot be compared by method " + match);
                }
                // Make sure the value is actually a number
                try{
                    //noinspection ResultOfMethodCallIgnored
                    Long.parseLong(value);
                }
                catch (NumberFormatException e){
                    Crashlytics.logException(e);
                    throw new IllegalArgumentException("ID cannot be compared to value " + value);
                }
            }
        }

        /**
         * Evaluate this rule on a data set
         * @param in The data to evaluate this rule on. Items in this list will be returned by this
         *           method if they match the query. Nothing is removed from the original list.
         * @return The filtered data as an {@link ArrayList<Song>} that only contains songs that
         *         match this rule that were in the original data set
         */
        public ArrayList<Song> evaluate (ArrayList<Song> in, Context context){
            switch (type){
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
            }
            return null;
        }

        /**
         * Logic to evaluate playlist rules. See {@link AutoPlaylist.Rule#evaluate(ArrayList, Context)}
         * @param in The data to evaluate this rule on. Items in this list will be returned by this
         *           method if they match the query. Nothing is removed from the original list.
         * @return The filtered data as an {@link ArrayList<Song>} that only contains songs that
         *         match this rule that were in the original data set
         */
        private ArrayList<Song> evaluatePlaylist (ArrayList<Song> in, Context context){
            ArrayList<Song> filteredSongs = new ArrayList<>();
            switch (field){
                case Field.ID:
                    final long id = Long.parseLong(value);
                    for (Playlist p : Library.getPlaylists()){
                        if (p.playlistId == id ^ match == Match.NOT_EQUALS){
                            for (Song s : Library.getPlaylistEntries(context, p)){
                                if (in.contains(s) && !filteredSongs.contains(s))
                                    filteredSongs.add(s);
                            }
                        }
                    }
                    break;
                case Field.NAME:
                    if (match == Match.EQUALS || match == Match.NOT_EQUALS){
                        for (Playlist p : Library.getPlaylists()){
                            if (p.playlistName.equalsIgnoreCase(value) ^ match == Match.NOT_EQUALS){
                                for (Song s : Library.getPlaylistEntries(context, p)){
                                    if (in.contains(s) && !filteredSongs.contains(s))
                                        filteredSongs.add(s);
                                }
                            }
                        }
                    }
                    else if (match == Match.CONTAINS || match == Match.NOT_CONTAINS){
                        for (Playlist p : Library.getPlaylists()){
                            if (p.playlistName.contains(value) ^ match == Match.NOT_EQUALS){
                                for (Song s : Library.getPlaylistEntries(context, p)){
                                    if (in.contains(s) && !filteredSongs.contains(s))
                                        filteredSongs.add(s);
                                }
                            }
                        }
                    }
                    break;
            }
            return filteredSongs;
        }

        /**
         * Logic to evaluate song rules. See {@link AutoPlaylist.Rule#evaluate(ArrayList, Context)}
         * @param in The data to evaluate this rule on. Items in this list will be returned by this
         *           method if they match the query. Nothing is removed from the original list
         * @param context A Context used to scan play and skip counts if necessary
         * @return The filtered data as an {@link ArrayList<Song>} that only contains songs that
         *         match this rule that were in the original data set
         */
        private ArrayList<Song> evaluateSong (ArrayList<Song> in, Context context){
            ArrayList<Song> filteredSongs = new ArrayList<>();
            switch (field){
                case Field.ID:
                    final long id = Long.parseLong(value);
                    for (Song s : in){
                        if (s.songId == id ^ match == Match.NOT_EQUALS){
                            filteredSongs.add(s);
                        }
                    }
                    break;
                case Field.NAME:
                    if (match == Match.EQUALS || match == Match.NOT_EQUALS){
                        for (Song s : in){
                            if (s.songName.equals(value) ^ match == Match.NOT_EQUALS){
                                filteredSongs.add(s);
                            }
                        }
                    }
                    else if (match == Match.CONTAINS || match == Match.NOT_CONTAINS){
                        for (Song s : in){
                            if (s.songName.contains(value) ^ match == Match.NOT_CONTAINS){
                                filteredSongs.add(s);
                            }
                        }
                    }
                    break;
                case Field.PLAY_COUNT:
                    // In the event that the play counts in this process have gone stale, make
                    // sure they're current
                    Library.loadPlayCounts(context);
                    final long playCount = Long.parseLong(value);
                    if (match == Match.EQUALS || match == Match.NOT_EQUALS){
                        for (Song s : in){
                            if (s.playCount() == playCount ^ match == Match.NOT_EQUALS){
                                filteredSongs.add(s);
                            }
                        }
                    }
                    else if (match == Match.LESS_THAN || match == Match.GREATER_THAN){
                        for (Song s : in){
                            if (s.playCount() != playCount && (s.playCount() < playCount ^ match == Match.GREATER_THAN)){
                                filteredSongs.add(s);
                            }
                        }
                    }
                    break;
                case Field.SKIP_COUNT:
                    // Refresh skip counts (see PLAY_COUNT case)
                    Library.loadPlayCounts(context);
                    final long skipCount = Long.parseLong(value);
                    if (match == Match.EQUALS || match == Match.NOT_EQUALS){
                        for (Song s : in){
                            if (s.skipCount() == skipCount ^ match == Match.NOT_EQUALS){
                                filteredSongs.add(s);
                            }
                        }
                    }
                    else if (match == Match.LESS_THAN || match == Match.GREATER_THAN){
                        for (Song s : in){
                            if (s.skipCount() != skipCount && (s.skipCount() < skipCount ^ match == Match.GREATER_THAN)){
                                filteredSongs.add(s);
                            }
                        }
                    }
                    break;
                case Field.YEAR:
                    final int year = Integer.parseInt(value);
                    if (match == Match.EQUALS || match == Match.NOT_EQUALS){
                        for (Song s : in){
                            if (s.year == year ^ match == Match.NOT_EQUALS){
                                filteredSongs.add(s);
                            }
                        }
                    }
                    else if (match == Match.LESS_THAN || match == Match.GREATER_THAN){
                        for (Song s : in){
                            if (s.year < year ^ match == Match.GREATER_THAN){
                                filteredSongs.add(s);
                            }
                        }
                    }
                    break;
                case Field.DATE_ADDED:
                    final int date = Integer.parseInt(value);
                    if (match == Match.EQUALS || match == Match.NOT_EQUALS){
                        for (Song s : in){
                            // Look at the day the song was added, not the time
                            // (This value is still in seconds)
                            int dayAdded = s.dateAdded - s.dateAdded % 86400; // 24 * 60 * 60
                            if (dayAdded == date ^ match == Match.NOT_EQUALS){
                                filteredSongs.add(s);
                            }
                        }
                    }
                    else if (match == Match.LESS_THAN || match == Match.GREATER_THAN){
                        for (Song s : in){
                            // Look at the day the song was added, not the time
                            // (This value is still in seconds)
                            int dayAdded = s.dateAdded - s.dateAdded % 86400; // 24 * 60 * 60
                            if (dayAdded < date ^ match == Match.GREATER_THAN){
                                filteredSongs.add(s);
                            }
                        }
                    }
                    break;
            }
            return filteredSongs;
        }

        /**
         * Logic to evaluate artist rules. See {@link AutoPlaylist.Rule#evaluate(ArrayList, Context)}
         * @param in The data to evaluate this rule on. Items in this list will be returned by this
         *           method if they match the query. Nothing is removed from the original list.
         * @return The filtered data as an {@link ArrayList<Song>} that only contains songs that
         *         match this rule that were in the original data set
         */
        private ArrayList<Song> evaluateArtist (ArrayList<Song> in){
            ArrayList<Song> filteredSongs = new ArrayList<>();
            switch (field){
                case Field.ID:
                    final long id = Long.parseLong(value);
                    for (Song s : in){
                        if (s.artistId == id ^ match == Match.NOT_EQUALS){
                            filteredSongs.add(s);
                        }
                    }
                    break;
                case Field.NAME:
                    if (match == Match.EQUALS || match == Match.NOT_EQUALS){
                        for (Song s : in){
                            if (s.artistName.equals(value) ^ match == Match.NOT_EQUALS){
                                filteredSongs.add(s);
                            }
                        }
                    }
                    else if (match == Match.CONTAINS || match == Match.NOT_CONTAINS){
                        for (Song s : in){
                            if (s.artistName.contains(value) ^ match == Match.NOT_CONTAINS){
                                filteredSongs.add(s);
                            }
                        }
                    }
                    break;
            }
            return filteredSongs;
        }

        /**
         * Logic to evaluate album rules. See {@link AutoPlaylist.Rule#evaluate(ArrayList, Context)}
         * @param in The data to evaluate this rule on. Items in this list will be returned by this
         *           method if they match the query. Nothing is removed from the original list.
         * @return The filtered data as an {@link ArrayList<Song>} that only contains songs that
         *         match this rule that were in the original data set
         */
        private ArrayList<Song> evaluateAlbum (ArrayList<Song> in){
            ArrayList<Song> filteredSongs = new ArrayList<>();
            switch (field) {
                case Field.ID:
                    final long id = Long.parseLong(value);
                    for (Song s : in) {
                        if (s.albumId == id ^ match == Match.NOT_EQUALS){
                            filteredSongs.add(s);
                        }
                    }
                    break;
                case Field.NAME:
                    if (match == Match.EQUALS || match == Match.NOT_EQUALS) {
                        for (Song s : in) {
                            if (s.albumName.equals(value) ^ match == Match.NOT_EQUALS){
                                filteredSongs.add(s);
                            }
                        }
                    } else if (match == Match.CONTAINS || match == Match.NOT_CONTAINS) {
                        for (Song s : in) {
                            if (s.albumName.contains(value) ^ match == Match.NOT_CONTAINS){
                                filteredSongs.add(s);
                            }
                        }
                    }
                    break;
            }
            return filteredSongs;
        }

        /**
         * Logic to evaluate genre rules. See {@link AutoPlaylist.Rule#evaluate(ArrayList, Context)}
         * @param in The data to evaluate this rule on. Items in this list will be returned by this
         *           method if they match the query. Nothing is removed from the original list.
         * @return The filtered data as an {@link ArrayList<Song>} that only contains songs that
         *         match this rule that were in the original data set
         */
        private ArrayList<Song> evaluateGenre (ArrayList<Song> in){
            ArrayList<Song> filteredSongs = new ArrayList<>();
            switch (field) {
                case Field.ID:
                    final Long id = Long.parseLong(value);
                    for (Song s : in) {
                        if (s.genreId == id ^ match == Match.NOT_EQUALS) filteredSongs.add(s);
                    }
                    break;
                case Field.NAME:
                    if (match == Match.EQUALS || match == Match.NOT_EQUALS){
                        for (Genre g : Library.getGenres()){
                            if (g.genreName.equals(value) ^ match == Match.NOT_EQUALS) {
                                for (Song s : in) {
                                    if (s.genreId == g.genreId) {
                                        filteredSongs.add(s);
                                    }
                                }
                            }
                        }
                    }
                    else if (match == Match.CONTAINS || match == Match.NOT_CONTAINS){
                        for (Genre g : Library.getGenres()){
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
