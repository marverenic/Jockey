package com.marverenic.music.pages;

import android.app.Activity;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;

import com.marverenic.music.R;
import com.marverenic.music.instances.Album;
import com.marverenic.music.instances.Song;
import com.marverenic.music.utils.Debug;
import com.marverenic.music.utils.Fetch;

import java.util.ArrayList;

public class AlbumPage {

    public static void onCreate(Object parent, ArrayList<Song> songEntries, ListView songListView, Activity activity) {
        if (activity.getActionBar() != null) activity.getActionBar().setTitle(((Album) parent).albumName);
        else Debug.log(Debug.LogLevel.WTF, "LibraryPageActivity", "Couldn't find the action bar", activity);

        Cursor cur = activity.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                new String[]{
                        MediaStore.Audio.Media.TITLE,
                        MediaStore.Audio.Media.ARTIST,
                        MediaStore.Audio.Media.ALBUM,
                        MediaStore.Audio.Media.DURATION,
                        MediaStore.Audio.Media.DATA,
                        MediaStore.Audio.Media.ALBUM_ID},
                MediaStore.Audio.Media.IS_MUSIC + "!= 0 AND " + MediaStore.Audio.Media.ALBUM_ID + "=?",
                new String[]{((Album) parent).albumId},
                MediaStore.Audio.Media.TRACK);
        cur.moveToFirst();

        for (int i = 0; i < cur.getCount(); i++) {
            cur.moveToPosition(i);
            songEntries.add(new Song(
                    cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.TITLE)),
                    cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.ARTIST)),
                    cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.ALBUM)),
                    cur.getInt(cur.getColumnIndex(MediaStore.Audio.Media.DURATION)),
                    cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.DATA)),
                    cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID))));
        }

        cur.close();

        Bitmap art = Fetch.fetchAlbumArtLocal(activity, ((Album) parent).albumId);

        if (art != null) {
            View artView = View.inflate(activity, R.layout.album_header, null);
            songListView.addHeaderView(artView, null, false);
            ((ImageView) activity.findViewById(R.id.header)).setImageBitmap(art);
        }
    }

}
