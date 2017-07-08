package com.marverenic.music.ui.library;

import android.content.Context;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.marverenic.music.BR;
import com.marverenic.music.R;
import com.marverenic.music.data.store.PreferenceStore;
import com.marverenic.music.ui.common.playlist.CreatePlaylistDialogFragment;
import com.marverenic.music.ui.library.playlist.edit.AutoPlaylistEditActivity;
import com.marverenic.music.view.FABMenu;

public class LibraryViewModel extends BaseObservable {

    private static final String TAG_MAKE_PLAYLIST = "CreatePlaylistDialog";

    private Context mContext;
    private FragmentManager mFragmentManager;

    private FragmentPagerAdapter mPagerAdapter;
    private int mPage;

    LibraryViewModel(Context context, FragmentManager fragmentManager,
                     PreferenceStore preferenceStore) {

        mContext = context;
        mFragmentManager = fragmentManager;

        setPage(preferenceStore.getDefaultPage());
        mPagerAdapter = new LibraryPagerAdapter(context, fragmentManager);
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

    private void createPlaylist() {
        new CreatePlaylistDialogFragment.Builder(mFragmentManager)
                .showSnackbarIn(R.id.library_pager)
                .show(TAG_MAKE_PLAYLIST);
    }

    private void createAutoPlaylist() {
        mContext.startActivity(AutoPlaylistEditActivity.newIntent(mContext));
    }

}
