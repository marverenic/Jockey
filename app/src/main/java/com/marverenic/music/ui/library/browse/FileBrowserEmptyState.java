package com.marverenic.music.ui.library.browse;

import android.content.Context;
import android.view.View;

import com.marverenic.music.R;
import com.marverenic.music.ui.common.LibraryEmptyState;

public class FileBrowserEmptyState extends LibraryEmptyState {

    private Context mContext;
    private OnRefreshClickListener mRefreshCallback;

    public FileBrowserEmptyState(Context context, OnRefreshClickListener refreshCallback) {
        super(context, null, null);
        mContext = context;
        mRefreshCallback = refreshCallback;
    }

    @Override
    public String getEmptyAction1Label() {
        return mContext.getString(R.string.action_try_again);
    }

    @Override
    public void onAction1(View button) {
        mRefreshCallback.onRefresh();
    }

    interface OnRefreshClickListener {
        void onRefresh();
    }

}
