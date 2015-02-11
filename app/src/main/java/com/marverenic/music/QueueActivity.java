package com.marverenic.music;

import android.app.Activity;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.marverenic.music.adapters.SongListAdapter;
import com.marverenic.music.utils.Themes;

public class QueueActivity extends Activity implements AdapterView.OnItemClickListener, View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Themes.setTheme(this);

        setContentView(R.layout.page_editable_list);

        ListView songListView = (ListView) findViewById(R.id.list);
        songListView.setAdapter(new SongListAdapter(Player.getInstance().getQueue(), this));
        songListView.setOnItemClickListener(this);

        Themes.themeActivity(R.layout.page_editable_list, getWindow().findViewById(android.R.id.content), this);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    public void onResume() {
        Themes.setApplicationIcon(this);
        super.onResume();
    }

    @Override
    public void onClick(View v) {
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Player.getInstance().changeSong(position);
        finish();
    }
}
