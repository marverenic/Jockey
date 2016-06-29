package com.marverenic.music.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.widget.TextView;

import com.marverenic.music.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DirectoryDialogFragment extends DialogFragment {

    private static final String KEY_DIRECTORY = "Directory";

    private File mDirectory;
    private List<File> mSubDirectories;
    private String[] mSubDirectoryNames;
    private OnDirectoryPickListener mCallback;

    public DirectoryDialogFragment setDirectory(File directory) {
        mDirectory = directory;
        return this;
    }

    public DirectoryDialogFragment setDirectoryPickListener(OnDirectoryPickListener callback) {
        mCallback = callback;
        return this;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            String path = savedInstanceState.getString(KEY_DIRECTORY);
            if (path != null) {
                mDirectory = new File(path);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mDirectory != null) {
            outState.putString(KEY_DIRECTORY, mDirectory.getAbsolutePath());
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (mDirectory == null) {
            setDirectory(Environment.getExternalStorageDirectory());
        }

        scanSubDirs();

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
                .setTitle(mDirectory.getAbsolutePath())
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_select, (dialogInterface, i) -> {
                    if (mCallback != null) {
                        mCallback.onDirectoryChosen(mDirectory);
                    }
                })
                .setNeutralButton(R.string.action_navigate_up, (dialogInterface, i) -> {
                    new DirectoryDialogFragment()
                            .setDirectory(mDirectory.getParentFile())
                            .setDirectoryPickListener(mCallback)
                            .show(getFragmentManager(), getTag());
                });

        if (mSubDirectories.isEmpty()) {
            builder.setMessage(R.string.empty_directory);
        } else {
            builder.setItems(mSubDirectoryNames, (dialog, which) -> {
                File directory = mSubDirectories.get(which);
                new DirectoryDialogFragment()
                        .setDirectory(directory)
                        .setDirectoryPickListener(mCallback)
                        .show(getFragmentManager(), getTag());
            });
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

        return dialog;
    }

    private void scanSubDirs() {
        mSubDirectories = new ArrayList<>();
        for (File f : mDirectory.listFiles()) {
            if (f.isDirectory() && f.canRead()) {
                mSubDirectories.add(f);
            }
        }

        Collections.sort(mSubDirectories,
                (f1, f2) -> f1.getAbsolutePath().compareToIgnoreCase(f2.getAbsolutePath()));

        mSubDirectoryNames = new String[mSubDirectories.size()];
        for (int i = 0; i < mSubDirectoryNames.length; i++) {
            mSubDirectoryNames[i] = mSubDirectories.get(i).getName();
        }
    }

    public interface OnDirectoryPickListener {
        void onDirectoryChosen(File directory);
    }
}
