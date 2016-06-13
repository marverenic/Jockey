package com.marverenic.music.viewmodel;

import android.content.Context;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.PopupMenu;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;

import com.marverenic.music.R;
import com.marverenic.music.activity.instance.AlbumActivity;
import com.marverenic.music.activity.instance.ArtistActivity;
import com.marverenic.music.instances.Library;
import com.marverenic.music.instances.PlaylistDialog;
import com.marverenic.music.instances.Song;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.utils.Navigate;

import java.util.List;

public class QueueSongViewModel extends SongViewModel {

    private Context mContext;
    private FragmentManager mFragmentManager;
    private OnRemoveListener mRemoveListener;

    public QueueSongViewModel(Context context, FragmentManager fragmentManager, List<Song> songs,
                              OnRemoveListener removeListener) {
        super(context, fragmentManager, songs);
        mContext = context;
        mFragmentManager = fragmentManager;
        mRemoveListener = removeListener;
    }

    public interface OnRemoveListener {
        void onRemove();
    }

    @Override
    public View.OnClickListener onClickSong() {
        return v -> PlayerController.changeSong(getIndex());
    }

    public int getNowPlayingIndicatorVisibility() {
        if (PlayerController.getQueuePosition() == getIndex()) {
            return View.VISIBLE;
        } else {
            return View.GONE;
        }
    }

    @Override
    public View.OnClickListener onClickMenu() {
        return v -> {
            PopupMenu menu = new PopupMenu(mContext, v, Gravity.END);
            String[] options = mContext.getResources().getStringArray(R.array.edit_queue_options);

            for (int i = 0; i < options.length;  i++) {
                menu.getMenu().add(Menu.NONE, i, i, options[i]);
            }

            menu.setOnMenuItemClickListener(onMenuItemClick(v));
            menu.show();
        };
    }

    private PopupMenu.OnMenuItemClickListener onMenuItemClick(View view) {
        return menuItem -> {
            switch (menuItem.getItemId()) {
                case 0: //Go to artist
                    Navigate.to(
                            mContext,
                            ArtistActivity.class,
                            ArtistActivity.ARTIST_EXTRA,
                            Library.findArtistById(getReference().getArtistId()));
                    return true;
                case 1: // Go to album
                    Navigate.to(
                            mContext,
                            AlbumActivity.class,
                            AlbumActivity.ALBUM_EXTRA,
                            Library.findAlbumById(getReference().getAlbumId()));
                    return true;
                case 2: // Add to playlist
                    PlaylistDialog.AddToNormal.alert(view, getReference(),
                            mContext.getResources().getString(
                                    R.string.header_add_song_name_to_playlist, getReference()));
                    return true;
                case 3: // Remove
                    int queuePosition = PlayerController.getQueuePosition();
                    int itemPosition = getIndex();

                    getSongs().remove(itemPosition);

                    PlayerController.editQueue(getSongs(),
                            (queuePosition > itemPosition)
                                    ? queuePosition - 1
                                    : queuePosition);

                    if (queuePosition == itemPosition) {
                        PlayerController.begin();
                    }

                    mRemoveListener.onRemove();
                    return true;
            }
            return false;
        };
    }
}
