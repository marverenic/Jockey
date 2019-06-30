package com.marverenic.music.ui.library.artist.contents;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;

import com.marverenic.music.model.Artist;
import com.marverenic.music.ui.BaseLibraryActivity;

public class ArtistActivity extends BaseLibraryActivity {

    private static final String ARTIST_EXTRA = "ArtistActivity.ARTIST";

    public static Intent newIntent(Context context, Artist artist) {
        Intent intent = new Intent(context, ArtistActivity.class);
        intent.putExtra(ARTIST_EXTRA, artist);

        return intent;
    }

    @Override
    protected Fragment onCreateFragment(Bundle savedInstanceState) {
        Artist artist = getIntent().getParcelableExtra(ARTIST_EXTRA);
        return ArtistFragment.newInstance(artist);
    }

    @Override
    public boolean isToolbarCollapsing() {
        return true;
    }

}
