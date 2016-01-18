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

import com.marverenic.music.PlayerController;
import com.marverenic.music.R;
import com.marverenic.music.activity.instance.AutoPlaylistEditActivity;
import com.marverenic.music.activity.instance.PlaylistActivity;
import com.marverenic.music.instances.AutoPlaylist;
import com.marverenic.music.instances.Library;
import com.marverenic.music.instances.Playlist;
import com.marverenic.music.utils.Navigate;
import com.marverenic.music.view.EnhancedAdapters.EnhancedViewHolder;
import com.marverenic.music.view.EnhancedAdapters.HeterogeneousAdapter;

import java.util.List;

public class PlaylistSection extends HeterogeneousAdapter.ListSection<Playlist> {

    public static final int ID = 3574;

    public PlaylistSection(@NonNull List<Playlist> data) {
        super(ID, data);
    }

    @Override
    public EnhancedViewHolder<Playlist> createViewHolder(HeterogeneousAdapter adapter,
                                                                      ViewGroup parent) {
        return new ViewHolder(
                LayoutInflater
                        .from(parent.getContext())
                        .inflate(R.layout.instance_playlist, parent, false));
    }

    public class ViewHolder extends EnhancedViewHolder<Playlist>
            implements View.OnClickListener, PopupMenu.OnMenuItemClickListener {

        private Context context;

        private TextView playlistName;
        private ImageView moreButton;
        private ImageView autoPlaylistIndicator;

        private Playlist reference;

        public ViewHolder(View itemView) {
            super(itemView);
            playlistName = (TextView) itemView.findViewById(R.id.instanceTitle);
            moreButton = (ImageView) itemView.findViewById(R.id.instanceMore);
            autoPlaylistIndicator = (ImageView) itemView.findViewById(R.id.instanceAutoIndicator);

            itemView.setOnClickListener(this);
            moreButton.setOnClickListener(this);

            context = itemView.getContext();
        }

        @Override
        public void update(Playlist item, int sectionPosition) {
            reference = item;

            if (item == null) {
                playlistName.setText("");
                moreButton.setVisibility(View.GONE);
            } else {
                playlistName.setText(item.getPlaylistName());
                moreButton.setVisibility(View.VISIBLE);
            }

            if (item instanceof AutoPlaylist) {
                autoPlaylistIndicator.setVisibility(View.VISIBLE);
            } else {
                autoPlaylistIndicator.setVisibility(View.GONE);
            }
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.instanceMore:
                    final PopupMenu menu = new PopupMenu(context, v, Gravity.END);
                    String[] options = context.getResources().getStringArray(
                            (reference instanceof AutoPlaylist)
                                    ? R.array.queue_options_smart_playlist
                                    : R.array.queue_options_playlist);
                    for (int i = 0; i < options.length;  i++) {
                        menu.getMenu().add(Menu.NONE, i, i, options[i]);
                    }
                    menu.setOnMenuItemClickListener(this);
                    menu.show();
                    break;
                default:
                    Navigate.to(context, PlaylistActivity.class,
                            PlaylistActivity.PLAYLIST_EXTRA, reference);
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
            switch (menuItem.getItemId()) {
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
}
