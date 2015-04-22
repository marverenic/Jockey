package com.marverenic.music.adapters;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.marverenic.music.Player;
import com.marverenic.music.PlayerController;
import com.marverenic.music.R;
import com.marverenic.music.activity.LibraryPageActivity;
import com.marverenic.music.activity.NowPlayingActivity;
import com.marverenic.music.instances.Library;
import com.marverenic.music.instances.LibraryScanner;
import com.marverenic.music.instances.Playlist;
import com.marverenic.music.instances.Song;
import com.marverenic.music.utils.Debug;
import com.marverenic.music.utils.Navigate;
import com.marverenic.music.utils.Themes;

import java.util.ArrayList;

public class SongListAdapter extends BaseAdapter implements SectionIndexer, AdapterView.OnItemClickListener {
    private ArrayList<Song> data;
    private Context context;
    private ArrayList<Character> sectionCharacter = new ArrayList<>();
    private ArrayList<Integer> sectionStartingPosition = new ArrayList<>();
    private ArrayList<Integer> sectionAtPosition = new ArrayList<>();

    public SongListAdapter(Context context, boolean enableSectionHeader) {
        this(Library.getSongs(), context, enableSectionHeader);
    }

    public SongListAdapter(ArrayList<Song> data, Context context, boolean enableSectionHeader) {
        super();
        this.data = data;
        this.context = context;

        if(enableSectionHeader) {
            String name;
            char thisChar;
            int sectionIndex = -1;
            for (int i = 0; i < data.size(); i++) {
                name = data.get(i).songName.toUpperCase();

                if (name.startsWith("THE ")) {
                    thisChar = name.charAt(4);
                } else if (name.startsWith("A ")) {
                    thisChar = name.charAt(2);
                } else {
                    thisChar = name.charAt(0);
                }

                if (sectionCharacter.size() == 0 || !sectionCharacter.get(sectionCharacter.size() - 1).equals(thisChar)) {
                    sectionIndex++;
                    sectionCharacter.add(thisChar);
                    sectionStartingPosition.add(i);
                }
                sectionAtPosition.add(sectionIndex);
            }
        }
    }

    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {
        View v = convertView;
        if (convertView == null) {
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.instance_song, parent, false);

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
                    String[] options = context.getResources().getStringArray(R.array.queue_options_song);
                    for (int i = 0; i < options.length;  i++) {
                        menu.getMenu().add(Menu.NONE, i, i, options[i]);
                    }
                    menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem menuItem) {
                            switch (menuItem.getItemId()){
                                case 0: //Queue this song next
                                    PlayerController.queueNext(s);
                                    return true;
                                case 1: //Queue this song last
                                    PlayerController.queueLast(s);
                                    return true;
                                case 2: //Go to artist
                                    Navigate.to(context, LibraryPageActivity.class, "entry", LibraryScanner.findArtistById(s.artistId));
                                    return true;
                                case 3: //Go to album
                                    Navigate.to(context, LibraryPageActivity.class, "entry", LibraryScanner.findAlbumById(s.albumId));
                                    return true;
                                case 4: //Add to playlist...
                                    ArrayList<Playlist> playlists = Library.getPlaylists();
                                    String[] playlistNames = new String[playlists.size()];

                                    for (int i = 0; i < playlists.size(); i++ ){
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
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        PlayerController.setQueue(data, position - ((ListView) parent).getHeaderViewsCount());
        PlayerController.begin();

        context.startService(new Intent(context, Player.class));
        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("switchToNowPlaying", true)) {
            Navigate.to(context, NowPlayingActivity.class);
        }
    }

    @Override
    public Object[] getSections() {
        return sectionCharacter.toArray();
    }

    @Override
    public int getPositionForSection(int sectionNumber) {
        return sectionStartingPosition.get(sectionNumber);
    }

    @Override
    public int getSectionForPosition(int itemPosition) {
        return sectionAtPosition.get(itemPosition);
    }

    public void updateData(ArrayList<Song> songLibrary) {
        this.data = songLibrary;
        notifyDataSetChanged();
    }
}
