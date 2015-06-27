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
import com.marverenic.music.instances.Playlist;
import com.marverenic.music.instances.viewholder.BlankViewHolder;
import com.marverenic.music.instances.viewholder.PlaylistViewHolder;
import com.marverenic.music.utils.Themes;
import com.marverenic.music.view.BackgroundDecoration;
import com.marverenic.music.view.DividerDecoration;

import java.util.ArrayList;

public class PlaylistFragment extends Fragment{

    private Adapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.list, container, false);
        RecyclerView playlistRecyclerView = (RecyclerView) view.findViewById(R.id.list);
        playlistRecyclerView.addItemDecoration(new BackgroundDecoration(Themes.getBackgroundElevated()));
        playlistRecyclerView.addItemDecoration(new DividerDecoration(getActivity(), new int[]{R.id.instance_blank}));

        int paddingH =(int) getActivity().getResources().getDimension(R.dimen.global_padding);
        view.setPadding(paddingH, 0, paddingH, 0);

        adapter = new Adapter();
        playlistRecyclerView.setAdapter(adapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        playlistRecyclerView.setLayoutManager(layoutManager);

        return view;
    }

    @Override
    public void onResume(){
        super.onResume();
        Library.addPlaylistListener(adapter);
    }

    @Override
    public void onPause(){
        super.onPause();
        Library.removePlaylistListener(adapter);
    }

    public class Adapter extends RecyclerView.Adapter implements Library.PlaylistChangeListener {

        public static final int PLAYLIST_VIEW = 0;
        public static final int EMPTY_VIEW = 1;

        /**
         * A clone of {@link Library#playlistLib} used to determine the index of removed playlists
         */
        private ArrayList<Playlist> data;

        public Adapter (){
            data = new ArrayList<>(Library.getPlaylists());
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            if (viewType == PLAYLIST_VIEW) {
                View itemView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.instance_playlist, viewGroup, false);
                return new PlaylistViewHolder(itemView);
            }
            else{
                return new BlankViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.instance_blank, viewGroup, false));
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i) {
            if (i < data.size()) {
                ((PlaylistViewHolder) viewHolder).update(data.get(i));
            }
        }

        @Override
        public int getItemCount() {
            return data.size() + 1;
        }

        @Override
        public int getItemViewType(int position){
            if (position < data.size()) return PLAYLIST_VIEW;
            else return EMPTY_VIEW;
        }

        @Override
        public void onPlaylistRemoved(Playlist removed) {
            if (data.contains(removed))
                notifyItemRemoved(data.indexOf(removed));

            data = new ArrayList<>(Library.getPlaylists());
        }

        @Override
        public void onPlaylistAdded(Playlist added) {
            data = new ArrayList<>(Library.getPlaylists());
            if (data.contains(added))
                notifyItemInserted(data.indexOf(added));
        }
    }

}
