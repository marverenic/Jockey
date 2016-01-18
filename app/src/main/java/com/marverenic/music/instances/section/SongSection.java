package com.marverenic.music.instances.section;

import android.support.annotation.NonNull;
import android.support.v7.widget.PopupMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.marverenic.music.PlayerController;
import com.marverenic.music.R;
import com.marverenic.music.activity.NowPlayingActivity;
import com.marverenic.music.activity.instance.AlbumActivity;
import com.marverenic.music.activity.instance.ArtistActivity;
import com.marverenic.music.instances.Library;
import com.marverenic.music.instances.PlaylistDialog;
import com.marverenic.music.instances.Song;
import com.marverenic.music.utils.Navigate;
import com.marverenic.music.utils.Prefs;
import com.marverenic.music.view.EnhancedAdapters.EnhancedViewHolder;
import com.marverenic.music.view.EnhancedAdapters.HeterogeneousAdapter;

import java.util.List;

public class SongSection extends HeterogeneousAdapter.ListSection<Song> {

    public static final int ID = 9149;

    public SongSection(@NonNull List<Song> data) {
        super(ID, data);
    }

    @Override
    public EnhancedViewHolder<Song> createViewHolder(HeterogeneousAdapter adapter,
                                                                  ViewGroup parent) {
        return new ViewHolder(
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.instance_song, parent, false),
                getData());
    }

    public static class ViewHolder extends EnhancedViewHolder<Song>
            implements View.OnClickListener, PopupMenu.OnMenuItemClickListener {

        private View itemView;
        private TextView songName;
        private TextView detailText;

        protected Song reference;
        protected int index;

        private List<Song> songList;

        public ViewHolder(View itemView, List<Song> songList) {
            super(itemView);
            this.itemView = itemView;
            this.songList = songList;

            songName = (TextView) itemView.findViewById(R.id.instanceTitle);
            detailText = (TextView) itemView.findViewById(R.id.instanceDetail);
            ImageView moreButton = (ImageView) itemView.findViewById(R.id.instanceMore);

            itemView.setOnClickListener(this);
            moreButton.setOnClickListener(this);
        }

        @Override
        public void update(Song s, int sectionPosition) {
            reference = s;
            index = sectionPosition;

            songName.setText(s.getSongName());
            detailText.setText(s.getArtistName() + " - " + s.getAlbumName());
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.instanceMore:
                    final PopupMenu menu = new PopupMenu(itemView.getContext(), v, Gravity.END);
                    String[] options = itemView.getResources()
                            .getStringArray(R.array.queue_options_song);

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

                        if (Prefs.getPrefs(itemView.getContext())
                                .getBoolean(Prefs.SWITCH_TO_PLAYING, true)) {
                            Navigate.to(itemView.getContext(), NowPlayingActivity.class);
                        }
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
                            Library.findArtistById(reference.getArtistId()));
                    return true;
                case 3: // Go to album
                    Navigate.to(
                            itemView.getContext(),
                            AlbumActivity.class,
                            AlbumActivity.ALBUM_EXTRA,
                            Library.findAlbumById(reference.getAlbumId()));
                    return true;
                case 4: //Add to playlist...
                    PlaylistDialog.AddToNormal.alert(itemView, reference, itemView.getContext()
                            .getString(R.string.header_add_song_name_to_playlist, reference));
                    return true;
            }
            return false;
        }
    }
}
