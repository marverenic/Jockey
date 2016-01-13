package com.marverenic.music.instances.viewholder;

import android.content.Context;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.marverenic.music.PlayerController;
import com.marverenic.music.R;
import com.marverenic.music.activity.instance.ArtistActivity;
import com.marverenic.music.instances.Artist;
import com.marverenic.music.instances.Library;
import com.marverenic.music.instances.PlaylistDialog;
import com.marverenic.music.utils.Navigate;

public class ArtistViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener,
        PopupMenu.OnMenuItemClickListener {

    private Context context;

    private TextView artistName;
    private Artist reference;

    public ArtistViewHolder(View itemView) {
        super(itemView);
        artistName = (TextView) itemView.findViewById(R.id.instanceTitle);
        ImageView moreButton = (ImageView) itemView.findViewById(R.id.instanceMore);

        itemView.setOnClickListener(this);
        moreButton.setOnClickListener(this);

        context = itemView.getContext();
    }

    public void update(Artist a) {
        reference = a;
        artistName.setText(a.getArtistName());
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.instanceMore:
                final PopupMenu menu = new PopupMenu(context, v, Gravity.END);
                String[] options =
                        context.getResources().getStringArray(R.array.queue_options_artist);
                for (int i = 0; i < options.length;  i++) {
                    menu.getMenu().add(Menu.NONE, i, i, options[i]);
                }
                menu.setOnMenuItemClickListener(this);
                menu.show();
                break;
            default:
                Navigate.to(context, ArtistActivity.class, ArtistActivity.ARTIST_EXTRA, reference);
                break;
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case 0: //Queue this artist next
                PlayerController.queueNext(Library.getArtistSongEntries(reference));
                return true;
            case 1: //Queue this artist last
                PlayerController.queueLast(Library.getArtistSongEntries(reference));
                return true;
            case 2: //Add to playlist...
                PlaylistDialog.AddToNormal.alert(
                        itemView,
                        Library.getArtistSongEntries(reference),
                        itemView.getContext()
                                .getString(R.string.header_add_song_name_to_playlist, reference));
                return true;
        }
        return false;
    }
}
