package com.marverenic.music.fragments;

import android.content.DialogInterface;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatEditText;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

        int paddingTop = (int) getActivity().getResources().getDimension(R.dimen.list_margin);
        int paddingH =(int) getActivity().getResources().getDimension(R.dimen.global_padding);
        view.setPadding(paddingH, paddingTop, paddingH, 0);

        // TODO Attach a FAB to create a new playlist if our list has all playlists in the library

        // Add an empty view to the bottom of the list
        View dummy = View.inflate(getActivity(), R.layout.instance_playlist, null);
        dummy.setMinimumHeight((int) getActivity().getResources().getDimension(R.dimen.list_height));
        dummy.setVisibility(View.INVISIBLE);
        playlistListView.addFooterView(dummy, null, false);

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
        fabParams.setMargins(margin, margin, 2 * margin, margin);

        // Set the FAB's background drawable
        FAB.setLayoutParams(fabParams);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            FAB.setElevation(getResources().getDimension(R.dimen.fab_elevation));
            FAB.setBackgroundResource(R.drawable.fab_background);
        }
        else{
            Themes.updateColors(getActivity());
            StateListDrawable background = new StateListDrawable();

            ShapeDrawable pressed = new ShapeDrawable(new OvalShape());
            pressed.getPaint().setColor(Themes.getAccent());
            background.addState(new int[]{android.R.attr.state_pressed}, pressed);

            ShapeDrawable normal = new ShapeDrawable(new OvalShape());
            normal.getPaint().setColor(Themes.getPrimary());
            background.addState(new int[]{}, normal);

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
                FAB.setBackground(background);
            else
                FAB.setBackgroundDrawable(background);
        }

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
                final AppCompatEditText input = new AppCompatEditText(getActivity());
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

                int padding = (int) getResources().getDimension(R.dimen.alert_padding);
                ((View) input.getParent()).setPadding(
                        padding - input.getPaddingLeft(),
                        padding,
                        padding - input.getPaddingRight(),
                        input.getPaddingBottom());
            }
        });

        playlistListView.setAdapter(adapter);
        playlistListView.setOnItemClickListener(adapter);
        playlistListView.setBackgroundColor(Themes.getBackgroundElevated());

        return view;
    }

    public void updateData(ArrayList<Playlist> playlistLibrary) {
        this.playlistLibrary = playlistLibrary;
        adapter.updateData(playlistLibrary);
    }

    public void updateData(){
        this.playlistLibrary = Library.getPlaylists();
        adapter.updateData();
    }
}