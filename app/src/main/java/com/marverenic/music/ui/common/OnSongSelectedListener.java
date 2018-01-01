package com.marverenic.music.ui.common;

import android.app.Activity;

import com.marverenic.music.data.store.PreferenceStore;
import com.marverenic.music.ui.BaseLibraryActivity;

import timber.log.Timber;

public interface OnSongSelectedListener {

    static OnSongSelectedListener defaultImplementation(Activity activity,
                                                        PreferenceStore preferenceStore) {
        if (!(activity instanceof BaseLibraryActivity)) {
            Timber.w("Activity is not an instance of BaseLibraryActivity. Using no-op listener");
            return null;
        }

        return () -> {
            if (preferenceStore.openNowPlayingOnNewQueue()) {
                ((BaseLibraryActivity) activity).expandBottomSheet();
            }
        };
    }

    void onSongSelected();

}
