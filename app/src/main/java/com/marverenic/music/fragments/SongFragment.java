package com.marverenic.music.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.marverenic.music.R;
import com.marverenic.music.Library;
import com.marverenic.music.instances.viewholder.SongViewHolder;
import com.marverenic.music.utils.Themes;
import com.marverenic.music.view.BackgroundDecoration;
import com.marverenic.music.view.DividerDecoration;

public class SongFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.list, container, false);
        RecyclerView songRecyclerView = (RecyclerView) view.findViewById(R.id.list);
        songRecyclerView.addItemDecoration(new BackgroundDecoration(Themes.getBackgroundElevated()));
        songRecyclerView.addItemDecoration(new DividerDecoration(getActivity()));

        int paddingH =(int) getActivity().getResources().getDimension(R.dimen.global_padding);
        view.setPadding(paddingH, 0, paddingH, 0);

        songRecyclerView.setAdapter(new Adapter());

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        songRecyclerView.setLayoutManager(layoutManager);

        return view;
    }


    public class Adapter extends RecyclerView.Adapter<SongViewHolder>{

        @Override
        public SongViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            SongViewHolder viewHolder = new SongViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.instance_song, viewGroup, false));
            viewHolder.setSongList(Library.getSongs());
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(SongViewHolder viewHolder, int i) {
            viewHolder.update(Library.getSongs().get(i));
        }

        @Override
        public int getItemCount() {
            return Library.getSongs().size();
        }
    }

}
