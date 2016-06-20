package com.marverenic.music.utils;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.widget.TextView;

import com.marverenic.music.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DirectoryDialog implements DialogInterface.OnClickListener {

    private Context mContext;
    private File mDirectory;
    private boolean mShown;
    private List<File> mSubDirectories;
    private String[] mSubDirectoryNames;
    private OnDirectoryChosenListener mCallback;

    public DirectoryDialog(Context context) {
        this(context, Environment.getExternalStorageDirectory());
    }

    public DirectoryDialog(Context context, File directory) {
        mContext = context;
        mDirectory = directory;
        mShown = false;
    }

    private void scanSubDirs() {
        mSubDirectories = new ArrayList<>();
        for (File f : mDirectory.listFiles()) {
            if (f.isDirectory() && f.canRead()) {
                mSubDirectories.add(f);
            }
        }

        mSubDirectoryNames = new String[mSubDirectories.size()];
        for (int i = 0; i < mSubDirectoryNames.length; i++) {
            mSubDirectoryNames[i] = mSubDirectories.get(i).getName();
        }
    }

    public void show() {
        if (mShown) {
            throw new IllegalStateException(
                    "Cannot show a DirectoryDialog that has already been shown");
        }
        mShown = true;
        prompt();
    }

    private void prompt() {
        scanSubDirs();

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext)
                .setTitle(mDirectory.getAbsolutePath())
                .setPositiveButton(R.string.action_select, this)
                .setNegativeButton(R.string.action_cancel, null)
                .setNeutralButton(R.string.action_navigate_up, this);

        if (mSubDirectories.isEmpty()) {
            builder.setMessage(R.string.empty_directory);
        } else {
            builder.setItems(mSubDirectoryNames, this);
        }

        AlertDialog dialog = builder.show();

        File parent = mDirectory.getParentFile();
        if (parent == null || !parent.canRead()) {
            dialog.getButton(DialogInterface.BUTTON_NEUTRAL).setEnabled(false);
        }

        // Ellipsize the header at the beginning to show a more informative file path
        TextView header = (TextView) dialog.findViewById(R.id.alertTitle);
        if (header != null) {
            header.setEllipsize(TextUtils.TruncateAt.START);
        }

    }

    public void setOnDirectoryChosenListener(OnDirectoryChosenListener callback) {
        mCallback = callback;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                if (mCallback != null) {
                    mCallback.onDirectoryChosen(mDirectory);
                }
                break;
            case DialogInterface.BUTTON_NEUTRAL:
                mDirectory = mDirectory.getParentFile();
                prompt();
                break;
        }

        if (which >= 0) {
            mDirectory = mSubDirectories.get(which);
            prompt();
        }
    }

    public interface OnDirectoryChosenListener {
        void onDirectoryChosen(File directory);
    }
}
