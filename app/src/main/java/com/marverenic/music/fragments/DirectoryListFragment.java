package com.marverenic.music.fragments;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.marverenic.heterogeneousadapter.DragDropAdapter;
import com.marverenic.heterogeneousadapter.EnhancedViewHolder;
import com.marverenic.heterogeneousadapter.HeterogeneousAdapter;
import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.data.store.PreferencesStore;
import com.marverenic.music.dialog.DirectoryDialogFragment;
import com.marverenic.music.instances.section.BasicEmptyState;
import com.marverenic.music.view.DragDividerDecoration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

public class DirectoryListFragment extends Fragment implements View.OnClickListener,
        DirectoryDialogFragment.OnDirectoryPickListener {

    private static final String TAG = "DirectoryListFragment";

    private static final String KEY_EXCLUDE_FLAG = "DirectoryListFragment.exclude";
    private static final String TAG_DIR_DIALOG = "DirectoryListFragment_DirectoryDialog";

    @Inject MusicStore mMusicStore;
    @Inject PreferencesStore mPreferencesStore;

    private boolean mExclude;
    private List<String> mDirectories;
    private Set<String> mOppositeDirectories;
    private DragDropAdapter mAdapter;

    public static DirectoryListFragment newInstance(boolean exclude) {
        DirectoryListFragment fragment = new DirectoryListFragment();

        Bundle args = new Bundle();
        args.putBoolean(KEY_EXCLUDE_FLAG, exclude);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mExclude = getArguments().getBoolean(KEY_EXCLUDE_FLAG);

        JockeyApplication.getComponent(this).inject(this);

        if (mExclude) {
            mDirectories = new ArrayList<>(mPreferencesStore.getExcludedDirectories());
            mOppositeDirectories = mPreferencesStore.getIncludedDirectories();
        } else {
            mDirectories = new ArrayList<>(mPreferencesStore.getIncludedDirectories());
            mOppositeDirectories = mPreferencesStore.getExcludedDirectories();
        }
        Collections.sort(mDirectories);

        Fragment directoryPicker = getFragmentManager().findFragmentByTag(TAG_DIR_DIALOG);
        if (directoryPicker instanceof DirectoryDialogFragment) {
            ((DirectoryDialogFragment) directoryPicker).setDirectoryPickListener(this);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup view =
                (ViewGroup) inflater.inflate(R.layout.fragment_directory_list, container, false);
        view.findViewById(R.id.fab).setOnClickListener(this);

        mAdapter = new DragDropAdapter();
        mAdapter.setEmptyState(new BasicEmptyState() {
            @Override
            public String getMessage() {
                if (mExclude) {
                    return getString(R.string.empty_excluded_dirs);
                } else {
                    return getString(R.string.empty_included_dirs);
                }
            }

            @Override
            public String getDetail() {
                if (mExclude) {
                    return getString(R.string.empty_excluded_dirs_detail);
                } else {
                    return getString(R.string.empty_included_dirs_detail);
                }
            }
        });

        mAdapter.addSection(new DirectorySection(mDirectories));

        RecyclerView list = (RecyclerView) view.findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(getContext()));
        mAdapter.attach(list);

        list.addItemDecoration(new DragDividerDecoration(getContext(), R.id.subheaderFrame));

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setTitle((mExclude)
                    ? R.string.pref_directory_exclude
                    : R.string.pref_directory_include);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        Set<String> previouslyIncluded = mPreferencesStore.getIncludedDirectories();
        Set<String> previouslyExcluded = mPreferencesStore.getExcludedDirectories();

        if (mExclude) {
            mPreferencesStore.setExcludedDirectories(mDirectories);
            mPreferencesStore.setIncludedDirectories(mOppositeDirectories);
        } else {
            mPreferencesStore.setIncludedDirectories(mDirectories);
            mPreferencesStore.setExcludedDirectories(mOppositeDirectories);
        }

        Set<String> currentlyIncluded = mPreferencesStore.getIncludedDirectories();
        Set<String> currentlyExcluded = mPreferencesStore.getExcludedDirectories();

        boolean isDifferent = !currentlyExcluded.equals(previouslyExcluded)
                || !currentlyIncluded.equals(previouslyIncluded);

        if (isDifferent) {
            mMusicStore.refresh().subscribe(
                    updated -> {},
                    throwable -> {Log.e(TAG, "Failed to refresh library", throwable);});
        }
    }

    @Override
    public void onClick(View v) {
        new DirectoryDialogFragment()
                .setDirectoryPickListener(this)
                .show(getFragmentManager(), TAG_DIR_DIALOG);
    }

    @Override
    public void onDirectoryChosen(final File directory) {
        if (mOppositeDirectories.contains(directory.getAbsolutePath())) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            AlertDialog.OnClickListener clickListener = (dialog, which) -> {
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    mOppositeDirectories.remove(directory.getAbsolutePath());
                    mDirectories.add(directory.getAbsolutePath());

                    if (mDirectories.size() == 1) {
                        mAdapter.notifyDataSetChanged();
                    } else {
                        mAdapter.notifyItemInserted(mDirectories.size() - 1);
                    }
                }
            };

            String message;
            String positiveAction;

            if (mExclude) {
                message = getString(R.string.confirm_dir_exclude_included, directory.getName());
                positiveAction = getString(R.string.action_exclude);
            } else {
                message = getString(R.string.confirm_dir_include_excluded, directory.getName());
                positiveAction = getString(R.string.action_include);
            }

            builder
                    .setMessage(message)
                    .setPositiveButton(positiveAction, clickListener)
                    .setNegativeButton(R.string.action_cancel, null)
                    .show();
        } else if (mDirectories.contains(directory.getAbsolutePath())) {
            String message = getString(
                    (mExclude)
                            ? R.string.confirm_dir_already_excluded
                            : R.string.confirm_dir_already_included,
                    directory.getName());

            //noinspection ConstantConditions
            Snackbar.make(getView(), message, Snackbar.LENGTH_SHORT).show();
        } else {
            mDirectories.add(directory.getAbsolutePath());
            if (mDirectories.size() == 1) {
                mAdapter.notifyDataSetChanged();
            } else {
                mAdapter.notifyItemInserted(mDirectories.size() - 1);
            }
        }
    }

    private class DirectorySection extends DragDropAdapter.ListSection<String> {

        public DirectorySection(List<String> data) {
            super(data);
        }

        @Override
        public EnhancedViewHolder<String> createViewHolder(HeterogeneousAdapter adapter,
                                                           ViewGroup parent) {
            return new DirectoryViewHolder(
                    LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.instance_directory, parent, false));
        }
    }

    private class DirectoryViewHolder extends EnhancedViewHolder<String>
            implements View.OnClickListener, PopupMenu.OnMenuItemClickListener {

        TextView directoryName;
        TextView directoryPath;
        String reference;
        int index;

        /**
         * @param itemView The view that this ViewHolder will manage
         */
        public DirectoryViewHolder(View itemView) {
            super(itemView);

            directoryName = (TextView) itemView.findViewById(R.id.instanceTitle);
            directoryPath = (TextView) itemView.findViewById(R.id.instanceDetail);

            itemView.findViewById(R.id.instanceMore).setOnClickListener(this);
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onUpdate(String item, int position) {
            reference = item;
            index = position;

            directoryPath.setText(item + File.separatorChar);

            int lastSeparatorIndex = item.lastIndexOf(File.separatorChar);
            if (lastSeparatorIndex < 0) {
                directoryName.setText(item);
            } else {
                directoryName.setText(item.substring(lastSeparatorIndex + 1));
            }
        }

        @Override
        public void onClick(View v) {
            PopupMenu menu = new PopupMenu(itemView.getContext(), v, Gravity.END);
            menu.getMenu().add(getString(R.string.action_remove));
            menu.setOnMenuItemClickListener(this);
            menu.show();
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            final String removedReference = reference;
            final int removedIndex = index;

            // Remove this view holder's reference
            mDirectories.remove(reference);
            if (mDirectories.isEmpty()) {
                mAdapter.notifyItemChanged(0);
            } else {
                mAdapter.notifyItemRemoved(index);
            }

            // Prompt a confirmation Snackbar with undo button
            Snackbar
                    .make(itemView, getString(R.string.message_removed_directory, reference),
                            Snackbar.LENGTH_LONG)
                    .setAction(R.string.action_undo,
                            v -> {
                                mDirectories.add(removedIndex, removedReference);
                                if (mDirectories.size() == 1) {
                                    mAdapter.notifyItemChanged(0);
                                } else {
                                    mAdapter.notifyItemRemoved(removedIndex);
                                }
                            })
                    .show();

            return true;
        }
    }
}
