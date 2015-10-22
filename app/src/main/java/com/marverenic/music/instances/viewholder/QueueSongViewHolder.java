package com.marverenic.music.instances.viewholder;

import android.app.Activity;
import android.support.v7.widget.PopupMenu;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.marverenic.music.Library;
import com.marverenic.music.PlayerController;
import com.marverenic.music.R;
import com.marverenic.music.activity.instance.AlbumActivity;
import com.marverenic.music.activity.instance.ArtistActivity;
import com.marverenic.music.instances.Song;
import com.marverenic.music.utils.Navigate;
import com.marverenic.music.utils.PlaylistDialog;

import java.util.ArrayList;

public class QueueSongViewHolder extends DraggableSongViewHolder {

    private Activity parentActivity;
    private OnRemovedListener removedListener;

    public interface OnRemovedListener{
        void onItemRemoved(int index);
    }

    public QueueSongViewHolder(Activity activity, View itemView, OnRemovedListener listener) {
        super(itemView, null);
        this.parentActivity = activity;
        this.removedListener = listener;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.instanceMore:
                final PopupMenu menu = new PopupMenu(itemView.getContext(), v, Gravity.END);
                String[] options = itemView.getResources()
                        .getStringArray(R.array.edit_queue_options);

                for (int i = 0; i < options.length;  i++) {
                    menu.getMenu().add(Menu.NONE, i, i, options[i]);
                }
                menu.setOnMenuItemClickListener(this);
                menu.show();
                break;
            default:
                PlayerController.changeSong(index);
                parentActivity.finish();
                break;
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case 0: //Go to artist
                Navigate.to(
                        itemView.getContext(),
                        ArtistActivity.class,
                        ArtistActivity.ARTIST_EXTRA,
                        Library.findArtistById(reference.artistId));
                return true;
            case 1: // Go to album
                Navigate.to(
                        itemView.getContext(),
                        AlbumActivity.class,
                        AlbumActivity.ALBUM_EXTRA,
                        Library.findAlbumById(reference.albumId));
                return true;
            case 2:
                PlaylistDialog.AddToNormal.alert(itemView, reference, itemView.getContext()
                        .getString(R.string.header_add_song_name_to_playlist, reference));
                return true;
            case 3: // Remove
                ArrayList<Song> editedQueue = PlayerController.getQueue();
                if (editedQueue != null) {
                    int queuePosition = PlayerController.getQueuePosition();
                    int itemPosition = getAdapterPosition();

                    editedQueue.remove(itemPosition);
                    removedListener.onItemRemoved(itemPosition);

                    PlayerController.editQueue(
                            editedQueue,
                            (queuePosition > itemPosition)
                                    ? queuePosition - 1
                                    : queuePosition);

                    if (queuePosition == itemPosition) {
                        PlayerController.begin();
                    }
                }
                return true;
        }
        return false;
    }
}
