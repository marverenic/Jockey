package com.marverenic.music.activity.instance;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.NinePatchDrawable;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;

import com.marverenic.adapter.DragDropAdapter;
import com.marverenic.adapter.DragDropDecoration;
import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.activity.BaseLibraryActivity;
import com.marverenic.music.adapter.LibraryEmptyState;
import com.marverenic.music.adapter.PlaylistSongSection;
import com.marverenic.music.data.store.PlayCountStore;
import com.marverenic.music.data.store.PlaylistStore;
import com.marverenic.music.model.Playlist;
import com.marverenic.music.model.Song;
import com.marverenic.music.view.DragBackgroundDecoration;
import com.marverenic.music.view.DragDividerDecoration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import timber.log.Timber;

public class PlaylistActivity extends BaseLibraryActivity
        implements PopupMenu.OnMenuItemClickListener {

    private static final String PLAYLIST_EXTRA = "PlaylistActivity.PLAYLIST";

    @Inject PlaylistStore mPlaylistStore;
    @Inject PlayCountStore mPlayCountStore;

    private List<Song> mSongs;
    private Playlist mReference;
    private RecyclerView mRecyclerView;
    private DragDropAdapter mAdapter;
    private PlaylistSongSection mSongSection;

    public static Intent newIntent(Context context, Playlist playlist) {
        Intent intent = new Intent(context, PlaylistActivity.class);
        intent.putExtra(PLAYLIST_EXTRA, playlist);

        return intent;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        JockeyApplication.getComponent(this).inject(this);

        mReference = getIntent().getParcelableExtra(PLAYLIST_EXTRA);

        mPlaylistStore.getSongs(mReference)
                .compose(bindToLifecycle())
                .map(ArrayList::new)
                .subscribe(
                        songs -> {
                            mSongs = songs;
                            setupAdapter();
                        }, throwable -> {
                            Timber.e(throwable, "Failed to get playlist contents");
                        });

        getSupportActionBar().setTitle(mReference.getPlaylistName());

        mRecyclerView = (RecyclerView) findViewById(R.id.list);
        setupRecyclerView();
        setupAdapter();
    }

    @Override
    protected int getContentLayoutResource() {
        return R.layout.activity_instance;
    }

    private void setupAdapter() {
        if (mRecyclerView == null) {
            return;
        }

        if (mAdapter == null) {
            mAdapter = new DragDropAdapter();
            mAdapter.setHasStableIds(true);
            mAdapter.attach(mRecyclerView);

            mAdapter.setEmptyState(new LibraryEmptyState(this) {
                @Override
                public String getEmptyMessage() {
                    return getString(R.string.empty_playlist);
                }

                @Override
                public String getEmptyMessageDetail() {
                    return getString(R.string.empty_playlist_detail);
                }

                @Override
                public String getEmptyAction1Label() {
                    return "";
                }
            });
        }

        if (mSongs == null) {
            mSongs = Collections.emptyList();
        }

        if (mSongSection == null) {
            mSongSection = new PlaylistSongSection(this, mPlaylistStore, mSongs, mReference);
            mAdapter.setDragSection(mSongSection);
        } else {
            mSongSection.setData(mSongs);
            mAdapter.notifyDataSetChanged();
        }
    }

    private void setupRecyclerView() {
        mRecyclerView.addItemDecoration(new DragBackgroundDecoration());
        mRecyclerView.addItemDecoration(new DragDividerDecoration(this, R.id.empty_layout));
        mRecyclerView.addItemDecoration(new DragDropDecoration(
                (NinePatchDrawable) ContextCompat.getDrawable(this, R.drawable.list_drag_shadow)));

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

        if (item.getItemId() == R.id.menu_playlist_sort) {
            PopupMenu sortMenu = new PopupMenu(this, findViewById(R.id.menu_playlist_sort), Gravity.END);
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
        Comparator<Song> sortComparator;

        switch (item.getItemId()) {
            case R.id.menu_sort_random:
                sortComparator = null;
                result = getResources().getString(R.string.message_sorted_playlist_random);
                break;
            case R.id.menu_sort_name:
                sortComparator = null;
                result = getResources().getString(R.string.message_sorted_playlist_name);
                break;
            case R.id.menu_sort_artist:
                sortComparator = Song.ARTIST_COMPARATOR;
                result = getResources().getString(R.string.message_sorted_playlist_artist);
                break;
            case R.id.menu_sort_album:
                sortComparator = Song.ALBUM_COMPARATOR;
                result = getResources().getString(R.string.message_sorted_playlist_album);
                break;
            case R.id.menu_sort_play:
                sortComparator = Song.playCountComparator(mPlayCountStore);
                result = getResources().getString(R.string.message_sorted_playlist_play);
                break;
            case R.id.menu_sort_skip:
                sortComparator = Song.skipCountComparator(mPlayCountStore);
                result = getResources().getString(R.string.message_sorted_playlist_skip);
                break;
            case R.id.menu_sort_date_added:
                sortComparator = Song.DATE_ADDED_COMPARATOR;
                result = getResources().getString(R.string.message_sorted_playlist_date_added);
                break;
            case R.id.menu_sort_date_played:
                sortComparator = Song.playCountComparator(mPlayCountStore);
                result = getResources().getString(R.string.message_sorted_playlist_date_played);
                break;
            default:
                return false;
        }

        mPlayCountStore.refresh()
                .observeOn(Schedulers.io())
                .map(ignoredValue -> {
                    if (sortComparator == null) {
                        Collections.sort(mSongs);
                    } else {
                        Collections.sort(mSongs, sortComparator);
                    }

                    mPlaylistStore.editPlaylist(mReference, mSongs);
                    return ignoredValue;
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        ignoredValue -> {
                            mAdapter.notifyDataSetChanged();
                            showUndoSortSnackbar(result, unsortedData);
                        }, throwable -> {
                            Timber.e(throwable, "onMenuItemClick: Failed to sort playlist");
                        });

        return true;
    }

    private void showUndoSortSnackbar(String unformattedMessage, List<Song> unsortedData) {
        String message = String.format(unformattedMessage, mReference);

        Snackbar.make(mRecyclerView, message, Snackbar.LENGTH_LONG)
                .setAction(
                        getResources().getString(R.string.action_undo),
                        v -> {
                            mSongs.clear();
                            mSongs.addAll(unsortedData);
                            mPlaylistStore.editPlaylist(mReference, unsortedData);
                            mAdapter.notifyDataSetChanged();
                        })
                .show();
    }
}
