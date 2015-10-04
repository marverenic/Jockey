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

import com.marverenic.music.Library;
import com.marverenic.music.PlayerController;
import com.marverenic.music.R;
import com.marverenic.music.activity.instance.AutoPlaylistEditActivity;
import com.marverenic.music.activity.instance.PlaylistActivity;
import com.marverenic.music.instances.AutoPlaylist;
import com.marverenic.music.instances.Playlist;
import com.marverenic.music.utils.Navigate;

public class PlaylistViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, PopupMenu.OnMenuItemClickListener{

    private Context context;

    private TextView playlistName;
    private ImageView moreButton;
    private ImageView autoPlaylistIndicator;

    private Playlist reference;

    public PlaylistViewHolder(View itemView) {
        super(itemView);
        playlistName = (TextView) itemView.findViewById(R.id.instanceTitle);
        moreButton = (ImageView) itemView.findViewById(R.id.instanceMore);
        autoPlaylistIndicator = (ImageView) itemView.findViewById(R.id.instanceAutoIndicator);

        itemView.setOnClickListener(this);
        moreButton.setOnClickListener(this);

        context = itemView.getContext();
    }

    public void update(Playlist p){
        reference = p;

        if (p == null){
            playlistName.setText("");
            moreButton.setVisibility(View.GONE);
        }
        else {
            playlistName.setText(p.playlistName);
            moreButton.setVisibility(View.VISIBLE);
        }

        if (p instanceof AutoPlaylist) {
            autoPlaylistIndicator.setVisibility(View.VISIBLE);
        } else {
            autoPlaylistIndicator.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View v){
        switch (v.getId()){
            case R.id.instanceMore:
                final PopupMenu menu = new PopupMenu(context, v, Gravity.END);
                String[] options = (reference instanceof AutoPlaylist)
                        ? context.getResources().getStringArray(R.array.queue_options_smart_playlist)
                        : context.getResources().getStringArray(R.array.queue_options_playlist);
                for (int i = 0; i < options.length;  i++) {
                    menu.getMenu().add(Menu.NONE, i, i, options[i]);
                }
                menu.setOnMenuItemClickListener(this);
                menu.show();
                break;
            default:
                Navigate.to(context, PlaylistActivity.class, PlaylistActivity.PLAYLIST_EXTRA, reference);
                break;
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        if (reference instanceof AutoPlaylist) {
            switch (menuItem.getItemId()) {
                case 2: //Edit this playlist
                    Navigate.to(context, AutoPlaylistEditActivity.class,
                            AutoPlaylistEditActivity.PLAYLIST_EXTRA, reference);
                    return true;
                case 3: // Delete this playlist
                    Library.removePlaylist(itemView, reference);
                    return true;
            }
        }
        switch (menuItem.getItemId()){
            case 0: //Queue this playlist next
                PlayerController.queueNext(Library.getPlaylistEntries(context, reference));
                return true;
            case 1: //Queue this playlist last
                PlayerController.queueLast(Library.getPlaylistEntries(context, reference));
                return true;
            case 2: //Delete this playlist
                Library.removePlaylist(itemView, reference);
                return true;
        }
        return false;
    }
}
