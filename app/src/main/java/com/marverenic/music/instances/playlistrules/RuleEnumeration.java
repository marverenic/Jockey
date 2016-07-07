package com.marverenic.music.instances.playlistrules;

import android.support.annotation.StringRes;
import android.text.InputType;

import com.marverenic.music.R;
import com.marverenic.music.instances.playlistrules.AutoPlaylistRule.Field;
import com.marverenic.music.instances.playlistrules.AutoPlaylistRule.Match;

import java.util.Arrays;
import java.util.List;

import static com.marverenic.music.instances.playlistrules.AutoPlaylistRule.CONTAINS;
import static com.marverenic.music.instances.playlistrules.AutoPlaylistRule.DATE_ADDED;
import static com.marverenic.music.instances.playlistrules.AutoPlaylistRule.DATE_PLAYED;
import static com.marverenic.music.instances.playlistrules.AutoPlaylistRule.EQUALS;
import static com.marverenic.music.instances.playlistrules.AutoPlaylistRule.GREATER_THAN;
import static com.marverenic.music.instances.playlistrules.AutoPlaylistRule.ID;
import static com.marverenic.music.instances.playlistrules.AutoPlaylistRule.LESS_THAN;
import static com.marverenic.music.instances.playlistrules.AutoPlaylistRule.NAME;
import static com.marverenic.music.instances.playlistrules.AutoPlaylistRule.NOT_EQUALS;
import static com.marverenic.music.instances.playlistrules.AutoPlaylistRule.PLAY_COUNT;
import static com.marverenic.music.instances.playlistrules.AutoPlaylistRule.SKIP_COUNT;

public enum RuleEnumeration {

    IS(R.string.rule_is,
            ID, EQUALS, InputType.TYPE_NULL),
    ISNT(R.string.rule_isnt,
            ID, NOT_EQUALS, InputType.TYPE_NULL),
    NAME_IS(R.string.rule_name_is,
            NAME, EQUALS, InputType.TYPE_TEXT_FLAG_CAP_WORDS),
    NAME_ISNT(R.string.rule_name_isnt,
            NAME, NOT_EQUALS, InputType.TYPE_TEXT_FLAG_CAP_WORDS),
    NAME_CONTAINS(R.string.rule_name_contains,
            NAME, CONTAINS, InputType.TYPE_TEXT_FLAG_CAP_WORDS),
    NAME_DOESNT_CONTAIN(R.string.rule_name_doesnt_contain,
            NAME, CONTAINS, InputType.TYPE_TEXT_FLAG_CAP_WORDS),
    PLAY_COUNT_LESS_THAN(R.string.rule_play_count_lt,
            PLAY_COUNT, LESS_THAN, InputType.TYPE_CLASS_NUMBER),
    PLAY_COUNT_EQUALS(R.string.rule_play_count_eq,
            PLAY_COUNT, EQUALS, InputType.TYPE_CLASS_NUMBER),
    PLAY_COUNT_GREATER_THAN(R.string.rule_play_count_gt,
            PLAY_COUNT, GREATER_THAN, InputType.TYPE_CLASS_NUMBER),
    SKIP_COUNT_LESS_THAN(R.string.rule_skip_count_lt,
            SKIP_COUNT, LESS_THAN, InputType.TYPE_CLASS_NUMBER),
    SKIP_COUNT_EQUALS(R.string.rule_skip_count_eq,
            SKIP_COUNT, EQUALS, InputType.TYPE_CLASS_NUMBER),
    SKIP_COUNT_GREATER_THAN(R.string.rule_skip_count_gt,
            SKIP_COUNT, GREATER_THAN, InputType.TYPE_CLASS_NUMBER),
    ADDED_BEFORE(R.string.rule_added_before,
            DATE_ADDED, LESS_THAN, InputType.TYPE_CLASS_DATETIME),
    ADDED_ON(R.string.rule_added_on,
            DATE_ADDED, EQUALS, InputType.TYPE_CLASS_DATETIME),
    ADDED_AFTER(R.string.rule_added_after,
            DATE_ADDED, GREATER_THAN, InputType.TYPE_CLASS_DATETIME),
    PLAYED_BEFORE(R.string.rule_played_before,
            DATE_PLAYED, LESS_THAN, InputType.TYPE_CLASS_DATETIME),
    PLAYED_ON(R.string.rule_played_on,
            DATE_PLAYED, EQUALS, InputType.TYPE_CLASS_DATETIME),
    PLAYER_AFTER(R.string.rule_played_after,
            DATE_PLAYED, GREATER_THAN, InputType.TYPE_CLASS_DATETIME);

    @StringRes private int mNameRes;
    @Field private int mField;
    @Match private int mMatch;
    private int mInputType;

    RuleEnumeration(@StringRes int nameRes, @Field int field, @Match int match, int inputType) {
        mNameRes = nameRes;
        mField = field;
        mMatch = match;
        mInputType = inputType;
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
