package com.marverenic.music.activity.instance;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

public class AutoPlaylistActivity extends BaseActivity
        implements PopupMenu.OnMenuItemClickListener {

    public static final String PLAYLIST_EXTRA = "AutoPlaylistActivity.Playlist";

    @Inject PlaylistStore mPlaylistStore;
    @Inject PlayCountStore mPlayCountStore;

    private List<Song> mSongs;
    private AutoPlaylist mReference;
    private RecyclerView mRecyclerView;
    private HeterogeneousAdapter mAdapter;
    private SongSection mSongSection;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instance);
        JockeyApplication.getComponent(this).inject(this);
    }

    private void setupAdapter() {
        if (mAdapter == null) {
            mAdapter = new HeterogeneousAdapter();

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
        }

        mAdapter = new HeterogeneousAdapter().addSection(new SongSection(this, mSongs));
        mRecyclerView.setAdapter(mAdapter);
    }

    private void setupRecyclerView() {
        mRecyclerView.addItemDecoration(new BackgroundDecoration());
        mRecyclerView.addItemDecoration(new DividerDecoration(this, R.id.empty_layout));

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(layoutManager);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        final List<Song> unsortedData = new ArrayList<>(mSongs);
        int sortFlag = -1;
        String result;

        switch (item.getItemId()) {
            case R.id.action_sort_random:
                Collections.shuffle(mSongs);
                result = getResources().getString(R.string.message_sorted_playlist_random);
                sortFlag = AutoPlaylistRule.ID;
                break;
            case R.id.action_sort_name:
                Collections.sort(mSongs);
                result = getResources().getString(R.string.message_sorted_playlist_name);
                sortFlag = AutoPlaylistRule.NAME;
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
                Collections.sort(mSongs, Song.playCountComparator(mPlayCountStore));
                result = getResources().getString(R.string.message_sorted_playlist_play);
                sortFlag = AutoPlaylistRule.PLAY_COUNT;
                break;
            case R.id.action_sort_skip:
                Collections.sort(mSongs, Song.skipCountComparator(mPlayCountStore));
                result = getResources().getString(R.string.message_sorted_playlist_skip);
                sortFlag = AutoPlaylistRule.SKIP_COUNT;
                break;
            case R.id.action_sort_date_added:
                Collections.sort(mSongs, Song.DATE_ADDED_COMPARATOR);
                result = getResources().getString(R.string.message_sorted_playlist_date_added);
                sortFlag = AutoPlaylistRule.DATE_ADDED;
                break;
            case R.id.action_sort_date_played:
                Collections.sort(mSongs, Song.playCountComparator(mPlayCountStore));
                result = getResources().getString(R.string.message_sorted_playlist_date_played);
                sortFlag = AutoPlaylistRule.DATE_PLAYED;
                break;
            default:
                return false;
        }

        mReference = new AutoPlaylist.Builder(mReference).setSortMethod(sortFlag).build();
        mPlaylistStore.editPlaylist(mReference);

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
}
