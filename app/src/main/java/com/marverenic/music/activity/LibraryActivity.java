package com.marverenic.music.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.marverenic.music.Library;
import com.marverenic.music.R;
import com.marverenic.music.activity.instance.AutoPlaylistEditor;
import com.marverenic.music.fragments.AlbumFragment;
import com.marverenic.music.fragments.ArtistFragment;
import com.marverenic.music.fragments.GenreFragment;
import com.marverenic.music.fragments.PlaylistFragment;
import com.marverenic.music.fragments.SongFragment;
import com.marverenic.music.utils.Navigate;
import com.marverenic.music.utils.PlaylistDialog;
import com.marverenic.music.utils.Prefs;
import com.marverenic.music.utils.Updater;
import com.marverenic.music.view.FABMenu;

public class LibraryActivity extends BaseActivity implements View.OnClickListener{

    PagerAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library);

        // Setup the FAB
        FABMenu fab = (FABMenu) findViewById(R.id.fab);
        fab.addChild(R.drawable.ic_add_36dp, this, R.string.playlist);
        fab.addChild(R.drawable.ic_add_36dp, this, R.string.playlist_auto);

        ViewPager pager = (ViewPager) findViewById(R.id.pager);

        SharedPreferences prefs = Prefs.getPrefs(this);
        int page = Integer.parseInt(prefs.getString(Prefs.DEFAULT_PAGE, "1"));
        if (page != 0 || !Library.hasRWPermission(this)) fab.setVisibility(View.GONE);

        adapter = new PagerAdapter(getSupportFragmentManager());
        adapter.setFloatingActionButton(fab);
        pager.setAdapter(adapter);
        pager.addOnPageChangeListener(adapter);
        ((TabLayout) findViewById(R.id.tabs)).setupWithViewPager(pager);

        pager.setCurrentItem(page);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setHomeButtonEnabled(false);
            getSupportActionBar().setDisplayShowHomeEnabled(false);
        }

        new Updater(this, findViewById(R.id.coordinator_layout)).execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_library, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Navigate.to(this, SettingsActivity.class);
                return true;
            case R.id.action_refresh_library:
                Library.resetAll();
                Library.scanAll(this);
                adapter.refreshFragments();
                return true;
            case R.id.search:
                Navigate.to(this, SearchActivity.class);
                return true;
            case R.id.action_about:
                Navigate.to(this, AboutActivity.class);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onClick(View v){
        super.onClick(v);
        if (v.getTag() != null) {
            if (v.getTag().equals("fab-" + getString(R.string.playlist))) {
                PlaylistDialog.MakeNormal.alert(findViewById(R.id.coordinator_layout));
            } else if (v.getTag().equals("fab-" + getString(R.string.playlist_auto))) {
                Navigate.to(this, AutoPlaylistEditor.class, AutoPlaylistEditor.PLAYLIST_EXTRA, null);
            }
        }
    }

    public class PagerAdapter extends FragmentPagerAdapter implements ViewPager.OnPageChangeListener {

        private Fragment playlistFragment;
        private Fragment songFragment;
        private Fragment artistFragment;
        private Fragment albumFragment;
        private Fragment genreFragment;

        private FABMenu fab;

        public PagerAdapter(FragmentManager fm) {
            super(fm);
        }

        public void setFloatingActionButton(FABMenu fab){
            this.fab = fab;
        }

        @Override
        public Fragment getItem(int position) {
            switch (position){
                case 0:
                    if (playlistFragment == null){
                        playlistFragment = new PlaylistFragment();
                    }
                    return playlistFragment;
                case 1:
                    if (songFragment == null){
                        songFragment = new SongFragment();
                    }
                    return songFragment;
                case 2:
                    if (artistFragment == null){
                        artistFragment = new ArtistFragment();
                    }
                    return artistFragment;
                case 3:
                    if (albumFragment == null){
                        albumFragment = new AlbumFragment();
                    }
                    return albumFragment;
                case 4:
                    if (genreFragment == null){
                        genreFragment = new GenreFragment();
                    }
                    return genreFragment;
            }
            return new Fragment();
        }

        public void refreshFragments(){
            if (playlistFragment != null && playlistFragment.getView() != null)
                ((RecyclerView) playlistFragment.getView().findViewById(R.id.list)).getAdapter().notifyDataSetChanged();
            if (songFragment != null && songFragment.getView() != null)
                ((RecyclerView) songFragment.getView().findViewById(R.id.list)).getAdapter().notifyDataSetChanged();
            if (artistFragment != null && artistFragment.getView() != null)
                ((RecyclerView) artistFragment.getView().findViewById(R.id.list)).getAdapter().notifyDataSetChanged();
            if (albumFragment != null && albumFragment.getView() != null)
                ((RecyclerView) albumFragment.getView().findViewById(R.id.list)).getAdapter().notifyDataSetChanged();
            if (genreFragment != null && genreFragment.getView() != null)
                ((RecyclerView) genreFragment.getView().findViewById(R.id.list)).getAdapter().notifyDataSetChanged();

            if (Library.hasRWPermission(LibraryActivity.this)) {
                Toast
                        .makeText(
                                LibraryActivity.this,
                                getResources().getString(R.string.confirm_refresh_library),
                                Toast.LENGTH_SHORT)
                        .show();
            }
        }

        @Override
        public int getCount() {
            return 5;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getResources().getString(R.string.header_playlists);
                case 1:
                    return getResources().getString(R.string.header_songs);
                case 2:
                    return getResources().getString(R.string.header_artists);
                case 3:
                    return getResources().getString(R.string.header_albums);
                case 4:
                    return getResources().getString(R.string.header_genres);
                default:
                    return "Page " + position;
            }
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            // Hide the fab when outside of the Playlist fragment

            // Don't show the FAB if we can't write to the library
            if (!Library.hasRWPermission(LibraryActivity.this)) return;

            // If the fab isn't supposed to change states, don't animate anything
            if (position != 0 && fab.getVisibility() == View.GONE) return;

            if (position == 0){
                fab.show();
            }
            else{
                fab.hide();
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    }
}
