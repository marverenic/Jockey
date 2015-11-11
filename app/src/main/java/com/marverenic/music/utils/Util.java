package com.marverenic.music.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.audiofx.AudioEffect;

import com.marverenic.music.PlayerController;
import com.marverenic.music.instances.Song;

public class Util {

    public static Intent getSystemEqIntent(Context c) {
        Intent systemEqualizer = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
        systemEqualizer.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, c.getPackageName());
        systemEqualizer.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, PlayerController.getAudioSessionId());

        ActivityInfo info = systemEqualizer.resolveActivityInfo(c.getPackageManager(), 0);
        if (info != null && !info.name.startsWith("com.android.musicfx")) {
            return systemEqualizer;
        } else {
            return null;
        }
    }


    public static Bitmap fetchFullArt(Song song){
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();

        try {
            retriever.setDataSource(song.location);
            byte[] stream = retriever.getEmbeddedPicture();
            if (stream != null)
                return BitmapFactory.decodeByteArray(stream, 0, stream.length);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
