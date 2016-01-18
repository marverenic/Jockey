package com.marverenic.music.activity;

import android.graphics.drawable.NinePatchDrawable;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;

import com.marverenic.music.PlayerController;
import com.marverenic.music.R;
import com.marverenic.music.instances.PlaylistDialog;
import com.marverenic.music.instances.Song;
import com.marverenic.music.instances.viewholder.DragDropSongViewHolder;
import com.marverenic.music.instances.viewholder.QueueSongViewHolder;
import com.marverenic.music.utils.Themes;
import com.marverenic.music.view.EnhancedAdapters.DragBackgroundDecoration;
import com.marverenic.music.view.EnhancedAdapters.DragDividerDecoration;
import com.marverenic.music.view.EnhancedAdapters.DragDropAdapter;
import com.marverenic.music.view.EnhancedAdapters.DragDropDecoration;
import com.marverenic.music.view.EnhancedAdapters.EnhancedViewHolder;

import java.util.ArrayList;
import java.util.List;

public class QueueActivity extends BaseActivity implements DragDropAdapter.ViewSupplier<Song>,
        DragDropAdapter.OnItemMovedListener, DragDropSongViewHolder.OnRemovedListener {

    private final List<Song> data = new ArrayList<>();
    private int lastPlayIndex;
    private DragDropAdapter<Song> adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instance_no_miniplayer);

        data.addAll(PlayerController.getQueue());
        lastPlayIndex = PlayerController.getQueuePosition();

        RecyclerView list = (RecyclerView) findViewById(R.id.list);

        adapter = new DragDropAdapter<>(data, this, this);
        adapter.attach(list);

        list.addItemDecoration(new DragBackgroundDecoration(Themes.getBackgroundElevated()));
        list.addItemDecoration(new DragDividerDecoration(this));
        list.addItemDecoration(new DragDropDecoration((NinePatchDrawable) getDrawableCompat(
                (Themes.isLight(this))
                        ? R.drawable.list_drag_shadow_light
                        : R.drawable.list_drag_shadow_dark)));

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        layoutManager.scrollToPosition(PlayerController.getQueuePosition());
        list.setLayoutManager(layoutManager);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_queue, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.save:
                PlaylistDialog.MakeNormal.alert(
                        findViewById(R.id.list),
                        PlayerController.getQueue());
                return true;
            case R.id.add_to_playlist:
                PlaylistDialog.AddToNormal.alert(
                        findViewById(R.id.list),
                        PlayerController.getQueue(),
                        R.string.header_add_queue_to_playlist);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        adapter.notifyItemChanged(lastPlayIndex);
        lastPlayIndex = PlayerController.getQueuePosition();
        adapter.notifyItemChanged(lastPlayIndex);
    }

    @Override
    public void updateMiniplayer() {

    }

    @Override
    public void onItemMoved(int from, int to) {
        if (from == to) return;

        // Calculate where the current song index is moving to
        final int nowPlayingIndex = PlayerController.getQueuePosition();
        int futureNowPlayingIndex;

        if (from == nowPlayingIndex) {
            futureNowPlayingIndex = to;
        } else if (from < nowPlayingIndex && to >= nowPlayingIndex) {
            futureNowPlayingIndex = nowPlayingIndex - 1;
        } else if (from > nowPlayingIndex && to <= nowPlayingIndex) {
            futureNowPlayingIndex = nowPlayingIndex + 1;
        } else {
            futureNowPlayingIndex = nowPlayingIndex;
        }

        // Push the change to the service
        PlayerController.editQueue(data, futureNowPlayingIndex);
    }

    @Override
    public EnhancedViewHolder<Song> createViewHolder(ViewGroup parent) {
        return new QueueSongViewHolder(
                LayoutInflater.from(this).inflate(R.layout.instance_song_queue, parent, false),
                this);
    }

    @Override
    public int getHandleId() {
        return R.id.handle;
    }

    @Override
    public void onItemRemoved(int index) {
        data.remove(index);
        adapter.notifyItemRemoved(index);
    }
}
