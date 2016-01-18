package com.marverenic.music.instances.viewholder;

import android.support.annotation.ArrayRes;
import android.support.annotation.Nullable;
import android.support.v7.widget.PopupMenu;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.marverenic.music.PlayerController;
import com.marverenic.music.R;
import com.marverenic.music.activity.NowPlayingActivity;
import com.marverenic.music.instances.Song;
import com.marverenic.music.utils.Navigate;
import com.marverenic.music.utils.Prefs;
import com.marverenic.music.view.EnhancedAdapters.EnhancedViewHolder;

import java.util.List;

public abstract class DragDropSongViewHolder extends EnhancedViewHolder<Song>
        implements View.OnClickListener, PopupMenu.OnMenuItemClickListener {

    private TextView songName;
    private TextView detailText;

    protected Song reference;
    protected int index;

    @ArrayRes
    private int menuRes;
    private List<Song> songList;

    public interface OnRemovedListener {
        void onItemRemoved(int index);
    }

    public DragDropSongViewHolder(View itemView, @Nullable List<Song> songList,
                                  @ArrayRes int menuRes) {
        super(itemView);
        this.songList = songList;
        this.menuRes = menuRes;

        songName = (TextView) itemView.findViewById(R.id.instanceTitle);
        detailText = (TextView) itemView.findViewById(R.id.instanceDetail);
        ImageView moreButton = (ImageView) itemView.findViewById(R.id.instanceMore);

        itemView.setOnClickListener(this);
        moreButton.setOnClickListener(this);
    }

    @Override
    public void update(Song item, int position) {
        reference = item;
        index = position;

        songName.setText(item.getSongName());
        detailText.setText(item.getArtistName() + " - " + item.getAlbumName());
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.instanceMore:
                final PopupMenu menu = new PopupMenu(itemView.getContext(), v, Gravity.END);
                String[] options = itemView.getResources().getStringArray(menuRes);

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
}
