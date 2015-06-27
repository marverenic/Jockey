package com.marverenic.music.activity;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import com.marverenic.music.Library;
import com.marverenic.music.R;
import com.marverenic.music.fragments.AlbumFragment;
import com.marverenic.music.fragments.ArtistFragment;
import com.marverenic.music.fragments.GenreFragment;
import com.marverenic.music.fragments.PlaylistFragment;
import com.marverenic.music.fragments.SongFragment;
import com.marverenic.music.utils.Navigate;
import com.marverenic.music.utils.Themes;
import com.marverenic.music.utils.Updater;

public class LibraryActivity extends BaseActivity implements View.OnClickListener{

    PagerAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library);

        // Setup the FAB
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(this);

        ViewPager pager = (ViewPager) findViewById(R.id.pager);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int page = Integer.parseInt(prefs.getString("prefDefaultPage", "1"));
        if (page != 0) fab.setVisibility(View.GONE);

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
    public void onClick(final View v){
        super.onClick(v);
        if (v.getId() == R.id.fab){
            final TextInputLayout layout = new TextInputLayout(this);
            final AppCompatEditText input = new AppCompatEditText(this);
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            input.setHint("Playlist name");
            layout.addView(input);
            layout.setErrorEnabled(true);

            int padding = (int) getResources().getDimension(R.dimen.alert_padding);
            ((View) input.getParent()).setPadding(
                    padding - input.getPaddingLeft(),
                    padding,
                    padding - input.getPaddingRight(),
                    input.getPaddingBottom());

            final AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("Create Playlist")
                    .setView(layout)
                    .setPositiveButton(
                            "Create",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Library.createPlaylist(v, input.getText().toString(), null);
                                }
                            })
                    .setNegativeButton(
                            "Cancel",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            })
                    .show();

            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Themes.getAccent());
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);

            input.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String error = Library.verifyPlaylistName(LibraryActivity.this, s.toString());
                    layout.setError(error);
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(error == null && s.length() > 0);
                    if (error == null && s.length() > 0){
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Themes.getAccent());
                    }
                    else{
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
                                getResources().getColor((Themes.isLight(LibraryActivity.this)
                                        ? R.color.secondary_text_disabled_material_light
                                        : R.color.secondary_text_disabled_material_dark)));
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    public class PagerAdapter extends FragmentPagerAdapter implements ViewPager.OnPageChangeListener {

        private Fragment playlistFragment;
        private Fragment songFragment;
        private Fragment artistFragment;
        private Fragment albumFragment;
        private Fragment genreFragment;

        private FloatingActionButton fab;

        public PagerAdapter(FragmentManager fm) {
            super(fm);
        }

        public void setFloatingActionButton(FloatingActionButton fab){
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

            Toast.makeText(LibraryActivity.this, getResources().getString(R.string.confirm_refresh_library), Toast.LENGTH_SHORT).show();
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

            // If the fab isn't supposed to change states, don't animate anything
            if (position != 0 && fab.getVisibility() == View.GONE) return;

            Animation fabAnim = AnimationUtils.loadAnimation(LibraryActivity.this,
                    (position == 0) ? R.anim.fab_in : R.anim.fab_out);
            fabAnim.setDuration(300);
            fabAnim.setInterpolator(LibraryActivity.this,
                    (position == 0)? android.R.interpolator.decelerate_quint : android.R.interpolator.accelerate_quint);
            //fabAnim.setFillEnabled(position != 0);
            //fabAnim.setFillAfter(position != 0);
            fab.startAnimation(fabAnim);

            if (position == 0){
                // If the FAB is reappearing, make sure it's visible
                fab.setVisibility(View.VISIBLE);
            }
            else {
                // If the FAB is fading away, make sure to hide it after the animation finishes
                fab.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        fab.setVisibility(View.GONE);
                    }
                }, 300);
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    }
}
