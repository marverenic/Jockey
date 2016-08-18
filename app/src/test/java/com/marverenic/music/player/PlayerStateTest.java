package com.marverenic.music.player;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.marverenic.music.player.PlayerState.COMPLETED;
import static com.marverenic.music.player.PlayerState.ERROR;
import static com.marverenic.music.player.PlayerState.IDLE;
import static com.marverenic.music.player.PlayerState.INITIALIZED;
import static com.marverenic.music.player.PlayerState.PAUSED;
import static com.marverenic.music.player.PlayerState.PREPARED;
import static com.marverenic.music.player.PlayerState.PREPARING;
import static com.marverenic.music.player.PlayerState.STARTED;
import static com.marverenic.music.player.PlayerState.STOPPED;
import static org.junit.Assert.assertEquals;

public class PlayerStateTest {

    @Test
    public void audioSessionIdIsCorrect() throws Exception {
        assertValidStatesForProperty("setAudioSessionId", PlayerState::canSetAudioSessionId,
                IDLE);
    }

    @Test
    public void audioStreamTypeIsCorrect() throws Exception {
        assertValidStatesForProperty("setAudioStreamType", PlayerState::canSetAudioStreamType,
                IDLE, INITIALIZED);
    }

    @Test
    public void setDataSourceIsCorrect() throws Exception {
        assertValidStatesForProperty("setDataSource", PlayerState::canSetDataSource,
                IDLE);
    }

    @Test
    public void prepareIsCorrect() throws Exception {
        assertValidStatesForProperty("prepare", PlayerState::canPrepare,
                INITIALIZED, STOPPED);
    }

    @Test
    public void getDurationIsCorrect() throws Exception {
        assertValidStatesForProperty("getDuration", PlayerState::canGetDuration,
                PREPARED, STARTED, PAUSED, STOPPED, COMPLETED);
    }

    @Test
    public void getCurrentPositionIsCorrect() throws Exception {
        assertValidStatesForProperty("getCurrentPosition", PlayerState::canGetCurrentPosition,
                IDLE, INITIALIZED, PREPARED, STARTED, PAUSED, STOPPED, COMPLETED);
    }

    @Test
    public void startIsCorrect() throws Exception {
        assertValidStatesForProperty("start", PlayerState::canStart,
                PREPARED, STARTED, PAUSED, COMPLETED);
    }

    @Test
    public void pauseIsCorrect() throws Exception {
        assertValidStatesForProperty("pause", PlayerState::canPause,
                STARTED, PAUSED, COMPLETED);
    }

    @Test
    public void seekIsCorrect() throws Exception {
        assertValidStatesForProperty("seek", PlayerState::canSeek,
                PREPARED, STARTED, PAUSED, COMPLETED);
    }

    @Test
    public void setVolumeIsCorrect() throws Exception {
        assertValidStatesForProperty("setVolume", PlayerState::canSetVolume,
                IDLE, INITIALIZED, STOPPED, PREPARED, STARTED, PAUSED, COMPLETED);
    }

    @Test
    public void stopIsCorrect() throws Exception {
        assertValidStatesForProperty("stop", PlayerState::canStop,
                PREPARED, STARTED, STOPPED, PAUSED, COMPLETED);
    }

    @Test
    public void resetIsCorrect() throws Exception {
        assertValidStatesForProperty("reset", PlayerState::canReset,
                IDLE, INITIALIZED, PREPARED, PREPARING, STARTED, PAUSED, STOPPED, COMPLETED, ERROR);
    }

    private interface Property {
        boolean isValidState(PlayerState state);
    }

    private void assertValidStatesForProperty(String propertyName, Property property,
                                              PlayerState... validStates) {

        for (PlayerState state : validStates) {
            assertEquals(state + " should be a valid state for " + propertyName,
                    true, property.isValidState(state));
        }

        List<PlayerState> invalidStates = new ArrayList<>(Arrays.asList(PlayerState.values()));
        invalidStates.removeAll(Arrays.asList(validStates));

        for (PlayerState state : invalidStates) {
            assertEquals(state + " is not a valid state for " + propertyName,
                    false, property.isValidState(state));
        }
    }

}
