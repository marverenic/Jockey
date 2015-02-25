package com.marverenic.music.pages;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.marverenic.music.R;
import com.marverenic.music.adapters.AlbumGridAdapter;
import com.marverenic.music.adapters.ArtistPageAdapter;
import com.marverenic.music.instances.Album;
import com.marverenic.music.instances.Artist;
import com.marverenic.music.instances.Library;
import com.marverenic.music.instances.Song;
import com.marverenic.music.utils.Debug;
import com.marverenic.music.utils.Fetch;

import java.util.ArrayList;

public class ArtistPage {

    public static void onCreate(Object parent, Activity activity) {
        if (activity.getActionBar() != null) activity.getActionBar().setTitle(((Artist) parent).artistName);
        else Debug.log(Debug.LogLevel.WTF, "LibraryPageActivity", "Couldn't find the action bar", activity);

        ArrayList<Song> songs = new ArrayList<>();
        ArrayList<Album> albums = new ArrayList<>();

        Cursor cur = activity.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                null,
                MediaStore.Audio.Media.IS_MUSIC + "!= 0 AND " + MediaStore.Audio.Media.ARTIST_ID + "=?",
                new String[]{((Artist) parent).artistId + ""},
                MediaStore.Audio.Media.TITLE + " ASC");
        for (int i = 0; i < cur.getCount(); i++) {
            cur.moveToPosition(i);
            songs.add(new Song(
                    cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.TITLE)),
                    cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.ARTIST)),
                    cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.ALBUM)),
                    cur.getInt(cur.getColumnIndex(MediaStore.Audio.Media.DURATION)),
                    cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.DATA)),
                    cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID))));
        }
        cur.close();

        cur = activity.getContentResolver().query(
                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                null,
                MediaStore.Audio.Media.ARTIST_ID + "=?",
                new String[]{((Artist) parent).artistId + ""},
                MediaStore.Audio.Albums.FIRST_YEAR + " DESC, " + MediaStore.Audio.Media.ALBUM + " ASC");

        for (int i = 0; i < cur.getCount(); i++) {
            cur.moveToPosition(i);
            albums.add(new Album(
                    cur.getString(cur.getColumnIndex(MediaStore.Audio.Albums._ID)),
                    cur.getString(cur.getColumnIndex(MediaStore.Audio.Albums.ALBUM)),
                    cur.getString(cur.getColumnIndex(MediaStore.Audio.Albums.ARTIST)),
                    cur.getString(cur.getColumnIndex(MediaStore.Audio.Albums.LAST_YEAR)),
                    cur.getString(cur.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART))));
        }
        cur.close();

        songs = Library.sortSongList(songs);

        ListView list = (ListView) activity.findViewById(R.id.list);
        initializeArtistHeader(list, albums, activity);
        ArtistPageAdapter adapter = new ArtistPageAdapter(activity, songs, albums);
        list.setAdapter(adapter);
        list.setOnItemClickListener(adapter);
        list.setOnItemLongClickListener(adapter);
    }

    public static void initializeArtistHeader(final View parent, final ArrayList<Album> albums, Activity activity) {
        final Context context = activity;
        final View infoHeader = View.inflate(activity, R.layout.artist_header_info, null);

        final Handler handler = new Handler(Looper.getMainLooper());

        new Thread(new Runnable() {
            @Override
            public void run() {
                final Fetch.ArtistBio bio = Fetch.fetchArtistBio(context, albums.get(0).artistName);
                if (bio != null) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            ((ImageView) infoHeader.findViewById(R.id.artist_image)).setImageBitmap(bio.art);

                            String bioText;
                            if (!bio.tags[0].equals("")) {
                                bioText = bio.tags[0].toUpperCase().charAt(0) + bio.tags[0].substring(1);
                                if (!bio.summary.equals("")) {
                                    bioText = bioText + " - " + bio.summary;
                                }
                            } else bioText = bio.summary;

                            ((TextView) infoHeader.findViewById(R.id.artist_bio)).setText(bioText);
                        }
                    });
                } else {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            //TODO This should probably fade out
                            ((ListView) parent).removeHeaderView(infoHeader);
                        }
                    });
                }
            }
        }).start();

        ((ListView) parent).addHeaderView(infoHeader, null, false);

        final View albumHeader = View.inflate(activity, R.layout.artist_header_albums, null);
        final GridView albumGrid = (GridView) albumHeader.findViewById(R.id.albumGrid);
        AlbumGridAdapter gridAdapter = new AlbumGridAdapter(albums, context);
        albumGrid.setAdapter(gridAdapter);

        int albumCount = albums.size();

        ((ListView) parent).addHeaderView(albumHeader, null, false);

        updateArtistGridLayout((GridView) activity.findViewById(R.id.albumGrid), albumCount, activity);
        updateArtistHeader((ViewGroup) activity.findViewById(R.id.artist_bio).getParent(), activity);
    }

    public static void updateArtistGridLayout(GridView albumGrid, int albumCount, Activity activity) {
        final long screenWidth = activity.getResources().getConfiguration().screenWidthDp;
        final float density = activity.getResources().getDisplayMetrics().density;
        final long globalPadding = (long) (activity.getResources().getDimension(R.dimen.global_padding) / density);
        final long gridPadding = (long) (activity.getResources().getDimension(R.dimen.grid_padding) / density);
        final long extraHeight = 60;
        final long minWidth = (long) (activity.getResources().getDimension(R.dimen.grid_width) / density);

        long availableWidth = screenWidth - 2 * (globalPadding + gridPadding);
        double numColumns = (availableWidth + gridPadding) / (minWidth + gridPadding);

        long columnWidth = (long) Math.floor(availableWidth / numColumns);
        long rowHeight = columnWidth + extraHeight;

        long numRows = (long) Math.ceil(albumCount / numColumns);

        long gridHeight = rowHeight * numRows + 2 * gridPadding;

        int height = (int) ((gridHeight * density));

        ViewGroup.LayoutParams albumParams = albumGrid.getLayoutParams();
        albumParams.height = height;
        albumGrid.setLayoutParams(albumParams);
    }

    public static void updateArtistHeader(final ViewGroup bioHolder, Activity activity) {
        final TextView bioText = (TextView) bioHolder.findViewById(R.id.artist_bio);

        final long viewHeight = (long) (activity.getResources().getDimension(R.dimen.artist_image_height));
        final long padding = (long) (activity.getResources().getDimension(R.dimen.list_margin));

        final long availableHeight = (long) Math.floor(viewHeight - 2 * padding);

        long maxLines = (long) Math.floor(availableHeight / (bioText.getLineHeight()));
        bioText.setMaxLines((int) maxLines);
    }

}
