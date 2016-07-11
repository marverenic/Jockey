package com.marverenic.music.activity.instance;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;

import com.marverenic.heterogeneousadapter.HeterogeneousAdapter;
import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.activity.BaseActivity;
import com.marverenic.music.data.store.MediaStoreUtil;
import com.marverenic.music.data.store.PlayCountStore;
import com.marverenic.music.data.store.PlaylistStore;
import com.marverenic.music.instances.AutoPlaylist;
import com.marverenic.music.instances.Song;
import com.marverenic.music.instances.playlistrules.AutoPlaylistRule;
import com.marverenic.music.instances.section.LibraryEmptyState;
import com.marverenic.music.instances.section.SongSection;
import com.marverenic.music.view.BackgroundDecoration;
import com.marverenic.music.view.DividerDecoration;

import java.util.List;

import javax.inject.Inject;

public class AutoPlaylistActivity extends BaseActivity
        implements PopupMenu.OnMenuItemClickListener {

    private static final String TAG = "AutoPlaylistActivity";

    public static final String PLAYLIST_EXTRA = "AutoPlaylistActivity.Playlist";

    @Inject PlaylistStore mPlaylistStore;
    @Inject PlayCountStore mPlayCountStore;

    private List<Song> mSongs;
    private AutoPlaylist mReference;
    private RecyclerView mRecyclerView;
    private HeterogeneousAdapter mAdapter;
    private SongSection mSongSection;

    public static Intent newIntent(Context context, AutoPlaylist playlist) {
        Intent intent = new Intent(context, AutoPlaylistActivity.class);
        intent.putExtra(PLAYLIST_EXTRA, playlist);

        return intent;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instance);
        JockeyApplication.getComponent(this).inject(this);

        mRecyclerView = (RecyclerView) findViewById(R.id.list);
        setupRecyclerView();

        mReference = getIntent().getParcelableExtra(PLAYLIST_EXTRA);
        mPlaylistStore.getSongs(mReference)
                .subscribe(
                        songs -> {
                            mSongs = songs;
                            setupAdapter();
                        }, throwable -> {
                            Log.e(TAG, "onCreate: Failed to get song contents", throwable);
                        });

        getSupportActionBar().setTitle(mReference.getPlaylistName());
    }

    private void setupAdapter() {
        if (mSongs == null) {
            return;
        }

        if (mAdapter == null) {
            mAdapter = new HeterogeneousAdapter();
            mAdapter.setHasStableIds(true);

            mAdapter.setEmptyState(new LibraryEmptyState(this, null) {
                @Override
                public String getEmptyMessage() {
                    return getString(R.string.empty_auto_playlist);
                }

                @Override
                public String getEmptyMessageDetail() {
                    return getString(R.string.empty_auto_playlist_detail);
                }

                @Override
                public String getEmptyAction1Label() {
                    return getString(R.string.action_edit_playlist_rules);
                }

                @Override
                public void onAction1() {
                    if (MediaStoreUtil.hasPermission(AutoPlaylistActivity.this)) {
                        Intent intent = AutoPlaylistEditActivity.newIntent
                                (AutoPlaylistActivity.this, mReference);

                        startActivity(intent);
                    } else {
                        super.onAction1();
                    }
                }
            });
            mRecyclerView.setAdapter(mAdapter);
        }

        if (mSongSection == null) {
            mSongSection = new SongSection(this, mSongs);
            mAdapter.addSection(mSongSection);
        } else {
            mSongSection.setData(mSongs);
            mAdapter.notifyDataSetChanged();
        }
    }

    private void setupRecyclerView() {
        mRecyclerView.addItemDecoration(new BackgroundDecoration());
        mRecyclerView.addItemDecoration(new DividerDecoration(this, R.id.empty_layout));

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(layoutManager);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_playlist, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mReference == null) {
            return super.onOptionsItemSelected(item);
        }

        if (item.getItemId() == R.id.action_sort) {
            PopupMenu sortMenu = new PopupMenu(this, findViewById(R.id.action_sort), Gravity.END);
            sortMenu.inflate(R.menu.sort_options_auto_playlist);
            sortMenu.setOnMenuItemClickListener(this);
            sortMenu.show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int sortFlag;
        String result;
        boolean ascending;

        switch (item.getItemId()) {
            case R.id.action_sort_random:
                result = getString(R.string.message_sorted_playlist_random);
                sortFlag = AutoPlaylistRule.ID;
                ascending = false;
                break;
            case R.id.action_sort_name:
                result = getString(R.string.message_sorted_playlist_name);
                sortFlag = AutoPlaylistRule.NAME;
                ascending = true;
                break;
            case R.id.action_sort_play:
                result = getString(R.string.message_sorted_playlist_play);
                sortFlag = AutoPlaylistRule.PLAY_COUNT;
                ascending = false;
                break;
            case R.id.action_sort_skip:
                result = getString(R.string.message_sorted_playlist_skip);
                sortFlag = AutoPlaylistRule.SKIP_COUNT;
                ascending = false;
                break;
            case R.id.action_sort_date_added:
                result = getString(R.string.message_sorted_playlist_date_added);
                sortFlag = AutoPlaylistRule.DATE_ADDED;
                ascending = false;
                break;
            case R.id.action_sort_date_played:
                result = getString(R.string.message_sorted_playlist_date_played);
                sortFlag = AutoPlaylistRule.DATE_PLAYED;
                ascending = false;
                break;
            default:
                return false;
        }

        int oldSortFlag = mReference.getSortMethod();
        if (oldSortFlag == mReference.getSortMethod()) {
            ascending = !mReference.isSortAscending();
        }

        boolean alreadyLoaded = mSongSection != null;

        mPlaylistStore.getSongs(mReference)
                .skip(alreadyLoaded ? 1 : 0)
                .take(1)
                .subscribe(ignoredValue -> {
                    String message = String.format(result, mReference);
                    Snackbar.make(mRecyclerView, message, Snackbar.LENGTH_LONG)
                            .setAction(getResources().getString(R.string.action_undo), v -> {
                                mReference = new AutoPlaylist.Builder(mReference)
                                        .setSortMethod(oldSortFlag)
                                        .build();

                                mPlaylistStore.editPlaylist(mReference);
                            })
                            .show();
                }, throwable -> {
                    Log.e(TAG, "Failed to set sort method", throwable);
                });

        mReference = new AutoPlaylist.Builder(mReference)
                .setSortMethod(sortFlag)
                .setSortAscending(ascending)
                .build();
        mPlaylistStore.editPlaylist(mReference);

        return true;
    }
}
