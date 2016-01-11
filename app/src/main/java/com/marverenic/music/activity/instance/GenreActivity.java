package com.marverenic.music.activity.instance;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.marverenic.music.R;
import com.marverenic.music.activity.BaseActivity;
import com.marverenic.music.instances.Genre;
import com.marverenic.music.instances.Library;
import com.marverenic.music.instances.Song;
import com.marverenic.music.instances.viewholder.SongViewHolder;
import com.marverenic.music.utils.Themes;
import com.marverenic.music.view.BackgroundDecoration;
import com.marverenic.music.view.DividerDecoration;

import java.util.ArrayList;

public class GenreActivity extends BaseActivity {

    public static final String GENRE_EXTRA = "album";
    private ArrayList<Song> data;
    private Genre reference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instance);

        reference = getIntent().getParcelableExtra(GENRE_EXTRA);

        if (reference != null) {
            data = Library.getGenreEntries(reference);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(reference.getGenreName());
            }
        } else {
            data = new ArrayList<>();
        }

        RecyclerView list = (RecyclerView) findViewById(R.id.list);
        list.setAdapter(new Adapter());
        list.addItemDecoration(new BackgroundDecoration(Themes.getBackgroundElevated()));
        list.addItemDecoration(new DividerDecoration(this));

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        list.setLayoutManager(layoutManager);
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
