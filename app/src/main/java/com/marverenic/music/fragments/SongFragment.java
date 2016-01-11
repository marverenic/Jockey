package com.marverenic.music.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.marverenic.music.instances.Library;
import com.marverenic.music.R;
import com.marverenic.music.instances.viewholder.EmptyStateViewHolder;
import com.marverenic.music.instances.viewholder.SongViewHolder;
import com.marverenic.music.utils.Themes;
import com.marverenic.music.view.BackgroundDecoration;
import com.marverenic.music.view.DividerDecoration;

public class SongFragment extends Fragment {

    private Adapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.list, container, false);
        RecyclerView songRecyclerView = (RecyclerView) view.findViewById(R.id.list);
        songRecyclerView.addItemDecoration(new BackgroundDecoration(Themes.getBackgroundElevated()));
        songRecyclerView.addItemDecoration(new DividerDecoration(getActivity()));

        int paddingH =(int) getActivity().getResources().getDimension(R.dimen.global_padding);
        view.setPadding(paddingH, 0, paddingH, 0);

        adapter = new Adapter();
        songRecyclerView.setAdapter(adapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        songRecyclerView.setLayoutManager(layoutManager);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Library.addRefreshListener(adapter);
        // Assume this fragment's data has gone stale since it was last in the foreground
        adapter.onLibraryRefreshed();
    }

    @Override
    public void onPause() {
        super.onPause();
        Library.removeRefreshListener(adapter);
    }

    public class Adapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
            implements Library.LibraryRefreshListener {

        public static final int EMPTY = 0;
        public static final int SONG = 1;

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            switch (viewType) {
                case EMPTY:
                    return new EmptyStateViewHolder(
                            LayoutInflater
                                    .from(viewGroup.getContext())
                                    .inflate(R.layout.instance_empty, viewGroup, false),
                            getActivity());
                case SONG:
                default:
                    return new SongViewHolder(
                            LayoutInflater
                                    .from(viewGroup.getContext())
                                    .inflate(R.layout.instance_song, viewGroup, false),
                            Library.getSongs());
            }
        }

        @Override
        public int getItemViewType(int position){
            if (Library.getSongs().isEmpty()) return EMPTY;
            return SONG;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
            if (getItemViewType(position) == SONG) {
                ((SongViewHolder) viewHolder).update(Library.getSongs().get(position), position);
            }
            else if (viewHolder instanceof EmptyStateViewHolder &&
                    Library.hasRWPermission(getActivity())) {
                EmptyStateViewHolder emptyHolder = ((EmptyStateViewHolder) viewHolder);
                emptyHolder.setReason(R.string.empty);
                emptyHolder.setDetail(R.string.empty_detail);
                emptyHolder.setButton1(R.string.action_try_again);
            }
        }

        @Override
        public int getItemCount() {
            return (Library.getSongs().isEmpty())? 1 : Library.getSongs().size();
        }

        @Override
        public void onLibraryRefreshed() {
            notifyDataSetChanged();
        }
    }

}
