package com.marverenic.music.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.marverenic.music.databinding.FragmentBaseToolbarBinding;

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

        setActivitySupportActionBar(toolbar);
        ActionBar actionBar = getActivitySupportActionBar();

        if (actionBar != null) {
            boolean showUpButton = canNavigateUp();
            actionBar.setDisplayHomeAsUpEnabled(showUpButton);
            actionBar.setHomeButtonEnabled(showUpButton);
            actionBar.setDisplayShowHomeEnabled(showUpButton);
        }
    }

    protected boolean canNavigateUp() {
        return false;
    }
}
