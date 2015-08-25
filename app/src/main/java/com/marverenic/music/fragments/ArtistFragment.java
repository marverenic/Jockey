package com.marverenic.music.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.marverenic.music.Library;
import com.marverenic.music.R;
import com.marverenic.music.instances.viewholder.ArtistViewHolder;
import com.marverenic.music.instances.viewholder.EmptyStateViewHolder;
import com.marverenic.music.utils.Themes;
import com.marverenic.music.view.BackgroundDecoration;
import com.marverenic.music.view.DividerDecoration;

public class ArtistFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.list, container, false);
        RecyclerView artistRecyclerView = (RecyclerView) view.findViewById(R.id.list);
        artistRecyclerView.addItemDecoration(new BackgroundDecoration(Themes.getBackgroundElevated()));
        artistRecyclerView.addItemDecoration(new DividerDecoration(getActivity()));

        int paddingH =(int) getActivity().getResources().getDimension(R.dimen.global_padding);
        view.setPadding(paddingH, 0, paddingH, 0);

        artistRecyclerView.setAdapter(new Adapter());

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        artistRecyclerView.setLayoutManager(layoutManager);

        return view;
    }

    public class Adapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        public static final int EMPTY = 0;
        public static final int ARTIST = 1;

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            switch (viewType) {
                case EMPTY:
                    return new EmptyStateViewHolder(
                            LayoutInflater
                                    .from(viewGroup.getContext())
                                    .inflate(R.layout.instance_empty, viewGroup, false),
                            getActivity());
                case ARTIST:
                default:
                    return new ArtistViewHolder(
                            LayoutInflater
                                    .from(viewGroup.getContext())
                                    .inflate(R.layout.instance_artist, viewGroup, false));
            }
        }

        @Override
        public int getItemViewType(int position){
            if (Library.getArtists().isEmpty()) return EMPTY;
            return ARTIST;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
            if (getItemViewType(position) == ARTIST) {
                ((ArtistViewHolder) viewHolder).update(Library.getArtists().get(position));
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
            return (Library.getArtists().isEmpty())? 1 : Library.getArtists().size();
        }
    }

}
