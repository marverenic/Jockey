package com.marverenic.music.viewmodel;

import android.content.Context;
import android.databinding.BaseObservable;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.PopupMenu;
import android.view.Gravity;
import android.view.View;

import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.activity.NowPlayingActivity;
import com.marverenic.music.activity.instance.AlbumActivity;
import com.marverenic.music.activity.instance.ArtistActivity;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.data.store.PreferenceStore;
import com.marverenic.music.dialog.AppendPlaylistDialogFragment;
import com.marverenic.music.model.Song;
import com.marverenic.music.player.PlayerController;

import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;

public class SongViewModel extends BaseObservable {

    private static final String TAG_PLAYLIST_DIALOG = "SongViewModel.PlaylistDialog";

    @Inject MusicStore mMusicStore;
    @Inject PreferenceStore mPrefStore;
    @Inject PlayerController mPlayerController;

    private Context mContext;
    private FragmentManager mFragmentManager;

    private List<Song> mSongList;
    private int mIndex;
    private Song mReference;

    public SongViewModel(Context context, FragmentManager fragmentManager, List<Song> songs) {
        mContext = context;
        mFragmentManager = fragmentManager;
        mSongList = songs;

        JockeyApplication.getComponent(mContext).inject(this);
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

        notifyChange();
    }

    public String getTitle() {
        return mReference.getSongName();
    }

    public String getDetail() {
        return mContext.getString(R.string.format_compact_song_info,
                mReference.getArtistName(), mReference.getAlbumName());
    }

    public View.OnClickListener onClickSong() {
        return v -> {
            mPlayerController.setQueue(mSongList, mIndex);
            mPlayerController.play();

            if (mPrefStore.openNowPlayingOnNewQueue()) {
                mContext.startActivity(NowPlayingActivity.newIntent(mContext));
            }
        };
    }

    public View.OnClickListener onClickMenu() {
        return v -> {
            final PopupMenu menu = new PopupMenu(mContext, v, Gravity.END);
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
                                mContext.startActivity(ArtistActivity.newIntent(mContext, artist));
                            }, throwable -> {
                                Timber.e(throwable, "Failed to find artist");
                            });

                    return true;
                case R.id.menu_item_navigate_to_album:
                    mMusicStore.findAlbumById(mReference.getAlbumId()).subscribe(
                            album -> {
                                mContext.startActivity(AlbumActivity.newIntent(mContext, album));
                            }, throwable -> {
                                Timber.e(throwable, "Failed to find album", throwable);
                            });
                    return true;
                case R.id.menu_item_add_to_playlist:
                    new AppendPlaylistDialogFragment.Builder(mContext, mFragmentManager)
                            .setSongs(mReference)
                            .showSnackbarIn(R.id.list)
                            .show(TAG_PLAYLIST_DIALOG);
                    return true;
            }
            return false;
        };
    }

}
