package com.marverenic.music.ui.library.playlist.edit;

import android.content.Context;
import android.databinding.Bindable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.marverenic.adapter.HeterogeneousAdapter;
import com.marverenic.music.BR;
import com.marverenic.music.model.AutoPlaylist;
import com.marverenic.music.model.playlistrules.AutoPlaylistRule;
import com.marverenic.music.ui.BaseViewModel;
import com.marverenic.music.view.BackgroundDecoration;
import com.marverenic.music.view.DividerDecoration;

public class AutoPlaylistEditViewModel extends BaseViewModel {

    private AutoPlaylist mOriginalPlaylist;
    private AutoPlaylist.Builder mEditedPlaylist;
    private int mScrollPosition;

    private HeterogeneousAdapter mAdapter;

    public AutoPlaylistEditViewModel(Context context, AutoPlaylist originalPlaylist,
                                     AutoPlaylist.Builder editedPlaylist) {
        super(context);
        mOriginalPlaylist = originalPlaylist;
        mEditedPlaylist = editedPlaylist;

        createAdapter();
    }

    private void createAdapter() {
        mAdapter = new HeterogeneousAdapter();

        mAdapter.addSection(new RuleHeaderSingleton(mOriginalPlaylist, mEditedPlaylist));
        mAdapter.addSection(new RuleSection(mEditedPlaylist.getRules()));
    }

    @Bindable
    public RecyclerView.Adapter getAdapter() {
        return mAdapter;
    }

    @Bindable
    public RecyclerView.LayoutManager getLayoutManager() {
        return new LinearLayoutManager(getContext());
    }

    @Bindable
    public RecyclerView.ItemDecoration[] getItemDecorations() {
        return new RecyclerView.ItemDecoration[] {
                new BackgroundDecoration(),
                new DividerDecoration(getContext())
        };
    }

    @Bindable
    public int getScrollPosition() {
        return mScrollPosition;
    }

    public void setScrollPosition(int scrollY) {
        if (scrollY != mScrollPosition) {
            mScrollPosition = scrollY;
            notifyPropertyChanged(BR.scrollPosition);
        }
    }

    public void addRule() {
        mEditedPlaylist.getRules().add(AutoPlaylistRule.emptyRule());
        mAdapter.notifyItemInserted(mEditedPlaylist.getRules().size());
    }

    public void focusPlaylistName() {
        setScrollPosition(0);
    }

}
