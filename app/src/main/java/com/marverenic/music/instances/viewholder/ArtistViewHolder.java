package com.marverenic.music.instances.viewholder;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.marverenic.music.Library;
import com.marverenic.music.PlayerController;
import com.marverenic.music.R;
import com.marverenic.music.activity.instance.ArtistActivity;
import com.marverenic.music.instances.Artist;
import com.marverenic.music.instances.Playlist;
import com.marverenic.music.utils.Navigate;
import com.marverenic.music.utils.Themes;

import java.util.ArrayList;

public class ArtistViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

    private Context context;

    private TextView artistName;
    private ImageView moreButton;
    private Artist reference;

    public ArtistViewHolder(View itemView) {
        super(itemView);
        artistName = (TextView) itemView.findViewById(R.id.instanceTitle);
        moreButton = (ImageView) itemView.findViewById(R.id.instanceMore);

        artistName.setTextColor(Themes.getListText());
        moreButton.setColorFilter(Themes.getListText());

        itemView.setOnClickListener(this);
        moreButton.setOnClickListener(this);

        context = itemView.getContext();
    }

    public void update(Artist a){
        reference = a;
        artistName.setText(a.artistName);
    }

    @Override
    public void onClick(View v){
        switch (v.getId()){
            case R.id.instanceMore:
                final PopupMenu menu = new PopupMenu(context, v, Gravity.END);
                String[] options = context.getResources().getStringArray(R.array.queue_options_artist);
                for (int i = 0; i < options.length;  i++) {
                    menu.getMenu().add(Menu.NONE, i, i, options[i]);
                }
                menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        switch (menuItem.getItemId()){
                            case 0: //Queue this artist next
                                PlayerController.queueNext(Library.getArtistSongEntries(reference));
                                return true;
                            case 1: //Queue this artist last
                                PlayerController.queueLast(Library.getArtistSongEntries(reference));
                                return true;
                            case 2: //Add to playlist...
                                ArrayList<Playlist> playlists = Library.getPlaylists();
                                String[] playlistNames = new String[playlists.size()];

                                for (int i = 0; i < playlists.size(); i++ ){
                                    playlistNames[i] = playlists.get(i).toString();
                                }

                                new AlertDialog.Builder(context)
                                        .setTitle("Add \"" + reference.artistName + "\" to playlist")
                                        .setItems(playlistNames, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                Library.addPlaylistEntries(
                                                        context,
                                                        Library.getPlaylists().get(which),
                                                        Library.getArtistSongEntries(reference));
                                            }
                                        })
                                        .setNegativeButton("Cancel", null)
                                        .show();
                                return true;
                        }
                        return false;
                    }
                });
                menu.show();
                break;
            default:
                Navigate.to(context, ArtistActivity.class, ArtistActivity.ARTIST_EXTRA, reference);
                break;
        }
    }
}
