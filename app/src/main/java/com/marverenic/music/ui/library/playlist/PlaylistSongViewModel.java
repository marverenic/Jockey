package com.marverenic.music.ui.library.playlist;

import android.content.Context;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.PopupMenu;
import android.view.Gravity;
import android.view.View;

import com.marverenic.music.R;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.model.Song;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.ui.library.SongViewModel;
import com.marverenic.music.ui.library.album.AlbumActivity;
import com.marverenic.music.ui.library.artist.ArtistActivity;

import timber.log.Timber;

public class PlaylistSongViewModel extends SongViewModel {

    private MusicStore mMusicStore;
    private PlayerController mPlayerController;

    private OnPlaylistEntriesChangeListener mRemoveListener;

    public PlaylistSongViewModel(Context context, FragmentManager fragmentManager,
                                 MusicStore musicStore, PlayerController playerController,
                                 OnPlaylistEntriesChangeListener listener) {

        super(context, fragmentManager, musicStore, playerController);
        mMusicStore = musicStore;
        mPlayerController = playerController;

        mRemoveListener = listener;
    }

    public interface OnPlaylistEntriesChangeListener {
        void onPlaylistEntriesChange();
    }

    @Override
    public View.OnClickListener onClickMenu() {
        return v -> {
            final PopupMenu menu = new PopupMenu(getContext(), v, Gravity.END);
            menu.inflate(R.menu.instance_song_playlist);
            menu.setOnMenuItemClickListener(onMenuItemClick(v));
            menu.show();
        };
    }

    private PopupMenu.OnMenuItemClickListener onMenuItemClick(View view) {
        return menuItem -> {
            switch (menuItem.getItemId()) {
                case R.id.menu_item_queue_item_next:
                    mPlayerController.queueNext(getReference());
                    return true;
                case R.id.menu_item_queue_item_last:
                    mPlayerController.queueLast(getReference());
                    return true;
                case R.id.menu_item_navigate_to_artist:
                    mMusicStore.findArtistById(getReference().getArtistId()).subscribe(
                            artist -> {
                                startActivity(ArtistActivity.newIntent(getContext(), artist));
                            }, throwable -> {
                                Timber.e(throwable, "Failed to find artist");
                            });

                    return true;
                case R.id.menu_item_navigate_to_album:
                    mMusicStore.findAlbumById(getReference().getAlbumId()).subscribe(
                            album -> {
                                startActivity(AlbumActivity.newIntent(getContext(), album));
                            }, throwable -> {
                                Timber.e(throwable, "Failed to find album");
                            });

                    return true;
                case R.id.menu_item_remove:
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
        String message = getString(R.string.message_removed_song, songName);

        Snackbar.make(snackbarContainer, message, Snackbar.LENGTH_LONG)
                .setAction(R.string.action_undo, view -> {
                    getSongs().add(removedIndex, removed);
                    mRemoveListener.onPlaylistEntriesChange();
                }).show();
    }
}
