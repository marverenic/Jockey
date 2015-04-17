package com.marverenic.music.activity;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.marverenic.music.R;
import com.marverenic.music.adapters.AlbumGridAdapter;
import com.marverenic.music.adapters.ArtistPageAdapter;
import com.marverenic.music.adapters.SongListAdapter;
import com.marverenic.music.instances.Album;
import com.marverenic.music.instances.Artist;
import com.marverenic.music.instances.Genre;
import com.marverenic.music.instances.Library;
import com.marverenic.music.instances.LibraryScanner;
import com.marverenic.music.instances.Song;
import com.marverenic.music.utils.Debug;
import com.marverenic.music.utils.Fetch;
import com.marverenic.music.utils.Navigate;
import com.marverenic.music.utils.Themes;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class LibraryPageActivity extends BaseActivity {

    public enum Type { ARTIST, ALBUM, GENRE, UNKNOWN }

    private Type type;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Object parent = getIntent().getParcelableExtra("entry");
        if (parent == null || !(parent instanceof Album || parent instanceof Artist || parent instanceof Genre)){
            setContentLayout(R.layout.page_error);
        }
        else{
            setContentLayout(R.layout.fragment_list_page);
        }
        setContentView(R.id.list_container);
        super.onCreate(savedInstanceState);

        if (parent != null) {
            final ListView songListView = (ListView) findViewById(R.id.list);
            ArrayList<Song> songEntries = null;
            ArrayList<Album> albumEntries;

            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(parent.toString());
            } else {
                Debug.log(Debug.LogLevel.WTF, "LibraryPageActivity", "Couldn't find the action bar", this);
            }

            if (parent instanceof Album) {
                type = Type.ALBUM;
                songEntries = LibraryScanner.getAlbumEntries((Album) parent);

                Bitmap art = Fetch.fetchAlbumArtLocal(((Album) parent).albumId);

                if (art != null) {
                    View artView = View.inflate(this, R.layout.album_header, null);
                    songListView.addHeaderView(artView, null, false);
                    ((ImageView) findViewById(R.id.header)).setImageBitmap(art);
                }
            } else if (parent instanceof Genre) {
                type = Type.GENRE;
                songEntries = LibraryScanner.getGenreEntries((Genre) parent);
            } else if (parent instanceof Artist) {
                type = Type.ARTIST;
                songEntries = LibraryScanner.getArtistSongEntries((Artist) parent);
                Library.sortSongList(songEntries);
                albumEntries = LibraryScanner.getArtistAlbumEntries((Artist) parent);
                Library.sortAlbumList(albumEntries);

                ListView list = (ListView) findViewById(R.id.list);
                initializeArtistHeader(list, albumEntries, ((Artist) parent).artistName, this);
                ArtistPageAdapter adapter = new ArtistPageAdapter(this, songEntries, albumEntries);
                list.setAdapter(adapter);
                list.setOnItemClickListener(adapter);
            }

            if (type != Type.ARTIST && songEntries != null) {
                SongListAdapter adapter;

                // Don't sort album entries
                if (type != Type.ALBUM) {
                    Library.sortSongList(songEntries);
                    adapter = new SongListAdapter(songEntries, this, true);
                } else {
                    adapter = new SongListAdapter(songEntries, this, false);
                }

                songListView.setAdapter(adapter);
                songListView.setOnItemClickListener(adapter);
            }
        } else {
            type = Type.UNKNOWN;
            setContentView(R.layout.page_error);
            Debug.log(Debug.LogLevel.WTF, "LibraryPageActivity", "An invalid item was passed as the parent object", this);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    private static void initializeArtistHeader(final View parent, ArrayList<Album> albums, final String artistName, Activity activity) {
        final Context context = activity;
        final View infoHeader = View.inflate(activity, R.layout.artist_header_info, null);

        final Handler handler = new Handler(Looper.getMainLooper());

        new Thread(new Runnable() {
            @Override
            public void run() {
                final Fetch.ArtistBio bio = Fetch.fetchArtistBio(context, artistName);
                if (bio != null) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if(bio.artURL != null && !bio.artURL.equals(""))
                                Picasso.with(context).load(bio.artURL).placeholder(R.drawable.art_default)
                                        .resizeDimen(R.dimen.grid_art_size, R.dimen.grid_art_size)
                                        .centerCrop()
                                        .into(((ImageView) infoHeader.findViewById(R.id.artist_image)));

                            String bioText;
                            if (!bio.tags[0].equals("")) {
                                bioText = bio.tags[0].toUpperCase().charAt(0) + bio.tags[0].substring(1);
                                if (!bio.summary.equals("")) {
                                    bioText = bioText + " - " + bio.summary;
                                }
                            } else bioText = bio.summary;

                            TextView bioTextView = ((TextView) infoHeader.findViewById(R.id.artist_bio));
                            bioTextView.setText(bioText);
                            ObjectAnimator bioTextAnimator = ObjectAnimator.ofObject(bioTextView,
                                    "textColor",
                                    new ArgbEvaluator(),
                                    Color.TRANSPARENT,
                                    Themes.getDetailText());
                            bioTextAnimator.setDuration(300).start();
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

    private static void updateArtistGridLayout(GridView albumGrid, int albumCount, Activity activity) {
        final short screenWidth = (short) activity.getResources().getConfiguration().screenWidthDp;
        final float density = activity.getResources().getDisplayMetrics().density;
        final short globalPadding = (short) (activity.getResources().getDimension(R.dimen.global_padding) / density);
        final short gridPadding = (short) (activity.getResources().getDimension(R.dimen.grid_padding) / density);
        final short scrollbarPadding = 32;
        final short extraHeight = (short) (4 * gridPadding
                + (activity.getResources().getDimension(R.dimen.grid_text_header_size) / density)
                + (activity.getResources().getDimension(R.dimen.grid_text_detail_size) / density));
        final short minWidth = (short) (activity.getResources().getDimension(R.dimen.grid_width) / density);

        short availableWidth = (short) (screenWidth - 2 * (globalPadding + gridPadding) - scrollbarPadding);
        double numColumns = (availableWidth + gridPadding) / (minWidth + gridPadding);

        short columnWidth = (short) Math.floor(availableWidth / numColumns);
        short rowHeight = (short) (columnWidth + extraHeight);

        short numRows = (short) Math.ceil(albumCount / numColumns);

        short gridHeight = (short) (rowHeight * numRows + 2 * gridPadding);

        int height = (int) ((gridHeight * density));

        ViewGroup.LayoutParams albumParams = albumGrid.getLayoutParams();
        albumParams.height = height;
        albumGrid.setLayoutParams(albumParams);
    }

    private static void updateArtistHeader(final ViewGroup bioHolder, Activity activity) {
        final TextView bioText = (TextView) bioHolder.findViewById(R.id.artist_bio);

        final long viewHeight = (long) (activity.getResources().getDimension(R.dimen.artist_image_height));
        final long padding = (long) (activity.getResources().getDimension(R.dimen.list_margin));

        final long availableHeight = (long) Math.floor(viewHeight - 2 * padding);

        long maxLines = (long) Math.floor(availableHeight / (bioText.getLineHeight()));
        bioText.setMaxLines((int) maxLines);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Navigate.up(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void themeActivity() {
        super.themeActivity();

        findViewById(R.id.list).setBackgroundColor(Themes.getBackgroundElevated());

        LayerDrawable backgroundDrawable = (LayerDrawable) getResources().getDrawable(R.drawable.header_frame);
        GradientDrawable bodyDrawable = ((GradientDrawable) backgroundDrawable.findDrawableByLayerId(R.id.body));
        GradientDrawable topDrawable = ((GradientDrawable) backgroundDrawable.findDrawableByLayerId(R.id.top));
        bodyDrawable.setColor(Themes.getBackground());
        topDrawable.setColor(Themes.getPrimary());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            findViewById(R.id.list_container).setBackground(backgroundDrawable);
        }
        else {
            findViewById(R.id.list_container).setBackgroundDrawable(backgroundDrawable);
        }
    }
}
