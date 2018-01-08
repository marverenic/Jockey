package com.marverenic.music.ui.library.browse;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.marverenic.adapter.HeterogeneousAdapter;
import com.marverenic.music.ui.BaseViewModel;
import com.marverenic.music.utils.Util;
import com.marverenic.music.view.BackgroundDecoration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

public class MusicBrowserViewModel extends BaseViewModel {

    private Stack<File> mHistory;
    private File mCurrentDirectory;

    private HeterogeneousAdapter mAdapter;
    private FolderSection mFolderSection;
    private FileSection mFileSection;
    private OnSongFileSelectedListener mSelectionListener;

    public MusicBrowserViewModel(Context context, File startingDirectory,
                                 @NonNull OnSongFileSelectedListener songSelectionListener) {
        super(context);
        mSelectionListener = songSelectionListener;
        mHistory = new Stack<>();

        mAdapter = new HeterogeneousAdapter();
        mFolderSection = new FolderSection(Collections.emptyList(), this::onClickFolder);
        mFileSection = new FileSection(Collections.emptyList(), this::onClickSong);
        mAdapter.addSection(mFolderSection);
        mAdapter.addSection(mFileSection);
        mAdapter.setHasStableIds(true);

        setDirectory(startingDirectory);
    }

    public void setDirectory(File directory) {
        mCurrentDirectory = directory;

        if (directory.canRead()) {
            List<File> folders = new ArrayList<>();
            List<File> files = new ArrayList<>();

            for (File file : directory.listFiles()) {
                if (file.isDirectory()) {
                    folders.add(file);
                } else if (Util.isFileMusic(file)) {
                    files.add(file);
                }
            }

            Collections.sort(folders);
            Collections.sort(files);

            mFolderSection.setData(folders);
            mFileSection.setData(files);
            mAdapter.notifyDataSetChanged();
        } else {
            mFolderSection.setData(Collections.emptyList());
        }
    }

    public RecyclerView.Adapter getAdapter() {
        return mAdapter;
    }

    public RecyclerView.LayoutManager getLayoutManager() {
        return new LinearLayoutManager(getContext());
    }

    public RecyclerView.ItemDecoration[] getItemDecorations() {
        return new RecyclerView.ItemDecoration[] {
                new BackgroundDecoration()
        };
    }

    private void onClickFolder(File folder) {
        mHistory.add(mCurrentDirectory);
        setDirectory(folder);
    }

    private void onClickSong(File song) {
        mSelectionListener.onSongFileSelected(song);
    }

    public boolean goBack() {
        if (!mHistory.isEmpty()) {
            setDirectory(mHistory.pop());
            return true;
        }
        return false;
    }

    interface OnSongFileSelectedListener {
        void onSongFileSelected(File song);
    }

}
