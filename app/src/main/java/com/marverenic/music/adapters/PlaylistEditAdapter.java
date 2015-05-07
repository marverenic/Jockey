package com.marverenic.music.adapters;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.DataSetObserver;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.marverenic.music.Player;
import com.marverenic.music.PlayerController;
import com.marverenic.music.R;
import com.marverenic.music.activity.ArtistActivity;
import com.marverenic.music.activity.InstanceActivity;
import com.marverenic.music.activity.NowPlayingActivity;
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

public class PlaylistEditAdapter extends BaseAdapter implements AdapterView.OnItemClickListener, DragSortListView.DropListener {

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
        data.remove(row);
        LibraryScanner.editPlaylist(context, playlist, data);
        notifyDataSetChanged();
        listView.invalidateViews();
    }

    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {
        View v = convertView;
        if (convertView == null) {
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.instance_song_drag, parent, false);

            ((TextView) v.findViewById(R.id.textSongTitle)).setTextColor(Themes.getListText());
            ((TextView) v.findViewById(R.id.textSongDetail)).setTextColor(Themes.getDetailText());
            ((ImageView) v.findViewById(R.id.instanceMore)).setColorFilter(Themes.getDetailText());
        }

        final Song s = data.get(position);

        if (s != null) {
            ((TextView) v.findViewById(R.id.textSongTitle)).setText(s.songName);
            ((TextView) v.findViewById(R.id.textSongDetail)).setText(s.artistName + " - " + s.albumName);

            v.findViewById(R.id.instanceMore).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final PopupMenu menu = new PopupMenu(context, v, Gravity.END);
                    String[] options = context.getResources().getStringArray(R.array.edit_playlist_options);
                    for (int i = 0; i < options.length;  i++) {
                        menu.getMenu().add(Menu.NONE, i, i, options[i]);
                    }
                    menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem menuItem) {
                            switch (menuItem.getItemId()) {
                                case 0: //Queue this song next
                                    PlayerController.queueNext(s);
                                    return true;
                                case 1: //Queue this song last
                                    PlayerController.queueLast(s);
                                    return true;
                                case 2: //Go to artist
                                    Navigate.to(context, ArtistActivity.class, ArtistActivity.ARTIST_EXTRA, LibraryScanner.findArtistById(s.artistId));
                                    return true;
                                case 3: //Go to album
                                    Navigate.to(context, InstanceActivity.class, "entry", LibraryScanner.findAlbumById(s.albumId));
                                    return true;
                                case 4: //Add to playlist...
                                    ArrayList<Playlist> playlists = Library.getPlaylists();
                                    String[] playlistNames = new String[playlists.size()];

                                    for (int i = 0; i < playlists.size(); i++) {
                                        playlistNames[i] = playlists.get(i).toString();
                                    }

                                    new AlertDialog.Builder(context).setTitle("Add \"" + s.songName + "\" to playlist")
                                            .setItems(playlistNames, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    LibraryScanner.addPlaylistEntry(context, Library.getPlaylists().get(which), s);
                                                }
                                            })
                                            .setNegativeButton("Cancel", null)
                                            .show();
                                    return true;
                                case 5: //Remove from playlist...
                                    new AlertDialog.Builder(context).setTitle(s.songName)
                                            .setMessage("Remove this song from playlist \"" + playlist.playlistName + "\"?")
                                            .setPositiveButton("Remove", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                    onRemoved(position - ((ListView) parent).getHeaderViewsCount());
                                                }
                                            })
                                            .setNegativeButton("Cancel", null)
                                            .show();
                                    return true;
                            }
                            return false;
                        }
                    });
                    menu.show();
                }
            });
        } else {
            Debug.log(Debug.LogLevel.WTF, "SongListAdapter", "The requested entry is null", context);
        }

        return v;
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
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
        PlayerController.setQueue(data, position - ((ListView) parent).getHeaderViewsCount());
        PlayerController.begin();

        context.startService(new Intent(context, Player.class));
        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("switchToNowPlaying", true)) {
            Navigate.to(context, NowPlayingActivity.class);
        }
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
