package com.marverenic.music.instances.section;

import android.content.Context;
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

import com.marverenic.music.player.PlayerController;
import com.marverenic.music.R;
import com.marverenic.music.activity.instance.ArtistActivity;
import com.marverenic.music.instances.Artist;
import com.marverenic.music.instances.Library;
import com.marverenic.music.instances.PlaylistDialog;
import com.marverenic.music.utils.Navigate;
import com.marverenic.music.view.EnhancedAdapters.EnhancedViewHolder;
import com.marverenic.music.view.EnhancedAdapters.HeterogeneousAdapter;

import java.util.List;

public class ArtistSection extends HeterogeneousAdapter.ListSection<Artist> {

    public static final int ID = 3401;

    public ArtistSection(@NonNull List<Artist> data) {
        super(ID, data);
    }

    @Override
    public EnhancedViewHolder<Artist> createViewHolder(HeterogeneousAdapter adapter,
                                                                    ViewGroup parent) {
        return new ViewHolder(
                LayoutInflater
                        .from(parent.getContext())
                        .inflate(R.layout.instance_artist, parent, false));
    }

    public class ViewHolder extends EnhancedViewHolder<Artist> implements View.OnClickListener,
            PopupMenu.OnMenuItemClickListener {

        private Context context;

        private TextView artistName;
        private Artist reference;

        public ViewHolder(View itemView) {
            super(itemView);
            artistName = (TextView) itemView.findViewById(R.id.instanceTitle);
            ImageView moreButton = (ImageView) itemView.findViewById(R.id.instanceMore);

            itemView.setOnClickListener(this);
            moreButton.setOnClickListener(this);

            context = itemView.getContext();
        }

        @Override
        public void update(Artist item, int sectionPosition) {
            reference = item;
            artistName.setText(item.getArtistName());
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
                    Navigate.to(context, ArtistActivity.class,
                            ArtistActivity.ARTIST_EXTRA, reference);
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
                            itemView.getContext().getString(
                                    R.string.header_add_song_name_to_playlist, reference));
                    return true;
            }
            return false;
        }
    }
}
