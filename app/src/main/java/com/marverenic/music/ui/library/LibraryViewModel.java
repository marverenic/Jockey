package com.marverenic.music.ui.library;

import android.databinding.BaseObservable;
import android.databinding.Bindable;

import com.marverenic.music.BR;
import com.marverenic.music.data.store.PreferenceStore;

public class LibraryViewModel extends BaseObservable {

    private int mPage;

    LibraryViewModel(PreferenceStore preferenceStore) {
        setPage(preferenceStore.getDefaultPage());
    }

    @Bindable
    public int getPage() {
        return mPage;
    }

    public void setPage(int page) {
        if (page != mPage) {
            mPage = page;
            notifyPropertyChanged(BR.page);
        }
    }

}
