package com.marverenic.music.player;

import android.media.audiofx.AudioEffect;

import timber.log.Timber;

public abstract class AudioEffectController<Effect extends AudioEffect> {

    private static final int NO_AUDIO_SESSION_ID = 0;

    private Effect mEffect;
    private Player mPlayer;
    private int mAudioSessionId;

    private final Player.OnAudioSessionIdChangeListener mIdListener = this::onNextAudioSessionId;

    public AudioEffectController() {
        mAudioSessionId = NO_AUDIO_SESSION_ID;
    }

    public Effect getEffect() {
        return mEffect;
    }

    protected abstract Effect onInitializeEffect(int audioSessionId);

    protected void onReleaseEffect(Effect effect, int audioSessionId) {
        if (mEffect != null) {
            mEffect.release();
        }

        mEffect = null;
    }

    public void setPlayer(Player player) {
        if (mPlayer != null) {
            throw new IllegalStateException("AudioEffectController is already bound");
        }

        mPlayer = player;
        player.addAudioSessionIdListener(mIdListener);
        onNextAudioSessionId(mPlayer.getAudioSessionId());
    }

    private void onNextAudioSessionId(int audioSessionId) {
        if (audioSessionId == NO_AUDIO_SESSION_ID) {
            Timber.w("The bound Player has no audio session ID. Cannot apply audio effect");
        }
        if (audioSessionId == mAudioSessionId) {
            return;
        }

        onReleaseEffect(mEffect, mAudioSessionId);

        mAudioSessionId = mPlayer.getAudioSessionId();
        mEffect = onInitializeEffect(mAudioSessionId);
    }

    public void release() {
        mPlayer.removeAudioSessionIdListener(mIdListener);
        mPlayer = null;

        onReleaseEffect(mEffect, mAudioSessionId);
        mAudioSessionId = NO_AUDIO_SESSION_ID;
    }

    public static abstract class Generator<Effect extends AudioEffect> {

        protected abstract Effect onInitializeEffect(int audioSessionId);

        protected void onReleaseEffect(Effect effect, int audioSessionId) {
            // Optional.
        }

        public final AudioEffectController<Effect> generate() {
            return new AudioEffectController<Effect>() {

                @Override
                protected Effect onInitializeEffect(int audioSessionId) {
                    return Generator.this.onInitializeEffect(audioSessionId);
                }

                @Override
                protected void onReleaseEffect(Effect effect, int audioSessionId) {
                    super.onReleaseEffect(effect, audioSessionId);
                    Generator.this.onReleaseEffect(effect, audioSessionId);
                }
            };
        }

    }

}
