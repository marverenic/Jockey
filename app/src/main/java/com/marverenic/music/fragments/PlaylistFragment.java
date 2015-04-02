package com.marverenic.music.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.marverenic.music.R;
import com.marverenic.music.adapters.PlaylistListAdapter;
import com.marverenic.music.adapters.SearchPagerAdapter;
import com.marverenic.music.instances.Library;
import com.marverenic.music.instances.LibraryScanner;
import com.marverenic.music.instances.Playlist;
import com.marverenic.music.utils.Themes;
import com.marverenic.music.view.FloatingActionButton;

import java.util.ArrayList;

public class PlaylistFragment extends Fragment {

    private ArrayList<Playlist> playlistLibrary;
    private PlaylistListAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null && getArguments().getParcelableArrayList(SearchPagerAdapter.DATA_KEY) != null){
            playlistLibrary = new ArrayList<>();
            for (Parcelable p : getArguments().getParcelableArrayList(SearchPagerAdapter.DATA_KEY)){
                playlistLibrary.add((Playlist) p);
            }
        }

        if (playlistLibrary == null) {
            adapter = new PlaylistListAdapter(getActivity());
        } else{
            adapter = new PlaylistListAdapter(playlistLibrary, getActivity());
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the standard list fragment layout and the add playlist FAB
        View view = inflater.inflate(R.layout.fragment_list, container, false);
        ListView playlistListView = (ListView) view.findViewById(R.id.list);

        // Create the FAB and set its parameters
        FloatingActionButton FAB = new FloatingActionButton(getActivity());

        RelativeLayout.LayoutParams fabParams = new RelativeLayout.LayoutParams(
                (int) getActivity().getResources().getDimension(R.dimen.fab_size),
                (int) getActivity().getResources().getDimension(R.dimen.fab_size));

        // Align it to the bottom right of the screen and set its margin
        fabParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
            fabParams.addRule(RelativeLayout.ALIGN_PARENT_END);
        }
        else{
            fabParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        }

        int margin = (int) getResources().getDimension(R.dimen.fab_padding);
        fabParams.setMargins(margin, margin, margin, margin);

        // Set the FAB's background drawable
        FAB.setLayoutParams(fabParams);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            FAB.setElevation(getResources().getDimension(R.dimen.window_elevation) + 1);
        }
        FAB.setBackgroundResource(R.drawable.fab_background);

        // Set the FAB's icon by adding an imageView child
        ImageView icon = new ImageView(getActivity());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            icon.setImageResource(R.drawable.ic_vector_add);
        else
            icon.setImageResource(R.drawable.ic_add);

        icon.setLayoutParams(new FrameLayout.LayoutParams(
                (int) getActivity().getResources().getDimension(R.dimen.fab_icon_size),
                (int) getActivity().getResources().getDimension(R.dimen.fab_icon_size),
                Gravity.CENTER));
        // Attach the icon to the FAB
        FAB.addView(icon);

        // Attach the FAB to the view
        ((RelativeLayout) playlistListView.getParent()).addView(FAB);

        // Set its action to create a new playlist
        FAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final EditText input = new EditText(getActivity());
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                input.setHint("Playlist name");

                new AlertDialog.Builder(getActivity())
                        .setTitle("Create Playlist")
                        .setView(input)
                        .setPositiveButton("Create", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                LibraryScanner.createPlaylist(getActivity().getApplicationContext(), input.getText().toString(), null);

                                updateData(Library.getPlaylists());
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        }).show();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    int padding = (int) getResources().getDimension(R.dimen.alert_padding);
                    ((View) input.getParent()).setPadding(
                            padding - input.getPaddingLeft(),
                            padding,
                            padding - input.getPaddingRight(),
                            input.getPaddingBottom());
                }
            }
        });

        // Most people probably don't have enough playlists to warrant fast scrolling...
        playlistListView.setFastScrollEnabled(false);

        playlistListView.setAdapter(adapter);
        playlistListView.setOnItemClickListener(adapter);
        playlistListView.setOnItemLongClickListener(adapter);

        Themes.themeFragment(R.layout.fragment_list, view, this);

        return view;
    }

    public void updateData(ArrayList<Playlist> playlistLibrary) {
        this.playlistLibrary = playlistLibrary;
        adapter.updateData(playlistLibrary);
    }
}