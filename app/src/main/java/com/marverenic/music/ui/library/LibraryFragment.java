package com.marverenic.music.ui.library;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.data.store.MediaStoreUtil;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.data.store.PlaylistStore;
import com.marverenic.music.data.store.PreferenceStore;
import com.marverenic.music.data.store.ThemeStore;
import com.marverenic.music.databinding.FragmentLibraryBinding;
import com.marverenic.music.ui.BaseFragment;
import com.marverenic.music.ui.about.AboutActivity;
import com.marverenic.music.ui.common.playlist.CreatePlaylistDialogFragment;
import com.marverenic.music.ui.library.playlist.edit.AutoPlaylistEditActivity;
import com.marverenic.music.ui.search.SearchActivity;
import com.marverenic.music.ui.settings.SettingsActivity;

import javax.inject.Inject;

import rx.Observable;
import timber.log.Timber;

public class LibraryFragment extends BaseFragment {

    private static final String TAG_MAKE_PLAYLIST = "CreatePlaylistDialog";

    @Inject MusicStore mMusicStore;
    @Inject PlaylistStore mPlaylistStore;
    @Inject ThemeStore mThemeStore;
    @Inject PreferenceStore mPrefStore;

    private FragmentLibraryBinding mBinding;
    private LibraryViewModel mViewModel;

    public static LibraryFragment newInstance() {
        return new LibraryFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        JockeyApplication.getComponent(this).inject(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        mBinding = FragmentLibraryBinding.inflate(inflater, container, false);
        mViewModel = new LibraryViewModel(mPrefStore);
        mBinding.setViewModel(mViewModel);

        initRefreshLayout();
        mMusicStore.loadAll();
        mPlaylistStore.loadPlaylists();

        // Setup the FAB
        mBinding.fab.addChild(R.drawable.ic_add_24dp, v -> {
            new CreatePlaylistDialogFragment.Builder(getFragmentManager())
                    .showSnackbarIn(R.id.library_pager)
                    .show(TAG_MAKE_PLAYLIST);
        }, R.string.playlist);

        mBinding.fab.addChild(R.drawable.ic_add_24dp, v -> {
            startActivity(AutoPlaylistEditActivity.newIntent(getContext()));
        }, R.string.playlist_auto);

        ViewPager pager = mBinding.libraryPager;
        AppBarLayout appBarLayout = mBinding.libraryAppBarLayout;

        appBarLayout.addOnOffsetChangedListener((layout, verticalOffset) ->
                pager.setPadding(pager.getPaddingLeft(),
                        pager.getPaddingTop(),
                        pager.getPaddingRight(),
                        layout.getTotalScrollRange() + verticalOffset));

        int page = mPrefStore.getDefaultPage();
        if (page != 0 || !MediaStoreUtil.hasPermission(getContext())) {
            mBinding.fab.setVisibility(View.GONE);
        }

        LibraryPagerAdapter adapter = new LibraryPagerAdapter(getContext(), getFragmentManager());
        adapter.setFloatingActionButton(mBinding.fab);
        pager.setAdapter(adapter);
        pager.addOnPageChangeListener(adapter);
        mBinding.libraryTabs.setupWithViewPager(pager);

        setHasOptionsMenu(true);

        return mBinding.getRoot();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.activity_library, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_library_settings:
                startActivity(SettingsActivity.newIntent(getContext()));
                return true;
            case R.id.menu_library_search:
                startActivity(SearchActivity.newIntent(getContext()));
                return true;
            case R.id.menu_library_about:
                startActivity(AboutActivity.newIntent(getContext()));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void initRefreshLayout() {
        SwipeRefreshLayout swipeRefreshLayout = mBinding.libraryRefreshLayout;
        swipeRefreshLayout.setSize(SwipeRefreshLayout.DEFAULT);
        swipeRefreshLayout.setColorSchemeColors(mThemeStore.getPrimaryColor(),
                mThemeStore.getAccentColor());
        swipeRefreshLayout.setEnabled(false);

        Observable.combineLatest(mMusicStore.isLoading(), mPlaylistStore.isLoading(),
                (musicLoading, playlistLoading) -> {
                    return musicLoading || playlistLoading;
                })
                .compose(bindToLifecycle())
                .subscribe(
                        refreshing -> {
                            swipeRefreshLayout.setEnabled(refreshing);
                            swipeRefreshLayout.setRefreshing(refreshing);
                        }, throwable -> {
                            Timber.e(throwable, "Failed to update refresh indicator");
                        });
    }
}
