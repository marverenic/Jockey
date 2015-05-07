package com.marverenic.music.adapters;

import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.marverenic.music.PlayerController;
import com.marverenic.music.R;
import com.marverenic.music.activity.InstanceActivity;
import com.marverenic.music.instances.Genre;
import com.marverenic.music.instances.Library;
import com.marverenic.music.instances.LibraryScanner;
import com.marverenic.music.instances.Playlist;
import com.marverenic.music.utils.Debug;
import com.marverenic.music.utils.Navigate;
import com.marverenic.music.utils.Themes;

import java.util.ArrayList;

public class GenreListAdapter extends BaseAdapter implements SectionIndexer, AdapterView.OnItemClickListener {
    private ArrayList<Genre> data;
    private Context context;
    private ArrayList<Character> sectionCharacter = new ArrayList<>();
    private ArrayList<Integer> sectionStartingPosition = new ArrayList<>();
    private ArrayList<Integer> sectionAtPosition = new ArrayList<>();

    public GenreListAdapter(Context context) {
        this(Library.getGenres(), context);
    }

    public GenreListAdapter(ArrayList<Genre> data, Context context) {
        super();
        this.data = data;
        this.context = context;

        String name;
        char thisChar;
        int sectionIndex = -1;
        for (int i = 0; i < data.size(); i++) {
            name = data.get(i).genreName.toUpperCase();

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
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.instance_genre, parent, false);
            ((TextView) v.findViewById(R.id.textGenreName)).setTextColor(Themes.getListText());
            ((ImageView) v.findViewById(R.id.instanceMore)).setColorFilter(Themes.getDetailText());
        }
        final Genre g = data.get(position);

        if (g != null) {
            ((TextView) v.findViewById(R.id.textGenreName)).setText(g.genreName);

            v.findViewById(R.id.instanceMore).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final PopupMenu menu = new PopupMenu(context, v, Gravity.END);
                    String[] options = context.getResources().getStringArray(R.array.queue_options_genre);
                    for (int i = 0; i < options.length;  i++) {
                        menu.getMenu().add(Menu.NONE, i, i, options[i]);
                    }
                    menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem menuItem) {
                            switch (menuItem.getItemId()){
                                case 0: //Queue this genre next
                                    PlayerController.queueNext(LibraryScanner.getGenreEntries(g));
                                    return true;
                                case 1: //Queue this genre last
                                    PlayerController.queueLast(LibraryScanner.getGenreEntries(g));
                                    return true;
                                case 2: //Add to playlist
                                    ArrayList<Playlist> playlists = Library.getPlaylists();
                                    String[] playlistNames = new String[playlists.size()];

                                    for (int i = 0; i < playlists.size(); i++ ){
                                        playlistNames[i] = playlists.get(i).toString();
                                    }

                                    new AlertDialog.Builder(context).setTitle("Add \"" + g.genreName + "\" to playlist")
                                            .setItems(playlistNames, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    LibraryScanner.addPlaylistEntries(
                                                            context,
                                                            Library.getPlaylists().get(which),
                                                            LibraryScanner.getGenreEntries(g));
                                                }
                                            })
                                            .setNeutralButton("Cancel", null)
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
            Debug.log(Debug.LogLevel.WTF, "GenreListAdapter", "The requested entry is null", context);
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
        Genre item = data.get(position);
        Navigate.to(context, InstanceActivity.class, "entry", item);
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

    public void updateData(ArrayList<Genre> data) {
        this.data = data;
        notifyDataSetChanged();
    }
}