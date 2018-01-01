package com.marverenic.music.ui.library.artist;

import android.content.Context;
import android.databinding.BaseObservable;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.PopupMenu;
import android.view.Gravity;
import android.view.View;

import com.marverenic.music.R;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.model.Artist;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.ui.common.playlist.AppendPlaylistDialogFragment;
import com.marverenic.music.ui.library.artist.contents.ArtistActivity;

import timber.log.Timber;

public class ArtistItemViewModel extends BaseObservable {

    private static final String TAG_PLAYLIST_DIALOG = "SongItemViewModel.PlaylistDialog";

    private MusicStore mMusicStore;
    private PlayerController mPlayerController;

    private Context mContext;
    private FragmentManager mFragmentManager;
    private Artist mArtist;

    public ArtistItemViewModel(Context context, FragmentManager fragmentManager,
                               MusicStore musicStore, PlayerController playerController) {
        mContext = context;
        mFragmentManager = fragmentManager;
        mMusicStore = musicStore;
        mPlayerController = playerController;
    }

    public void setArtist(Artist artist) {
        mArtist = artist;
        notifyChange();
    }

    public String getName() {
        return mArtist.getArtistName();
    }

    public View.OnClickListener onClickArtist() {
        return v -> mContext.startActivity(ArtistActivity.newIntent(mContext, mArtist));
    }

    public View.OnClickListener onClickMenu() {
        return v -> {
            PopupMenu menu = new PopupMenu(mContext, v, Gravity.END);
            menu.inflate(R.menu.instance_artist);
            menu.setOnMenuItemClickListener(onMenuItemClick());
            menu.show();
        };
    }

    private PopupMenu.OnMenuItemClickListener onMenuItemClick() {
        return menuItem -> {
            switch (menuItem.getItemId()) {
                case R.id.menu_item_queue_item_next:
                    mMusicStore.getSongs(mArtist).subscribe(
                            mPlayerController::queueNext,
                            throwable -> {
                                Timber.e(throwable, "Failed to get songs");
                            });

                    return true;
                case R.id.menu_item_queue_item_last:
                    mMusicStore.getSongs(mArtist).subscribe(
                            mPlayerController::queueLast,
                            throwable -> {
                                Timber.e(throwable, "Failed to get songs");
                            });

                    return true;
                case R.id.menu_item_add_to_playlist:
                    mMusicStore.getSongs(mArtist).subscribe(
                            songs -> {
                                new AppendPlaylistDialogFragment.Builder(mContext, mFragmentManager)
                                        .setSongs(songs, mArtist.getArtistName())
                                        .showSnackbarIn(R.id.list)
                                        .show(TAG_PLAYLIST_DIALOG);
                            }, throwable -> {
                                Timber.e(throwable, "Failed to get songs");
                            });

                    return true;
            }
            return false;
        };
    }

}
