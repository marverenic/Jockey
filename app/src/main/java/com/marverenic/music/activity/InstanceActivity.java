package com.marverenic.music.activity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;

import com.marverenic.music.R;
import com.marverenic.music.adapters.SongListAdapter;
import com.marverenic.music.instances.Album;
import com.marverenic.music.instances.Artist;
import com.marverenic.music.instances.Genre;
import com.marverenic.music.instances.Library;
import com.marverenic.music.instances.LibraryScanner;
import com.marverenic.music.instances.Song;
import com.marverenic.music.utils.Debug;
import com.marverenic.music.utils.Fetch;
import com.marverenic.music.utils.Themes;

import java.util.ArrayList;

public class InstanceActivity extends BaseActivity {

    public enum Type { ALBUM, GENRE, UNKNOWN }

    private Type type;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Object parent = getIntent().getParcelableExtra("entry");
        if (parent == null || !(parent instanceof Album || parent instanceof Genre)){
            setContentLayout(R.layout.page_error);
            type = Type.UNKNOWN;
        }
        else{
            setContentLayout(R.layout.fragment_list_page);
        }
        setContentView(R.id.list_container);
        super.onCreate(savedInstanceState);

        if (parent != null && type != Type.UNKNOWN) {
            final ListView songListView = (ListView) findViewById(R.id.list);
            ArrayList<Song> songEntries = null;

            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(parent.toString());
            } else {
                Debug.log(Debug.LogLevel.WTF, "LibraryPageActivity", "Couldn't find the action bar", this);
            }

            if (parent instanceof Album) {
                type = Type.ALBUM;
                songEntries = LibraryScanner.getAlbumEntries((Album) parent);

                Bitmap art = Fetch.fetchAlbumArtLocal(((Album) parent).albumId);

                if (art != null) {
                    View artView = View.inflate(this, R.layout.album_header, null);
                    songListView.addHeaderView(artView, null, false);
                    ((ImageView) findViewById(R.id.header)).setImageBitmap(art);
                }
            } else if (parent instanceof Genre) {
                type = Type.GENRE;
                songEntries = LibraryScanner.getGenreEntries((Genre) parent);
            } else if (parent instanceof Artist) {
                Log.w("InstanceActivity","Artist pages are now part of ArtistActivity");
                finish();
            }

            if (songEntries != null) {
                SongListAdapter adapter;

                // Don't sort album entries
                if (type != Type.ALBUM) {
                    Library.sortSongList(songEntries);
                    adapter = new SongListAdapter(songEntries, this, true);
                } else {
                    adapter = new SongListAdapter(songEntries, this, false);
                }

                songListView.setAdapter(adapter);
                songListView.setOnItemClickListener(adapter);
            }
        } else {
            type = Type.UNKNOWN;
            setContentView(R.layout.page_error);
            Debug.log(Debug.LogLevel.WTF, "LibraryPageActivity", "An invalid item was passed as the parent object", this);
        }
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
