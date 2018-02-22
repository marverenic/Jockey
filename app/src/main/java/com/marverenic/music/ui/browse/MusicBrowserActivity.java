package com.marverenic.music.ui.browse;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;

import com.marverenic.music.model.Song;
import com.marverenic.music.ui.SingleFragmentActivity;
import com.marverenic.music.utils.UriUtils;

import java.io.File;

public class MusicBrowserActivity extends SingleFragmentActivity {

    private static final String EXTRA_STARTING_DIRECTORY = "MusicBrowserActivity.StartingDirectory";

    public static Intent newIntent(Context context, Song targetSong) {
        String path = UriUtils.getPathFromUri(context, targetSong.getLocation());
        return newIntent(context, new File(path).getParentFile());
    }

    public static Intent newIntent(Context context, File startingDirectory) {
        Intent intent = new Intent(context, MusicBrowserActivity.class);
        intent.putExtra(EXTRA_STARTING_DIRECTORY, startingDirectory.getAbsolutePath());
        return intent;
    }

    @Override
    protected Fragment onCreateFragment(Bundle savedInstanceState) {
        String startingPath = getIntent().getStringExtra(EXTRA_STARTING_DIRECTORY);
        return MusicBrowserFragment.newInstance(new File(startingPath));
    }

    @Override
    public void setSupportActionBar(@Nullable Toolbar toolbar) {
        super.setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

}
