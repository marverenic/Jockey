package com.marverenic.music.ui.library.browse;

import android.content.Context;
import android.databinding.Bindable;
import android.support.annotation.Nullable;

import com.marverenic.music.BR;
import com.marverenic.music.ui.BaseViewModel;

import java.io.File;

public class FileViewModel extends BaseViewModel {

    private File mFile;
    @Nullable
    private OnFileSelectedListener mSelectionListener;

    public FileViewModel(Context context) {
        super(context);
    }

    public void setFileSelectionListener(OnFileSelectedListener selectionListener) {
        mSelectionListener = selectionListener;
    }

    public void setFile(File file) {
        mFile = file;
        notifyPropertyChanged(BR.fileName);
    }

    @Bindable
    public String getFileName() {
        return mFile.getName();
    }

    public void onClickFile() {
        if (mSelectionListener != null) {
            mSelectionListener.onFileSelected(mFile);
        }
    }

    interface OnFileSelectedListener {
        void onFileSelected(File file);
    }

}
