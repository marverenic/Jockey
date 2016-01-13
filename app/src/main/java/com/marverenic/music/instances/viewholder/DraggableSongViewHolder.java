package com.marverenic.music.instances.viewholder;

import android.view.View;
import android.widget.ImageView;

import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemViewHolder;
import com.marverenic.music.R;
import com.marverenic.music.instances.Song;

import java.util.List;

public class DraggableSongViewHolder extends SongViewHolder implements DraggableItemViewHolder {

    private ImageView dragHandle;
    private View itemView;
    private int flags = 0;

    public DraggableSongViewHolder(View itemView, List<Song> songList) {
        super(itemView, songList);
        this.itemView = itemView;
        dragHandle = (ImageView) itemView.findViewById(R.id.handle);
        dragHandle.setOnClickListener(null);
    }

    public ImageView getDragHandle() {
        return dragHandle;
    }

    public View getItemView() {
        return itemView;
    }

    @Override
    public void setDragStateFlags(int i) {
        flags = i;
    }

    @Override
    public int getDragStateFlags() {
        return flags;
    }
}
