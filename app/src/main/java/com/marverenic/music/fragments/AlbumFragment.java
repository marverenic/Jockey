package com.marverenic.music.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import com.marverenic.music.R;
import com.marverenic.music.adapters.AlbumGridAdapter;
import com.marverenic.music.instances.Album;
import com.marverenic.music.instances.Library;
import com.marverenic.music.utils.Themes;

import java.util.ArrayList;

public class AlbumFragment extends Fragment {

    public ArrayList<Album> albumLibrary;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // inflate the root view of the fragment
        View view = inflater.inflate(R.layout.fragment_grid, container, false);


        // initialize the adapter
        AlbumGridAdapter adapter;
        if (albumLibrary == null) {
            adapter = new AlbumGridAdapter(Library.getAlbums(), getActivity());
        } else {
            adapter = new AlbumGridAdapter(albumLibrary, getActivity());
        }

        // initialize the GridView
        GridView gridView = (GridView) view.findViewById(R.id.albumGrid);
        gridView.setAdapter(adapter);
        //gridView.setOnItemClickListener(this);

        Themes.themeFragment(R.layout.fragment_grid, view, this);

        return view;
    }

	/*public void libraryRefresh () {
        AlbumGridAdapter adapter = (AlbumGridAdapter) ((GridView) getView().findViewById(R.id.albumGrid)).getAdapter();
		adapter.refresh(Library.getAlbums());
	}*/

    /*@Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Album item = ((Album) (((GridView) getView().findViewById(R.id.albumGrid)).getAdapter()).getItem(position));

        Intent intent = new Intent(getActivity(), LibraryPageActivity.class);
        intent.putExtra("entry", item);

        startActivity(intent);
    }*/
}
