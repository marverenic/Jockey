package com.marverenic.music.ui.library;

import android.content.Context;
import android.databinding.Bindable;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;

import com.marverenic.music.BR;
import com.marverenic.music.R;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.data.store.PlaylistStore;
import com.marverenic.music.data.store.PreferenceStore;
import com.marverenic.music.data.store.ThemeStore;
import com.marverenic.music.ui.BaseViewModel;
import com.marverenic.music.ui.common.playlist.CreatePlaylistDialogFragment;
import com.marverenic.music.ui.library.playlist.edit.AutoPlaylistEditActivity;
import com.marverenic.music.view.FABMenu;
import com.trello.rxlifecycle.components.support.RxFragment;

import rx.Observable;
import timber.log.Timber;

public class LibraryViewModel extends BaseViewModel {

    private static final String TAG_MAKE_PLAYLIST = "CreatePlaylistDialog";

    private Context mContext;
    private FragmentManager mFragmentManager;
    private ThemeStore mThemeStore;

    private FragmentPagerAdapter mPagerAdapter;
    private boolean mRefreshing;
    private int mPage;

    LibraryViewModel(RxFragment fragment, PreferenceStore preferenceStore,
                     ThemeStore themeStore, MusicStore musicStore, PlaylistStore playlistStore) {

        super(fragment);

        mContext = fragment.getContext();
        mFragmentManager = fragment.getFragmentManager();
        mThemeStore = themeStore;

        setPage(preferenceStore.getDefaultPage());
        mPagerAdapter = new LibraryPagerAdapter(getContext(), mFragmentManager);

        Observable.combineLatest(musicStore.isLoading(), playlistStore.isLoading(),
                (musicLoading, playlistLoading) -> {
                    return musicLoading || playlistLoading;
                })
                .compose(bindToLifecycle())
                .subscribe(this::setLibraryRefreshing, throwable -> {
                            Timber.e(throwable, "Failed to update refresh indicator");
                        });
    }

    @Bindable
    public int getPage() {
        return mPage;
    }

    public void setPage(int page) {
        if (page != mPage) {
            mPage = page;
            notifyPropertyChanged(BR.page);
            notifyPropertyChanged(BR.fabVisible);
        }
    }

    @Bindable
    public FragmentPagerAdapter getPagerAdapter() {
        notifyPropertyChanged(BR.page);
        return mPagerAdapter;
    }

    @Bindable
    public boolean isFabVisible() {
        return mPage == 0;
    }

    @Bindable
    public FABMenu.MenuItem[] getFabMenuItems() {
        return new FABMenu.MenuItem[] {
                new FABMenu.MenuItem(R.drawable.ic_add_24dp, v -> createPlaylist(),
                        R.string.playlist),
                new FABMenu.MenuItem(R.drawable.ic_add_24dp, v -> createAutoPlaylist(),
                        R.string.playlist_auto)
        };
    }

    private void setLibraryRefreshing(boolean refreshing) {
        mRefreshing = refreshing;
        notifyPropertyChanged(BR.libraryRefreshing);
    }

    @Bindable
    public boolean isLibraryRefreshing() {
        return mRefreshing;
    }

    @Bindable
    public int[] getRefreshIndicatorColors() {
        return new int[] {
                ContextCompat.getColor(mContext, mThemeStore.getPrimaryColor().getPrimaryColorRes()),
                ContextCompat.getColor(mContext, mThemeStore.getAccentColor().getAccentColorRes())
        };
    }

    private void createPlaylist() {
        new CreatePlaylistDialogFragment.Builder(mFragmentManager)
                .showSnackbarIn(R.id.library_pager)
                .show(TAG_MAKE_PLAYLIST);
    }

    private void createAutoPlaylist() {
        getContext().startActivity(AutoPlaylistEditActivity.newIntent(getContext()));
    }

}
