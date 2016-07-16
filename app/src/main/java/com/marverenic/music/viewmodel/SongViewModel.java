package com.marverenic.music.viewmodel;

import android.content.Context;
import android.databinding.BaseObservable;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.PopupMenu;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;

import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.activity.NowPlayingActivity;
import com.marverenic.music.activity.instance.AlbumActivity;
import com.marverenic.music.activity.instance.ArtistActivity;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.data.store.PreferencesStore;
import com.marverenic.music.dialog.AppendPlaylistDialogFragment;
import com.marverenic.music.instances.Song;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.utils.Navigate;

import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;

public class SongViewModel extends BaseObservable {

    private static final String TAG_PLAYLIST_DIALOG = "SongViewModel.PlaylistDialog";

    @Inject MusicStore mMusicStore;
    @Inject PreferencesStore mPrefStore;

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
            PlayerController.setQueue(mSongList, mIndex);
            PlayerController.begin();

            if (mPrefStore.openNowPlayingOnNewQueue()) {
                Navigate.to(mContext, NowPlayingActivity.class);
            }
        };
    }

    public View.OnClickListener onClickMenu() {
        return v -> {
            final PopupMenu menu = new PopupMenu(mContext, v, Gravity.END);
            String[] options = mContext.getResources()
                    .getStringArray(R.array.queue_options_song);

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
                    PlayerController.queueNext(mReference);
                    return true;
                case 1: //Queue this song last
                    PlayerController.queueLast(mReference);
                    return true;
                case 2: //Go to artist
                    mMusicStore.findArtistById(mReference.getArtistId()).subscribe(
                            artist -> {
                                Navigate.to(mContext, ArtistActivity.class,
                                        ArtistActivity.ARTIST_EXTRA, artist);
                            }, throwable -> {
                                Timber.e(throwable, "Failed to find artist");
                            });

                    return true;
                case 3: // Go to album
                    mMusicStore.findAlbumById(mReference.getAlbumId()).subscribe(
                            album -> {
                                Navigate.to(mContext, AlbumActivity.class,
                                        AlbumActivity.ALBUM_EXTRA, album);
                            }, throwable -> {
                                Timber.e(throwable, "Failed to find album", throwable);
                            });

                    return true;
                case 4: //Add to playlist...
                    AppendPlaylistDialogFragment.newInstance()
                            .setSong(mReference)
                            .showSnackbarIn(R.id.list)
                            .show(mFragmentManager, TAG_PLAYLIST_DIALOG);
                    return true;
            }
            return false;
        };
    }

}
