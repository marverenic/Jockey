package com.marverenic.music.fragments;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import com.marverenic.music.R;
import com.marverenic.music.adapters.AlbumGridAdapter;
import com.marverenic.music.adapters.SearchPagerAdapter;
import com.marverenic.music.instances.Album;
import com.marverenic.music.utils.Themes;

import java.util.ArrayList;

public class AlbumFragment extends Fragment {

    private ArrayList<Album> albumLibrary;
    private AlbumGridAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null && getArguments().getParcelableArrayList(SearchPagerAdapter.DATA_KEY) != null){
            albumLibrary = new ArrayList<>();
            for (Parcelable p : getArguments().getParcelableArrayList(SearchPagerAdapter.DATA_KEY)){
                albumLibrary.add((Album) p);
            }
        }

        // initialize the adapter
        if (albumLibrary == null) {
            adapter = new AlbumGridAdapter(getActivity());
        } else {
            adapter = new AlbumGridAdapter(albumLibrary, getActivity());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // inflate the root view of the fragment
        View view = inflater.inflate(R.layout.fragment_grid, container, false);

        int paddingTop = (int) getActivity().getResources().getDimension(R.dimen.list_margin);
        int paddingH =(int) getActivity().getResources().getDimension(R.dimen.global_padding);
        view.setPadding(paddingH, paddingTop, paddingH, 0);

        // initialize the GridView
        GridView gridView = (GridView) view.findViewById(R.id.albumGrid);
        gridView.setAdapter(adapter);
        gridView.setBackgroundColor(Themes.getBackgroundElevated());

        return view;
    }

    public void updateData(ArrayList<Album> albumLibrary) {
        this.albumLibrary = albumLibrary;
        adapter.updateData(albumLibrary);
    }
}
