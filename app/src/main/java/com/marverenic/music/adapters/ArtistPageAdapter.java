package com.marverenic.music.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.marverenic.music.LibraryPageActivity;
import com.marverenic.music.NowPlayingActivity;
import com.marverenic.music.Player;
import com.marverenic.music.PlayerService;
import com.marverenic.music.R;
import com.marverenic.music.instances.Album;
import com.marverenic.music.instances.LibraryScanner;
import com.marverenic.music.instances.Song;
import com.marverenic.music.utils.Debug;
import com.marverenic.music.utils.Navigate;
import com.marverenic.music.utils.Themes;

import java.util.ArrayList;

public class ArtistPageAdapter extends BaseAdapter implements SectionIndexer, AdapterView.OnItemLongClickListener, AdapterView.OnItemClickListener {
    public ArrayList<Song> songs;
    public ArrayList<Album> albums;
    private Context context;
    private ArrayList<Character> sectionCharacter = new ArrayList<>();
    private ArrayList<Integer> sectionStartingPosition = new ArrayList<>();
    private ArrayList<Integer> sectionAtPosition = new ArrayList<>();

    @SuppressWarnings("unchecked")
    public ArtistPageAdapter(Context context, ArrayList<Song> songs, ArrayList<Album> albums) {
        super();
        this.songs = (ArrayList<Song>) songs.clone();
        this.albums = (ArrayList<Album>) albums.clone();
        this.context = context;

        String name;
        char thisChar;
        int sectionIndex = -1;
        for(int i = 0; i < songs.size(); i++){
            name = songs.get(i).songName.toUpperCase();

            if (name.startsWith("THE ")){
                thisChar = name.charAt(4);
            }
            else if (name.startsWith("A ")){
                thisChar = name.charAt(2);
            }
            else{
                thisChar = name.charAt(0);
            }

            if(sectionCharacter.size() == 0 || !sectionCharacter.get(sectionCharacter.size() - 1).equals(thisChar)) {
                sectionIndex++;
                sectionCharacter.add(thisChar);
                sectionStartingPosition.add(i);
            }
            sectionAtPosition.add(sectionIndex);
        }
    }

    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {
        View v = convertView;
        if (convertView == null) {
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.instance_song, parent, false);
        }
        Song s = songs.get(position);

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
    public int getCount() {
        if (songs != null) {
            return songs.size();
        } else return 0;
    }

    @Override
    public Object getItem(int position) {
        if (songs != null) {
            return songs.get(position);
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        return (long) position;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        PlayerService.setQueue(context, songs, position - ((ListView) parent).getHeaderViewsCount());
        PlayerService.begin();

        context.startService(new Intent(context, Player.class));
        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("switchToNowPlaying", true)) {
            Navigate.to(context, NowPlayingActivity.class);
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        final Song item = songs.get(position - ((ListView) parent).getHeaderViewsCount());

        AlertDialog.Builder dialog = new AlertDialog.Builder(context);

        dialog.setTitle(item.songName)
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // There's nothing to do here
                    }
                })
                .setItems(R.array.queue_options_artist_page, new DialogInterface.OnClickListener() {
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
                            case 2: //Go to album
                                Navigate.to(context, LibraryPageActivity.class, "entry", LibraryScanner.findAlbumById(item.albumId));
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
}
