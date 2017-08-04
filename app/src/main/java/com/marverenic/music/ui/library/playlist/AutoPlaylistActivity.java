package com.marverenic.music.ui.library.playlist;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.marverenic.music.model.AutoPlaylist;
import com.marverenic.music.ui.BaseLibraryActivity;

public class AutoPlaylistActivity extends BaseLibraryActivity {

    private static final String PLAYLIST_EXTRA = "AutoPlaylistActivity.PLAYLIST";

    public static Intent newIntent(Context context, AutoPlaylist playlist) {
        Intent intent = new Intent(context, AutoPlaylistActivity.class);
        intent.putExtra(PLAYLIST_EXTRA, playlist);

        return intent;
    }

    @Override
    protected Fragment onCreateFragment(Bundle savedInstanceState) {
        AutoPlaylist playlist = getIntent().getParcelableExtra(PLAYLIST_EXTRA);
        return PlaylistFragment.newInstance(playlist);
    }

}
