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
import com.marverenic.music.activity.instance.GenreActivity;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.dialog.AppendPlaylistDialogFragment;
import com.marverenic.music.instances.Genre;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.utils.Navigate;

import javax.inject.Inject;

import timber.log.Timber;

import static com.marverenic.music.activity.instance.GenreActivity.GENRE_EXTRA;

public class GenreViewModel extends BaseObservable {

    private static final String TAG_PLAYLIST_DIALOG = "GenreViewModel.PlaylistDialog";

    @Inject MusicStore mMusicStore;

    private Context mContext;
    private FragmentManager mFragmentManager;
    private Genre mGenre;

    public GenreViewModel(Context context, FragmentManager fragmentManager) {
        mContext = context;
        mFragmentManager = fragmentManager;

        JockeyApplication.getComponent(mContext).inject(this);
    }

    public void setGenre(Genre genre) {
        mGenre = genre;
        notifyChange();
    }

    public String getName() {
        return mGenre.getGenreName();
    }

    public View.OnClickListener onClickGenre() {
        return v -> Navigate.to(mContext, GenreActivity.class, GENRE_EXTRA, mGenre);
    }

    public View.OnClickListener onClickMenu() {
        return v -> {
            final PopupMenu menu = new PopupMenu(mContext, v, Gravity.END);
            String[] options = mContext.getResources().getStringArray(R.array.queue_options_genre);

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
                case 0: //Queue this genre next
                    mMusicStore.getSongs(mGenre).subscribe(
                            PlayerController::queueNext,
                            throwable -> {
                                Timber.e(throwable, "Failed to get songs");
                            });

                    return true;
                case 1: //Queue this genre last
                    mMusicStore.getSongs(mGenre).subscribe(
                            PlayerController::queueLast,
                            throwable -> {
                                Timber.e(throwable, "Failed to get songs");
                            });

                    return true;
                case 2: //Add to playlist
                    mMusicStore.getSongs(mGenre).subscribe(
                            songs -> {
                                new AppendPlaylistDialogFragment.Builder(mContext, mFragmentManager)
                                        .setSongs(songs, mGenre.getGenreName())
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
