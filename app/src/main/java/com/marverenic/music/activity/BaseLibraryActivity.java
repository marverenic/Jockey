package com.marverenic.music.activity;

import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.design.widget.BottomSheetBehavior;
import android.view.View;

import com.marverenic.music.R;
import com.marverenic.music.databinding.ActivityLibraryBaseWrapperBinding;
import com.marverenic.music.viewmodel.BaseLibraryActivityViewModel;

public abstract class BaseLibraryActivity extends BaseActivity {

    private ActivityLibraryBaseWrapperBinding mBinding;
    private BaseLibraryActivityViewModel mViewModel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_library_base_wrapper);
        mViewModel = new BaseLibraryActivityViewModel(this, !isToolbarCollapsing());
        mBinding.setViewModel(mViewModel);

        getLayoutInflater().inflate(getContentLayoutResource(),
                mBinding.libraryBaseWrapperContainer, true);

        setupToolbar();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mViewModel.onActivityExitForeground();
        mBinding.executePendingBindings();
    }

    @Override
    public void onResume() {
        super.onResume();
        mViewModel.onActivityEnterForeground();
        mBinding.executePendingBindings();
    }

    @Override
    public void onBackPressed() {
        BottomSheetBehavior<View> bottomSheet = BottomSheetBehavior.from(mBinding.miniplayerHolder);
        if (bottomSheet.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheet.setState(BottomSheetBehavior.STATE_COLLAPSED);
        } else {
            super.onBackPressed();
        }
    }

    @LayoutRes
    protected abstract int getContentLayoutResource();

    public boolean isToolbarCollapsing() {
        return false;
    }

    public void expandBottomSheet() {
        BottomSheetBehavior<View> bottomSheet = BottomSheetBehavior.from(mBinding.miniplayerHolder);
        bottomSheet.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

}
