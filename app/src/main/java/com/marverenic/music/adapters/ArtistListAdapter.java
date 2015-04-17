package com.marverenic.music.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.marverenic.music.PlayerController;
import com.marverenic.music.R;
import com.marverenic.music.activity.LibraryPageActivity;
import com.marverenic.music.instances.Artist;
import com.marverenic.music.instances.Library;
import com.marverenic.music.instances.LibraryScanner;
import com.marverenic.music.instances.Playlist;
import com.marverenic.music.utils.Debug;
import com.marverenic.music.utils.Navigate;
import com.marverenic.music.utils.Themes;

import java.util.ArrayList;

public class ArtistListAdapter extends BaseAdapter implements SectionIndexer, AdapterView.OnItemClickListener {
    private ArrayList<Artist> data;
    private Context context;
    private ArrayList<Character> sectionCharacter = new ArrayList<>();
    private ArrayList<Integer> sectionStartingPosition = new ArrayList<>();
    private ArrayList<Integer> sectionAtPosition = new ArrayList<>();

    public ArtistListAdapter(Context context){
        this(Library.getArtists(), context);
    }

    public ArtistListAdapter(ArrayList<Artist> data, Context context) {
        super();
        this.data = data;
        this.context = context;

        char thisChar;
        int sectionIndex = -1;
        for(int i = 0; i < data.size(); i++){
            thisChar = data.get(i).artistName.toUpperCase().charAt(0);

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
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.instance_artist, parent, false);

            ((TextView) v.findViewById(R.id.textArtistName)).setTextColor(Themes.getListText());
            ((ImageView) v.findViewById(R.id.instanceMore)).setColorFilter(Themes.getDetailText());
        }
        final Artist a = data.get(position);

        if (a != null) {
            TextView tt = (TextView) v.findViewById(R.id.textArtistName);
            if (tt != null) {
                tt.setText(a.artistName);
            }
            v.findViewById(R.id.instanceMore).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final PopupMenu menu = new PopupMenu(context, v, Gravity.END);
                    String[] options = context.getResources().getStringArray(R.array.queue_options_artist);
                    for (int i = 0; i < options.length;  i++) {
                        menu.getMenu().add(Menu.NONE, i, i, options[i]);
                    }
                    menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem menuItem) {
                            switch (menuItem.getItemId()){
                                case 0: //Queue this artist next
                                    PlayerController.queueNext(LibraryScanner.getArtistSongEntries(a));
                                    return true;
                                case 1: //Queue this artist last
                                    PlayerController.queueLast(LibraryScanner.getArtistSongEntries(a));
                                    return true;
                                case 2: //Add to playlist...
                                    ArrayList<Playlist> playlists = Library.getPlaylists();
                                    String[] playlistNames = new String[playlists.size()];

                                    for (int i = 0; i < playlists.size(); i++ ){
                                        playlistNames[i] = playlists.get(i).toString();
                                    }

                                    new AlertDialog.Builder(context).setTitle("Add \"" + a.artistName + "\" to playlist")
                                            .setItems(playlistNames, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    LibraryScanner.addPlaylistEntries(
                                                            context,
                                                            Library.getPlaylists().get(which),
                                                            LibraryScanner.getArtistSongEntries(a));
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
            Debug.log(Debug.LogLevel.WTF, "ArtistListAdapter", "The requested entry is null", context);
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
        Artist item = data.get(position);
        Navigate.to(context, LibraryPageActivity.class, "entry", item);
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

    public void updateData(ArrayList<Artist> artistLibrary) {
        this.data = artistLibrary;
        notifyDataSetChanged();
    }
}
