package com.marverenic.music.adapters;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.marverenic.music.PlayerService;
import com.marverenic.music.R;
import com.marverenic.music.instances.Song;
import com.marverenic.music.utils.Debug;
import com.marverenic.music.utils.Themes;
import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;

import java.util.ArrayList;

public class QueueEditAdapter extends BaseAdapter {

    private ArrayList<Song> data;
    private Context context;

    public QueueEditAdapter(Context context) {
        super();
        this.data = PlayerService.getQueue();
        this.context = context;
    }

    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {
        View v = convertView;
        if (convertView == null) {
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.instance_song_drag, parent, false);
        }
        Song s = data.get(position);

        if (s != null) {
            TextView tt = (TextView) v.findViewById(R.id.textSongTitle);
            TextView tt1 = (TextView) v.findViewById(R.id.textSongDetail);
            if (tt != null) {
                tt.setText(s.songName);
                tt.setTextColor(Themes.getListText());
            }
            if (tt1 != null) {
                tt1.setText(s.artistName + " - " + s.albumName);
                tt1.setTextColor(Themes.getDetailText());
            }
        } else {
            Debug.log(Debug.LogLevel.WTF, "SongListAdapter", "The requested entry is null", context);
        }

        return v;
    }

    @Override
    public int getItemViewType(int position) {
        return R.layout.instance_song;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return data.size() == 0;
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {

    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {

    }

    @Override
    public int getCount() {
        if (data != null) {
            return data.size();
        } else return 0;
    }

    @Override
    public Object getItem(int position) {
        if (data != null) {
            return data.get(position);
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        return (long) position;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public boolean isEnabled(int position) {
        return true;
    }

    public ArrayList<Song> move (int from, int to){
        Song song = data.remove(from);
        data.add(to, song);
        notifyDataSetChanged();
        return data;
    }

    public static class dragSortController extends DragSortController {

        DragSortListView listView;
        BaseAdapter adapter;

        public dragSortController(DragSortListView listView, BaseAdapter adapter, int handleId) {
            super(listView, handleId, ON_DOWN, FLING_REMOVE);
            this.listView = listView;
            this.adapter = adapter;
            setBackgroundColor(Themes.getBackgroundElevated());
        }

        @Override
        public View onCreateFloatView(int position) {
            return super.onCreateFloatView(position);
            /*View v = listView.getChildAt(position + listView.getHeaderViewsCount() - listView.getFirstVisiblePosition());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) v.setElevation(8f);
            return v;*/
        }

        @Override
        public void onDestroyFloatView(View floatView) {
            //do nothing; block super from crashing
        }

        @Override
        public int startDragPosition(MotionEvent ev) {
            return super.startDragPosition(ev);
        }
    }
}
