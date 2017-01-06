package com.marverenic.music.viewmodel;

import android.content.Context;
import android.databinding.Bindable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.PopupMenu;
import android.view.Gravity;
import android.view.View;

import com.marverenic.music.BR;
import com.marverenic.music.R;
import com.marverenic.music.activity.instance.AlbumActivity;
import com.marverenic.music.activity.instance.ArtistActivity;
import com.marverenic.music.dialog.AppendPlaylistDialogFragment;
import com.marverenic.music.model.Song;

import java.util.List;

import rx.Subscription;
import timber.log.Timber;

import static android.support.design.widget.Snackbar.LENGTH_LONG;

public class QueueSongViewModel extends SongViewModel {

    private static final String TAG_PLAYLIST_DIALOG = "QueueSongViewModel.PlaylistDialog";

    private Context mContext;
    private FragmentManager mFragmentManager;
    private OnRemoveListener mRemoveListener;
    private Subscription mNowPlayingSubscription;

    private boolean mPlaying;

    public QueueSongViewModel(Context context, FragmentManager fragmentManager, List<Song> songs,
                              OnRemoveListener removeListener) {
        super(context, fragmentManager, songs);
        mContext = context;
        mFragmentManager = fragmentManager;
        mRemoveListener = removeListener;
    }

    @Override
    public void setSong(List<Song> songList, int index) {
        super.setSong(songList, index);

        if (mNowPlayingSubscription != null) {
            mNowPlayingSubscription.unsubscribe();
        }

        // TODO bind to lifecycle
        mNowPlayingSubscription = mPlayerController.getQueuePosition()
                .subscribe(queuePosition -> {
                    mPlaying = (queuePosition == getIndex());
                    notifyPropertyChanged(BR.nowPlayingIndicatorVisibility);
                }, throwable -> {
                    Timber.e(throwable, "Failed to update playing indicator");
                });
    }

    public interface OnRemoveListener {
        void onRemove();
    }

    @Override
    public View.OnClickListener onClickSong() {
        return v -> mPlayerController.changeSong(getIndex());
    }

    @Bindable
    public int getNowPlayingIndicatorVisibility() {
        if (mPlaying) {
            return View.VISIBLE;
        } else {
            return View.GONE;
        }
    }

    @Override
    public View.OnClickListener onClickMenu() {
        return v -> {
            PopupMenu menu = new PopupMenu(mContext, v, Gravity.END);
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
                    mContext.startActivity(ArtistActivity.newIntent(mContext, artist));
                }, throwable -> {
                    Timber.e(throwable, "Failed to find artist");
                });
    }

    private void navigateToAlbum() {
        mMusicStore.findAlbumById(getReference().getAlbumId()).subscribe(
                album -> {
                    mContext.startActivity(AlbumActivity.newIntent(mContext, album));
                }, throwable -> {
                    Timber.e(throwable, "Failed to find album");
                });
    }

    private void addToPlaylist() {
        new AppendPlaylistDialogFragment.Builder(mContext, mFragmentManager)
                .setTitle(mContext.getResources().getString(
                        R.string.header_add_song_name_to_playlist, getReference()))
                .setSongs(getSongs())
                .showSnackbarIn(R.id.now_playing_artwork)
                .show(TAG_PLAYLIST_DIALOG);
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
                    String message = mContext.getString(R.string.message_removed_song,
                            removed.getSongName());

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
