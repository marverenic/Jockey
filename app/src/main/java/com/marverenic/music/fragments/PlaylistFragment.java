package com.marverenic.music.fragments;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.marverenic.music.R;
import com.marverenic.music.adapters.PlaylistListAdapter;
import com.marverenic.music.adapters.SearchPagerAdapter;
import com.marverenic.music.instances.Playlist;
import com.marverenic.music.utils.Themes;

import java.util.ArrayList;

public class PlaylistFragment extends Fragment {

    private ArrayList<Playlist> playlistLibrary;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null && getArguments().getParcelableArrayList(SearchPagerAdapter.DATA_KEY) != null){
            playlistLibrary = new ArrayList<>();
            for (Parcelable p : getArguments().getParcelableArrayList(SearchPagerAdapter.DATA_KEY)){
                playlistLibrary.add((Playlist) p);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list, container, false);
        ListView playlistListView = (ListView) view.findViewById(R.id.list);

        // Most people probably don't have enough playlists to warrant fast scrolling...
        playlistListView.setFastScrollEnabled(false);

        PlaylistListAdapter adapter;
        if (playlistLibrary == null) {
            adapter = new PlaylistListAdapter(getActivity());
        } else{
            adapter = new PlaylistListAdapter(playlistLibrary, getActivity());
        }

        playlistListView.setAdapter(adapter);
        playlistListView.setOnItemClickListener(adapter);
        playlistListView.setOnItemLongClickListener(adapter);

        Themes.themeFragment(R.layout.fragment_list, view, this);

        return view;
    }
}