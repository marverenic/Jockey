package com.marverenic.music.ui.library;

import android.app.Activity;
import android.content.Context;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.PopupMenu;
import android.view.Gravity;
import android.view.View;

import com.marverenic.music.BR;
import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.data.store.PreferenceStore;
import com.marverenic.music.model.Song;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.ui.BaseActivity;
import com.marverenic.music.ui.BaseFragment;
import com.marverenic.music.ui.BaseLibraryActivity;
import com.marverenic.music.ui.common.playlist.AppendPlaylistDialogFragment;
import com.marverenic.music.ui.library.album.AlbumActivity;
import com.marverenic.music.ui.library.artist.ArtistActivity;
import com.trello.rxlifecycle.ActivityEvent;
import com.trello.rxlifecycle.FragmentEvent;
import com.trello.rxlifecycle.LifecycleTransformer;

import java.util.List;

import javax.inject.Inject;

import rx.Observable;
import rx.Subscription;
import timber.log.Timber;

public class SongViewModel extends BaseObservable {

    private static final String TAG_PLAYLIST_DIALOG = "SongViewModel.PlaylistDialog";

    @Inject protected MusicStore mMusicStore;
    @Inject protected PreferenceStore mPrefStore;
    @Inject protected PlayerController mPlayerController;

    private Context mContext;
    private Activity mActivity;
    private FragmentManager mFragmentManager;
    private LifecycleTransformer<?> mLifecycleTransformer;
    private Subscription mNowPlayingSubscription;

    private List<Song> mSongList;
    private int mIndex;
    private boolean mIsPlaying;
    private Song mReference;

    public SongViewModel(BaseActivity activity, List<Song> songs) {
        this(activity, activity.getSupportFragmentManager(),
                activity.bindUntilEvent(ActivityEvent.DESTROY), songs);
    }

    public SongViewModel(BaseFragment fragment, List<Song> songs) {
        this(fragment.getActivity(), fragment.getFragmentManager(),
                fragment.bindUntilEvent(FragmentEvent.DESTROY), songs);
    }

    public SongViewModel(Activity activity, FragmentManager fragmentManager,
                         LifecycleTransformer<?> lifecycleTransformer, List<Song> songs) {
        mContext = activity;
        mActivity = activity;
        mFragmentManager = fragmentManager;
        mLifecycleTransformer = lifecycleTransformer;
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

    private <T> LifecycleTransformer<T> bindToLifecycle() {
        //noinspection unchecked
        return (LifecycleTransformer<T>) mLifecycleTransformer;
    }

    protected Observable<Boolean> isPlaying() {
        return mPlayerController.getNowPlaying()
                .map(playing -> playing != null && playing.equals(getReference()));
    }

    public void setSong(List<Song> songList, int index) {
        mSongList = songList;
        mIndex = index;
        mReference = songList.get(index);

        if (mNowPlayingSubscription != null) {
            mNowPlayingSubscription.unsubscribe();
        }

        mIsPlaying = false;
        mNowPlayingSubscription = isPlaying()
                .compose(bindToLifecycle())
                .subscribe(isPlaying -> {
                    mIsPlaying = isPlaying;
                    notifyPropertyChanged(BR.nowPlayingIndicatorVisibility);
                }, throwable -> {
                    Timber.e(throwable, "Failed to update playing indicator");
                });

        notifyPropertyChanged(BR.title);
        notifyPropertyChanged(BR.detail);
    }

    @Bindable
    public int getNowPlayingIndicatorVisibility() {
        if (mIsPlaying) {
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
        return mContext.getString(R.string.format_compact_song_info,
                mReference.getArtistName(), mReference.getAlbumName());
    }

    public View.OnClickListener onClickSong() {
        return v -> {
            mPlayerController.setQueue(mSongList, mIndex);
            mPlayerController.play();

            if (mPrefStore.openNowPlayingOnNewQueue() && mActivity instanceof BaseLibraryActivity) {
                ((BaseLibraryActivity) mActivity).expandBottomSheet();
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
