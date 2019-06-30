package com.marverenic.music.ui.library.playlist.contents.edit;

import android.content.Context;
import androidx.databinding.Bindable;
import androidx.recyclerview.widget.RecyclerView;

import com.marverenic.adapter.HeterogeneousAdapter;
import com.marverenic.music.BR;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.data.store.PlaylistStore;
import com.marverenic.music.model.AutoPlaylist;
import com.marverenic.music.model.playlistrules.AutoPlaylistRule;
import com.marverenic.music.ui.BaseViewModel;
import com.marverenic.music.view.BackgroundDecoration;
import com.marverenic.music.view.DividerDecoration;

public class AutoPlaylistEditViewModel extends BaseViewModel {

    private PlaylistStore mPlaylistStore;
    private MusicStore mMusicStore;

    private AutoPlaylist mOriginalPlaylist;
    private AutoPlaylist.Builder mEditedPlaylist;
    private int mScrollPosition;

    private HeterogeneousAdapter mAdapter;

    public AutoPlaylistEditViewModel(Context context, AutoPlaylist originalPlaylist,
                                     AutoPlaylist.Builder editedPlaylist,
                                     PlaylistStore playlistStore, MusicStore musicStore) {
        super(context);
        mOriginalPlaylist = originalPlaylist;
        mEditedPlaylist = editedPlaylist;
        mPlaylistStore = playlistStore;
        mMusicStore = musicStore;

        createAdapter();
    }

    private void createAdapter() {
        mAdapter = new HeterogeneousAdapter();

        mAdapter.addSection(new RuleHeaderSingleton(mOriginalPlaylist, mEditedPlaylist, mPlaylistStore));
        mAdapter.addSection(new RuleSection(mEditedPlaylist.getRules(), mMusicStore, mPlaylistStore));
    }

    @Bindable
    public RecyclerView.Adapter getAdapter() {
        return mAdapter;
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
