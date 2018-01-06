package com.marverenic.music.ui.library.browse;

import android.databinding.Bindable;

import com.marverenic.music.BR;
import com.marverenic.music.ui.BaseViewModel;

import java.io.File;

public class FolderViewModel extends BaseViewModel {

    private File mFolder;

    public FolderViewModel() {
        super(null);
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
        // TODO perform navigation
    }

}
