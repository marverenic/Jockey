package com.marverenic.music.ui.library.genre;

import android.content.Context;
import androidx.databinding.BaseObservable;
import androidx.fragment.app.FragmentManager;
import androidx.appcompat.widget.PopupMenu;
import android.view.Gravity;
import android.view.View;

import com.marverenic.music.R;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.data.store.PlaylistStore;
import com.marverenic.music.model.Genre;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.ui.common.playlist.AppendPlaylistDialogFragment;
import com.marverenic.music.ui.library.genre.contents.GenreActivity;

import timber.log.Timber;

public class GenreItemViewModel extends BaseObservable {

    private static final String TAG_PLAYLIST_DIALOG = "GenreItemViewModel.PlaylistDialog";

    private MusicStore mMusicStore;
    private PlaylistStore mPlaylistStore;
    private PlayerController mPlayerController;

    private Context mContext;
    private FragmentManager mFragmentManager;
    private Genre mGenre;

    public GenreItemViewModel(Context context, FragmentManager fragmentManager,
                              MusicStore musicStore, PlaylistStore playlistStore,
                              PlayerController playerController) {
        mContext = context;
        mFragmentManager = fragmentManager;
        mMusicStore = musicStore;
        mPlaylistStore = playlistStore;
        mPlayerController = playerController;
    }

    public void setGenre(Genre genre) {
        mGenre = genre;
        notifyChange();
    }

    public String getName() {
        return mGenre.getGenreName();
    }

    public View.OnClickListener onClickGenre() {
        return v -> mContext.startActivity(GenreActivity.newIntent(mContext, mGenre));
    }

    public View.OnClickListener onClickMenu() {
        return v -> {
            final PopupMenu menu = new PopupMenu(mContext, v, Gravity.END);
            menu.inflate(R.menu.instance_genre);
            menu.setOnMenuItemClickListener(onMenuItemClick());
            menu.show();
        };
    }

    private PopupMenu.OnMenuItemClickListener onMenuItemClick() {
        return menuItem -> {
            switch (menuItem.getItemId()) {
                case R.id.menu_item_queue_item_next:
                    mMusicStore.getSongs(mGenre).subscribe(
                            mPlayerController::queueNext,
                            throwable -> {
                                Timber.e(throwable, "Failed to get songs");
                            });

                    return true;
                case R.id.menu_item_queue_item_last:
                    mMusicStore.getSongs(mGenre).subscribe(
                            mPlayerController::queueLast,
                            throwable -> {
                                Timber.e(throwable, "Failed to get songs");
                            });

                    return true;
                case R.id.menu_item_add_to_playlist:
                    mMusicStore.getSongs(mGenre).subscribe(
                            songs -> {
                                new AppendPlaylistDialogFragment.Builder(mContext, mFragmentManager)
                                        .setSongs(songs, mGenre.getGenreName())
                                        .showSnackbarIn(R.id.list)
                                        .show(TAG_PLAYLIST_DIALOG, mPlaylistStore);
                            }, throwable -> {
                                Timber.e(throwable, "Failed to get songs");
                            });

                    return true;
            }
            return false;
        };
    }

}
