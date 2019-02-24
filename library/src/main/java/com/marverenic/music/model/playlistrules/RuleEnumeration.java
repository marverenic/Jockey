package com.marverenic.music.model.playlistrules;

import android.support.annotation.StringRes;
import android.text.InputType;

import com.marverenic.music.library.R;
import com.marverenic.music.model.playlistrules.AutoPlaylistRule.Field;
import com.marverenic.music.model.playlistrules.AutoPlaylistRule.Match;

import java.util.Arrays;
import java.util.List;

import static com.marverenic.music.model.playlistrules.AutoPlaylistRule.CONTAINS;
import static com.marverenic.music.model.playlistrules.AutoPlaylistRule.DATE_ADDED;
import static com.marverenic.music.model.playlistrules.AutoPlaylistRule.DATE_PLAYED;
import static com.marverenic.music.model.playlistrules.AutoPlaylistRule.EQUALS;
import static com.marverenic.music.model.playlistrules.AutoPlaylistRule.GREATER_THAN;
import static com.marverenic.music.model.playlistrules.AutoPlaylistRule.ID;
import static com.marverenic.music.model.playlistrules.AutoPlaylistRule.LESS_THAN;
import static com.marverenic.music.model.playlistrules.AutoPlaylistRule.NAME;
import static com.marverenic.music.model.playlistrules.AutoPlaylistRule.NOT_EQUALS;
import static com.marverenic.music.model.playlistrules.AutoPlaylistRule.PLAY_COUNT;
import static com.marverenic.music.model.playlistrules.AutoPlaylistRule.SKIP_COUNT;

public enum RuleEnumeration {

    IS(R.string.rule_is, ID, EQUALS),
    ISNT(R.string.rule_isnt, ID, NOT_EQUALS),
    NAME_IS(R.string.rule_name_is, NAME, EQUALS),
    NAME_ISNT(R.string.rule_name_isnt, NAME, NOT_EQUALS),
    NAME_CONTAINS(R.string.rule_name_contains, NAME, CONTAINS),
    NAME_DOESNT_CONTAIN(R.string.rule_name_doesnt_contain, NAME, CONTAINS),
    PLAY_COUNT_LESS_THAN(R.string.rule_play_count_lt, PLAY_COUNT, LESS_THAN),
    PLAY_COUNT_EQUALS(R.string.rule_play_count_eq, PLAY_COUNT, EQUALS),
    PLAY_COUNT_GREATER_THAN(R.string.rule_play_count_gt, PLAY_COUNT, GREATER_THAN),
    SKIP_COUNT_LESS_THAN(R.string.rule_skip_count_lt, SKIP_COUNT, LESS_THAN),
    SKIP_COUNT_EQUALS(R.string.rule_skip_count_eq, SKIP_COUNT, EQUALS),
    SKIP_COUNT_GREATER_THAN(R.string.rule_skip_count_gt, SKIP_COUNT, GREATER_THAN),
    ADDED_BEFORE(R.string.rule_added_before, DATE_ADDED, LESS_THAN),
    ADDED_ON(R.string.rule_added_on, DATE_ADDED, EQUALS),
    ADDED_AFTER(R.string.rule_added_after, DATE_ADDED, GREATER_THAN),
    PLAYED_BEFORE(R.string.rule_played_before, DATE_PLAYED, LESS_THAN),
    PLAYED_ON(R.string.rule_played_on, DATE_PLAYED, EQUALS),
    PLAYER_AFTER(R.string.rule_played_after, DATE_PLAYED, GREATER_THAN);

    private static final int NO_INPUT_TYPE = InputType.TYPE_NULL;
    private static final int TEXT_INPUT_TYPE = InputType.TYPE_CLASS_TEXT
            | InputType.TYPE_TEXT_FLAG_CAP_WORDS | InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE
            | InputType.TYPE_TEXT_FLAG_AUTO_CORRECT;
    private static final int NUMBER_INPUT_TYPE = InputType.TYPE_CLASS_NUMBER;
    private static final int DATE_INPUT_TYPE = InputType.TYPE_CLASS_DATETIME;

    @StringRes private int mNameRes;
    @Field private int mField;
    @Match private int mMatch;
    private int mInputType;

    static {
        IS.mInputType = NO_INPUT_TYPE;
        ISNT.mInputType = NO_INPUT_TYPE;
        NAME_IS.mInputType = TEXT_INPUT_TYPE;
        NAME_ISNT.mInputType = TEXT_INPUT_TYPE;
        NAME_CONTAINS.mInputType = TEXT_INPUT_TYPE;
        NAME_DOESNT_CONTAIN.mInputType = TEXT_INPUT_TYPE;
        PLAY_COUNT_LESS_THAN.mInputType = NUMBER_INPUT_TYPE;
        PLAY_COUNT_EQUALS.mInputType = NUMBER_INPUT_TYPE;
        PLAY_COUNT_GREATER_THAN.mInputType = NUMBER_INPUT_TYPE;
        SKIP_COUNT_LESS_THAN.mInputType = NUMBER_INPUT_TYPE;
        SKIP_COUNT_EQUALS.mInputType = NUMBER_INPUT_TYPE;
        SKIP_COUNT_GREATER_THAN.mInputType = NUMBER_INPUT_TYPE;
        ADDED_BEFORE.mInputType = DATE_INPUT_TYPE;
        ADDED_ON.mInputType = DATE_INPUT_TYPE;
        ADDED_AFTER.mInputType = DATE_INPUT_TYPE;
        PLAYED_BEFORE.mInputType = DATE_INPUT_TYPE;
        PLAYED_ON.mInputType = DATE_INPUT_TYPE;
        PLAYER_AFTER.mInputType = DATE_INPUT_TYPE;
    }

    RuleEnumeration(@StringRes int nameRes, @Field int field, @Match int match) {
        mNameRes = nameRes;
        mField = field;
        mMatch = match;
    }

    @StringRes
    public int getNameRes() {
        return mNameRes;
    }

    @Field
    public int getField() {
        return mField;
    }

    @Match
    public int getMatch() {
        return mMatch;
    }

    public int getInputType() {
        return mInputType;
    }

    public long getId() {
        return (long) getField() << 32 | getMatch();
    }

    public static RuleEnumeration from(@Field int field, @Match int match) {
        for (RuleEnumeration rule : values()) {
            if (rule.getField() == field && rule.getMatch() == match) {
                return rule;
            }
        }
        return null;
    }

    public static List<RuleEnumeration> getAllSongRules() {
        return Arrays.asList(values());
    }

    public static List<RuleEnumeration> getAllArtistRules() {
        return Arrays.asList(IS, ISNT, NAME_IS, NAME_ISNT, NAME_CONTAINS, NAME_DOESNT_CONTAIN);
    }

    public static List<RuleEnumeration> getAllAlbumRules() {
        return Arrays.asList(IS, ISNT, NAME_IS, NAME_ISNT, NAME_CONTAINS, NAME_DOESNT_CONTAIN);
    }

    public static List<RuleEnumeration> getAllPlaylistRules() {
        return Arrays.asList(IS, ISNT, NAME_IS, NAME_ISNT, NAME_CONTAINS, NAME_DOESNT_CONTAIN);
    }

    public static List<RuleEnumeration> getAllGenreRules() {
        return Arrays.asList(IS, ISNT, NAME_IS, NAME_ISNT, NAME_CONTAINS, NAME_DOESNT_CONTAIN);
    }
}
