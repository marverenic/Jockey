package com.marverenic.music;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;

import com.marverenic.music.adapters.PlaylistEditAdapter;
import com.marverenic.music.instances.LibraryScanner;
import com.marverenic.music.instances.Playlist;
import com.marverenic.music.utils.Navigate;
import com.marverenic.music.utils.Themes;
import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;

public class PlaylistActivity extends Activity {

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

            final Context context = this;
            PlaylistEditAdapter adapter = new PlaylistEditAdapter(LibraryScanner.getPlaylistEntries(this, playlist), null, this, new PlaylistEditAdapter.OnDataRemoveListener() {
                @Override
                public void onRowRemoved(int row){
                    LibraryScanner.removePlaylistEntry(context, playlist, row);
                }
            });
            DragSortListView listView = (DragSortListView) findViewById(R.id.list);
            DragSortController controller = new PlaylistEditAdapter.dragSortController(listView, adapter, R.id.handle);
            listView.setOnItemClickListener(adapter);
            listView.setOnItemLongClickListener(adapter);
            listView.setAdapter(adapter);
            listView.setFloatViewManager(controller);
            listView.setOnTouchListener(controller);
            listView.setDragListener(new DragSortListView.DragListener() {
                @Override
                public void drag(int from, int to) {

                }
            });
        }

        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Navigate.back(this);
    }

}
