package com.marverenic.music.ui.library.browse;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.marverenic.music.ui.BaseViewModel;

import java.io.File;

public class MusicBrowserViewModel extends BaseViewModel {

    private File mCurrentDirectory;

    public MusicBrowserViewModel(Context context, File startingDirectory) {
        super(context);
        setDirectory(startingDirectory);
    }

    public void setDirectory(File directory) {
        mCurrentDirectory = directory;
    }

    public RecyclerView.Adapter getAdapter() {
        return null;
    }

    public RecyclerView.LayoutManager getLayoutManager() {
        return new LinearLayoutManager(getContext());
    }
}
