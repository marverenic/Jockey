package com.marverenic.music.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.marverenic.music.R;
import com.marverenic.music.adapters.ArtistListAdapter;
import com.marverenic.music.utils.Themes;

public class ArtistFragment extends Fragment {


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list, container, false);
        ListView artistListView = (ListView) view.findViewById(R.id.list);

        ArtistListAdapter adapter = new ArtistListAdapter(getActivity());

        artistListView.setAdapter(adapter);
        artistListView.setOnItemClickListener(adapter);
        artistListView.setOnItemLongClickListener(adapter);

        Themes.themeFragment(R.layout.fragment_list, view, this);

        return view;
    }
}
