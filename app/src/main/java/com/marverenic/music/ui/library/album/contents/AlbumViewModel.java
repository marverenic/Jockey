package com.marverenic.music.ui.library.album.contents;

import android.content.ContentUris;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.DisplayMetrics;

import androidx.annotation.Nullable;
import androidx.databinding.Bindable;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.GenericRequestBuilder;
import com.bumptech.glide.Glide;
import com.marverenic.adapter.HeterogeneousAdapter;
import com.marverenic.music.R;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.data.store.PlaylistStore;
import com.marverenic.music.data.store.PreferenceStore;
import com.marverenic.music.model.Album;
import com.marverenic.music.model.Song;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.ui.BaseViewModel;
import com.marverenic.music.ui.common.BasicEmptyState;
import com.marverenic.music.ui.common.OnSongSelectedListener;
import com.marverenic.music.ui.common.ShuffleAllSection;
import com.marverenic.music.ui.library.song.SongSection;
import com.marverenic.music.utils.MediaStoreThumbnailLoader;
import com.marverenic.music.view.BackgroundDecoration;
import com.marverenic.music.view.DividerDecoration;

import java.util.Collections;
import java.util.List;

public class AlbumViewModel extends BaseViewModel {

    private Album mAlbum;

    private HeterogeneousAdapter mAdapter;
    private SongSection mSongSection;
    private ShuffleAllSection mShuffleAllSection;

    public AlbumViewModel(Context context, Album album, PlayerController playerController,
                          MusicStore musicStore, PlaylistStore playlistStore,
                          PreferenceStore preferenceStore, FragmentManager fragmentManager,
                          @Nullable OnSongSelectedListener songSelectedListener) {
        super(context);
        mAlbum = album;

        mSongSection = new SongSection(Collections.emptyList(), getContext(),
                playerController, musicStore, playlistStore, fragmentManager, songSelectedListener);
        mShuffleAllSection = new ShuffleAllSection(Collections.emptyList(), preferenceStore,
                playerController, songSelectedListener);

        mAdapter = new HeterogeneousAdapter();
        mAdapter.addSection(mShuffleAllSection);
        mAdapter.addSection(mSongSection);

        mAdapter.setEmptyState(new BasicEmptyState() {
            @Override
            public String getMessage() {
                return getString(R.string.empty);
            }
        });
    }

    public void setAlbumSongs(List<Song> songs) {
        mSongSection.setData(songs);
        mShuffleAllSection.setData(songs);
    }

    public void setCurrentSong(Song nowPlaying) {
        mSongSection.setCurrentSong(nowPlaying);
    }

    @Bindable
    public int getHeroImageHeight() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;

        int maxHeight = screenHeight / 2;

        // prefer a 1:1 aspect ratio
        return Math.min(screenWidth, maxHeight);
    }

    @Bindable
    public GenericRequestBuilder getHeroImage() {
        if (Build.VERSION.SDK_INT >= 29) {
            Uri thumbnailUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                    mAlbum.getAlbumId()
            );

            return Glide.with(getContext())
                    .using(new MediaStoreThumbnailLoader(getContext()))
                    .load(thumbnailUri)
                    .decoder(new MediaStoreThumbnailLoader.Decoder(getContext()))
                    .centerCrop();
        } else {
            return Glide.with(getContext()).load(mAlbum.getArtUri()).centerCrop();
        }
    }

    @Bindable
    public RecyclerView.Adapter getAdapter() {
        return mAdapter;
    }

    @Bindable
    public RecyclerView.ItemDecoration[] getItemDecorations() {
        return new RecyclerView.ItemDecoration[] {
                new BackgroundDecoration(),
                new DividerDecoration(getContext(), R.id.empty_layout)
        };
    }

}
