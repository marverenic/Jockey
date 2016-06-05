package com.marverenic.music.activity.instance;

import android.graphics.drawable.NinePatchDrawable;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;

import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.activity.BaseActivity;
import com.marverenic.music.data.store.MediaStoreUtil;
import com.marverenic.music.data.store.PlaylistStore;
import com.marverenic.music.instances.AutoPlaylist;
import com.marverenic.music.instances.Playlist;
import com.marverenic.music.instances.Song;
import com.marverenic.music.instances.section.LibraryEmptyState;
import com.marverenic.music.instances.section.PlaylistSongSection;
import com.marverenic.music.instances.viewholder.DragDropSongViewHolder;
import com.marverenic.music.utils.Navigate;
import com.marverenic.music.utils.Themes;
import com.marverenic.music.view.EnhancedAdapters.DragBackgroundDecoration;
import com.marverenic.music.view.EnhancedAdapters.DragDividerDecoration;
import com.marverenic.music.view.EnhancedAdapters.DragDropAdapter;
import com.marverenic.music.view.EnhancedAdapters.DragDropDecoration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

public class PlaylistActivity extends BaseActivity implements PopupMenu.OnMenuItemClickListener,
        DragDropSongViewHolder.OnRemovedListener {

    public static final String PLAYLIST_EXTRA = "playlist";

    @Inject PlaylistStore mPlaylistStore;

    private List<Song> mSongs;
    private Playlist mReference;
    private RecyclerView mRecyclerView;
    private DragDropAdapter mAdapter;
    private PlaylistSongSection mSongSection;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instance);
        JockeyApplication.getComponent(this).inject(this);

        mReference = getIntent().getParcelableExtra(PLAYLIST_EXTRA);

        mPlaylistStore.getSongs(mReference)
                .compose(bindToLifecycle())
                .subscribe(
                        songs -> {
                            mSongs = songs;
                            setupAdapter();
                        });

        getSupportActionBar().setTitle(mReference.getPlaylistName());

        mRecyclerView = (RecyclerView) findViewById(R.id.list);
        setupRecyclerView();
        setupAdapter();
    }

    private void setupAdapter() {
        if (mRecyclerView == null) {
            return;
        }

        if (mAdapter == null) {
            mAdapter = new DragDropAdapter();
            mAdapter.attach(mRecyclerView);

            mAdapter.setEmptyState(new LibraryEmptyState(this, null) {
                @Override
                public String getEmptyMessage() {
                    if (mReference instanceof AutoPlaylist) {
                        return getString(R.string.empty_auto_playlist);
                    } else {
                        return getString(R.string.empty_playlist);
                    }
                }

                @Override
                public String getEmptyMessageDetail() {
                    if (mReference instanceof AutoPlaylist) {
                        return getString(R.string.empty_auto_playlist_detail);
                    } else {
                        return getString(R.string.empty_playlist_detail);
                    }
                }

                @Override
                public String getEmptyAction1Label() {
                    if (mReference instanceof AutoPlaylist) {
                        return getString(R.string.action_edit_playlist_rules);
                    } else {
                        return "";
                    }
                }

                @Override
                public void onAction1() {
                    if (mReference instanceof AutoPlaylist
                            && MediaStoreUtil.hasPermission(PlaylistActivity.this)) {
                        Navigate.to(PlaylistActivity.this, AutoPlaylistEditActivity.class,
                                AutoPlaylistEditActivity.PLAYLIST_EXTRA, mReference);
                    } else {
                        super.onAction1();
                    }
                }
            });
        }

        if (mSongs == null) {
            mSongs = Collections.emptyList();
        }

        if (mSongSection == null) {
            mSongSection = new PlaylistSongSection(mSongs, this, this, mReference);
            mAdapter.setDragSection(mSongSection);
        } else {
            mSongSection.setData(mSongs);
            mAdapter.notifyDataSetChanged();
        }
    }

    private void setupRecyclerView() {
        mRecyclerView.addItemDecoration(
                new DragBackgroundDecoration(Themes.getBackgroundElevated()));
        mRecyclerView.addItemDecoration(new DragDividerDecoration(this, R.id.empty_layout));
        mRecyclerView.addItemDecoration(new DragDropDecoration(
                (NinePatchDrawable) getDrawableCompat(R.drawable.list_drag_shadow)));

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
            sortMenu.inflate(R.menu.sort_options);
            sortMenu.setOnMenuItemClickListener(this);
            sortMenu.show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        final List<Song> unsortedData = new ArrayList<>(mSongs);
        String result;

        switch (item.getItemId()) {
            case R.id.action_sort_random:
                Collections.shuffle(mSongs);
                result = getResources().getString(R.string.message_sorted_playlist_random);
                break;
            case R.id.action_sort_name:
                Collections.sort(mSongs);
                result = getResources().getString(R.string.message_sorted_playlist_name);
                break;
            case R.id.action_sort_artist:
                Collections.sort(mSongs, Song.ARTIST_COMPARATOR);
                result = getResources().getString(R.string.message_sorted_playlist_artist);
                break;
            case R.id.action_sort_album:
                Collections.sort(mSongs, Song.ALBUM_COMPARATOR);
                result = getResources().getString(R.string.message_sorted_playlist_album);
                break;
            case R.id.action_sort_play:
                Collections.sort(mSongs, Song.PLAY_COUNT_COMPARATOR);
                result = getResources().getString(R.string.message_sorted_playlist_play);
                break;
            case R.id.action_sort_skip:
                Collections.sort(mSongs, Song.SKIP_COUNT_COMPARATOR);
                result = getResources().getString(R.string.message_sorted_playlist_skip);
                break;
            case R.id.action_sort_date_added:
                Collections.sort(mSongs, Song.DATE_ADDED_COMPARATOR);
                result = getResources().getString(R.string.message_sorted_playlist_date_added);
                break;
            case R.id.action_sort_date_played:
                Collections.sort(mSongs, Song.DATE_PLAYED_COMPARATOR);
                result = getResources().getString(R.string.message_sorted_playlist_date_played);
                break;
            default:
                return false;
        }

        mPlaylistStore.editPlaylist(mReference, mSongs);
        mAdapter.notifyDataSetChanged();

        Snackbar
                .make(
                        mRecyclerView,
                        String.format(result, mReference),
                        Snackbar.LENGTH_LONG)
                .setAction(
                        getResources().getString(R.string.action_undo),
                        v -> {
                            mSongs.clear();
                            mSongs.addAll(unsortedData);
                            mPlaylistStore.editPlaylist(mReference, unsortedData);
                            mAdapter.notifyDataSetChanged();
                        })
                .show();

        return true;
    }

    @Override
    public void onItemRemoved(final int index) {
        final Song removed = mSongs.remove(index);

        mPlaylistStore.editPlaylist(mReference, mSongs);
        mAdapter.notifyItemRemoved(index);

        Snackbar
                .make(
                        mRecyclerView,
                        getResources().getString(
                                R.string.message_removed_song,
                                removed.getSongName()),
                        Snackbar.LENGTH_LONG)
                .setAction(R.string.action_undo, v -> {
                    mSongs.add(index, removed);
                    mPlaylistStore.editPlaylist(mReference, mSongs);
                    if (mSongs.size() > 1) {
                        mAdapter.notifyItemInserted(index);
                    } else {
                        mAdapter.notifyItemChanged(index);
                    }
                })
                .show();
    }
}
