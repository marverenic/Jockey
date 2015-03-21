package com.marverenic.music.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.marverenic.music.LibraryPageActivity;
import com.marverenic.music.PlayerService;
import com.marverenic.music.R;
import com.marverenic.music.instances.Genre;
import com.marverenic.music.instances.Library;
import com.marverenic.music.instances.LibraryScanner;
import com.marverenic.music.instances.Playlist;
import com.marverenic.music.utils.Debug;
import com.marverenic.music.utils.Navigate;
import com.marverenic.music.utils.Themes;

import java.util.ArrayList;

public class GenreListAdapter extends BaseAdapter implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {
    private ArrayList<Genre> data;
    private Context context;

    public GenreListAdapter(Context context) {
        this(Library.getGenres(), context);
    }

    public GenreListAdapter(ArrayList<Genre> data, Context context) {
        super();
        this.data = data;
        this.context = context;
    }

    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {
        View v = convertView;
        if (convertView == null) {
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.instance_genre, parent, false);
        }
        Genre p = data.get(position);

        if (p != null) {
            TextView tt = (TextView) v.findViewById(R.id.textGenreName);
            if (tt != null) {
                tt.setText(p.genreName);
                tt.setTextColor(Themes.getListText());
            }
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
        Navigate.to(context, LibraryPageActivity.class, "entry", item);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        final Genre item = data.get(position);

        AlertDialog.Builder dialog = new AlertDialog.Builder(context);

        dialog.setTitle(item.genreName)
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // There's nothing to do here
                    }
                })
                .setItems(R.array.queue_options_genre, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0: //Queue this genre next
                                PlayerService.queueNext(context, LibraryScanner.getGenreEntries(item));
                                break;
                            case 1: //Queue this genre last
                                PlayerService.queueLast(context, LibraryScanner.getGenreEntries(item));
                                break;
                            case 2: //Add to playlist
                                ArrayList<Playlist> playlists = Library.getPlaylists();
                                String[] playlistNames = new String[playlists.size()];

                                for (int i = 0; i < playlists.size(); i++ ){
                                    playlistNames[i] = playlists.get(i).toString();
                                }

                                new AlertDialog.Builder(context).setTitle("Add \"" + item.genreName + "\" to playlist")
                                        .setItems(playlistNames, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                LibraryScanner.addPlaylistEntries(
                                                        context,
                                                        Library.getPlaylists().get(which),
                                                        LibraryScanner.getGenreEntries(item));
                                            }
                                        })
                                        .setNeutralButton("Cancel", null)
                                        .show();
                                break;
                            default:
                                break;
                        }
                    }
                });
        dialog.create().show();
        return true;
    }

    public void updateData(ArrayList<Genre> data) {
        this.data = data;
        notifyDataSetChanged();
    }
}