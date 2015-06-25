package com.marverenic.music.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.marverenic.music.Library;
import com.marverenic.music.R;
import com.marverenic.music.instances.viewholder.AlbumViewHolder;
import com.marverenic.music.utils.Themes;
import com.marverenic.music.view.BackgroundDecoration;
import com.marverenic.music.view.GridSpacingDecoration;
import com.marverenic.music.view.ViewUtils;

public class AlbumFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.list, container, false);
        RecyclerView albumRecyclerView = (RecyclerView) view.findViewById(R.id.list);
        albumRecyclerView.addItemDecoration(new BackgroundDecoration(Themes.getBackgroundElevated()));

        int paddingH =(int) getActivity().getResources().getDimension(R.dimen.global_padding);
        view.setPadding(paddingH, 0, paddingH, 0);

        albumRecyclerView.setAdapter(new Adapter());

        int numColumns = ViewUtils.getNumberOfGridColumns(getActivity());

        GridLayoutManager layoutManager = new GridLayoutManager(getActivity(), numColumns);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        albumRecyclerView.setLayoutManager(layoutManager);

        albumRecyclerView.addItemDecoration(new GridSpacingDecoration((int) getResources().getDimension(R.dimen.grid_margin), numColumns));

        return view;
    }


    public class Adapter extends RecyclerView.Adapter<AlbumViewHolder>{

        @Override
        public AlbumViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            return new AlbumViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.instance_album, viewGroup, false));
        }

        @Override
        public void onBindViewHolder(AlbumViewHolder viewHolder, int i) {
            viewHolder.update(Library.getAlbums().get(i));
        }

        @Override
        public int getItemCount() {
            return Library.getAlbums().size();
        }
    }

}
