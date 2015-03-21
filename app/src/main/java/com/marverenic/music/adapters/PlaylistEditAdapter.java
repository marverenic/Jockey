package com.marverenic.music.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.DataSetObserver;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.marverenic.music.LibraryPageActivity;
import com.marverenic.music.NowPlayingActivity;
import com.marverenic.music.Player;
import com.marverenic.music.PlayerService;
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

public class PlaylistEditAdapter extends BaseAdapter implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener, DragSortListView.DropListener {

    private ArrayList<Song> data;
    private Playlist playlist;
    private Context context;
    private DragSortListView listView;

    public PlaylistEditAdapter(ArrayList<Song> data, Playlist playlist, Context context, DragSortListView listView) {
        super();
        this.data = new ArrayList<>(data);
        this.playlist = playlist;
        this.context = context;
        this.listView = listView;

        DragSortController controller = new PlaylistEditAdapter.dragSortController(listView, this, R.id.handle);
        listView.setOnItemClickListener(this);
        listView.setOnItemLongClickListener(this);
        listView.setAdapter(this);
        listView.setDropListener(this);
        listView.setFloatViewManager(controller);
        listView.setOnTouchListener(controller);
    }

    @Override
    public void drop(int from, int to) {
        if (from != to) {
            data.add(to, data.remove(from));
            LibraryScanner.editPlaylist(context, playlist, data);
            notifyDataSetChanged();
            listView.invalidateViews();
        }
    }

    public void onRemoved(int row){
        LibraryScanner.removePlaylistEntry(context, playlist, row);
        data.remove(row);
        notifyDataSetChanged();
        listView.invalidateViews();
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
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        PlayerService.setQueue(context, data, position - ((ListView) parent).getHeaderViewsCount());
        PlayerService.begin();

        context.startService(new Intent(context, Player.class));
        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("switchToNowPlaying", true)) {
            Navigate.to(context, NowPlayingActivity.class);
        }
    }

    @Override
    public boolean onItemLongClick(final AdapterView<?> parent, final View view, final int position, final long id) {
        final Song item = data.get(position - ((ListView) parent).getHeaderViewsCount());

        AlertDialog.Builder dialog = new AlertDialog.Builder(context);

        dialog.setTitle(item.songName)
                .setNegativeButton("Cancel", null)
                .setItems(R.array.edit_playlist_options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0: //Queue this song next
                                if (!PlayerService.isInitialized())
                                    context.startService(new Intent(context, Player.class));
                                PlayerService.queueNext(context, item);
                                break;
                            case 1: //Queue this song last
                                if (!PlayerService.isInitialized())
                                    context.startService(new Intent(context, Player.class));
                                PlayerService.queueLast(context, item);
                                break;
                            case 2: //Go to artist
                                Navigate.to(context, LibraryPageActivity.class, "entry", LibraryScanner.findArtistById(item.artistId));
                                break;
                            case 3: //Go to album
                                Navigate.to(context, LibraryPageActivity.class, "entry", LibraryScanner.findAlbumById(item.albumId));
                                break;
                            case 4: //Add to playlist...
                                ArrayList<Playlist> playlists = Library.getPlaylists();
                                String[] playlistNames = new String[playlists.size()];

                                for (int i = 0; i < playlists.size(); i++ ){
                                    playlistNames[i] = playlists.get(i).toString();
                                }

                                new AlertDialog.Builder(context).setTitle("Add \"" + item.songName + "\" to playlist")
                                        .setItems(playlistNames, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                LibraryScanner.addPlaylistEntry(context, Library.getPlaylists().get(which), item);
                                            }
                                        })
                                        .setNegativeButton("Cancel", null)
                                        .show();
                                break;
                            case 5: //Remove from playlist...
                                new AlertDialog.Builder(context).setTitle(item.songName)
                                        .setMessage("Remove this song from playlist \"" + playlist.playlistName + "\"?")
                                        .setPositiveButton("Remove", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                onRemoved(position - ((ListView) parent).getHeaderViewsCount());
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
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public boolean isEnabled(int position) {
        return true;
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
