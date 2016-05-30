package com.marverenic.music.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.instances.Library;
import com.marverenic.music.instances.Song;
import com.marverenic.music.instances.section.LibraryEmptyState;
import com.marverenic.music.instances.section.SongSection;
import com.marverenic.music.utils.Themes;
import com.marverenic.music.view.BackgroundDecoration;
import com.marverenic.music.view.DividerDecoration;
import com.marverenic.music.view.EnhancedAdapters.HeterogeneousAdapter;

import java.util.List;

import javax.inject.Inject;

public class SongFragment extends Fragment implements Library.LibraryRefreshListener {

    @Inject MusicStore mMusicStore;

    private HeterogeneousAdapter adapter;
    private List<Song> mSongs;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.list, container, false);

        int paddingH = (int) getActivity().getResources().getDimension(R.dimen.global_padding);
        view.setPadding(paddingH, 0, paddingH, 0);

        JockeyApplication.getComponent(this).inject(this);
        mMusicStore.getSongs().subscribe(
                songs -> {
                    mSongs = songs;
                    setupAdapter();
                },
                Throwable::printStackTrace);

        return view;
    }

    private void setupAdapter() {
        adapter = new HeterogeneousAdapter();
        adapter.addSection(new SongSection(mSongs));
        adapter.setEmptyState(new LibraryEmptyState(getActivity()));

        // noinspection ConstantConditions
        RecyclerView list = (RecyclerView) getView().findViewById(R.id.list);
        list.addItemDecoration(new BackgroundDecoration(Themes.getBackgroundElevated()));
        list.addItemDecoration(new DividerDecoration(getActivity(), R.id.empty_layout));
        list.setAdapter(adapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        list.setLayoutManager(layoutManager);
    }

    @Override
    public void onResume() {
        super.onResume();
        Library.addRefreshListener(this);
        // Assume this fragment's data has gone stale since it was last in the foreground
        onLibraryRefreshed();
    }

    @Override
    public void onPause() {
        super.onPause();
        Library.removeRefreshListener(this);
    }

    @Override
    public void onLibraryRefreshed() {
        adapter.notifyDataSetChanged();
    }
}
