package com.marverenic.music;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.marverenic.music.adapters.SongListAdapter;
import com.marverenic.music.instances.Album;
import com.marverenic.music.instances.Artist;
import com.marverenic.music.instances.Genre;
import com.marverenic.music.instances.Library;
import com.marverenic.music.instances.Playlist;
import com.marverenic.music.instances.Song;
import com.marverenic.music.pages.AlbumPage;
import com.marverenic.music.pages.ArtistPage;
import com.marverenic.music.pages.GenrePage;
import com.marverenic.music.pages.PlaylistPage;
import com.marverenic.music.utils.Debug;
import com.marverenic.music.utils.Navigate;
import com.marverenic.music.utils.Themes;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.ArrayList;

public class LibraryPageActivity extends Activity implements View.OnClickListener {

    public enum Type { PLAYLIST, ARTIST, ALBUM, GENRE, UNKNOWN }

    private Type type;

    private BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            update();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Themes.setTheme(this);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        Object parent = getIntent().getParcelableExtra("entry");

        if (parent != null) {

            setContentView(R.layout.fragment_list_page);
            final ListView songListView = (ListView) findViewById(R.id.list);
            ArrayList<Song> songEntries = new ArrayList<>();

            if (parent instanceof Playlist) {
                type = Type.PLAYLIST;
                PlaylistPage.onCreate(parent, songEntries, this);
            }
            else if (parent instanceof Album) {
                type = Type.ALBUM;
                AlbumPage.onCreate(parent, songEntries, songListView, this);
            }
            else if (parent instanceof Genre) {
                type = Type.GENRE;
                GenrePage.onCreate(parent, songEntries, this);
            }
            else if (parent.getClass().equals(Artist.class)) {
                type = Type.ARTIST;
                ArtistPage.onCreate(parent, this);
            }

            if (type != Type.ARTIST) {
                if (type != Type.ALBUM) {
                    songEntries = Library.sortSongList(songEntries);
                }
                else{
                    songListView.setFastScrollEnabled(false);
                }

                SongListAdapter adapter = new SongListAdapter(songEntries, this);
                songListView.setAdapter(adapter);
                songListView.setOnItemClickListener(adapter);
                songListView.setOnItemLongClickListener(adapter);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (getActionBar() != null)
                    getActionBar().setElevation(getResources().getDimension(R.dimen.header_elevation));
                else
                    Debug.log(Debug.LogLevel.WTF, "LibraryPageActivity", "Couldn't find the action bar", this);
            }

            Themes.themeActivity(R.layout.fragment_list, getWindow().findViewById(android.R.id.content), this);
        } else {
            type = Type.UNKNOWN;
            setContentView(R.layout.page_error);
            Debug.log(Debug.LogLevel.WTF, "LibraryPageActivity", "An invalid item was passed as the parent object", this);
        }
        registerReceiver(updateReceiver, new IntentFilter(Player.UPDATE_BROADCAST));
        update();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.miniplayer:
                Navigate.to(this, NowPlayingActivity.class);
                update();
                break;
            case R.id.playButton:
                PlayerService.togglePlay();
                update();
                break;
            case R.id.skipButton:
                PlayerService.skip();
                update();
                break;
        }
    }

    @Override
    public void onDestroy() {
        try {
            unregisterReceiver(updateReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    public void update() {
        if (type != Type.UNKNOWN) {
            if (PlayerService.isInitialized() && PlayerService.getNowPlaying() != null) {
                final TextView songTitle = (TextView) findViewById(R.id.textNowPlayingTitle);
                final TextView artistName = (TextView) findViewById(R.id.textNowPlayingDetail);

                songTitle.setText(PlayerService.getNowPlaying().songName);
                artistName.setText(PlayerService.getNowPlaying().artistName);

                if (!PlayerService.isPlaying()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        ((ImageButton) findViewById(R.id.playButton)).setImageResource(R.drawable.ic_vector_play);
                        ((ImageButton) findViewById(R.id.playButton)).setImageTintList(ColorStateList.valueOf(Themes.getListText()));
                    } else {
                        if (Themes.isLight(this)) {
                            ((ImageButton) findViewById(R.id.playButton)).setImageResource(R.drawable.ic_play_miniplayer_light);
                        } else {
                            ((ImageButton) findViewById(R.id.playButton)).setImageResource(R.drawable.ic_play_miniplayer);
                        }
                    }
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        ((ImageButton) findViewById(R.id.playButton)).setImageResource(R.drawable.ic_vector_pause);
                        ((ImageButton) findViewById(R.id.playButton)).setImageTintList(ColorStateList.valueOf(Themes.getListText()));
                    } else {
                        if (Themes.isLight(this)) {
                            ((ImageButton) findViewById(R.id.playButton)).setImageResource(R.drawable.ic_pause_miniplayer_light);
                        } else {
                            ((ImageButton) findViewById(R.id.playButton)).setImageResource(R.drawable.ic_pause_miniplayer);
                        }
                    }
                }

                if (PlayerService.getArt() != null) {
                    ((ImageView) findViewById(R.id.imageArtwork)).setImageBitmap(PlayerService.getArt());
                } else {
                    ((ImageView) findViewById(R.id.imageArtwork)).setImageResource(R.drawable.art_default);
                }

                FrameLayout.LayoutParams listLayoutParams = (FrameLayout.LayoutParams) (findViewById(R.id.list)).getLayoutParams();
                listLayoutParams.bottomMargin = getResources().getDimensionPixelSize(R.dimen.now_playing_ticker_height);
                (findViewById(R.id.list)).setLayoutParams(listLayoutParams);

                FrameLayout.LayoutParams playerLayoutParams = (FrameLayout.LayoutParams) (findViewById(R.id.miniplayer)).getLayoutParams();
                playerLayoutParams.height = getResources().getDimensionPixelSize(R.dimen.now_playing_ticker_height);
                (findViewById(R.id.miniplayer)).setLayoutParams(playerLayoutParams);
            } else {
                FrameLayout.LayoutParams listLayoutParams = (FrameLayout.LayoutParams) (findViewById(R.id.list)).getLayoutParams();
                listLayoutParams.bottomMargin = 0;
                (findViewById(R.id.list)).setLayoutParams(listLayoutParams);

                FrameLayout.LayoutParams playerLayoutParams = (FrameLayout.LayoutParams) (findViewById(R.id.miniplayer)).getLayoutParams();
                playerLayoutParams.height = 0;
                (findViewById(R.id.miniplayer)).setLayoutParams(playerLayoutParams);
            }
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
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
    public void onPause() {
        if (ImageLoader.getInstance().isInited()) {
            ImageLoader.getInstance().clearMemoryCache();
        }
        super.onPause();
    }
}
