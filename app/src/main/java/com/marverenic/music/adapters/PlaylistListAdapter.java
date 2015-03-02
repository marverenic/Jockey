package com.marverenic.music.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.marverenic.music.LibraryPageActivity;
import com.marverenic.music.PlayerService;
import com.marverenic.music.R;
import com.marverenic.music.instances.Library;
import com.marverenic.music.instances.LibraryScanner;
import com.marverenic.music.instances.Playlist;
import com.marverenic.music.utils.Debug;
import com.marverenic.music.utils.Navigate;
import com.marverenic.music.utils.Themes;

import java.util.ArrayList;

public class PlaylistListAdapter extends BaseAdapter implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {
    private ArrayList<Playlist> data;
    private Context context;

    public PlaylistListAdapter(Context context) {
        this(Library.getPlaylists(), context);
    }

    public PlaylistListAdapter(ArrayList<Playlist> data, Context context){
        super();
        this.data = data;
        this.context = context;
    }

    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {
        View v = convertView;
        if (convertView == null) {
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.instance_playlist, parent, false);
        }
        Playlist p = data.get(position);

        if (p != null) {
            TextView tt = (TextView) v.findViewById(R.id.textPlaylistName);
            if (tt != null) {
                tt.setText(p.playlistName);
                tt.setTextColor(Themes.getListText());
            }
        } else {
            Debug.log(Debug.LogLevel.WTF, "PlaylistListAdapter", "The requested entry is null", context);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ((ListView) parent).setSelector(Themes.getTouchRipple(context));
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
        Navigate.to(context, LibraryPageActivity.class, "entry", item);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        final Playlist item = data.get(position);

        AlertDialog.Builder dialog = new AlertDialog.Builder(context);


        dialog.setTitle(item.playlistName)
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // There's nothing to do here
                    }
                })
                .setItems(R.array.queue_options_playlist, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0: //Queue this playlist next
                                PlayerService.queueNext(context, LibraryScanner.getPlaylistEntries(context, item));
                                break;
                            case 1: //Queue this playlist last
                                PlayerService.queueLast(context, LibraryScanner.getPlaylistEntries(context, item));
                                break;
                            default:
                                break;
                        }
                    }
                });
        dialog.create().show();
        return true;
    }

    public void updateData(ArrayList<Playlist> playlistLibrary) {
        this.data = playlistLibrary;
        notifyDataSetChanged();
    }
}