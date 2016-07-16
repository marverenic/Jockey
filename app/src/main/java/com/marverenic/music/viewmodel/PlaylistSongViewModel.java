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
import com.marverenic.music.instances.Song;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.utils.Navigate;

import java.util.List;

import timber.log.Timber;

public class PlaylistSongViewModel extends SongViewModel {

    private static final String TAG = "PlaylistSongViewModel";

    private Context mContext;
    private OnRemoveListener mRemoveListener;

    public PlaylistSongViewModel(Context context, FragmentManager fragmentManager, List<Song> songs,
                                 OnRemoveListener onRemoveListener) {
        super(context, fragmentManager, songs);
        mContext = context;
        mRemoveListener = onRemoveListener;
    }

    public interface OnRemoveListener {
        void onRemove();
    }

    @Override
    public View.OnClickListener onClickMenu() {
        return v -> {
            final PopupMenu menu = new PopupMenu(mContext, v, Gravity.END);
            String[] options = mContext.getResources()
                    .getStringArray(R.array.edit_playlist_options);

            for (int i = 0; i < options.length;  i++) {
                menu.getMenu().add(Menu.NONE, i, i, options[i]);
            }
            menu.setOnMenuItemClickListener(onMenuItemClick());
            menu.show();
        };
    }

    private PopupMenu.OnMenuItemClickListener onMenuItemClick() {
        return menuItem -> {
            switch (menuItem.getItemId()) {
                case 0: //Queue this song next
                    PlayerController.queueNext(getReference());
                    return true;
                case 1: //Queue this song last
                    PlayerController.queueLast(getReference());
                    return true;
                case 2: //Go to artist
                    mMusicStore.findArtistById(getReference().getArtistId()).subscribe(
                            artist -> {
                                Navigate.to(mContext, ArtistActivity.class,
                                        ArtistActivity.ARTIST_EXTRA, artist);
                            }, throwable -> {
                                Timber.e(throwable, "Failed to find artist");
                            });

                    return true;
                case 3: // Go to album
                    mMusicStore.findAlbumById(getReference().getAlbumId()).subscribe(
                            album -> {
                                Navigate.to(mContext, AlbumActivity.class,
                                        AlbumActivity.ALBUM_EXTRA, album);
                            }, throwable -> {
                                Timber.e(throwable, "Failed to find album");
                            });

                    return true;
                case 4: // Remove
                    getSongs().remove(getIndex());
                    mRemoveListener.onRemove();
                    return true;
            }
            return false;
        };
    }
}
