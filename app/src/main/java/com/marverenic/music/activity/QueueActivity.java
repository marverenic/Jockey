package com.marverenic.music.activity;

import android.graphics.drawable.NinePatchDrawable;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;

import com.marverenic.music.PlayerController;
import com.marverenic.music.R;
import com.marverenic.music.instances.PlaylistDialog;
import com.marverenic.music.instances.Song;
import com.marverenic.music.instances.section.LibraryEmptyState;
import com.marverenic.music.instances.section.QueueSection;
import com.marverenic.music.utils.Themes;
import com.marverenic.music.view.EnhancedAdapters.DragBackgroundDecoration;
import com.marverenic.music.view.EnhancedAdapters.DragDividerDecoration;
import com.marverenic.music.view.EnhancedAdapters.DragDropDecoration;
import com.marverenic.music.view.EnhancedAdapters.DragDropAdapter;

import java.util.ArrayList;
import java.util.List;

public class QueueActivity extends BaseActivity {

    private final List<Song> data = new ArrayList<>();
    private int lastPlayIndex;
    private DragDropAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instance_no_miniplayer);

        data.addAll(PlayerController.getQueue());
        lastPlayIndex = PlayerController.getQueuePosition();

        RecyclerView list = (RecyclerView) findViewById(R.id.list);

        adapter = new DragDropAdapter();
        adapter.setDragSection(new QueueSection(data));
        adapter.setEmptyState(new LibraryEmptyState(this) {
            @Override
            public String getEmptyMessage() {
                return getString(R.string.empty_queue);
            }

            @Override
            public String getEmptyMessageDetail() {
                return getString(R.string.empty_queue_detail);
            }

            @Override
            public String getEmptyAction1Label() {
                return "";
            }
        });
        adapter.attach(list);

        list.addItemDecoration(new DragBackgroundDecoration(Themes.getBackgroundElevated()));
        list.addItemDecoration(new DragDividerDecoration(this, R.id.empty_layout));
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
}
