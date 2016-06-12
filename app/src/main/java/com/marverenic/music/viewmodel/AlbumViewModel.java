package com.marverenic.music.viewmodel;

import android.content.Context;
import android.databinding.BaseObservable;
import android.support.v7.widget.PopupMenu;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;

import com.marverenic.music.R;
import com.marverenic.music.activity.instance.AlbumActivity;
import com.marverenic.music.activity.instance.ArtistActivity;
import com.marverenic.music.instances.Album;
import com.marverenic.music.instances.Library;
import com.marverenic.music.instances.PlaylistDialog;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.utils.Navigate;

public class AlbumViewModel extends BaseObservable {

    private Context mContext;
    private Album mAlbum;

    public AlbumViewModel(Context context) {
        mContext = context;
    }

    public void setAlbum(Album album) {
        mAlbum = album;
        notifyChange();
    }

    public String getAlbumTitle() {
        return mAlbum.getAlbumName();
    }

    public String getAlbumArtist() {
        return mAlbum.getArtistName();
    }

    public View.OnClickListener onClickAlbum() {
        return v -> Navigate.to(mContext, AlbumActivity.class, AlbumActivity.ALBUM_EXTRA, mAlbum);
    }

    public View.OnClickListener onClickMenu() {
        return v -> {
            PopupMenu menu = new android.support.v7.widget.PopupMenu(mContext, v, Gravity.END);
            String[] options = mContext.getResources().getStringArray(R.array.queue_options_album);
            for (int i = 0; i < options.length; i++) {
                menu.getMenu().add(Menu.NONE, i, i, options[i]);
            }
            menu.setOnMenuItemClickListener(onMenuItemClick(v));
            menu.show();
        };
    }

    private PopupMenu.OnMenuItemClickListener onMenuItemClick(View view) {
        return menuItem -> {
            switch (menuItem.getItemId()) {
                case 0: //Queue this album next
                    PlayerController.queueNext(Library.getAlbumEntries(mAlbum));
                    return true;
                case 1: //Queue this album last
                    PlayerController.queueLast(Library.getAlbumEntries(mAlbum));
                    return true;
                case 2: //Go to artist
                    Navigate.to(
                            mContext,
                            ArtistActivity.class,
                            ArtistActivity.ARTIST_EXTRA,
                            Library.findArtistById(mAlbum.getArtistId()));
                    return true;
                case 3: //Add to playlist...
                    PlaylistDialog.AddToNormal.alert(view, Library.getAlbumEntries(mAlbum),
                            mContext.getString(R.string.header_add_song_name_to_playlist, mAlbum));
                    return true;
            }
            return false;
        };
    }
}
