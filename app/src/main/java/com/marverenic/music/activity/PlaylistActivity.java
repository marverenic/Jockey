package com.marverenic.music.activity;

import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ListView;

import com.marverenic.music.R;
import com.marverenic.music.adapters.PlaylistEditAdapter;
import com.marverenic.music.instances.LibraryScanner;
import com.marverenic.music.instances.Playlist;
import com.marverenic.music.utils.Themes;
import com.mobeta.android.dslv.DragSortListView;

public class PlaylistActivity extends BaseActivity{

    public static final String PLAYLIST_ENTRY = "playlist_entry";
    private Playlist playlist;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setContentLayout(R.layout.page_editable_list);
        setContentView(R.id.list);
        super.onCreate(savedInstanceState);

        Themes.setTheme(this);

        final Object parent = getIntent().getParcelableExtra(PLAYLIST_ENTRY);

        if (parent instanceof Playlist) {
            playlist = (Playlist) parent;

            getSupportActionBar().setTitle(playlist.playlistName);
        }

        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    public void onResume() {
        // Recreate the adapter in case the playlist has been edited since this activity was paused
        // The adapter will initialize attach itself and all necessary controllers in its constructor
        // There is no need to create a variable for it
        if (playlist != null) new PlaylistEditAdapter(
                LibraryScanner.getPlaylistEntries(this, playlist),
                playlist,
                this,
                (DragSortListView) findViewById(R.id.list));

        super.onResume();
    }

    @Override
    public void themeActivity() {
        super.themeActivity();

        findViewById(R.id.list).setBackgroundColor(Themes.getBackgroundElevated());

        ListView list = (ListView) findViewById(R.id.list);
        list.setDividerHeight((int) getResources().getDisplayMetrics().density);

        LayerDrawable backgroundDrawable = (LayerDrawable) getResources().getDrawable(R.drawable.header_frame);
        GradientDrawable bodyDrawable = ((GradientDrawable) backgroundDrawable.findDrawableByLayerId(R.id.body));
        GradientDrawable topDrawable = ((GradientDrawable) backgroundDrawable.findDrawableByLayerId(R.id.top));
        bodyDrawable.setColor(Themes.getBackground());
        topDrawable.setColor(Themes.getPrimary());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            ((ViewGroup) findViewById(R.id.list).getParent()).setBackground(backgroundDrawable);
        }
        else {
            ((ViewGroup) findViewById(R.id.list).getParent()).setBackgroundDrawable(backgroundDrawable);
        }
    }

}
