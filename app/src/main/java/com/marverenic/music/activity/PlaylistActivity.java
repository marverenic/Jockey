package com.marverenic.music.activity;

import android.media.AudioManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.marverenic.music.R;
import com.marverenic.music.adapters.PlaylistEditAdapter;
import com.marverenic.music.instances.Library;
import com.marverenic.music.instances.LibraryScanner;
import com.marverenic.music.instances.Playlist;
import com.marverenic.music.instances.Song;
import com.marverenic.music.utils.Themes;
import com.mobeta.android.dslv.DragSortListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

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

            getSupportActionBar().setTitle(playlist.playlistName);
        }

        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    public void onResume() {
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
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.playlist_sort, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_sort_name:
                if (playlist != null){
                    ArrayList<Song> sortedList = LibraryScanner.getPlaylistEntries(this, playlist);
                    Library.sortSongList(sortedList);
                    LibraryScanner.editPlaylist(this, playlist, sortedList);

                    new PlaylistEditAdapter(
                            LibraryScanner.getPlaylistEntries(this, playlist),
                            playlist,
                            this,
                            (DragSortListView) findViewById(R.id.list));

                    Toast.makeText(
                            this,
                            String.format(getResources().getString(R.string.message_sorted_playlist_name), playlist),
                            Toast.LENGTH_SHORT)
                            .show();
                }
                return true;
            case R.id.action_sort_artist:
                if (playlist != null){
                    ArrayList<Song> sortedList = LibraryScanner.getPlaylistEntries(this, playlist);

                    Comparator<Song> artistComparator = new Comparator<Song>() {
                        @Override
                        public int compare(Song o1, Song o2) {
                            String o1c = o1.artistName.toLowerCase(Locale.ENGLISH);
                            String o2c = o2.artistName.toLowerCase(Locale.ENGLISH);
                            if (o1c.startsWith("the ")) {
                                o1c = o1c.substring(4);
                            } else if (o1c.startsWith("a ")) {
                                o1c = o1c.substring(2);
                            }
                            if (o2c.startsWith("the ")) {
                                o2c = o2c.substring(4);
                            } else if (o2c.startsWith("a ")) {
                                o2c = o2c.substring(2);
                            }
                            if (!o1c.matches("[a-z]") && o2c.matches("[a-z]")) {
                                return o2c.compareTo(o1c);
                            }
                            return o1c.compareTo(o2c);
                        }
                    };
                    Collections.sort(sortedList, artistComparator);

                    LibraryScanner.editPlaylist(this, playlist, sortedList);

                    new PlaylistEditAdapter(
                            LibraryScanner.getPlaylistEntries(this, playlist),
                            playlist,
                            this,
                            (DragSortListView) findViewById(R.id.list));

                    Toast.makeText(
                            this,
                            String.format(getResources().getString(R.string.message_sorted_playlist_artist), playlist),
                            Toast.LENGTH_SHORT)
                            .show();
                }
                return true;
            case R.id.action_sort_album:
                if (playlist != null){
                    ArrayList<Song> sortedList = LibraryScanner.getPlaylistEntries(this, playlist);

                    Comparator<Song> albumComparator = new Comparator<Song>() {
                        @Override
                        public int compare(Song o1, Song o2) {
                            String o1c = o1.albumName.toLowerCase(Locale.ENGLISH);
                            String o2c = o2.albumName.toLowerCase(Locale.ENGLISH);
                            if (o1c.startsWith("the ")) {
                                o1c = o1c.substring(4);
                            } else if (o1c.startsWith("a ")) {
                                o1c = o1c.substring(2);
                            }
                            if (o2c.startsWith("the ")) {
                                o2c = o2c.substring(4);
                            } else if (o2c.startsWith("a ")) {
                                o2c = o2c.substring(2);
                            }
                            if (!o1c.matches("[a-z]") && o2c.matches("[a-z]")) {
                                return o2c.compareTo(o1c);
                            }
                            return o1c.compareTo(o2c);
                        }
                    };
                    Collections.sort(sortedList, albumComparator);

                    LibraryScanner.editPlaylist(this, playlist, sortedList);

                    new PlaylistEditAdapter(
                            LibraryScanner.getPlaylistEntries(this, playlist),
                            playlist,
                            this,
                            (DragSortListView) findViewById(R.id.list));

                    Toast.makeText(
                            this,
                            String.format(getResources().getString(R.string.message_sorted_playlist_album), playlist),
                            Toast.LENGTH_SHORT)
                            .show();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void themeActivity() {
        super.themeActivity();
        findViewById(R.id.list).setBackgroundColor(Themes.getBackgroundElevated());
    }

    @Override
    public void update() {
        updateMiniplayer();
    }

}
