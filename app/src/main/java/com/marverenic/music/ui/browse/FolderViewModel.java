package com.marverenic.music.ui.browse;

import androidx.databinding.Bindable;
import androidx.annotation.Nullable;

import com.marverenic.music.BR;
import com.marverenic.music.ui.BaseViewModel;

import java.io.File;

public class FolderViewModel extends BaseViewModel {

    private File mFolder;
    @Nullable
    private OnFolderSelectedListener mSelectionListener;

    public FolderViewModel() {
        super(null);
    }

    public void setSelectionListener(@Nullable OnFolderSelectedListener selectionListener) {
        mSelectionListener = selectionListener;
    }

    public void setFolder(File folder) {
        mFolder = folder;
        notifyPropertyChanged(BR.folderName);
    }

    @Bindable
    public String getFolderName() {
        return mFolder.getName();
    }

    public void onClickFolder() {
        if (mSelectionListener != null) {
            mSelectionListener.onFolderSelected(mFolder);
        }
    }

    interface OnFolderSelectedListener {
        void onFolderSelected(File directory);
    }

}
