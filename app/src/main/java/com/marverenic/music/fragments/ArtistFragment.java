package com.marverenic.music.fragments;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.marverenic.music.R;
import com.marverenic.music.adapters.ArtistListAdapter;
import com.marverenic.music.adapters.SearchPagerAdapter;
import com.marverenic.music.instances.Artist;
import com.marverenic.music.utils.Themes;

import java.util.ArrayList;

public class ArtistFragment extends Fragment {

    private ArrayList<Artist> artistLibrary;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null && getArguments().getParcelableArrayList(SearchPagerAdapter.DATA_KEY) != null){
            artistLibrary = new ArrayList<>();
            for (Parcelable p : getArguments().getParcelableArrayList(SearchPagerAdapter.DATA_KEY)){
                artistLibrary.add((Artist) p);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list, container, false);
        ListView artistListView = (ListView) view.findViewById(R.id.list);

        ArtistListAdapter adapter;
        if (artistLibrary == null) {
            adapter = new ArtistListAdapter(getActivity());
        } else {
            adapter = new ArtistListAdapter(artistLibrary, getActivity());
        }

        artistListView.setAdapter(adapter);
        artistListView.setOnItemClickListener(adapter);
        artistListView.setOnItemLongClickListener(adapter);

        Themes.themeFragment(R.layout.fragment_list, view, this);

        return view;
    }
}
