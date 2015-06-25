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
import com.marverenic.music.instances.viewholder.GenreViewHolder;
import com.marverenic.music.utils.Themes;
import com.marverenic.music.view.BackgroundDecoration;
import com.marverenic.music.view.DividerDecoration;

public class GenreFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.list, container, false);
        RecyclerView genreRecyclerView = (RecyclerView) view.findViewById(R.id.list);
        genreRecyclerView.addItemDecoration(new BackgroundDecoration(Themes.getBackgroundElevated()));
        genreRecyclerView.addItemDecoration(new DividerDecoration(getActivity()));

        int paddingH =(int) getActivity().getResources().getDimension(R.dimen.global_padding);
        view.setPadding(paddingH, 0, paddingH, 0);

        genreRecyclerView.setAdapter(new Adapter());

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        genreRecyclerView.setLayoutManager(layoutManager);

        return view;
    }


    public class Adapter extends RecyclerView.Adapter<GenreViewHolder>{

        @Override
        public GenreViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View itemView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.instance_genre, viewGroup, false);
            return new GenreViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(GenreViewHolder viewHolder, int i) {
            viewHolder.update(Library.getGenres().get(i));
        }

        @Override
        public int getItemCount() {
            return Library.getGenres().size();
        }
    }

}
