package com.marverenic.music.instances.viewholder;

import android.support.annotation.NonNull;
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
import com.marverenic.music.activity.NowPlayingActivity;
import com.marverenic.music.activity.instance.AlbumActivity;
import com.marverenic.music.activity.instance.ArtistActivity;
import com.marverenic.music.instances.Playlist;
import com.marverenic.music.instances.Song;
import com.marverenic.music.utils.Navigate;
import com.marverenic.music.utils.PlaylistDialog;
import com.marverenic.music.utils.Prefs;

import java.util.ArrayList;

public class SongViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, PopupMenu.OnMenuItemClickListener{

    private View itemView;
    private TextView songName;
    private TextView detailText;

    protected Song reference;
    protected int index;

    private Playlist playlistReference;
    private OnRemovedListener removalListener;
    private ArrayList<Song> songList;

    public interface OnRemovedListener{
        void onSongRemoved(View view, Song song);
    }

    public SongViewHolder(View itemView, ArrayList<Song> songList) {
        super(itemView);
        this.itemView = itemView;
        this.songList = songList;

        songName = (TextView) itemView.findViewById(R.id.instanceTitle);
        detailText = (TextView) itemView.findViewById(R.id.instanceDetail);
        ImageView moreButton = (ImageView) itemView.findViewById(R.id.instanceMore);

        itemView.setOnClickListener(this);
        moreButton.setOnClickListener(this);
    }

    public void setPlaylist(@NonNull Playlist playlist, @NonNull OnRemovedListener listener){
        // Set a playlist for this viewholder to add a remove button to the context menu
        this.playlistReference = playlist;
        this.removalListener = listener;
    }

    public void update(Song s, int index) {
        reference = s;
        this.index = index;

        songName.setText(s.songName);
        detailText.setText(s.artistName + " - " + s.albumName);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.instanceMore:
                final PopupMenu menu = new PopupMenu(itemView.getContext(), v, Gravity.END);
                String[] options = itemView.getResources()
                        .getStringArray(
                                (playlistReference == null)
                                        ? R.array.queue_options_song
                                        : R.array.queue_options_song_playlist);

                for (int i = 0; i < options.length;  i++) {
                    menu.getMenu().add(Menu.NONE, i, i, options[i]);
                }
                menu.setOnMenuItemClickListener(this);
                menu.show();
                break;
            default:
                if (songList != null) {
                    PlayerController.setQueue(songList, index);
                    PlayerController.begin();

                    if (Prefs.getPrefs(itemView.getContext()).getBoolean(Prefs.SWITCH_TO_PLAYING, true))
                        Navigate.to(itemView.getContext(), NowPlayingActivity.class);
                }
                break;
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case 0: //Queue this song next
                PlayerController.queueNext(reference);
                return true;
            case 1: //Queue this song last
                PlayerController.queueLast(reference);
                return true;
            case 2: //Go to artist
                Navigate.to(
                        itemView.getContext(),
                        ArtistActivity.class,
                        ArtistActivity.ARTIST_EXTRA,
                        Library.findArtistById(reference.artistId));
                return true;
            case 3: // Go to album
                Navigate.to(
                        itemView.getContext(),
                        AlbumActivity.class,
                        AlbumActivity.ALBUM_EXTRA,
                        Library.findAlbumById(reference.albumId));
                return true;
            case 4:
                if (playlistReference == null) { //Add to playlist...
                    PlaylistDialog.AddToNormal.alert(itemView, reference, itemView.getContext()
                            .getString(R.string.header_add_song_name_to_playlist, reference));
                } else { // Remove
                    removalListener.onSongRemoved(itemView, reference);
                }
                return true;
        }
        return false;
    }
}
