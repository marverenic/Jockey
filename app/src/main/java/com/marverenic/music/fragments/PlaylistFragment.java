package com.marverenic.music.fragments;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.marverenic.music.Library;
import com.marverenic.music.R;
import com.marverenic.music.instances.Playlist;
import com.marverenic.music.instances.Song;
import com.marverenic.music.instances.viewholder.BlankViewHolder;
import com.marverenic.music.instances.viewholder.PlaylistViewHolder;
import com.marverenic.music.utils.Themes;
import com.marverenic.music.view.BackgroundDecoration;
import com.marverenic.music.view.DividerDecoration;

import java.util.ArrayList;

public class PlaylistFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.list, container, false);
        RecyclerView playlistRecyclerView = (RecyclerView) view.findViewById(R.id.list);
        playlistRecyclerView.addItemDecoration(new BackgroundDecoration(Themes.getBackgroundElevated()));
        playlistRecyclerView.addItemDecoration(new DividerDecoration(getActivity(), new int[]{R.id.instance_blank}));

        int paddingH =(int) getActivity().getResources().getDimension(R.dimen.global_padding);
        view.setPadding(paddingH, 0, paddingH, 0);

        playlistRecyclerView.setAdapter(new Adapter());

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        playlistRecyclerView.setLayoutManager(layoutManager);

        return view;
    }


    public class Adapter extends RecyclerView.Adapter implements PlaylistViewHolder.OnDeleteCallback {

        public static final int PLAYLIST_VIEW = 0;
        public static final int EMPTY_VIEW = 1;

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            if (viewType == PLAYLIST_VIEW) {
                View itemView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.instance_playlist, viewGroup, false);
                PlaylistViewHolder viewHolder = new PlaylistViewHolder(itemView);
                viewHolder.setRemoveCallback(this);
                return viewHolder;
            }
            else{
                return new BlankViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.instance_blank, viewGroup, false));
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i) {
            if (i < Library.getPlaylists().size()) {
                ((PlaylistViewHolder) viewHolder).update(Library.getPlaylists().get(i));
            }
        }

        @Override
        public int getItemCount() {
            return Library.getPlaylists().size() + 1;
        }

        @Override
        public int getItemViewType(int position){
            if (position < Library.getPlaylists().size()) return PLAYLIST_VIEW;
            else return EMPTY_VIEW;
        }

        @Override
        public void onPlaylistDelete(RecyclerView.ViewHolder viewHolder, final Playlist removed) {
            final ArrayList<Song> contents = Library.getPlaylistEntries(getActivity(), removed);
            Snackbar snackbar = Snackbar.make(
                    getActivity().findViewById(R.id.coordinator_layout),
                    String.format(getResources().getString(R.string.message_removed_playlist), removed),
                    Snackbar.LENGTH_LONG);

            snackbar.setAction("Undo", new View.OnClickListener() { // TODO String Resource
                @Override
                public void onClick(View v) {
                    Playlist recreated = Library.createPlaylist(getActivity(), removed.playlistName, contents);
                    notifyItemInserted(Library.getPlaylists().indexOf(recreated));
                }
            });

            Library.removePlaylist(getActivity(), removed);
            notifyItemRemoved(viewHolder.getAdapterPosition());
            snackbar.show();
        }
    }

}
