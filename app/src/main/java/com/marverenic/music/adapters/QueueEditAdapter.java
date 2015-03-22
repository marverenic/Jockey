package com.marverenic.music.adapters;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.marverenic.music.LibraryPageActivity;
import com.marverenic.music.PlayerService;
import com.marverenic.music.QueueActivity;
import com.marverenic.music.R;
import com.marverenic.music.instances.Library;
import com.marverenic.music.instances.LibraryScanner;
import com.marverenic.music.instances.Playlist;
import com.marverenic.music.instances.Song;
import com.marverenic.music.utils.Debug;
import com.marverenic.music.utils.Navigate;
import com.marverenic.music.utils.Themes;
import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;

import java.util.ArrayList;

public class QueueEditAdapter extends BaseAdapter implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener, DragSortListView.DropListener{

    private ArrayList<Song> data;
    private QueueActivity activity;
    private ListView listView;

    public QueueEditAdapter(QueueActivity activity, DragSortListView listView) {
        super();
        this.data = new ArrayList<>(PlayerService.getQueue());
        this.activity = activity;
        this.listView = listView;

        DragSortController controller = new QueueEditAdapter.dragSortController(listView, this, R.id.handle);
        listView.setOnItemClickListener(this);
        listView.setOnItemLongClickListener(this);
        listView.setAdapter(this);
        listView.setDropListener(this);
        listView.setFloatViewManager(controller);
        listView.setOnTouchListener(controller);
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
                if (position == PlayerService.getPosition()){
                    tt.setTextColor(Themes.getPrimary());
                }
                else{
                    tt.setTextColor(Themes.getListText());
                }
            }
            if (tt1 != null) {
                tt1.setText(s.artistName + " - " + s.albumName);
                if (position == PlayerService.getPosition()){
                    tt1.setTextColor(Themes.getAccent());
                }
                else{
                    tt1.setTextColor(Themes.getDetailText());
                }
            }
        } else {
            Debug.log(Debug.LogLevel.WTF, "SongListAdapter", "The requested entry is null", activity);
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

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        PlayerService.changeSong(position);
        Navigate.back(activity);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, final int position, long id) {
        final Song item = data.get(position - listView.getHeaderViewsCount());

        AlertDialog.Builder dialog = new AlertDialog.Builder(activity);

        dialog.setTitle(item.songName)
                .setNegativeButton("Cancel", null)
                .setItems(R.array.edit_queue_options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0: //Go to artist
                                Navigate.to(activity, LibraryPageActivity.class, "entry", LibraryScanner.findArtistById(item.artistId));
                                break;
                            case 1: //Go to album
                                Navigate.to(activity, LibraryPageActivity.class, "entry", LibraryScanner.findAlbumById(item.albumId));
                                break;
                            case 2: //Add to playlist...
                                ArrayList<Playlist> playlists = Library.getPlaylists();
                                String[] playlistNames = new String[playlists.size()];

                                for (int i = 0; i < playlists.size(); i++ ){
                                    playlistNames[i] = playlists.get(i).toString();
                                }

                                new AlertDialog.Builder(activity).setTitle("Add \"" + item.songName + "\" to playlist")
                                        .setItems(playlistNames, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                LibraryScanner.addPlaylistEntry(activity, Library.getPlaylists().get(which), item);
                                            }
                                        })
                                        .setNegativeButton("Cancel", null)
                                        .show();
                                break;
                            case 3: //Remove from queue
                                new AlertDialog.Builder(activity).setTitle(item.songName)
                                        .setMessage("Remove this song from the queue?")
                                        .setPositiveButton("Remove", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                data.remove(position);
                                                notifyDataSetChanged();

                                                listView.invalidateViews();

                                                if (PlayerService.getPosition() == position){
                                                    // If the current song was removed
                                                    PlayerService.changeQueue(activity, data, position);
                                                }
                                                else if (PlayerService.getPosition() > position){
                                                    // If a song that was before the current playing song was removed...
                                                    PlayerService.changeQueue(activity, data, PlayerService.getPosition() - 1);
                                                }
                                                else {
                                                    // If a song that was after the current playing song was removed...
                                                    PlayerService.changeQueue(activity, data, PlayerService.getPosition());
                                                }
                                            }
                                        })
                                        .setNegativeButton("Cancel", null)
                                        .show();
                                break;
                            default:
                                break;
                        }
                    }
                });
        dialog.show();
        return true;
    }

    @Override
    public void drop(int from, int to) {
        Song song = data.remove(from);
        data.add(to, song);

        notifyDataSetChanged();
        listView.invalidateViews();

        if (PlayerService.getPosition() == from){
            // If the current song was moved in the queue
            PlayerService.changeQueue(activity, data, to);
        }
        else if (PlayerService.getPosition() < from && PlayerService.getPosition() >= to){
            // If a song that was after the current playing song was moved to a position before the current song...
            PlayerService.changeQueue(activity, data, PlayerService.getPosition() + 1);
        }
        else if (PlayerService.getPosition() > from && PlayerService.getPosition() <= to){
            // If a song that was before the current playing song was moved to a position after the current song...
            PlayerService.changeQueue(activity, data, PlayerService.getPosition() - 1);
        }
        else{
            // If the number of songs before and after the currently playing song hasn't changed...
            PlayerService.changeQueue(activity, data, PlayerService.getPosition());
        }
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
