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
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.marverenic.music.LibraryPageActivity;
import com.marverenic.music.PlayerService;
import com.marverenic.music.R;
import com.marverenic.music.instances.Artist;
import com.marverenic.music.instances.Library;
import com.marverenic.music.instances.LibraryScanner;
import com.marverenic.music.utils.Debug;
import com.marverenic.music.utils.Navigate;
import com.marverenic.music.utils.Themes;

import java.util.ArrayList;

public class ArtistListAdapter extends BaseAdapter implements SectionIndexer, AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {
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
        }
        Artist p = data.get(position);

        if (p != null) {
            TextView tt = (TextView) v.findViewById(R.id.textArtistName);
            if (tt != null) {
                tt.setText(p.artistName);
                tt.setTextColor(Themes.getListText());
            }
        } else {
            Debug.log(Debug.LogLevel.WTF, "ArtistListAdapter", "The requested entry is null", context);
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
        Artist item = data.get(position);
        Navigate.to(context, LibraryPageActivity.class, "entry", item);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        final Artist item = data.get(position);

        AlertDialog.Builder dialog = new AlertDialog.Builder(context);

        dialog.setTitle(item.artistName)
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // There's nothing to do here
                    }
                })
                .setItems(R.array.queue_options_artist, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0: //Queue this artist next
                                PlayerService.queueNext(context, LibraryScanner.getArtistSongEntries(item));
                                break;
                            case 1: //Queue this artist last
                                PlayerService.queueLast(context, LibraryScanner.getArtistSongEntries(item));
                                break;
                            default:
                                break;
                        }
                    }
                });
        dialog.create().show();
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
