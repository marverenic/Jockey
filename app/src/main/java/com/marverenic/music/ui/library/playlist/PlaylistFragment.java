package com.marverenic.music.ui.library.playlist;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.PopupMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.data.store.PlayCountStore;
import com.marverenic.music.data.store.PlaylistStore;
import com.marverenic.music.data.store.PreferenceStore;
import com.marverenic.music.databinding.FragmentPlaylistBinding;
import com.marverenic.music.model.AutoPlaylist;
import com.marverenic.music.model.Playlist;
import com.marverenic.music.model.Song;
import com.marverenic.music.model.playlistrules.AutoPlaylistRule;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.ui.BaseToolbarFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import timber.log.Timber;

public class PlaylistFragment extends BaseToolbarFragment {

    private static final String ARG_PLAYLIST = "PlaylistFragment.PLAYLIST";

    @Inject PlayerController mPlayerController;
    @Inject MusicStore mMusicStore;
    @Inject PlaylistStore mPlaylistStore;
    @Inject PlayCountStore mPlayCountStore;
    @Inject PreferenceStore mPreferenceStore;

    private Playlist mPlaylist;
    private FragmentPlaylistBinding mBinding;

    public static PlaylistFragment newInstance(Playlist playlist) {
        PlaylistFragment fragment = new PlaylistFragment();

        Bundle args = new Bundle();
        args.putParcelable(ARG_PLAYLIST, playlist);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPlaylist = getArguments().getParcelable(ARG_PLAYLIST);

        JockeyApplication.getComponent(this).inject(this);
    }

    @Override
    protected String getFragmentTitle() {
        return mPlaylist.getPlaylistName();
    }

    @Override
    protected View onCreateContentView(LayoutInflater inflater, @Nullable ViewGroup container,
                                       @Nullable Bundle savedInstanceState) {

        mBinding = FragmentPlaylistBinding.inflate(inflater, container, false);
        PlaylistViewModel viewModel = new PlaylistViewModel(getContext(), getFragmentManager(),
                mPlayerController, mMusicStore, mPlaylistStore, mPreferenceStore, mPlaylist);

        mPlayerController.getNowPlaying()
                .compose(bindToLifecycle())
                .subscribe(viewModel::setCurrentSong, throwable -> {
                    Timber.e(throwable, "Failed to update current song");
                });

        mBinding.setViewModel(viewModel);

        mPlaylistStore.getSongs(mPlaylist)
                .compose(bindToLifecycle())
                .distinctUntilChanged()
                .map(ArrayList::new)
                .subscribe(viewModel::setSongs, throwable -> {
                    Timber.e(throwable, "Failed to get playlist contents");
                });

        setHasOptionsMenu(true);
        return mBinding.getRoot();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.activity_playlist, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_playlist_sort && getView() != null) {
            View anchor = getView().findViewById(R.id.menu_playlist_sort);
            PopupMenu sortMenu = new PopupMenu(getContext(), anchor, Gravity.END);

            if (mPlaylist instanceof AutoPlaylist) {
                sortMenu.inflate(R.menu.sort_options_auto_playlist);
                sortMenu.setOnMenuItemClickListener(this::onAutoPlaylistSortOptionSelected);
            } else {
                sortMenu.inflate(R.menu.sort_options);
                sortMenu.setOnMenuItemClickListener(this::onSortOptionSelected);
            }

            sortMenu.show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected boolean canNavigateUp() {
        return true;
    }

    private boolean onSortOptionSelected(MenuItem item) {
        String message;
        Comparator<Song> comparator;

        switch (item.getItemId()) {
            case R.id.menu_sort_random:
                comparator = null;
                message = getResources().getString(R.string.message_sorted_playlist_random);
                break;
            case R.id.menu_sort_name:
                comparator = Song::compareTo;
                message = getResources().getString(R.string.message_sorted_playlist_name);
                break;
            case R.id.menu_sort_artist:
                comparator = Song.ARTIST_COMPARATOR;
                message = getResources().getString(R.string.message_sorted_playlist_artist);
                break;
            case R.id.menu_sort_album:
                comparator = Song.ALBUM_COMPARATOR;
                message = getResources().getString(R.string.message_sorted_playlist_album);
                break;
            case R.id.menu_sort_play:
                comparator = Song.playCountComparator(mPlayCountStore);
                message = getResources().getString(R.string.message_sorted_playlist_play);
                break;
            case R.id.menu_sort_skip:
                comparator = Song.skipCountComparator(mPlayCountStore);
                message = getResources().getString(R.string.message_sorted_playlist_skip);
                break;
            case R.id.menu_sort_date_added:
                comparator = Song.DATE_ADDED_COMPARATOR;
                message = getResources().getString(R.string.message_sorted_playlist_date_added);
                break;
            case R.id.menu_sort_date_played:
                comparator = Song.playCountComparator(mPlayCountStore);
                message = getResources().getString(R.string.message_sorted_playlist_date_played);
                break;
            default:
                return false;
        }

        mPlaylistStore.getSongs(mPlaylist).first().subscribe(unsorted -> {
            applySort(unsorted, comparator, message);
        }, throwable -> {
            Timber.e(throwable, "Failed to sort playlist songs");
        });

        return true;
    }

    private void applySort(List<Song> unsorted, Comparator<Song> comparator, String confirmation) {
        List<Song> sorted = new ArrayList<>(unsorted);

        mPlayCountStore.refresh()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(ignoredValue -> {
                    if (comparator == null) {
                        Collections.shuffle(sorted);
                    } else {
                        Collections.sort(sorted, comparator);
                    }

                    mPlaylistStore.editPlaylist(mPlaylist, sorted);
                    return ignoredValue;
                })
                .subscribe(
                        ignoredValue -> {
                            showUndoSortSnackbar(confirmation, v -> {
                                mPlaylistStore.editPlaylist(mPlaylist, unsorted);
                            });
                        }, throwable -> {
                            Timber.e(throwable, "onMenuItemClick: Failed to sort playlist");
                        });
    }

    private boolean onAutoPlaylistSortOptionSelected(MenuItem item) {
        int sortFlag;
        String result;
        boolean ascending;

        switch (item.getItemId()) {
            case R.id.menu_sort_random:
                result = getString(R.string.message_sorted_playlist_random);
                sortFlag = AutoPlaylistRule.ID;
                ascending = false;
                break;
            case R.id.menu_sort_name:
                result = getString(R.string.message_sorted_playlist_name);
                sortFlag = AutoPlaylistRule.NAME;
                ascending = true;
                break;
            case R.id.menu_sort_play:
                result = getString(R.string.message_sorted_playlist_play);
                sortFlag = AutoPlaylistRule.PLAY_COUNT;
                ascending = false;
                break;
            case R.id.menu_sort_skip:
                result = getString(R.string.message_sorted_playlist_skip);
                sortFlag = AutoPlaylistRule.SKIP_COUNT;
                ascending = false;
                break;
            case R.id.menu_sort_date_added:
                result = getString(R.string.message_sorted_playlist_date_added);
                sortFlag = AutoPlaylistRule.DATE_ADDED;
                ascending = false;
                break;
            case R.id.menu_sort_date_played:
                result = getString(R.string.message_sorted_playlist_date_played);
                sortFlag = AutoPlaylistRule.DATE_PLAYED;
                ascending = false;
                break;
            default:
                return false;
        }

        AutoPlaylist playlist = (AutoPlaylist) mPlaylist;

        int oldSortFlag = playlist.getSortMethod();
        if (oldSortFlag == playlist.getSortMethod()) {
            ascending = !playlist.isSortAscending();
        }

        applyAutoPlaylistSort(sortFlag, ascending, result);
        return true;
    }

    private void applyAutoPlaylistSort(int sortFlag, boolean ascending, String confirmation) {
        int oldSortFlag = ((AutoPlaylist) mPlaylist).getSortMethod();
        boolean wasAscending = ((AutoPlaylist) mPlaylist).isSortAscending();

        mPlaylistStore.getSongs(mPlaylist)
                .take(1)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(ignoredValue -> {
                    showUndoSortSnackbar(confirmation, v -> {
                        setAutoPlaylistSortMethod(oldSortFlag, wasAscending);
                    });
                }, throwable -> {
                    Timber.e(throwable, "Failed to set sort method");
                });

        setAutoPlaylistSortMethod(sortFlag, ascending);
    }

    private void setAutoPlaylistSortMethod(int sortMethod, boolean ascending) {
        mPlaylist = new AutoPlaylist.Builder((AutoPlaylist) mPlaylist)
                .setSortMethod(sortMethod)
                .setSortAscending(ascending)
                .build();
        mPlaylistStore.editPlaylist((AutoPlaylist) mPlaylist);
    }

    private void showUndoSortSnackbar(String unformattedMessage, View.OnClickListener undoAction) {
        String message = String.format(unformattedMessage, mPlaylist);

        Snackbar.make(mBinding.getRoot(), message, Snackbar.LENGTH_LONG)
                .setAction(getResources().getString(R.string.action_undo), undoAction)
                .show();
    }
}
