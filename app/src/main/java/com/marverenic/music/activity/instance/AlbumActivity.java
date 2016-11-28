package com.marverenic.music.activity.instance;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.marverenic.heterogeneousadapter.HeterogeneousAdapter;
import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.activity.BaseActivity;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.instances.Album;
import com.marverenic.music.instances.Song;
import com.marverenic.music.adapter.LibraryEmptyState;
import com.marverenic.music.adapter.SongSection;
import com.marverenic.music.view.BackgroundDecoration;
import com.marverenic.music.view.DividerDecoration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;

public class AlbumActivity extends BaseActivity {

    private static final String ALBUM_EXTRA = "AlbumActivity.ALBUM";

    @Inject MusicStore mMusicStore;

    private HeterogeneousAdapter mAdapter;
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
        setContentView(R.layout.activity_instance_artwork);
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
                    .into((ImageView) findViewById(R.id.backdrop));
        } else {
            mSongs = Collections.emptyList();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }

        ImageView artistImage = (ImageView) findViewById(R.id.backdrop);
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

        if (mSongSection != null) {
            mSongSection.setData(mSongs);
            mAdapter.notifyDataSetChanged();
        } else {
            mSongSection = new SongSection(this, mSongs);
            mAdapter.addSection(mSongSection);
        }
    }
}
