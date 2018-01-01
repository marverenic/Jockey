package com.marverenic.music.ui.library.playlist.contents.edit;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.marverenic.music.model.AutoPlaylist;
import com.marverenic.music.ui.SingleFragmentActivity;

public class AutoPlaylistEditActivity extends SingleFragmentActivity {

    private static final String PLAYLIST_EXTRA = "AutoPlaylistEditActivity.PLAYLIST";

    public static Intent newIntent(Context context) {
        return newIntent(context, null);
    }

    public static Intent newIntent(Context context, AutoPlaylist target) {
        Intent intent = new Intent(context, AutoPlaylistEditActivity.class);
        intent.putExtra(PLAYLIST_EXTRA, target);

        return intent;
    }

    @Override
    protected Fragment onCreateFragment(Bundle savedInstanceState) {
        AutoPlaylist playlist = getIntent().getParcelableExtra(PLAYLIST_EXTRA);
        return AutoPlaylistEditFragment.newInstance(playlist);
    }

}
