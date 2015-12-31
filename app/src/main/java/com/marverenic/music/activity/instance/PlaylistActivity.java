package com.marverenic.music.activity.instance;

import android.graphics.drawable.NinePatchDrawable;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemAdapter;
import com.h6ah4i.android.widget.advrecyclerview.draggable.ItemDraggableRange;
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager;
import com.marverenic.music.Library;
import com.marverenic.music.R;
import com.marverenic.music.activity.BaseActivity;
import com.marverenic.music.instances.AutoPlaylist;
import com.marverenic.music.instances.Playlist;
import com.marverenic.music.instances.Song;
import com.marverenic.music.instances.viewholder.DraggableSongViewHolder;
import com.marverenic.music.instances.viewholder.EmptyStateViewHolder;
import com.marverenic.music.instances.viewholder.SongViewHolder;
import com.marverenic.music.utils.Themes;
import com.marverenic.music.view.BackgroundDecoration;
import com.marverenic.music.view.DividerDecoration;

import java.util.ArrayList;
import java.util.Collections;

public class PlaylistActivity extends BaseActivity implements PopupMenu.OnMenuItemClickListener {

    public static final String PLAYLIST_EXTRA = "playlist";
    private ArrayList<Song> data;
    private Playlist reference;
    private Adapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instance);

        reference = getIntent().getParcelableExtra(PLAYLIST_EXTRA);

        if (reference != null) {
            data = Library.getPlaylistEntries(this, reference);
            if (getSupportActionBar() != null)
                getSupportActionBar().setTitle(reference.playlistName);
        } else {
            data = new ArrayList<>();
        }

        RecyclerView list = (RecyclerView) findViewById(R.id.list);

        RecyclerViewDragDropManager dragDropManager = new RecyclerViewDragDropManager();
        this.adapter = new Adapter();
        RecyclerView.Adapter adapter = dragDropManager.createWrappedAdapter(this.adapter);

        //noinspection deprecation
        dragDropManager.setDraggingItemShadowDrawable(
                (NinePatchDrawable) getResources().getDrawable(
                        (Themes.isLight(this))
                                ? R.drawable.list_drag_shadow_light
                                : R.drawable.list_drag_shadow_dark));

        list.setAdapter(adapter);
        list.addItemDecoration(new BackgroundDecoration(Themes.getBackgroundElevated()));
        list.addItemDecoration(new DividerDecoration(this));

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        list.setLayoutManager(layoutManager);

        dragDropManager.attachRecyclerView(list);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.activity_playlist, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (reference == null) {
            return super.onOptionsItemSelected(item);
        }

        if (item.getItemId() == R.id.action_sort) {
            PopupMenu sortMenu = new PopupMenu(this, findViewById(R.id.action_sort), Gravity.END);
            sortMenu.inflate(
                    (reference instanceof AutoPlaylist)
                            ? R.menu.sort_options_auto_playlist
                            : R.menu.sort_options);
            sortMenu.setOnMenuItemClickListener(this);
            sortMenu.show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        final ArrayList<Song> unsortedData = new ArrayList<>(data);
        int sortFlag = -1;
        String result;

        switch (item.getItemId()){
            case R.id.action_sort_random:
                Collections.shuffle(data);
                result = getResources().getString(R.string.message_sorted_playlist_random);
                sortFlag = AutoPlaylist.Rule.Field.ID;
                break;
            case R.id.action_sort_name:
                Collections.sort(data);
                result = getResources().getString(R.string.message_sorted_playlist_name);
                sortFlag = AutoPlaylist.Rule.Field.NAME;
                break;
            case R.id.action_sort_artist:
                Collections.sort(data, Song.ARTIST_COMPARATOR);
                result = getResources().getString(R.string.message_sorted_playlist_artist);
                break;
            case R.id.action_sort_album:
                Collections.sort(data, Song.ALBUM_COMPARATOR);
                result = getResources().getString(R.string.message_sorted_playlist_album);
                break;
            case R.id.action_sort_play:
                Collections.sort(data, Song.PLAY_COUNT_COMPARATOR);
                result = getResources().getString(R.string.message_sorted_playlist_play);
                sortFlag = AutoPlaylist.Rule.Field.PLAY_COUNT;
                break;
            case R.id.action_sort_skip:
                Collections.sort(data, Song.SKIP_COUNT_COMPARATOR);
                result = getResources().getString(R.string.message_sorted_playlist_skip);
                sortFlag = AutoPlaylist.Rule.Field.SKIP_COUNT;
                break;
            case R.id.action_sort_date_added:
                Collections.sort(data, Song.DATE_ADDED_COMPARATOR);
                result = getResources().getString(R.string.message_sorted_playlist_date_added);
                sortFlag = AutoPlaylist.Rule.Field.DATE_ADDED;
                break;
            case R.id.action_sort_date_played:
                Collections.sort(data, Song.DATE_PLAYED_COMPARATOR);
                result = getResources().getString(R.string.message_sorted_playlist_date_played);
                sortFlag = AutoPlaylist.Rule.Field.DATE_PLAYED;
                break;
            default:
                return false;
        }

        if (reference instanceof AutoPlaylist) {
            ((AutoPlaylist) reference).sortMethod = sortFlag;
            Library.editAutoPlaylist(this, (AutoPlaylist) reference);
        } else {
            Library.editPlaylist(this, reference, data);
        }

        adapter.notifyDataSetChanged();

        Snackbar
                .make(
                        findViewById(R.id.list),
                        String.format(result, reference),
                        Snackbar.LENGTH_LONG)
                .setAction(
                        getResources().getString(R.string.action_undo),
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                data = unsortedData;
                                Library.editPlaylist(PlaylistActivity.this, reference, unsortedData);
                                adapter.notifyDataSetChanged();
                            }
                        })
                .show();

        return true;
    }

    public class Adapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements DraggableItemAdapter<DraggableSongViewHolder>, SongViewHolder.OnRemovedListener {

        public static final int EMPTY = 0;
        public static final int SONG = 1;
        public static final int AUTO_SONG = 2;

        public Adapter(){
            setHasStableIds(true);
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            switch (viewType) {
                case EMPTY:
                    return new EmptyStateViewHolder(
                            LayoutInflater
                                    .from(viewGroup.getContext())
                                    .inflate(R.layout.instance_empty, viewGroup, false),
                            PlaylistActivity.this);
                case AUTO_SONG:
                    return new SongViewHolder(
                            LayoutInflater
                                    .from(viewGroup.getContext())
                                    .inflate(R.layout.instance_song, viewGroup, false),
                            data);
                case SONG:
                default:
                    DraggableSongViewHolder vh = new DraggableSongViewHolder(
                            LayoutInflater.from(viewGroup.getContext())
                                    .inflate(R.layout.instance_song_drag, viewGroup, false),
                            data);
                    if (!(reference instanceof AutoPlaylist)) {
                        vh.setPlaylist(reference, this);
                    }

                    return vh;
            }
        }

        @Override
        public long getItemId(int position){
            if (data.isEmpty()) return 0;
            return data.get(position).songId;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
            if (getItemViewType(position) == SONG || getItemViewType(position) == AUTO_SONG) {
                ((SongViewHolder) viewHolder).update(data.get(position), position);

            } else if (viewHolder instanceof EmptyStateViewHolder &&
                    Library.hasRWPermission(PlaylistActivity.this)) {
                if (reference instanceof AutoPlaylist){
                    EmptyStateViewHolder emptyHolder = ((EmptyStateViewHolder) viewHolder);
                    emptyHolder.setReason(R.string.empty_auto_playlist);
                    emptyHolder.setDetail(R.string.empty_auto_playlist_detail);
                }
                else {
                    EmptyStateViewHolder emptyHolder = ((EmptyStateViewHolder) viewHolder);
                    emptyHolder.setReason(R.string.empty_playlist);
                    emptyHolder.setDetail(R.string.empty_playlist_detail);
                }
            }
        }

        @Override
        public int getItemViewType(int position){
            if (data.isEmpty()) return EMPTY;
            return (reference instanceof AutoPlaylist)? AUTO_SONG : SONG;
        }

        @Override
        public int getItemCount() {
            return (data.isEmpty())? 1 : data.size();
        }

        @Override
        public boolean onCheckCanStartDrag(DraggableSongViewHolder viewHolder, int position, int x, int y){
            final View containerView = viewHolder.itemView;
            final View dragHandleView = viewHolder.dragHandle;

            final int offsetX =(int) (ViewCompat.getTranslationX(containerView) + 0.5f);

            final int tx = (int) (ViewCompat.getTranslationX(dragHandleView) + 0.5f);
            final int left = dragHandleView.getLeft() + tx;
            final int right = dragHandleView.getRight() + tx;

            return (x - offsetX >= left) && (x - offsetX <= right);
        }

        @Override
        public ItemDraggableRange onGetItemDraggableRange(DraggableSongViewHolder songViewHolder, int position) {
            return null;
        }

        @Override
        public void onMoveItem(int from, int to) {
            if (from == to) return;

            data.add(to, data.remove(from));
            Library.editPlaylist(PlaylistActivity.this, reference, data);
        }

        @Override
        public void onSongRemoved(View view, final Song song) {
            RecyclerView recyclerView = (RecyclerView) view.getParent();
            final int position = recyclerView.getChildAdapterPosition(view);

            data.remove(position);

            Library.editPlaylist(PlaylistActivity.this, reference, data);
            notifyItemRemoved(position);

            Snackbar
                    .make(
                            view,
                            getResources().getString(R.string.message_removed_song, song.songName),
                            Snackbar.LENGTH_LONG)
                    .setAction(R.string.action_undo, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            data.add(position, song);
                            Library.editPlaylist(PlaylistActivity.this, reference, data);
                            if (data.size() > 1) {
                                notifyItemInserted(position);
                            } else {
                                notifyItemChanged(position);
                            }
                        }
                    })
                    .show();
        }
    }

}
