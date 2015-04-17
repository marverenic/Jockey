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
import com.marverenic.music.activity.PlaylistActivity;
import com.marverenic.music.instances.Library;
import com.marverenic.music.instances.LibraryScanner;
import com.marverenic.music.instances.Playlist;
import com.marverenic.music.utils.Debug;
import com.marverenic.music.utils.Navigate;
import com.marverenic.music.utils.Themes;

import java.util.ArrayList;

public class PlaylistListAdapter extends BaseAdapter implements SectionIndexer, AdapterView.OnItemClickListener {
    private ArrayList<Playlist> data;
    private Context context;
    private ArrayList<Character> sectionCharacter = new ArrayList<>();
    private ArrayList<Integer> sectionStartingPosition = new ArrayList<>();
    private ArrayList<Integer> sectionAtPosition = new ArrayList<>();

    public PlaylistListAdapter(Context context) {
        this(Library.getPlaylists(), context);
    }

    public PlaylistListAdapter(ArrayList<Playlist> data, Context context){
        super();
        this.data = data;
        this.context = context;

        String name;
        char thisChar;
        int sectionIndex = -1;
        for (int i = 0; i < data.size(); i++) {
            name = data.get(i).playlistName.toUpperCase();

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

    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {
        View v = convertView;
        if (convertView == null) {
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.instance_playlist, parent, false);

            TextView tt = (TextView) v.findViewById(R.id.textPlaylistName);
            tt.setTextColor(Themes.getListText());
            ((ImageView) v.findViewById(R.id.instanceMore)).setColorFilter(Themes.getDetailText());
        }

        final Playlist p = data.get(position);

        if (p != null) {
            TextView tt = (TextView) v.findViewById(R.id.textPlaylistName);
            if (tt != null) {
                tt.setText(p.playlistName);
            }

            v.findViewById(R.id.instanceMore).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final PopupMenu menu = new PopupMenu(context, v, Gravity.END);
                    String[] options = context.getResources().getStringArray(R.array.queue_options_playlist);
                    for (int i = 0; i < options.length;  i++) {
                        menu.getMenu().add(Menu.NONE, i, i, options[i]);
                    }
                    menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem menuItem) {
                            switch (menuItem.getItemId()){
                                case 0: //Queue this playlist next
                                    PlayerController.queueNext(LibraryScanner.getPlaylistEntries(context, p));
                                    return true;
                                case 1: //Queue this playlist last
                                    PlayerController.queueLast(LibraryScanner.getPlaylistEntries(context, p));
                                    return true;
                                case 2: //Delete this playlist
                                    new AlertDialog.Builder(context)
                                            .setTitle("Delete \"" + p.playlistName + "\"?")
                                            .setMessage("Deleting this playlist will permanently remove it from your device")
                                            .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    LibraryScanner.removePlaylist(context, p);
                                                    updateData(Library.getPlaylists());
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
            Debug.log(Debug.LogLevel.WTF, "PlaylistListAdapter", "The requested entry is null", context);
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
        Playlist item = data.get(position);
        Navigate.to(context, PlaylistActivity.class, PlaylistActivity.PLAYLIST_ENTRY, item);
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

    public void updateData(ArrayList<Playlist> playlistLibrary) {
        this.data = playlistLibrary;
        notifyDataSetChanged();
    }

    public void updateData(){
        this.data = Library.getPlaylists();
        notifyDataSetChanged();
    }
}