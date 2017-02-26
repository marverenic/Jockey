package com.marverenic.music.activity;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.LayoutRes;

import com.marverenic.music.R;
import com.marverenic.music.databinding.ActivityLibraryBaseWrapperBinding;

public abstract class BaseLibraryActivity extends BaseActivity {

    private ActivityLibraryBaseWrapperBinding mBinding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_library_base_wrapper);
        getLayoutInflater().inflate(getContentLayoutResource(),
                mBinding.libraryBaseWrapperContainer, true);

        setupToolbar();
    }

    @LayoutRes
    protected abstract int getContentLayoutResource();

}
