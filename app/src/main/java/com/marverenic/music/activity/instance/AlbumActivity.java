package com.marverenic.music.activity.instance;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.marverenic.adapter.HeterogeneousAdapter;
import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.activity.BaseLibraryActivity;
import com.marverenic.music.adapter.LibraryEmptyState;
import com.marverenic.music.adapter.ShuffleAllSection;
import com.marverenic.music.adapter.SongSection;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.model.Album;
import com.marverenic.music.model.Song;
import com.marverenic.music.view.BackgroundDecoration;
import com.marverenic.music.view.DividerDecoration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;

public class AlbumActivity extends BaseLibraryActivity {

    private static final String ALBUM_EXTRA = "AlbumActivity.ALBUM";

    @Inject MusicStore mMusicStore;

    private HeterogeneousAdapter mAdapter;
    private ShuffleAllSection mShuffleAllSection;
    private SongSection mSongSection;
    private List<Song> mSongs;

    public static Intent newIntent(Context context, Album album) {
        Intent intent = new Intent(context, AlbumActivity.class);
        intent.putExtra(ALBUM_EXTRA, album);

        return intent;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        JockeyApplication.getComponent(this).inject(this);

        Album reference = getIntent().getParcelableExtra(ALBUM_EXTRA);

        if (reference != null) {
            mMusicStore.getSongs(reference)
                    .compose(bindToLifecycle())
                    .subscribe(
                            songs -> {
                                mSongs = new ArrayList<>(songs);
                                Collections.sort(mSongs, Song.TRACK_COMPARATOR);
                                setupAdapter();
                            }, throwable -> {
                                Timber.e(throwable, "Failed to get song contents");
                            });

            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(reference.getAlbumName());
            }

            Glide.with(this).load(reference.getArtUri())
                    .centerCrop()
                    .into((ImageView) findViewById(R.id.activity_backdrop));
        } else {
            mSongs = Collections.emptyList();
        }

        ImageView artistImage = (ImageView) findViewById(R.id.activity_backdrop);
        artistImage.getLayoutParams().height = calculateHeroHeight();

        mAdapter = new HeterogeneousAdapter();
        setupAdapter();
        mAdapter.setEmptyState(new LibraryEmptyState(this) {
            @Override
            public String getEmptyMessage() {
                if (reference == null) {
                    return getString(R.string.empty_error_album);
                } else {
                    return super.getEmptyMessage();
                }
            }

            @Override
            public String getEmptyMessageDetail() {
                if (reference == null) {
                    return "";
                } else {
                    return super.getEmptyMessageDetail();
                }
            }

            @Override
            public String getEmptyAction1Label() {
                return "";
            }
        });

        RecyclerView list = (RecyclerView) findViewById(R.id.list);
        list.setAdapter(mAdapter);
        list.addItemDecoration(new BackgroundDecoration());
        list.addItemDecoration(new DividerDecoration(this, R.id.empty_layout));

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        list.setLayoutManager(layoutManager);
    }

    @Override
    protected int getContentLayoutResource() {
        return R.layout.activity_instance_artwork;
    }

    @Override
    public boolean isToolbarCollapsing() {
        return true;
    }

    private int calculateHeroHeight() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;

        int maxHeight = screenHeight / 2;

        // prefer a 1:1 aspect ratio
        return Math.min(screenWidth, maxHeight);
    }

    private void setupAdapter() {
        if (mAdapter == null || mSongs == null) {
            return;
        }

        if (mSongSection != null && mShuffleAllSection != null) {
            mSongSection.setData(mSongs);
            mShuffleAllSection.setData(mSongs);
            mAdapter.notifyDataSetChanged();
        } else {
            mSongSection = new SongSection(this, mSongs);
            mShuffleAllSection = new ShuffleAllSection(this, mSongs);
            mAdapter.addSection(mShuffleAllSection);
            mAdapter.addSection(mSongSection);
        }
    }
}
