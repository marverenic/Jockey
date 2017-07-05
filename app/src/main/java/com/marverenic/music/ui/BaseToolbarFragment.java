package com.marverenic.music.ui;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.marverenic.music.databinding.FragmentBaseToolbarBinding;

import timber.log.Timber;

public abstract class BaseToolbarFragment extends BaseFragment {

    private FragmentBaseToolbarBinding mBinding;

    protected abstract String getFragmentTitle();

    @Override
    public final View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                                   @Nullable Bundle savedInstanceState) {

        mBinding = FragmentBaseToolbarBinding.inflate(inflater, container, false);
        View contentView = onCreateContentView(inflater, container, savedInstanceState);

        setUpToolbar(mBinding.toolbarContainer.toolbar);

        mBinding.fragmentContents.addView(contentView);
        return mBinding.getRoot();
    }

    protected abstract View onCreateContentView(LayoutInflater inflater,
                                                @Nullable ViewGroup container,
                                                @Nullable Bundle savedInstanceState);

    protected void setUpToolbar(Toolbar toolbar) {
        toolbar.setTitle(getFragmentTitle());

        Activity parentActivity = getActivity();
        if (parentActivity instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) parentActivity;
            activity.setSupportActionBar(toolbar);

            ActionBar actionBar = activity.getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(true);
                actionBar.setHomeButtonEnabled(true);
                actionBar.setDisplayShowHomeEnabled(true);
            }

        } else {
            Timber.w("Hosting activity is not an AppCompatActivity. Toolbar will not be bound.");
        }
    }

    protected boolean canNavigateUp() {
        return false;
    }
}
