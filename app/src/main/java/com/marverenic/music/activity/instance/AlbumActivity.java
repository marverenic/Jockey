package com.marverenic.music.activity.instance;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.marverenic.music.instances.Library;
import com.marverenic.music.R;
import com.marverenic.music.activity.BaseActivity;
import com.marverenic.music.instances.Album;
import com.marverenic.music.instances.Song;
import com.marverenic.music.instances.viewholder.SongViewHolder;
import com.marverenic.music.utils.Themes;
import com.marverenic.music.view.BackgroundDecoration;
import com.marverenic.music.view.DividerDecoration;

import java.util.ArrayList;

public class AlbumActivity extends BaseActivity {

    public static final String ALBUM_EXTRA = "album";
    private ArrayList<Song> data;
    private Album reference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instance_artwork);

        reference = getIntent().getParcelableExtra(ALBUM_EXTRA);
        if (reference != null) {
            data = Library.getAlbumEntries(reference);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(reference.getAlbumName());
            }
        } else {
            data = new ArrayList<>();
        }

        Glide.with(this).load(reference.getArtUri())
                .centerCrop()
                .into((ImageView) findViewById(R.id.backdrop));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }

        RecyclerView songRecyclerView = (RecyclerView) findViewById(R.id.list);
        songRecyclerView.setAdapter(new Adapter());
        songRecyclerView.addItemDecoration(new BackgroundDecoration(Themes.getBackgroundElevated()));
        songRecyclerView.addItemDecoration(new DividerDecoration(this));

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        songRecyclerView.setLayoutManager(layoutManager);
    }

    public class Adapter extends RecyclerView.Adapter<SongViewHolder>{

        @Override
        public SongViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            return new SongViewHolder(
                    LayoutInflater.from(viewGroup.getContext())
                            .inflate(R.layout.instance_song, viewGroup, false),
                    data);
        }

        @Override
        public void onBindViewHolder(SongViewHolder viewHolder, int i) {
            viewHolder.update(data.get(i), i);
        }

        @Override
        public int getItemCount() {
            return data.size();
        }
    }
}
