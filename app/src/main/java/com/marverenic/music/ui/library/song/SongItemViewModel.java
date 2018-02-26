package com.marverenic.music.ui.library.song;

import android.content.Context;
import android.databinding.Bindable;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.PopupMenu;
import android.view.Gravity;
import android.view.View;

import com.marverenic.music.BR;
import com.marverenic.music.R;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.model.Song;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.ui.BaseViewModel;
import com.marverenic.music.ui.browse.MusicBrowserActivity;
import com.marverenic.music.ui.common.OnSongSelectedListener;
import com.marverenic.music.ui.common.playlist.AppendPlaylistDialogFragment;
import com.marverenic.music.ui.library.album.contents.AlbumActivity;
import com.marverenic.music.ui.library.artist.contents.ArtistActivity;

import java.util.List;

import timber.log.Timber;

public class SongItemViewModel extends BaseViewModel {

    private static final String TAG_PLAYLIST_DIALOG = "SongItemViewModel.PlaylistDialog";

    private MusicStore mMusicStore;
    private PlayerController mPlayerController;

    private FragmentManager mFragmentManager;

    private List<Song> mSongList;
    private int mIndex;
    private Song mReference;
    private Song mCurrentlyPlayingSong;

    @Nullable
    private OnSongSelectedListener mSongListener;

    public SongItemViewModel(Context context, FragmentManager fragmentManager,
                             MusicStore musicStore, PlayerController playerController,
                             @Nullable OnSongSelectedListener songSelectedListener) {
        super(context);
        mFragmentManager = fragmentManager;
        mMusicStore = musicStore;
        mPlayerController = playerController;
        mSongListener = songSelectedListener;
    }

    public void setIndex(int index) {
        setSong(mSongList, index);
    }

    protected int getIndex() {
        return mIndex;
    }

    protected Song getReference() {
        return mReference;
    }

    protected List<Song> getSongs() {
        return mSongList;
    }

    public void setSong(List<Song> songList, int index) {
        mSongList = songList;
        mIndex = index;
        mReference = songList.get(index);

        notifyPropertyChanged(BR.nowPlayingIndicatorVisibility);
        notifyPropertyChanged(BR.title);
        notifyPropertyChanged(BR.detail);
    }

    public void setCurrentlyPlayingSong(Song song) {
        mCurrentlyPlayingSong = song;
        notifyPropertyChanged(BR.nowPlayingIndicatorVisibility);
    }

    protected boolean isPlaying() {
        return mCurrentlyPlayingSong != null && mCurrentlyPlayingSong.equals(getReference());
    }

    @Bindable
    public int getNowPlayingIndicatorVisibility() {
        if (isPlaying()) {
            return View.VISIBLE;
        } else {
            return View.GONE;
        }
    }

    @Bindable
    public String getTitle() {
        return mReference.getSongName();
    }

    @Bindable
    public String getDetail() {
        return getString(R.string.format_compact_song_info,
                mReference.getArtistName(), mReference.getAlbumName());
    }

    public View.OnClickListener onClickSong() {
        return v -> {
            mPlayerController.setQueue(mSongList, mIndex);
            mPlayerController.play();

            if (mSongListener != null) {
                mSongListener.onSongSelected();
            }
        };
    }

    public View.OnClickListener onClickMenu() {
        return v -> {
            final PopupMenu menu = new PopupMenu(getContext(), v, Gravity.END);
            menu.inflate(R.menu.instance_song);
            menu.setOnMenuItemClickListener(onMenuItemClick());
            menu.show();
        };
    }

    private PopupMenu.OnMenuItemClickListener onMenuItemClick() {
        return menuItem -> {
            switch (menuItem.getItemId()) {
                case R.id.menu_item_queue_item_next:
                    mPlayerController.queueNext(mReference);
                    return true;
                case R.id.menu_item_queue_item_last:
                    mPlayerController.queueLast(mReference);
                    return true;
                case R.id.menu_item_navigate_to_artist:
                    mMusicStore.findArtistById(mReference.getArtistId()).subscribe(
                            artist -> {
                                startActivity(ArtistActivity.newIntent(getContext(), artist));
                            }, throwable -> {
                                Timber.e(throwable, "Failed to find artist");
                            });

                    return true;
                case R.id.menu_item_navigate_to_album:
                    mMusicStore.findAlbumById(mReference.getAlbumId()).subscribe(
                            album -> {
                                startActivity(AlbumActivity.newIntent(getContext(), album));
                            }, throwable -> {
                                Timber.e(throwable, "Failed to find album", throwable);
                            });
                    return true;
                case R.id.menu_item_navigate_to_folder:
                    getContext().startActivity(MusicBrowserActivity.newIntent(getContext(), mReference));
                    return true;
                case R.id.menu_item_add_to_playlist:
                    new AppendPlaylistDialogFragment.Builder(getContext(), mFragmentManager)
                            .setSongs(mReference)
                            .showSnackbarIn(R.id.list)
                            .show(TAG_PLAYLIST_DIALOG);
                    return true;
            }
            return false;
        };
    }

}
