package com.marverenic.music.activity.instance;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.marverenic.adapter.HeterogeneousAdapter;
import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.activity.BaseActivity;
import com.marverenic.music.adapter.LibraryEmptyState;
import com.marverenic.music.adapter.SongSection;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.model.Genre;
import com.marverenic.music.model.Song;
import com.marverenic.music.view.BackgroundDecoration;
import com.marverenic.music.view.DividerDecoration;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;

public class GenreActivity extends BaseActivity {

    private static final String GENRE_EXTRA = "GenreActivity.GENRE";

    @Inject MusicStore mMusicStore;

    private Genre reference;
    private List<Song> mSongs;
    private HeterogeneousAdapter mAdapter;
    private SongSection mSongSection;

    public static Intent newIntent(Context context, Genre genre) {
        Intent intent = new Intent(context, GenreActivity.class);
        intent.putExtra(GENRE_EXTRA, genre);

        return intent;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instance);
        JockeyApplication.getComponent(this).inject(this);

        reference = getIntent().getParcelableExtra(GENRE_EXTRA);

        if (reference != null) {
            mMusicStore.getSongs(reference)
                    .compose(bindToLifecycle())
                    .subscribe(
                            songs -> {
                                mSongs = songs;
                                setupAdapter();
                            }, throwable -> {
                                Timber.e(throwable, "Failed to get song contents");
                            });

            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(reference.getGenreName());
            }
        } else {
            mSongs = new ArrayList<>();
        }

        mAdapter = new HeterogeneousAdapter();
        setupAdapter();
        mAdapter.setEmptyState(new LibraryEmptyState(this) {
            @Override
            public String getEmptyMessage() {
                if (reference == null) {
                    return getString(R.string.empty_error_genre);
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
