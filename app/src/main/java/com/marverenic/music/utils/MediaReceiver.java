package com.marverenic.music.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;

import com.marverenic.music.PlayerService;

public class MediaReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (PlayerService.isPlaying()) {
            if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
                if (intent.getIntExtra("state", -1) != 1) {
                    PlayerService.pause();
                }
            } else if (intent.getAction().equals(AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
                PlayerService.pause();
            }
        }
    }
}