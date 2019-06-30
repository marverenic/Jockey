package com.marverenic.music.ui.library.song;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.marverenic.adapter.EnhancedViewHolder;
import com.marverenic.adapter.HeterogeneousAdapter;
import com.marverenic.music.R;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.data.store.PlaylistStore;
import com.marverenic.music.databinding.InstanceSongBinding;
import com.marverenic.music.model.ModelUtil;
import com.marverenic.music.model.Song;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.ui.common.OnSongSelectedListener;
import com.marverenic.music.view.MeasurableSection;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView.SectionedAdapter;

import java.util.List;

import rx.subjects.BehaviorSubject;
import timber.log.Timber;

public class SongSection extends HeterogeneousAdapter.ListSection<Song>
        implements SectionedAdapter, MeasurableSection {

    private Context mContext;
    private PlayerController mPlayerController;
    private MusicStore mMusicStore;
    private PlaylistStore mPlaylistStore;
    private FragmentManager mFragmentManager;
    @Nullable private OnSongSelectedListener mSongListener;

    private BehaviorSubject<Song> mCurrentSong;

    public SongSection(@NonNull List<Song> data, Context context, PlayerController playerController,
                       MusicStore musicStore, PlaylistStore playlistStore, FragmentManager fragmentManager,
                       @Nullable OnSongSelectedListener songSelectedListener) {
        super(data);
        mContext = context;
        mMusicStore = musicStore;
        mPlaylistStore = playlistStore;
        mPlayerController = playerController;
        mFragmentManager = fragmentManager;
        mSongListener = songSelectedListener;

        mCurrentSong = BehaviorSubject.create();
    }

    public void setCurrentSong(Song nowPlaying) {
        mCurrentSong.onNext(nowPlaying);
    }

    @Override
    public int getId(int position) {
        return (int) get(position).getSongId();
    }

    @Override
    public EnhancedViewHolder<Song> createViewHolder(HeterogeneousAdapter adapter,
                                                     ViewGroup parent) {
        InstanceSongBinding binding = InstanceSongBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);

        return new ViewHolder(binding);
    }

    @NonNull
    @Override
    public String getSectionName(int position) {
        String title = ModelUtil.sortableTitle(get(position).getSongName(), mContext.getResources());
        return Character.toString(title.charAt(0)).toUpperCase();
    }

    @Override
    public int getViewTypeHeight(RecyclerView recyclerView) {
        return recyclerView.getResources().getDimensionPixelSize(R.dimen.list_height)
                + recyclerView.getResources().getDimensionPixelSize(R.dimen.divider_height);
    }

    private class ViewHolder extends EnhancedViewHolder<Song> {

        private InstanceSongBinding mBinding;

        public ViewHolder(InstanceSongBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
            SongItemViewModel viewModel = new SongItemViewModel(binding.getRoot().getContext(),
                    mFragmentManager, mMusicStore, mPlaylistStore, mPlayerController, mSongListener);
            binding.setViewModel(viewModel);

            mCurrentSong.subscribe(viewModel::setCurrentlyPlayingSong, throwable -> {
                Timber.e("Failed to set current song", throwable);
            });
        }

        @Override
        public void onUpdate(Song s, int sectionPosition) {
            mBinding.getViewModel().setSong(getData(), sectionPosition);
            mBinding.executePendingBindings();
        }
    }
}
