package com.marverenic.music.activity;

import android.media.AudioManager;
import android.os.Bundle;

import com.marverenic.music.R;
import com.marverenic.music.adapters.PlaylistEditAdapter;
import com.marverenic.music.instances.LibraryScanner;
import com.marverenic.music.instances.Playlist;
import com.marverenic.music.utils.Themes;
import com.mobeta.android.dslv.DragSortListView;

public class PlaylistActivity extends BaseActivity{

    public static final String PLAYLIST_ENTRY = "playlist_entry";
    private Playlist playlist;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setContentLayout(R.layout.page_editable_list);
        setContentView(R.id.list);
        super.onCreate(savedInstanceState);

        Themes.setTheme(this);

        final Object parent = getIntent().getParcelableExtra(PLAYLIST_ENTRY);

        if (parent instanceof Playlist) {
            playlist = (Playlist) parent;

            if (getActionBar() != null) getActionBar().setTitle(playlist.playlistName);
        }

        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    public void onResume() {
        update();

        // Recreate the adapter in case the playlist has been edited since this activity was paused
        // The adapter will initialize attach itself and all necessary controllers in its constructor
        // There is no need to create a variable for it
        if (playlist != null) new PlaylistEditAdapter(
                LibraryScanner.getPlaylistEntries(this, playlist),
                playlist,
                this,
                (DragSortListView) findViewById(R.id.list));

        super.onResume();
    }

    @Override
    public void themeActivity() {
        Themes.themeActivity(R.layout.page_editable_list, getWindow().getDecorView().findViewById(android.R.id.content), this);
    }

}
