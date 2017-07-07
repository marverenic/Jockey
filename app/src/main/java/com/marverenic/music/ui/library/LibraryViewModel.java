package com.marverenic.music.ui.library;

import android.content.Context;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.marverenic.music.BR;
import com.marverenic.music.data.store.PreferenceStore;

public class LibraryViewModel extends BaseObservable {

    private Context mContext;

    private FragmentPagerAdapter mPagerAdapter;
    private int mPage;

    LibraryViewModel(Context context, FragmentManager fragmentManager,
                     PreferenceStore preferenceStore) {

        mContext = context;

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

}
