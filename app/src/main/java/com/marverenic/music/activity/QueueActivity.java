package com.marverenic.music.activity;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemAdapter;
import com.h6ah4i.android.widget.advrecyclerview.draggable.ItemDraggableRange;
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager;
import com.marverenic.music.Library;
import com.marverenic.music.PlayerController;
import com.marverenic.music.R;
import com.marverenic.music.instances.Playlist;
import com.marverenic.music.instances.Song;
import com.marverenic.music.instances.viewholder.DraggableSongViewHolder;
import com.marverenic.music.utils.Themes;
import com.marverenic.music.view.BackgroundDecoration;
import com.marverenic.music.view.DividerDecoration;

import java.util.ArrayList;

public class QueueActivity extends BaseActivity {

    private ArrayList<Song> data;
    private Adapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instance);

        data = PlayerController.getQueue();

        RecyclerView songRecyclerView = (RecyclerView) findViewById(R.id.list);

        this.adapter = new Adapter();
        RecyclerViewDragDropManager dragDropManager = new RecyclerViewDragDropManager();
        RecyclerView.Adapter adapter = dragDropManager.createWrappedAdapter(this.adapter);
        // TODO drag drop shadow
        // dragDropManager.setDraggingItemShadowDrawable((NinePatchDrawable) getResources().getDrawable(R.drawable.material_shadow_z3));

        songRecyclerView.setAdapter(adapter);
        songRecyclerView.addItemDecoration(new BackgroundDecoration(Themes.getBackgroundElevated()));
        songRecyclerView.addItemDecoration(new DividerDecoration(this));

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        songRecyclerView.setLayoutManager(layoutManager);

        songRecyclerView.addItemDecoration(new BackgroundDecoration(Themes.getBackgroundElevated()));
        songRecyclerView.addItemDecoration(new DividerDecoration(this).disableExtraPadding());
        // Padding is disabled because for some reason these views already get 1dp of spacing...

        dragDropManager.attachRecyclerView(songRecyclerView);
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
                final TextInputLayout layout = new TextInputLayout(this);
                final AppCompatEditText input = new AppCompatEditText(this);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                input.setHint(R.string.hint_playlist_name);
                layout.addView(input);
                layout.setErrorEnabled(true);

                int padding = (int) getResources().getDimension(R.dimen.alert_padding);
                ((View) input.getParent()).setPadding(
                        padding - input.getPaddingLeft(),
                        padding,
                        padding - input.getPaddingRight(),
                        input.getPaddingBottom());

                final AlertDialog saveNewPlaylistDialog = new AlertDialog.Builder(this)
                        .setTitle(R.string.header_create_playlist)
                        .setView(layout)
                        .setPositiveButton(
                                R.string.action_create,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Library.createPlaylist(findViewById(R.id.list), input.getText().toString(), PlayerController.getQueue());
                                    }
                                })
                        .setNegativeButton(
                                R.string.action_cancel,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.cancel();
                                    }
                                }).show();

                saveNewPlaylistDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Themes.getAccent());
                saveNewPlaylistDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);

                input.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        String error = Library.verifyPlaylistName(QueueActivity.this, s.toString());
                        layout.setError(error);
                        saveNewPlaylistDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(error == null && s.length() > 0);
                        if (error == null && s.length() > 0){
                            saveNewPlaylistDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Themes.getAccent());
                        }
                        else{
                            saveNewPlaylistDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
                                    getResources().getColor((Themes.isLight(QueueActivity.this)
                                            ? R.color.secondary_text_disabled_material_light
                                            : R.color.secondary_text_disabled_material_dark)));
                        }
                    }

                    @Override
                    public void afterTextChanged(Editable s) {}
                });

                return true;
            case R.id.add_to_playlist:
                ArrayList<Playlist> playlists = Library.getPlaylists();
                String[] playlistNames = new String[playlists.size()];

                for (int i = 0; i < playlists.size(); i++ ){
                    playlistNames[i] = playlists.get(i).toString();
                }
                final AlertDialog addToPlaylistDialog = new AlertDialog.Builder(QueueActivity.this)
                        .setTitle(R.string.header_add_queue_to_playlist)
                        .setItems(playlistNames, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Library.addPlaylistEntries(
                                        findViewById(R.id.list),
                                        Library.getPlaylists().get(which),
                                        PlayerController.getQueue());
                            }
                        })
                        .setNegativeButton(R.string.action_cancel, null)
                        .show();

                addToPlaylistDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Themes.getAccent());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void update(){
        data = PlayerController.getQueue();
        adapter.notifyDataSetChanged();
    }

    public class Adapter extends RecyclerView.Adapter<DraggableSongViewHolder> implements DraggableItemAdapter<DraggableSongViewHolder>, View.OnClickListener {

        public Adapter(){
            setHasStableIds(true);
        }

        @Override
        public DraggableSongViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            DraggableSongViewHolder viewHolder = new DraggableSongViewHolder(
                    LayoutInflater
                            .from(viewGroup.getContext())
                            .inflate(R.layout.instance_song_drag, viewGroup, false),
                    data);

            viewHolder.setClickListener(this);
            return viewHolder;
        }

        @Override
        public void onClick(View view) {
            PlayerController.changeSong(((RecyclerView) findViewById(R.id.list)).getChildAdapterPosition(view));
            finish();
        }

        @Override
        public long getItemId(int position){
            return data.get(position).songId;
        }

        @Override
        public void onBindViewHolder(DraggableSongViewHolder viewHolder, int i) {
            viewHolder.update(data.get(i));
            if (data.get(i).equals(PlayerController.getNowPlaying())){
                if (Themes.isLight(QueueActivity.this))
                    viewHolder.highlight(Themes.getPrimaryDark(), Themes.getPrimary());
                else
                    viewHolder.highlight(Themes.getPrimary(), Themes.getPrimaryDark());
            }
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        @Override
        public boolean onCheckCanStartDrag(DraggableSongViewHolder viewHolder, int position, int x, int y){
            final View containerView = viewHolder.itemView;
            final View dragHandleView = viewHolder.dragHandle;

            final int offsetX = (int) (ViewCompat.getTranslationX(containerView) + 0.5f);

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

            // Calculate where the current song index is moving to
            final int nowPlayingIndex = data.indexOf(PlayerController.getNowPlaying());
            int futureNowPlayingIndex;

            if (from == nowPlayingIndex) futureNowPlayingIndex = to;
            else if (from < nowPlayingIndex && to >= nowPlayingIndex) futureNowPlayingIndex = nowPlayingIndex - 1;
            else if (from > nowPlayingIndex && to <= nowPlayingIndex) futureNowPlayingIndex = nowPlayingIndex + 1;
            else futureNowPlayingIndex = nowPlayingIndex;

            // Modify the Array and push the change to the service
            data.add(to, data.remove(from));
            PlayerController.editQueue(data, futureNowPlayingIndex);
        }
    }

}
