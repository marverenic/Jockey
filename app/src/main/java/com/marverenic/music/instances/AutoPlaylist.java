package com.marverenic.music.instances;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import com.crashlytics.android.Crashlytics;
import com.marverenic.music.Library;

import java.util.ArrayList;
import java.util.HashSet;

public class AutoPlaylist implements Parcelable {

    public static final int UNLIMITED_ENTRIES = -1;

    /**
     * How many items can be stored in this playlist. Default is unlimited
     */
    public int maximumEntries = UNLIMITED_ENTRIES;
    /**
     * Whether or not a song has to match all rules in order to appear in the playlist. Default is true
     */
    public boolean matchAllRules = true;
    /**
     * The rules to match when building the playlist
     */
    public Rule[] rules;

    public AutoPlaylist (int maximumEntries, boolean matchAllRules, Rule... rules){
        this.maximumEntries = maximumEntries;
        this.matchAllRules = matchAllRules;
        this.rules = rules;
    }

    public ArrayList<Song> generatePlaylist(Context context){
        if (matchAllRules) {
            ArrayList<Song> songs = Library.getSongs();
            for (Rule r : rules) {
                r.evaluate(songs, context);
            }
            return songs;
        }
        else{
            HashSet<Song> songs = new HashSet<>(); // Use a Set to prevent duplicates
            final ArrayList<Song> allSongs = Library.getSongs();
            for (Rule r : rules){
                songs.addAll(r.evaluate(allSongs, context));
            }
            return new ArrayList<>(songs);
        }
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
        //TODO
    }
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        //TODO
    }

    public static class Rule {

        public enum TYPE {PLAYLIST, SONG, ARTIST, ALBUM, GENRE}
        public enum FIELD {ID, NAME, PLAY_COUNT, SKIP_COUNT, YEAR, DATE_ADDED}
        public enum MATCH {EQUALS, NOT_EQUALS, CONTAINS, NOT_CONTAINS, LESS_THAN, GREATER_THAN}

        private TYPE type;
        private MATCH match;
        private FIELD field;
        private String value;

        public Rule(TYPE type, FIELD field, MATCH match, String value){
            validate(type, field, match, value);
            this.type = type;
            this.field = field;
            this.match = match;
            this.value = value;
        }

        private void validate(TYPE type, FIELD field, MATCH match, String value){
            // Only Songs have play counts and skip counts
            if ((type != TYPE.SONG) && (field == FIELD.PLAY_COUNT || field == FIELD.SKIP_COUNT)) {
                throw new IllegalArgumentException(type + " type does not have field " + field);
            }
            // Only Albums and Songs have years
            if ((type != TYPE.ALBUM && type != TYPE.SONG) && (field == FIELD.YEAR)){
                throw new IllegalArgumentException(type + " type does not have field " + field);
            }
            // Only Songs have dates added
            if (type != TYPE.SONG && field == FIELD.DATE_ADDED){
                throw new IllegalArgumentException(type + " type does not have field " + field);
            }

            if (field == FIELD.ID){
                // IDs can only be compared by equals or !equals
                if (match == MATCH.CONTAINS || match == MATCH.NOT_CONTAINS || match == MATCH.LESS_THAN || match == MATCH.GREATER_THAN){
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
            else if (field == FIELD.NAME){
                // Names can't be compared by < or >... that doesn't even make sense...
                if (match == MATCH.GREATER_THAN || match == MATCH.LESS_THAN){
                    throw new IllegalArgumentException("Name cannot be compared by method " + match);
                }
            }
            else if (field == FIELD.SKIP_COUNT || field == FIELD.PLAY_COUNT || field == FIELD.YEAR || field == FIELD.DATE_ADDED){
                // Numeric values can't be compared by contains or !contains
                if (match == MATCH.CONTAINS || match == MATCH.NOT_CONTAINS){
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
         *           method if they match the query. Nothing is removed from this list.
         * @return The filtered data as an {@link ArrayList<Song>} that only contains songs that
         *         match this rule that were in the original data set
         */
        public ArrayList<Song> evaluate (ArrayList<Song> in, Context context){
            ArrayList<Song> filteredSongs = new ArrayList<>();
            switch (type){
                case PLAYLIST: /** Playlist */
                    switch (field){
                        case ID: /** ID */
                            final long id = Long.getLong(value);
                            if (match == MATCH.EQUALS){ /** Equals */
                                for (Playlist p : Library.getPlaylists()){
                                    if (p.playlistId == id){
                                        for (Song s : Library.getPlaylistEntries(context, p)){
                                            if (in.contains(s) && !filteredSongs.contains(s))
                                                filteredSongs.add(s);
                                        }
                                    }
                                }
                            }
                            else if (match == MATCH.NOT_EQUALS){ /** Doesn't equal */
                                for (Playlist p : Library.getPlaylists()){
                                    if (p.playlistId != id){
                                        for (Song s : Library.getPlaylistEntries(context, p)){
                                            if (in.contains(s) && !filteredSongs.contains(s))
                                                filteredSongs.add(s);
                                        }
                                    }
                                }
                            }
                            break;
                        case NAME: /** Title */
                            if (match == MATCH.EQUALS){ /** Equals */
                                for (Playlist p : Library.getPlaylists()){
                                    if (p.playlistName.equalsIgnoreCase(value)){
                                        for (Song s : Library.getPlaylistEntries(context, p)){
                                            if (in.contains(s) && !filteredSongs.contains(s))
                                                filteredSongs.add(s);
                                        }
                                    }
                                }
                            }
                            else if (match == MATCH.NOT_EQUALS){ /** Doesn't equal */
                                for (Playlist p : Library.getPlaylists()){
                                    if (!p.playlistName.equalsIgnoreCase(value)) {
                                        for (Song s : Library.getPlaylistEntries(context, p)) {
                                            if (in.contains(s) && !filteredSongs.contains(s))
                                                filteredSongs.add(s);
                                        }
                                    }
                                }
                            }
                            else if (match == MATCH.CONTAINS){ /** Contains */
                                for (Playlist p : Library.getPlaylists()){
                                    if (p.playlistName.contains(value)){
                                        for (Song s : Library.getPlaylistEntries(context, p)){
                                            if (in.contains(s) && !filteredSongs.contains(s))
                                                filteredSongs.add(s);
                                        }
                                    }
                                }
                            }
                            else if (match == MATCH.NOT_CONTAINS){ /** Doesn't contain */
                                for (Playlist p : Library.getPlaylists()){
                                    if (!p.playlistName.contains(value)){
                                        for (Song s : Library.getPlaylistEntries(context, p)){
                                            if (in.contains(s) && !filteredSongs.contains(s))
                                                filteredSongs.add(s);
                                        }
                                    }
                                }
                            }
                            break;
                    }
                    break;
                case SONG: /** Song */
                    switch (field){
                        case ID: /** ID */
                            final long id = Long.getLong(value);
                            if (match == MATCH.EQUALS){ /** Equals */
                                for (Song s : in){
                                    if (s.songId == id) filteredSongs.add(s);
                                }
                            }
                            else if (match == MATCH.NOT_EQUALS){ /** Doesn't equal */
                                for (Song s : in){
                                    if (s.songId != id) filteredSongs.add(s);
                                }
                            }
                            break;
                        case NAME: /** Name */
                            if (match == MATCH.EQUALS){ /** Equals */
                                for (Song s : in){
                                    if (s.songName.equals(value)) filteredSongs.add(s);
                                }
                            }
                            else if (match == MATCH.NOT_EQUALS){ /** Doesn't equal */
                                for (Song s : in){
                                    if (!s.songName.equals(value)) filteredSongs.add(s);
                                }
                            }
                            else if (match == MATCH.CONTAINS){ /** Contains */
                                for (Song s : in){
                                    if (s.songName.contains(value)) filteredSongs.add(s);
                                }
                            }
                            else if (match == MATCH.NOT_CONTAINS){ /** Doesn't contain */
                                for (Song s : in){
                                    if (!s.songName.contains(value)) filteredSongs.add(s);
                                }
                            }
                            break;
                        case PLAY_COUNT: /** Play count */
                            final Long playCount = Long.getLong(value);
                            if (match == MATCH.EQUALS){ /** Equals */
                                for (Song s : in){
                                    if (s.playCount == playCount) filteredSongs.add(s);
                                }
                            }
                            else if (match == MATCH.NOT_EQUALS){ /** Doesn't equal */
                                for (Song s : in){
                                    if (s.playCount != playCount) filteredSongs.add(s);
                                }
                            }
                            else if (match == MATCH.LESS_THAN){ /** Is less than */
                                for (Song s : in){
                                    if (s.playCount < playCount) filteredSongs.add(s);
                                }
                            }
                            else if (match == MATCH.GREATER_THAN){ /** Is greater than */
                                for (Song s : in){
                                    if (s.playCount > playCount) filteredSongs.add(s);
                                }
                            }
                            break;
                        case SKIP_COUNT: /** Skip count */
                            final Long skipCount = Long.getLong(value);
                            if (match == MATCH.EQUALS){ /** Equals */
                                for (Song s : in){
                                    if (s.skipCount == skipCount) filteredSongs.add(s);
                                }
                            }
                            else if (match == MATCH.NOT_EQUALS){ /** Doesn't equal */
                                for (Song s : in){
                                    if (s.skipCount != skipCount) filteredSongs.add(s);
                                }
                            }
                            else if (match == MATCH.LESS_THAN){ /** Is less than */
                                for (Song s : in){
                                    if (s.skipCount < skipCount) filteredSongs.add(s);
                                }
                            }
                            else if (match == MATCH.GREATER_THAN){ /** Is greater than */
                                for (Song s : in){
                                    if (s.skipCount > skipCount) filteredSongs.add(s);
                                }
                            }
                            break;
                        case YEAR: /** Year */
                            //TODO
                            break;
                        case DATE_ADDED: /** Date added */
                            //TODO
                            break;
                    }
                    break;
                case ARTIST: /** Artist */
                    switch (field){
                        case ID: /** ID */
                            final long id = Long.getLong(value);
                            if (match == MATCH.EQUALS){ /** Equals */
                                for (Song s : in){
                                    if (s.artistId == id) filteredSongs.add(s);
                                }
                            }
                            else if (match == MATCH.NOT_EQUALS){ /** Doesn't equal */
                                for (Song s : in){
                                    if (s.artistId != id) filteredSongs.add(s);
                                }
                            }
                            break;
                        case NAME: /** Name */
                            if (match == MATCH.EQUALS){ /** Equals */
                                for (Song s : in){
                                    if (s.artistName.equals(value)) filteredSongs.add(s);
                                }
                            }
                            else if (match == MATCH.NOT_EQUALS){ /** Doesn't equal */
                                for (Song s : in){
                                    if (!s.artistName.equals(value)) filteredSongs.add(s);
                                }
                            }
                            else if (match == MATCH.CONTAINS){ /** Contains */
                                for (Song s : in){
                                    if (s.artistName.contains(value)) filteredSongs.add(s);
                                }
                            }
                            else if (match == MATCH.NOT_CONTAINS){ /** Doesn't contain */
                                for (Song s : in){
                                    if (!s.artistName.contains(value)) filteredSongs.add(s);
                                }
                            }
                            break;
                    }
                    break;
                case ALBUM: /** Album */
                    switch (field) {
                        case ID: /** ID */
                            final long id = Long.getLong(value);
                            if (match == MATCH.EQUALS) { /** Equals */
                                for (Song s : in) {
                                    if (s.albumId == id) filteredSongs.add(s);
                                }
                            } else if (match == MATCH.NOT_EQUALS) { /** Doesn't equal */
                                for (Song s : in) {
                                    if (s.albumId != id) filteredSongs.add(s);
                                }
                            }
                            break;
                        case NAME: /** Name */
                            if (match == MATCH.EQUALS) { /** Equals */
                                for (Song s : in) {
                                    if (s.albumName.equals(value)) filteredSongs.add(s);
                                }
                            } else if (match == MATCH.NOT_EQUALS) { /** Doesn't equal */
                                for (Song s : in) {
                                    if (!s.albumName.equals(value)) filteredSongs.add(s);
                                }
                            } else if (match == MATCH.CONTAINS) { /** Contains */
                                for (Song s : in) {
                                    if (s.albumName.contains(value)) filteredSongs.add(s);
                                }
                            } else if (match == MATCH.NOT_CONTAINS) { /** Doesn't contain */
                                for (Song s : in) {
                                    if (!s.albumName.contains(value)) filteredSongs.add(s);
                                }
                            }
                            break;
                        case YEAR: /** Year */
                            //TODO
                            break;
                    }
                    break;
                case GENRE: /** Genre */
                    switch (field) {
                        case ID: /** ID */
                            final Long id = Long.getLong(value);
                            if (match == MATCH.EQUALS) { /** Equals */
                                for (Song s : in) {
                                    if (s.genreId == id) filteredSongs.add(s);
                                }
                            } else if (match == MATCH.NOT_EQUALS) { /** Doesn't equal */
                                for (Song s : in) {
                                    if (s.genreId != id) filteredSongs.add(s);
                                }
                            }
                            break;
                        case NAME: /** Name */
                            //TODO
                            break;
                    }
                    break;
            }
            return filteredSongs;
        }
    }
}
