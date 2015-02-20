package com.marverenic.music.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.marverenic.music.LibraryPageActivity;
import com.marverenic.music.NowPlayingActivity;
import com.marverenic.music.Player;
import com.marverenic.music.R;
import com.marverenic.music.instances.Album;
import com.marverenic.music.instances.Song;
import com.marverenic.music.utils.Debug;
import com.marverenic.music.utils.Navigate;
import com.marverenic.music.utils.Themes;

import java.util.ArrayList;

public class ArtistPageAdapter extends BaseAdapter implements AdapterView.OnItemLongClickListener, AdapterView.OnItemClickListener {
    public ArrayList<Song> songs;
    public ArrayList<Album> albums;
    private Context context;

    @SuppressWarnings("unchecked")
    public ArtistPageAdapter(Context context, ArrayList<Song> songs, ArrayList<Album> albums) {
        super();
        this.songs = (ArrayList<Song>) songs.clone();
        this.albums = (ArrayList<Album>) albums.clone();
        this.context = context;
    }

    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {
        View v = convertView;
        if (convertView == null) {
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.instance_song, parent, false);
        }
        Song s = songs.get(position);

        if (s != null) {
            TextView tt = (TextView) v.findViewById(R.id.textSongTitle);
            TextView tt1 = (TextView) v.findViewById(R.id.textSongDetail);
            if (tt != null) {
                tt.setText(s.songName);
                tt.setTextColor(Themes.getListText());
            }
            if (tt1 != null) {
                tt1.setText(s.artistName + " - " + s.albumName);
                tt1.setTextColor(Themes.getDetailText());
            }
        } else {
            Debug.log(Debug.WTF, "SongListAdapter", "The requested entry is null", context);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ((ListView) parent).setSelector(Themes.getTouchRipple(context));
        }

        return v;
    }

    @Override
    public int getCount() {
        if (songs != null) {
            return songs.size();
        } else return 0;
    }

    @Override
    public Object getItem(int position) {
        if (songs != null) {
            return songs.get(position);
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        return (long) position;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Player.setQueue(songs, position - ((ListView) parent).getHeaderViewsCount());
        Player.begin();

        context.startService(new Intent(context, Player.class));
        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("switchToNowPlaying", true)) {
            Navigate.to(context, NowPlayingActivity.class);
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        final Song item = songs.get(position - ((ListView) parent).getHeaderViewsCount());

        AlertDialog.Builder dialog = new AlertDialog.Builder(context);

        dialog.setTitle(item.songName)
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // There's nothing to do here
                    }
                })
                .setItems(R.array.queue_options_artist_page, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0: //Queue this song next
                                if (!Player.initialized())
                                    context.startService(new Intent(context, Player.class));
                                Player.getInstance().queueNext(item);
                                break;
                            case 1: //Queue this song last
                                if (!Player.initialized())
                                    context.startService(new Intent(context, Player.class));
                                Player.getInstance().queueLast(item);
                                break;
                            case 2: //Go to album
                                Album album;

                                Cursor curAlbum = context.getContentResolver().query(
                                        MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                                        null,
                                        MediaStore.Audio.Media.ALBUM + " =? AND " + MediaStore.Audio.Media.ARTIST + " =?",
                                        new String[]{item.albumName, item.artistName},
                                        MediaStore.Audio.Albums.ALBUM + " ASC");
                                curAlbum.moveToFirst();

                                album = new Album(
                                        curAlbum.getString(curAlbum.getColumnIndex(MediaStore.Audio.Albums._ID)),
                                        curAlbum.getString(curAlbum.getColumnIndex(MediaStore.Audio.Albums.ALBUM)),
                                        curAlbum.getString(curAlbum.getColumnIndex(MediaStore.Audio.Albums.ARTIST)),
                                        curAlbum.getString(curAlbum.getColumnIndex(MediaStore.Audio.Albums.LAST_YEAR)),
                                        curAlbum.getString(curAlbum.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART)));

                                curAlbum.close();

                                Navigate.to(context, LibraryPageActivity.class, "entry", album);
                                break;
                            default:
                                break;
                        }
                    }
                });
        dialog.show();
        return true;
    }
}
