package com.marverenic.music.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.marverenic.music.R;
import com.marverenic.music.adapters.PlaylistListAdapter;
import com.marverenic.music.utils.Themes;

public class PlaylistFragment extends Fragment {
    private ListView playlistListView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list, container, false);
        playlistListView = (ListView) view.findViewById(R.id.list);

        PlaylistListAdapter adapter = new PlaylistListAdapter(getActivity());

        playlistListView.setAdapter(adapter);
        playlistListView.setOnItemClickListener(adapter);
        playlistListView.setOnItemLongClickListener(adapter);

        Themes.themeFragment(R.layout.fragment_list, view, this);

        return view;
    }
}