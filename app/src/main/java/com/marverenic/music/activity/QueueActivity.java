package com.marverenic.music.activity;

import android.graphics.drawable.NinePatchDrawable;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemAdapter;
import com.h6ah4i.android.widget.advrecyclerview.draggable.ItemDraggableRange;
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager;
import com.marverenic.music.PlayerController;
import com.marverenic.music.R;
import com.marverenic.music.instances.PlaylistDialog;
import com.marverenic.music.instances.Song;
import com.marverenic.music.instances.viewholder.QueueSongViewHolder;
import com.marverenic.music.utils.Themes;
import com.marverenic.music.view.BackgroundDecoration;
import com.marverenic.music.view.DividerDecoration;

import java.util.List;

public class QueueActivity extends BaseActivity {

    private List<Song> data;
    private Adapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instance_no_miniplayer);

        data = PlayerController.getQueue();

        RecyclerView list = (RecyclerView) findViewById(R.id.list);

        adapter = new Adapter();
        RecyclerViewDragDropManager dragDropManager = new RecyclerViewDragDropManager();
        RecyclerView.Adapter wrappedAdapter = dragDropManager.createWrappedAdapter(adapter);

        //noinspection deprecation
        dragDropManager.setDraggingItemShadowDrawable(
                (NinePatchDrawable) getResources().getDrawable(
                        (Themes.isLight(this))
                                ? R.drawable.list_drag_shadow_light
                                : R.drawable.list_drag_shadow_dark));

        list.setAdapter(wrappedAdapter);
        list.addItemDecoration(new BackgroundDecoration(Themes.getBackgroundElevated()));
        list.addItemDecoration(new DividerDecoration(this));

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        layoutManager.scrollToPosition(PlayerController.getQueuePosition());
        list.setLayoutManager(layoutManager);

        dragDropManager.attachRecyclerView(list);
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
        data = PlayerController.getQueue();
        adapter.notifyDataSetChanged();
    }

    @Override
    public void updateMiniplayer() {

    }

    public class Adapter extends RecyclerView.Adapter<QueueSongViewHolder>
            implements DraggableItemAdapter<QueueSongViewHolder>,
            QueueSongViewHolder.OnRemovedListener {

        private static final int REGULAR_VIEW = 0;
        private static final int HIGHLIT_VIEW = 1;

        public Adapter() {
            setHasStableIds(true);
        }

        @Override
        public QueueSongViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            return new QueueSongViewHolder(
                    QueueActivity.this,
                    LayoutInflater
                            .from(viewGroup.getContext())
                            .inflate(
                                    (viewType == HIGHLIT_VIEW)
                                            ? R.layout.instance_song_drag_highlight
                                            : R.layout.instance_song_drag,
                                    viewGroup,
                                    false),
                    this);
        }

        @Override
        public int getItemViewType(int position) {
            if (position == PlayerController.getQueuePosition()) {
                return HIGHLIT_VIEW;
            }
            return REGULAR_VIEW;
        }

        @Override
        public long getItemId(int position) {
            return data.get(position).getSongId();
        }

        @Override
        public void onBindViewHolder(QueueSongViewHolder viewHolder, int i) {
            viewHolder.update(data.get(i), i);
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        @Override
        public boolean onCheckCanStartDrag(QueueSongViewHolder viewHolder, int position,
                                           int x, int y) {
            final View containerView = viewHolder.getItemView();
            final View dragHandleView = viewHolder.getDragHandle();

            final int offsetX = (int) (ViewCompat.getTranslationX(containerView) + 0.5f);

            final int tx = (int) (ViewCompat.getTranslationX(dragHandleView) + 0.5f);
            final int left = dragHandleView.getLeft() + tx;
            final int right = dragHandleView.getRight() + tx;

            return (x - offsetX >= left) && (x - offsetX <= right);
        }

        @Override
        public ItemDraggableRange onGetItemDraggableRange(QueueSongViewHolder viewHolder,
                                                          int position) {
            return null;
        }

        @Override
        public void onMoveItem(int from, int to) {
            if (from == to) return;

            // Calculate where the current song index is moving to
            final int nowPlayingIndex = data.indexOf(PlayerController.getNowPlaying());
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

            // Modify the Array and push the change to the service
            data.add(to, data.remove(from));
            PlayerController.editQueue(data, futureNowPlayingIndex);
        }

        @Override
        public void onItemRemoved(int index) {
            data.remove(index);
            notifyDataSetChanged();
        }
    }

}
