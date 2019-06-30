package com.marverenic.music.ui;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.marverenic.music.databinding.FragmentBaseToolbarBinding;

public abstract class BaseToolbarFragment extends BaseFragment {

    private FragmentBaseToolbarBinding mBinding;

    @Override
    public final View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                   @Nullable Bundle savedInstanceState) {

        mBinding = FragmentBaseToolbarBinding.inflate(inflater, container, false);
        View contentView = onCreateContentView(inflater, container, savedInstanceState);

        setUpToolbar(mBinding.toolbarContainer.toolbar);

        mBinding.fragmentContents.addView(contentView);
        return mBinding.getRoot();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onNavigateUp();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected abstract View onCreateContentView(LayoutInflater inflater,
                                                @Nullable ViewGroup container,
                                                @Nullable Bundle savedInstanceState);

    protected abstract String getFragmentTitle();

    protected void updateFragmentTitle() {
        mBinding.toolbarContainer.toolbar.setTitle(getFragmentTitle());
    }

    protected Drawable getUpButtonDrawable() {
        return null;
    }

    protected void setUpToolbar(Toolbar toolbar) {
        toolbar.setTitle(getFragmentTitle());

        setActivitySupportActionBar(toolbar);
        ActionBar actionBar = getActivitySupportActionBar();

        if (actionBar != null) {
            boolean showUpButton = canNavigateUp();
            actionBar.setDisplayHomeAsUpEnabled(showUpButton);
            actionBar.setHomeButtonEnabled(showUpButton);
            actionBar.setDisplayShowHomeEnabled(showUpButton);

            actionBar.setHomeAsUpIndicator(getUpButtonDrawable());
        }
    }

    protected boolean canNavigateUp() {
        return false;
    }

    protected void onNavigateUp() {
        getActivity().finish();
    }
}
