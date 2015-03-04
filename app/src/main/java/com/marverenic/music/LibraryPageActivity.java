package com.marverenic.music;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.marverenic.music.adapters.AlbumGridAdapter;
import com.marverenic.music.adapters.ArtistPageAdapter;
import com.marverenic.music.adapters.SongListAdapter;
import com.marverenic.music.instances.Album;
import com.marverenic.music.instances.Artist;
import com.marverenic.music.instances.Genre;
import com.marverenic.music.instances.Library;
import com.marverenic.music.instances.LibraryScanner;
import com.marverenic.music.instances.Playlist;
import com.marverenic.music.instances.Song;
import com.marverenic.music.utils.Debug;
import com.marverenic.music.utils.Fetch;
import com.marverenic.music.utils.Navigate;
import com.marverenic.music.utils.Themes;
import com.squareup.picasso.Picasso;

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
            ArrayList<Song> songEntries = null;
            ArrayList<Album> albumEntries;

            if (getActionBar() != null){
                getActionBar().setTitle(parent.toString());
            }
            else{
                Debug.log(Debug.LogLevel.WTF, "LibraryPageActivity", "Couldn't find the action bar", this);
            }

            if (parent instanceof Playlist) {
                type = Type.PLAYLIST;
                songEntries = LibraryScanner.getPlaylistEntries(this, (Playlist) parent);
            }
            else if (parent instanceof Album) {
                type = Type.ALBUM;
                songEntries = LibraryScanner.getAlbumEntries((Album) parent);

                Bitmap art = Fetch.fetchAlbumArtLocal(((Album) parent).albumId);

                if (art != null) {
                    View artView = View.inflate(this, R.layout.album_header, null);
                    songListView.addHeaderView(artView, null, false);
                    ((ImageView) findViewById(R.id.header)).setImageBitmap(art);
                }
            }
            else if (parent instanceof Genre) {
                type = Type.GENRE;
                songEntries = LibraryScanner.getGenreEntries((Genre) parent);
            }
            else if (parent.getClass().equals(Artist.class)) {
                type = Type.ARTIST;
                songEntries = LibraryScanner.getArtistSongEntries((Artist) parent);
                Library.sortSongList(songEntries);
                albumEntries = LibraryScanner.getArtistAlbumEntries((Artist) parent);
                Library.sortAlbumList(albumEntries);

                ListView list = (ListView) findViewById(R.id.list);
                initializeArtistHeader(list, albumEntries, this);
                ArtistPageAdapter adapter = new ArtistPageAdapter(this, songEntries, albumEntries);
                list.setAdapter(adapter);
                list.setOnItemClickListener(adapter);
                list.setOnItemLongClickListener(adapter);
            }

            if (type != Type.ARTIST && songEntries != null) {
                SongListAdapter adapter;

                // Don't sort album or playlist entries
                if (type != Type.ALBUM && type != Type.PLAYLIST) {
                    Library.sortSongList(songEntries);
                    adapter = new SongListAdapter(songEntries, this, true);
                }
                else {
                    adapter = new SongListAdapter(songEntries, this, false);
                }

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

    private static void initializeArtistHeader(final View parent, final ArrayList<Album> albums, Activity activity) {
        final Context context = activity;
        final View infoHeader = View.inflate(activity, R.layout.artist_header_info, null);

        final Handler handler = new Handler(Looper.getMainLooper());

        new Thread(new Runnable() {
            @Override
            public void run() {
                final Fetch.ArtistBio bio = Fetch.fetchArtistBio(context, albums.get(0).artistName);
                if (bio != null) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if(bio.artURL != null && !bio.artURL.equals(""))
                                Picasso.with(context).load(bio.artURL).placeholder(R.drawable.art_default)
                                        .resizeDimen(R.dimen.grid_art_size, R.dimen.grid_art_size)
                                        .into(((ImageView) infoHeader.findViewById(R.id.artist_image)));

                            String bioText;
                            if (!bio.tags[0].equals("")) {
                                bioText = bio.tags[0].toUpperCase().charAt(0) + bio.tags[0].substring(1);
                                if (!bio.summary.equals("")) {
                                    bioText = bioText + " - " + bio.summary;
                                }
                            } else bioText = bio.summary;

                            ((TextView) infoHeader.findViewById(R.id.artist_bio)).setText(bioText);
                        }
                    });
                } else {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            //TODO This should probably fade out
                            ((ListView) parent).removeHeaderView(infoHeader);
                        }
                    });
                }
            }
        }).start();

        ((ListView) parent).addHeaderView(infoHeader, null, false);

        final View albumHeader = View.inflate(activity, R.layout.artist_header_albums, null);
        final GridView albumGrid = (GridView) albumHeader.findViewById(R.id.albumGrid);
        AlbumGridAdapter gridAdapter = new AlbumGridAdapter(albums, context);
        albumGrid.setAdapter(gridAdapter);

        int albumCount = albums.size();

        ((ListView) parent).addHeaderView(albumHeader, null, false);

        updateArtistGridLayout((GridView) activity.findViewById(R.id.albumGrid), albumCount, activity);
        updateArtistHeader((ViewGroup) activity.findViewById(R.id.artist_bio).getParent(), activity);
    }

    private static void updateArtistGridLayout(GridView albumGrid, int albumCount, Activity activity) {
        final long screenWidth = activity.getResources().getConfiguration().screenWidthDp;
        final float density = activity.getResources().getDisplayMetrics().density;
        final long globalPadding = (long) (activity.getResources().getDimension(R.dimen.global_padding) / density);
        final long gridPadding = (long) (activity.getResources().getDimension(R.dimen.grid_padding) / density);
        final long extraHeight = 60;
        final long minWidth = (long) (activity.getResources().getDimension(R.dimen.grid_width) / density);

        long availableWidth = screenWidth - 2 * (globalPadding + gridPadding);
        double numColumns = (availableWidth + gridPadding) / (minWidth + gridPadding);

        long columnWidth = (long) Math.floor(availableWidth / numColumns);
        long rowHeight = columnWidth + extraHeight;

        long numRows = (long) Math.ceil(albumCount / numColumns);

        long gridHeight = rowHeight * numRows + 2 * gridPadding;

        int height = (int) ((gridHeight * density));

        ViewGroup.LayoutParams albumParams = albumGrid.getLayoutParams();
        albumParams.height = height;
        albumGrid.setLayoutParams(albumParams);
    }

    private static void updateArtistHeader(final ViewGroup bioHolder, Activity activity) {
        final TextView bioText = (TextView) bioHolder.findViewById(R.id.artist_bio);

        final long viewHeight = (long) (activity.getResources().getDimension(R.dimen.artist_image_height));
        final long padding = (long) (activity.getResources().getDimension(R.dimen.list_margin));

        final long availableHeight = (long) Math.floor(viewHeight - 2 * padding);

        long maxLines = (long) Math.floor(availableHeight / (bioText.getLineHeight()));
        bioText.setMaxLines((int) maxLines);
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
                        ((ImageButton) findViewById(R.id.playButton)).setImageResource(R.drawable.ic_play);
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
                        ((ImageButton) findViewById(R.id.playButton)).setImageResource(R.drawable.ic_pause);
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
}
