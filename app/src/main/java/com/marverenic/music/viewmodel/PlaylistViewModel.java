package com.marverenic.music.viewmodel;

import android.content.Context;
import android.databinding.BaseObservable;
import android.support.v7.widget.PopupMenu;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;

import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.activity.instance.AutoPlaylistEditActivity;
import com.marverenic.music.activity.instance.PlaylistActivity;
import com.marverenic.music.instances.AutoPlaylist;
import com.marverenic.music.instances.Library;
import com.marverenic.music.instances.Playlist;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.utils.Navigate;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.marverenic.music.activity.instance.PlaylistActivity.PLAYLIST_EXTRA;

public class PlaylistViewModel extends BaseObservable {

    private Context mContext;
    private Playlist mPlaylist;

    public PlaylistViewModel(Context context) {
        mContext = context;
        JockeyApplication.getComponent(mContext).inject(this);
    }

    public void setPlaylist(Playlist playlist) {
        mPlaylist = playlist;
        notifyChange();
    }

    public String getName() {
        return mPlaylist.getPlaylistName();
    }

    public int getSmartIndicatorVisibility() {
        if (mPlaylist instanceof AutoPlaylist) {
            return VISIBLE;
        } else {
            return GONE;
        }
    }

    public View.OnClickListener onClickPlaylist() {
        return v -> Navigate.to(mContext, PlaylistActivity.class, PLAYLIST_EXTRA, mPlaylist);
    }

    public View.OnClickListener onClickMenu() {
        return v -> {
            PopupMenu menu = new PopupMenu(mContext, v, Gravity.END);
            String[] options = mContext.getResources().getStringArray(
                    (mPlaylist instanceof AutoPlaylist)
                            ? R.array.queue_options_smart_playlist
                            : R.array.queue_options_playlist);

            for (int i = 0; i < options.length;  i++) {
                menu.getMenu().add(Menu.NONE, i, i, options[i]);
            }

            menu.setOnMenuItemClickListener(
                    (mPlaylist instanceof  AutoPlaylist)
                            ? onSmartMenuItemClick(v)
                            : onMenuItemClick(v));

            menu.show();
        };
    }

    private PopupMenu.OnMenuItemClickListener onMenuItemClick(View view) {
        return menuItem -> {
            switch (menuItem.getItemId()) {
                case 0: //Queue this playlist next
                    PlayerController.queueNext(Library.getPlaylistEntries(mContext, mPlaylist));
                    return true;
                case 1: //Queue this playlist last
                    PlayerController.queueLast(Library.getPlaylistEntries(mContext, mPlaylist));
                    return true;
                case 2: //Delete this playlist
                    Library.removePlaylist(view, mPlaylist);
                    return true;
            }
            return false;
        };
    }

    private PopupMenu.OnMenuItemClickListener onSmartMenuItemClick(View view) {
        return menuItem -> {
            switch (menuItem.getItemId()) {
                case 0: //Queue this playlist next
                    PlayerController.queueNext(Library.getPlaylistEntries(mContext, mPlaylist));
                    return true;
                case 1: //Queue this playlist last
                    PlayerController.queueLast(Library.getPlaylistEntries(mContext, mPlaylist));
                    return true;
                case 2: //Edit this playlist
                    Navigate.to(mContext, AutoPlaylistEditActivity.class,
                            PLAYLIST_EXTRA, mPlaylist);
                    return true;
                case 3: // Delete this playlist
                    Library.removePlaylist(view, mPlaylist);
                    return true;
            }
            return false;
        };
    }

}
