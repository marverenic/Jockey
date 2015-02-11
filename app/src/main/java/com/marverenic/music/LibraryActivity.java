package com.marverenic.music;

import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.marverenic.music.adapters.LibraryPagerAdapter;
import com.marverenic.music.instances.Album;
import com.marverenic.music.instances.Artist;
import com.marverenic.music.instances.Genre;
import com.marverenic.music.instances.Library;
import com.marverenic.music.instances.Playlist;
import com.marverenic.music.instances.Song;
import com.marverenic.music.utils.Debug;
import com.marverenic.music.utils.SlidingTabLayout;
import com.marverenic.music.utils.Themes;

public class LibraryActivity extends FragmentActivity implements View.OnClickListener {

    private BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            update();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Debug.log(Debug.VERBOSE, "LibraryActivity", "Jockey has started", this);

        Themes.setTheme(this);

        // Check for versions of Jockey published by ensiluxrum
        PackageManager pm = getPackageManager();
        boolean hasOldVersion;
        try {
            pm.getPackageInfo("com.ensiluxrum.music", PackageManager.GET_ACTIVITIES);
            hasOldVersion = true;
        } catch (PackageManager.NameNotFoundException e) {
            hasOldVersion = false;
        }

        if (hasOldVersion)
            new AlertDialog.Builder(this)
                    .setTitle("An old version of Jockey was found")
                    .setMessage("You should uninstall it to avoid conflicts and general confusion.")
                    .setPositiveButton("Uninstall it for me", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(Intent.ACTION_DELETE);
                            intent.setData(Uri.parse("package:com.ensiluxrum.music"));
                            startActivity(intent);
                        }
                    })
                    .setNeutralButton("Maybe later", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .show();

        if (!isTaskRoot()) {
            finish();
            return;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        int page = Integer.parseInt(prefs.getString("prefDefaultPage", "1"));

        setContentView(R.layout.activity_library);
        ViewPager pager = (ViewPager) findViewById(R.id.pager);
        LibraryPagerAdapter adapter = new LibraryPagerAdapter(this);
        pager.setAdapter(adapter);
        pager.setCurrentItem(page);

        Themes.themeActivity(R.layout.activity_library, getWindow().getDecorView().findViewById(android.R.id.content), this);

        SlidingTabLayout tabs = ((SlidingTabLayout) findViewById(R.id.pagerSlidingTabs));
        tabs.setViewPager(pager);
        tabs.setActivePage(page);

        if (Library.isEmpty()) {
            libraryScanAll();
        }

        startService(new Intent(this, Player.class));

        registerReceiver(updateReceiver, new IntentFilter(Player.UPDATE_BROADCAST));

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        if (getResources().getConfiguration().smallestScreenWidthDp < 700 && getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            tabs.setMini(true);
        } else {
            tabs.setMini(false);
        }
    }

    @Override
    public void onResume() {
        if (Themes.hasChanged(this)) {
            recreate();
        }
        update();
        Themes.setApplicationIcon(this);
        registerReceiver(updateReceiver, new IntentFilter(Player.UPDATE_BROADCAST));
        super.onResume();
    }

    @Override
    public void onPause() {
        try {
            unregisterReceiver(updateReceiver);
        } catch (Exception e) {
            Debug.log(Debug.ERROR, "LibraryActivity", "Unable to unregister receiver", this);
        }
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        // Associate searchable configuration with the SearchView
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(LibraryActivity.this, SettingsActivity.class));
                return true;
            case R.id.action_refresh_library:
                Library.resetAll();
                libraryScanAll();
                Toast.makeText(this, "Library refreshed.", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.search:
                startActivity(new Intent(LibraryActivity.this, SearchActivity.class));
                return true;
            case R.id.action_about:
                startActivity(new Intent(LibraryActivity.this, AboutActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.miniplayer:
                startActivity(new Intent(LibraryActivity.this, NowPlayingActivity.class));
                update();
                break;
            case R.id.playButton:
                Player.getInstance().pause();
                update();
                break;
            case R.id.skipButton:
                Player.getInstance().skip();
                update();
                break;
        }
    }

    public void update() {
        if (Player.getInstance() != null && Player.getNowPlaying() != null) {
            final TextView songTitle = (TextView) findViewById(R.id.textNowPlayingTitle);
            final TextView artistName = (TextView) findViewById(R.id.textNowPlayingDetail);

            songTitle.setText(Player.getNowPlaying().songName);
            artistName.setText(Player.getNowPlaying().artistName);

            if (!Player.getInstance().isPlaying()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    ((ImageButton) findViewById(R.id.playButton)).setImageResource(R.drawable.ic_vector_play);
                else
                    ((ImageButton) findViewById(R.id.playButton)).setImageResource(R.drawable.ic_play);
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    ((ImageButton) findViewById(R.id.playButton)).setImageResource(R.drawable.ic_vector_pause);
                else
                    ((ImageButton) findViewById(R.id.playButton)).setImageResource(R.drawable.ic_pause);
            }

            if (Player.getInstance().getArt() != null) {
                ((ImageView) findViewById(R.id.imageArtwork)).setImageBitmap(Player.getInstance().getArt());
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ((ImageView) findViewById(R.id.imageArtwork)).setImageResource(R.drawable.art_default);
                }
            }
            RelativeLayout.LayoutParams pagerLayoutParams = (RelativeLayout.LayoutParams) (findViewById(R.id.pager)).getLayoutParams();
            pagerLayoutParams.bottomMargin = getResources().getDimensionPixelSize(R.dimen.now_playing_ticker_height);
            (findViewById(R.id.pager)).setLayoutParams(pagerLayoutParams);

            FrameLayout.LayoutParams playerLayoutParams = (FrameLayout.LayoutParams) (findViewById(R.id.miniplayer)).getLayoutParams();
            playerLayoutParams.height = getResources().getDimensionPixelSize(R.dimen.now_playing_ticker_height);
            (findViewById(R.id.miniplayer)).setLayoutParams(playerLayoutParams);
        } else {
            RelativeLayout.LayoutParams pagerLayoutParams = (RelativeLayout.LayoutParams) (findViewById(R.id.pager)).getLayoutParams();
            pagerLayoutParams.bottomMargin = 0;
            (findViewById(R.id.pager)).setLayoutParams(pagerLayoutParams);

            FrameLayout.LayoutParams playerLayoutParams = (FrameLayout.LayoutParams) (findViewById(R.id.miniplayer)).getLayoutParams();
            playerLayoutParams.height = 0;
            (findViewById(R.id.miniplayer)).setLayoutParams(playerLayoutParams);
        }
    }

    private void libraryScanAll() {
        songLibraryScan();
        artistLibraryScan();
        albumLibraryScan();
        playlistLibraryScan();
        genreLibraryScan();
    }

    private void songLibraryScan() {
        Cursor cur = getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                null,
                MediaStore.Audio.Media.IS_MUSIC + "!= 0",
                null,
                MediaStore.Audio.Media.TITLE + " ASC");

        for (int i = 0; i < cur.getCount(); i++) {
            cur.moveToPosition(i);
            Library.add(new Song(
                    cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.TITLE)),
                    cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.ARTIST)),
                    cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.ALBUM)),
                    cur.getInt(cur.getColumnIndex(MediaStore.Audio.Media.DURATION)),
                    cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.DATA)),
                    cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID))));
        }
        cur.close();
        ((LibraryPagerAdapter) ((ViewPager) findViewById(R.id.pager)).getAdapter()).refreshLibrary();
    }

    private void artistLibraryScan() {
        Cursor cur = getContentResolver().query(
                MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
                null,
                null,
                null,
                MediaStore.Audio.Artists.ARTIST + " ASC");

        for (int i = 0; i < cur.getCount(); i++) {
            cur.moveToPosition(i);
            Library.add(new Artist(
                    cur.getLong(cur.getColumnIndex(MediaStore.Audio.Artists._ID)),
                    cur.getString(cur.getColumnIndex(MediaStore.Audio.Artists.ARTIST))));
        }
        cur.close();
        ((LibraryPagerAdapter) ((ViewPager) findViewById(R.id.pager)).getAdapter()).refreshLibrary();
    }

    private void albumLibraryScan() {
        Cursor cur = getContentResolver().query(
                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                null,
                null,
                null,
                MediaStore.Audio.Albums.ALBUM + " ASC");
        for (int i = 0; i < cur.getCount(); i++) {
            cur.moveToPosition(i);
            Library.add(new Album(
                    cur.getString(cur.getColumnIndex(MediaStore.Audio.Albums._ID)),
                    cur.getString(cur.getColumnIndex(MediaStore.Audio.Albums.ALBUM)),
                    cur.getString(cur.getColumnIndex(MediaStore.Audio.Albums.ARTIST)),
                    cur.getString(cur.getColumnIndex(MediaStore.Audio.Albums.LAST_YEAR)),
                    cur.getString(cur.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART))));
        }
        cur.close();
        ((LibraryPagerAdapter) ((ViewPager) findViewById(R.id.pager)).getAdapter()).refreshLibrary();
    }

    private void playlistLibraryScan() {
        Cursor cur = getContentResolver().query(
                MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                null, null, null,
                MediaStore.Audio.Playlists.NAME + " ASC");

        for (int i = 0; i < cur.getCount(); i++) {
            cur.moveToPosition(i);
            Library.add(new Playlist(
                    cur.getLong(cur.getColumnIndex(MediaStore.Audio.Playlists._ID)),
                    cur.getString(cur.getColumnIndex(MediaStore.Audio.Playlists.NAME))));
        }
        cur.close();
        ((LibraryPagerAdapter) ((ViewPager) findViewById(R.id.pager)).getAdapter()).refreshLibrary();
    }

    private void genreLibraryScan() {
        Cursor cur = getContentResolver().query(
                MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
                null, null, null,
                MediaStore.Audio.Genres.NAME + " ASC");

        for (int i = 0; i < cur.getCount(); i++) {
            cur.moveToPosition(i);
            Library.add(new Genre(
                    cur.getLong(cur.getColumnIndex(MediaStore.Audio.Genres._ID)),
                    cur.getString(cur.getColumnIndex(MediaStore.Audio.Genres.NAME))));
        }
        cur.close();
        ((LibraryPagerAdapter) ((ViewPager) findViewById(R.id.pager)).getAdapter()).refreshLibrary();
    }
}
