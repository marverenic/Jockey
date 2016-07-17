package com.marverenic.music.viewmodel;

import android.content.Context;
import android.support.design.widget.Snackbar;
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

    private Context mContext;
    private OnPlaylistEntriesChangeListener mRemoveListener;

    public PlaylistSongViewModel(Context context, FragmentManager fragmentManager, List<Song> songs,
                                 OnPlaylistEntriesChangeListener listener) {
        super(context, fragmentManager, songs);
        mContext = context;
        mRemoveListener = listener;
    }

    public interface OnPlaylistEntriesChangeListener {
        void onPlaylistEntriesChange();
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
            menu.setOnMenuItemClickListener(onMenuItemClick(v));
            menu.show();
        };
    }

    private PopupMenu.OnMenuItemClickListener onMenuItemClick(View view) {
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
                    removeFromPlaylist(view);
                    return true;
            }
            return false;
        };
    }

    private void removeFromPlaylist(View snackbarContainer) {
        Song removed = getReference();
        int removedIndex = getIndex();

        getSongs().remove(getIndex());
        mRemoveListener.onPlaylistEntriesChange();

        String songName = removed.getSongName();
        String message = mContext.getString(R.string.message_removed_song, songName);

        Snackbar.make(snackbarContainer, message, Snackbar.LENGTH_LONG)
                .setAction(R.string.action_undo, view -> {
                    getSongs().add(removedIndex, removed);
                    mRemoveListener.onPlaylistEntriesChange();
                }).show();
    }
}
