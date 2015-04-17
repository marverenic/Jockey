package com.marverenic.music.fragments;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.marverenic.music.R;
import com.marverenic.music.adapters.SearchPagerAdapter;
import com.marverenic.music.adapters.SongListAdapter;
import com.marverenic.music.instances.Song;
import com.marverenic.music.utils.Themes;

import java.util.ArrayList;

public class SongFragment extends Fragment {

    private ArrayList<Song> songLibrary;
    SongListAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null && getArguments().getParcelableArrayList(SearchPagerAdapter.DATA_KEY) != null){
            songLibrary = new ArrayList<>();
            for (Parcelable p : getArguments().getParcelableArrayList(SearchPagerAdapter.DATA_KEY)){
                songLibrary.add((Song) p);
            }
        }

        if (songLibrary == null) {
            adapter = new SongListAdapter(getActivity(), true);
        } else {
            adapter = new SongListAdapter(songLibrary, getActivity(), true);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list, container, false);
        ListView songListView = (ListView) view.findViewById(R.id.list);

        songListView.setAdapter(adapter);
        songListView.setOnItemClickListener(adapter);
        songListView.setBackgroundColor(Themes.getBackgroundElevated());

        return view;
    }

    public void updateData(ArrayList<Song> songLibrary) {
        this.songLibrary = songLibrary;
        adapter.updateData(songLibrary);
    }
}
