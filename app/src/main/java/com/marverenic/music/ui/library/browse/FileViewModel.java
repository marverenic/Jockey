package com.marverenic.music.ui.library.browse;

import android.content.Context;
import android.databinding.Bindable;

import com.marverenic.music.BR;
import com.marverenic.music.ui.BaseViewModel;

import java.io.File;

public class FileViewModel extends BaseViewModel {

    private File mFile;

    public FileViewModel(Context context) {
        super(context);
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
        // TDOO
    }

}
