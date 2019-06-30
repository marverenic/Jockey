package com.marverenic.music.ui.library.artist;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.marverenic.adapter.EnhancedViewHolder;
import com.marverenic.adapter.HeterogeneousAdapter;
import com.marverenic.music.R;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.data.store.PlaylistStore;
import com.marverenic.music.databinding.InstanceArtistBinding;
import com.marverenic.music.model.Artist;
import com.marverenic.music.model.ModelUtil;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.view.MeasurableSection;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView.SectionedAdapter;

import java.util.List;

public class ArtistSection extends HeterogeneousAdapter.ListSection<Artist>
        implements SectionedAdapter, MeasurableSection {

    private Context mContext;
    private FragmentManager mFragmentManager;
    private MusicStore mMusicStore;
    private PlaylistStore mPlaylistStore;
    private PlayerController mPlayerController;

    public ArtistSection(@NonNull List<Artist> data, Context context, FragmentManager fragmentManager,
                         MusicStore musicStore, PlaylistStore playlistStore,
                         PlayerController playerController) {
        super(data);
        mContext = context;
        mFragmentManager = fragmentManager;
        mMusicStore = musicStore;
        mPlaylistStore = playlistStore;
        mPlayerController = playerController;
    }

    @Override
    public int getId(int position) {
        return get(position).getArtistId();
    }

    @Override
    public EnhancedViewHolder<Artist> createViewHolder(HeterogeneousAdapter adapter,
                                                       ViewGroup parent) {
        InstanceArtistBinding binding = InstanceArtistBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);

        return new ViewHolder(binding);
    }

    @NonNull
    @Override
    public String getSectionName(int position) {
        String title = ModelUtil.sortableTitle(get(position).getArtistName(), mContext.getResources());
        return Character.toString(title.charAt(0)).toUpperCase();
    }

    @Override
    public int getViewTypeHeight(RecyclerView recyclerView) {
        return recyclerView.getResources().getDimensionPixelSize(R.dimen.list_height)
                + recyclerView.getResources().getDimensionPixelSize(R.dimen.divider_height);
    }

    public class ViewHolder extends EnhancedViewHolder<Artist> {

        private InstanceArtistBinding mBinding;

        public ViewHolder(InstanceArtistBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
            mBinding.setViewModel(new ArtistItemViewModel(itemView.getContext(), mFragmentManager,
                    mMusicStore, mPlaylistStore, mPlayerController));
        }

        @Override
        public void onUpdate(Artist item, int sectionPosition) {
            mBinding.getViewModel().setArtist(item);
            mBinding.executePendingBindings();
        }
    }
}
