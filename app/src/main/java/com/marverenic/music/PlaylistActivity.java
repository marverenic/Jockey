package com.marverenic.music;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import com.marverenic.music.adapters.PlaylistEditAdapter;
import com.marverenic.music.fragments.MiniplayerManager;
import com.marverenic.music.instances.LibraryScanner;
import com.marverenic.music.instances.Playlist;
import com.marverenic.music.utils.Debug;
import com.marverenic.music.utils.Navigate;
import com.marverenic.music.utils.Themes;
import com.mobeta.android.dslv.DragSortListView;

public class PlaylistActivity extends Activity implements View.OnClickListener{

    private BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            update();
        }
    };

    public static final String PLAYLIST_ENTRY = "playlist_entry";
    private Playlist playlist;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Themes.setTheme(this);
        setContentView(R.layout.page_editable_list);
        Themes.themeActivity(R.layout.page_editable_list, getWindow().getDecorView().findViewById(android.R.id.content), this);

        final Object parent = getIntent().getParcelableExtra(PLAYLIST_ENTRY);

        if (parent instanceof Playlist) {
            playlist = (Playlist) parent;

            if (getActionBar() != null) getActionBar().setTitle(playlist.playlistName);

            registerReceiver(updateReceiver, new IntentFilter(Player.UPDATE_BROADCAST));
        }

        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    public void onResume() {
        update();
        Themes.setApplicationIcon(this);

        // Recreate the adapter in case the playlist has been edited since this activity was paused
        // The adapter will initialize attach itself and all necessary controllers in its constructor
        // There is no need to create a variable for it
        if (playlist != null) new PlaylistEditAdapter(
                LibraryScanner.getPlaylistEntries(this, playlist),
                playlist,
                this,
                (DragSortListView) findViewById(R.id.list));

        registerReceiver(updateReceiver, new IntentFilter(Player.UPDATE_BROADCAST));
        super.onResume();
    }

    @Override
    public void onPause() {
        LibraryScanner.saveLibrary(this);
        try {
            unregisterReceiver(updateReceiver);
        } catch (Exception e) {
            Debug.log(Debug.LogLevel.ERROR, "PlaylistActivity", "Unable to unregister receiver", this);
        }
        super.onPause();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case android.R.id.home:
                Navigate.up(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Navigate.back(this);
    }

    @Override
    public void onClick(View v) {
        MiniplayerManager.onClick(v.getId(), this, R.id.list);
    }

    public void update() {
        MiniplayerManager.update(this, R.id.list);
    }

}
