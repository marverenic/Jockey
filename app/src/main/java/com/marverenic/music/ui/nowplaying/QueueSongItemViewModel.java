package com.marverenic.music.ui.nowplaying;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.PopupMenu;
import android.view.Gravity;
import android.view.View;

import com.marverenic.music.BR;
import com.marverenic.music.R;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.data.store.PlaylistStore;
import com.marverenic.music.model.Song;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.ui.browse.MusicBrowserActivity;
import com.marverenic.music.ui.common.OnSongSelectedListener;
import com.marverenic.music.ui.common.playlist.AppendPlaylistDialogFragment;
import com.marverenic.music.ui.library.album.contents.AlbumActivity;
import com.marverenic.music.ui.library.artist.contents.ArtistActivity;
import com.marverenic.music.ui.library.song.SongItemViewModel;

import timber.log.Timber;

import static android.support.design.widget.Snackbar.LENGTH_LONG;

public class QueueSongItemViewModel extends SongItemViewModel {

    private static final String TAG_PLAYLIST_DIALOG = "QueueSongItemViewModel.PlaylistDialog";

    private MusicStore mMusicStore;
    private PlaylistStore mPlaylistStore;
    private PlayerController mPlayerController;

    private FragmentManager mFragmentManager;
    private OnRemoveListener mRemoveListener;

    private int mCurrentSongIndex;

    public QueueSongItemViewModel(Context context, FragmentManager fragmentManager,
                                  MusicStore musicStore, PlaylistStore playlistStore,
                                  PlayerController playerController,
                                  OnRemoveListener removeListener,
                                  @Nullable OnSongSelectedListener songSelectedListener) {

        super(context, fragmentManager, musicStore, playlistStore, playerController, songSelectedListener);
        mFragmentManager = fragmentManager;
        mMusicStore = musicStore;
        mPlaylistStore = playlistStore;
        mPlayerController = playerController;

        mRemoveListener = removeListener;
    }

    public interface OnRemoveListener {
        void onRemove();
    }

    public void setCurrentlyPlayingSongIndex(int queueIndex) {
        mCurrentSongIndex = queueIndex;
        notifyPropertyChanged(BR.nowPlayingIndicatorVisibility);
    }

    @Override
    protected boolean isPlaying() {
        return mCurrentSongIndex == getIndex();
    }

    @Override
    public View.OnClickListener onClickSong() {
        return v -> mPlayerController.changeSong(getIndex());
    }

    @Override
    public View.OnClickListener onClickMenu() {
        return v -> {
            PopupMenu menu = new PopupMenu(getContext(), v, Gravity.END);
            menu.inflate(getReference().isInLibrary()
                    ? R.menu.instance_song_queue
                    : R.menu.instance_song_queue_remote);
            menu.setOnMenuItemClickListener(onMenuItemClick(v));
            menu.show();
        };
    }

    private PopupMenu.OnMenuItemClickListener onMenuItemClick(View view) {
        return menuItem -> {
            switch (menuItem.getItemId()) {
                case R.id.menu_item_navigate_to_artist:
                    navigateToArtist();
                    return true;
                case R.id.menu_item_navigate_to_album:
                    navigateToAlbum();
                    return true;
                case R.id.menu_item_navigate_to_folder:
                    getContext().startActivity(MusicBrowserActivity.newIntent(getContext(), getReference()));
                    return true;
                case R.id.menu_item_add_to_playlist:
                    addToPlaylist();
                    return true;
                case R.id.menu_item_remove:
                    removeFromQueue(view);
                    return true;
            }
            return false;
        };
    }

    private void navigateToArtist() {
        mMusicStore.findArtistById(getReference().getArtistId()).subscribe(
                artist -> {
                    startActivity(ArtistActivity.newIntent(getContext(), artist));
                }, throwable -> {
                    Timber.e(throwable, "Failed to find artist");
                });
    }

    private void navigateToAlbum() {
        mMusicStore.findAlbumById(getReference().getAlbumId()).subscribe(
                album -> {
                    startActivity(AlbumActivity.newIntent(getContext(), album));
                }, throwable -> {
                    Timber.e(throwable, "Failed to find album");
                });
    }

    private void addToPlaylist() {
        new AppendPlaylistDialogFragment.Builder(getContext(), mFragmentManager)
                .setTitle(getString(R.string.header_add_song_name_to_playlist, getReference()))
                .setSongs(getReference())
                .showSnackbarIn(R.id.now_playing_artwork)
                .show(TAG_PLAYLIST_DIALOG, mPlaylistStore);
    }

    private void removeFromQueue(View snackbarContainer) {
        mPlayerController.getQueuePosition().take(1)
                .subscribe(oldQueuePosition -> {
                    int itemPosition = getIndex();

                    getSongs().remove(itemPosition);

                    int newQueuePosition = (oldQueuePosition > itemPosition)
                            ? oldQueuePosition - 1
                            : oldQueuePosition;

                    newQueuePosition = Math.min(newQueuePosition, getSongs().size() - 1);
                    newQueuePosition = Math.max(newQueuePosition, 0);

                    mPlayerController.editQueue(getSongs(), newQueuePosition);

                    if (oldQueuePosition == itemPosition) {
                        mPlayerController.play();
                    }

                    mRemoveListener.onRemove();

                    Song removed = getReference();
                    String message = getString(R.string.message_removed_song, removed.getSongName());

                    Snackbar.make(snackbarContainer, message, LENGTH_LONG)
                            .setAction(R.string.action_undo, v -> {
                                getSongs().add(itemPosition, removed);
                                mPlayerController.editQueue(getSongs(), oldQueuePosition);
                                if (oldQueuePosition == itemPosition) {
                                    mPlayerController.play();
                                }
                                mRemoveListener.onRemove();
                            })
                            .show();
                }, throwable -> {
                    Timber.e(throwable, "Failed to remove song from queue");
                });
    }
}
