package com.marverenic.music.viewmodel;

import android.content.Context;
import android.databinding.BaseObservable;
import android.support.v7.widget.PopupMenu;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;

import com.marverenic.music.R;
import com.marverenic.music.activity.instance.ArtistActivity;
import com.marverenic.music.instances.Artist;
import com.marverenic.music.instances.Library;
import com.marverenic.music.instances.PlaylistDialog;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.utils.Navigate;

import static com.marverenic.music.activity.instance.ArtistActivity.ARTIST_EXTRA;

public class ArtistViewModel extends BaseObservable {

    private Context mContext;
    private Artist mArtist;

    public ArtistViewModel(Context context) {
        mContext = context;
    }

    public void setArtist(Artist artist) {
        mArtist = artist;
        notifyChange();
    }

    public String getName() {
        return mArtist.getArtistName();
    }

    public View.OnClickListener onClickArtist() {
        return v -> Navigate.to(mContext, ArtistActivity.class, ARTIST_EXTRA, mArtist);
    }

    public View.OnClickListener onClickMenu() {
        return v -> {
            PopupMenu menu = new PopupMenu(mContext, v, Gravity.END);

            String[] options = mContext.getResources().getStringArray(R.array.queue_options_artist);
            for (int i = 0; i < options.length;  i++) {
                menu.getMenu().add(Menu.NONE, i, i, options[i]);
            }

            menu.setOnMenuItemClickListener(onMenuItemClick(v));
            menu.show();
        };
    }

    private PopupMenu.OnMenuItemClickListener onMenuItemClick(View view) {
        return menuItem -> {
            switch (menuItem.getItemId()) {
                case 0: //Queue this artist next
                    PlayerController.queueNext(Library.getArtistSongEntries(mArtist));
                    return true;
                case 1: //Queue this artist last
                    PlayerController.queueLast(Library.getArtistSongEntries(mArtist));
                    return true;
                case 2: //Add to playlist...
                    PlaylistDialog.AddToNormal.alert(view, Library.getArtistSongEntries(mArtist),
                            mContext.getString(R.string.header_add_song_name_to_playlist, mArtist));
                    return true;
            }
            return false;
        };
    }

}
