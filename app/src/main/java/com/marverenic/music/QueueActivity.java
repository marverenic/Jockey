package com.marverenic.music;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.marverenic.music.adapters.QueueEditAdapter;
import com.marverenic.music.fragments.MiniplayerManager;
import com.marverenic.music.instances.Song;
import com.marverenic.music.utils.Navigate;
import com.marverenic.music.utils.Themes;
import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;

import java.util.ArrayList;

public class QueueActivity extends Activity implements AdapterView.OnItemClickListener, View.OnClickListener {

    private BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            update();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Themes.setTheme(this);

        setContentView(R.layout.page_editable_list);

        Themes.themeActivity(R.layout.page_editable_list, getWindow().findViewById(android.R.id.content), this);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        final Context context = this;
        final QueueEditAdapter adapter = new QueueEditAdapter(this);
        final DragSortListView listView = (DragSortListView) findViewById(R.id.list);
        DragSortController controller = new QueueEditAdapter.dragSortController(listView, adapter, R.id.handle);
        listView.setOnItemClickListener(this);
        listView.setAdapter(adapter);
        listView.setFloatViewManager(controller);
        listView.setOnTouchListener(controller);
        listView.setDragEnabled(true);
        listView.setDropListener(new DragSortListView.DropListener() {
            @Override
            public void drop(int from, int to) {
                ArrayList<Song> data = adapter.move(from, to);
                adapter.notifyDataSetChanged();
                listView.invalidateViews();

                if (PlayerService.getPosition() == from){
                    // If the current song was moved in the queue
                    PlayerService.changeQueue(context, data, to);
                }
                else if (PlayerService.getPosition() < from && PlayerService.getPosition() >= to){
                    // If a song that was after the current playing song was moved to a position before the current song...
                    PlayerService.changeQueue(context, data, PlayerService.getPosition() + 1);
                }
                else if (PlayerService.getPosition() > from && PlayerService.getPosition() <= to){
                    // If a song that was before the current playing song was moved to a position after the current song...
                    PlayerService.changeQueue(context, data, PlayerService.getPosition() - 1);
                }
                else{
                    // If the number of songs before and after the currently playing song hasn't changed...
                    PlayerService.changeQueue(context, data, PlayerService.getPosition());
                }
            }
        });

        MiniplayerManager.hide(this, R.id.list);
        registerReceiver(updateReceiver, new IntentFilter(Player.UPDATE_BROADCAST));
    }

    private void update (){
        ((ListView) findViewById(R.id.list)).invalidateViews();
    }

    @Override
    public void onDestroy() {
        try {
            unregisterReceiver(updateReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    @Override
    public void onResume() {
        Themes.setApplicationIcon(this);
        super.onResume();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Navigate.up(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Navigate.back(this);
    }

    @Override
    public void onClick(View v) {
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        PlayerService.changeSong(position);
        Navigate.back(this);
    }
}
