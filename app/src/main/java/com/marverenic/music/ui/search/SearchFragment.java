package com.marverenic.music.ui.search;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.data.store.PlaylistStore;
import com.marverenic.music.data.store.PreferenceStore;
import com.marverenic.music.databinding.FragmentSearchBinding;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.ui.BaseToolbarFragment;
import com.marverenic.music.ui.common.OnSongSelectedListener;
import com.marverenic.music.utils.StringUtils;

import javax.inject.Inject;

import timber.log.Timber;

public class SearchFragment extends BaseToolbarFragment {

    private static final String KEY_SAVED_QUERY = "SearchActivity.LAST_QUERY";

    @Inject PlayerController mPlayerController;
    @Inject MusicStore mMusicStore;
    @Inject PlaylistStore mPlaylistStore;
    @Inject PreferenceStore mPreferenceStore;

    private FragmentSearchBinding mBinding;
    private SearchViewModel mViewModel;

    private String initialQuery = "";

    public static SearchFragment newInstance() {
        return new SearchFragment();
    }

    @Override
    protected String getFragmentTitle() {
        return getString(R.string.header_search);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        JockeyApplication.getComponent(this).inject(this);

        if (savedInstanceState != null) {
            initialQuery = savedInstanceState.getString(KEY_SAVED_QUERY, initialQuery);
        }
    }

    @Override
    protected View onCreateContentView(LayoutInflater inflater, @Nullable ViewGroup container,
                                       @Nullable Bundle savedInstanceState) {
        mBinding = FragmentSearchBinding.inflate(inflater, container, false);
        mViewModel = new SearchViewModel(getContext(), getFragmentManager(), mPlayerController,
                mMusicStore, mPlaylistStore,
                OnSongSelectedListener.defaultImplementation(getActivity(), mPreferenceStore),
                initialQuery);

        mPlayerController.getNowPlaying()
                .compose(bindToLifecycle())
                .subscribe(mViewModel::setCurrentSong, throwable -> {
                    Timber.e(throwable, "Failed to update current song");
                });

        mBinding.setViewModel(mViewModel);
        setHasOptionsMenu(true);

        return mBinding.getRoot();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.activity_search, menu);

        MenuItem searchItem = menu.findItem(R.id.menu_library_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mViewModel.setSearchQuery(newText);
                return true;
            }
        });

        searchView.setIconified(false);
        if (!StringUtils.isEmpty(mViewModel.getSearchQuery())) {
            searchView.setQuery(mViewModel.getSearchQuery(), true);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_SAVED_QUERY, mViewModel.getSearchQuery());
    }

    @Override
    protected boolean canNavigateUp() {
        return true;
    }

    public void setSearchQuery(String query) {
        if (mViewModel != null) {
            mViewModel.setSearchQuery(query);
        } else {
            initialQuery = query;
        }
    }
}
