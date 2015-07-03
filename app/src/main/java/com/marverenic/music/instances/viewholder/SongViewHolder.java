package com.marverenic.music.instances.viewholder;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
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
import com.marverenic.music.activity.NowPlayingActivity;
import com.marverenic.music.activity.instance.AlbumActivity;
import com.marverenic.music.activity.instance.ArtistActivity;
import com.marverenic.music.instances.Playlist;
import com.marverenic.music.instances.Song;
import com.marverenic.music.utils.Navigate;
import com.marverenic.music.utils.Themes;

import java.util.ArrayList;

public class SongViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, PopupMenu.OnMenuItemClickListener{

    private View itemView;
    private TextView songName;
    private TextView detailText;
    private ImageView moreButton;
    private Song reference;
    private ArrayList<Song> songList;

    private View.OnClickListener customListener;

    public SongViewHolder(View itemView) {
        super(itemView);
        this.itemView = itemView;

        songName = (TextView) itemView.findViewById(R.id.instanceTitle);
        detailText = (TextView) itemView.findViewById(R.id.instanceDetail);
        moreButton = (ImageView) itemView.findViewById(R.id.instanceMore);

        itemView.setOnClickListener(this);
        moreButton.setOnClickListener(this);
    }

    public void setSongList(ArrayList<Song> songList){
        this.songList = songList;
    }

    public void update(Song s){
        reference = s;

        songName.setText(s.songName);
        detailText.setText(s.artistName + " - " + s.albumName);
    }

    public void highlight(int primaryColor, int detailColor){
        songName.setTextColor(primaryColor);
        detailText.setTextColor(detailColor);
    }

    public void setClickListener(View.OnClickListener listener){
        customListener = listener;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.instanceMore:
                final PopupMenu menu = new PopupMenu(itemView.getContext(), v, Gravity.END);
                String[] options = itemView.getResources().getStringArray(R.array.queue_options_song);
                for (int i = 0; i < options.length;  i++) {
                    menu.getMenu().add(Menu.NONE, i, i, options[i]);
                }
                menu.setOnMenuItemClickListener(this);
                menu.show();
                break;
            default:
                if (customListener != null){
                    customListener.onClick(v);
                }
                else if (songList != null) {
                    // TODO set list index to properly play song lists with duplicate song entries
                    PlayerController.setQueue(songList, songList.indexOf(reference));
                    PlayerController.begin();

                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(itemView.getContext());
                    if (prefs.getBoolean("prefSwitchToNowPlaying", true))
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
            case 4: //Add to playlist...
                ArrayList<Playlist> playlists = Library.getPlaylists();
                String[] playlistNames = new String[playlists.size()];

                for (int i = 0; i < playlists.size(); i++) {
                    playlistNames[i] = playlists.get(i).toString();
                }

                AlertDialog playlistDialog = new AlertDialog.Builder(itemView.getContext())
                        .setTitle(itemView.getContext().getString(R.string.header_add_song_name_to_playlist, reference.songName))
                        .setItems(playlistNames, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Library.addPlaylistEntry(
                                        itemView.getContext(),
                                        Library.getPlaylists().get(which),
                                        reference);
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
