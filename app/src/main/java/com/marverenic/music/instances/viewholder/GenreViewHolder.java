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
import com.marverenic.music.activity.instance.GenreActivity;
import com.marverenic.music.instances.Genre;
import com.marverenic.music.instances.Playlist;
import com.marverenic.music.utils.Navigate;
import com.marverenic.music.utils.Themes;

import java.util.ArrayList;

public class GenreViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, PopupMenu.OnMenuItemClickListener{

    private Context context;

    private TextView genreName;
    private ImageView moreButton;
    private Genre reference;

    public GenreViewHolder(View itemView) {
        super(itemView);
        genreName = (TextView) itemView.findViewById(R.id.instanceTitle);
        moreButton = (ImageView) itemView.findViewById(R.id.instanceMore);

        itemView.setOnClickListener(this);
        moreButton.setOnClickListener(this);

        context = itemView.getContext();
    }

    public void update(Genre g){
        reference = g;
        genreName.setText(g.genreName);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.instanceMore:
                final PopupMenu menu = new PopupMenu(context, v, Gravity.END);
                String[] options = context.getResources().getStringArray(R.array.queue_options_genre);
                for (int i = 0; i < options.length;  i++) {
                    menu.getMenu().add(Menu.NONE, i, i, options[i]);
                }
                menu.setOnMenuItemClickListener(this);
                menu.show();
                break;
            default:
                Navigate.to(context, GenreActivity.class, GenreActivity.GENRE_EXTRA, reference);
                break;
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        switch (menuItem.getItemId()){
            case 0: //Queue this genre next
                PlayerController.queueNext(Library.getGenreEntries(reference));
                return true;
            case 1: //Queue this genre last
                PlayerController.queueLast(Library.getGenreEntries(reference));
                return true;
            case 2: //Add to playlist
                ArrayList<Playlist> playlists = Library.getPlaylists();
                String[] playlistNames = new String[playlists.size()];

                for (int i = 0; i < playlists.size(); i++ ){
                    playlistNames[i] = playlists.get(i).toString();
                }

                AlertDialog playlistDialog = new AlertDialog.Builder(context)
                        .setTitle(context.getString(R.string.header_add_song_name_to_playlist, reference.genreName))
                        .setItems(playlistNames, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Library.addPlaylistEntries(
                                        itemView,
                                        Library.getPlaylists().get(which),
                                        Library.getGenreEntries(reference));
                            }
                        })
                        .setNegativeButton(R.string.action_cancel, null)
                        .show();

                playlistDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Themes.getAccent());

                return true;
        }
        return false;
    }
}
