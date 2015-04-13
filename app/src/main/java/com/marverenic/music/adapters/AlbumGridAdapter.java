package com.marverenic.music.adapters;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.marverenic.music.PlayerController;
import com.marverenic.music.R;
import com.marverenic.music.activity.LibraryPageActivity;
import com.marverenic.music.instances.Album;
import com.marverenic.music.instances.Library;
import com.marverenic.music.instances.LibraryScanner;
import com.marverenic.music.instances.Playlist;
import com.marverenic.music.utils.Fetch;
import com.marverenic.music.utils.Navigate;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class AlbumGridAdapter extends BaseAdapter implements SectionIndexer, View.OnClickListener, View.OnLongClickListener {

    private ArrayList<Album> data;
    private Context context;
    private ArrayList<Character> sectionCharacter = new ArrayList<>();
    private ArrayList<Integer> sectionStartingPosition = new ArrayList<>();
    private ArrayList<Integer> sectionAtPosition = new ArrayList<>();

    public AlbumGridAdapter(Context context){
        this(Library.getAlbums(), context);
    }

    public AlbumGridAdapter(ArrayList<Album> data, Context context) {
        this.data = data;
        this.context = context;

        String name;
        char thisChar;
        int sectionIndex = -1;
        for(int i = 0; i < data.size(); i++){
            name = data.get(i).albumName.toUpperCase();

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
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final AlbumViewHolder viewHolder;
        final Album a = data.get(position);

        if (convertView == null) {
            // inflate the GridView item layout
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(R.layout.instance_album, parent, false);
            convertView.findViewById(R.id.albumInstance).setOnClickListener(this);
            convertView.findViewById(R.id.albumInstance).setOnLongClickListener(this);

            // initialize the view holder
            viewHolder = new AlbumViewHolder();
            viewHolder.art = (ImageView) convertView.findViewById(R.id.imageAlbumArt);
            viewHolder.title = (TextView) convertView.findViewById(R.id.textAlbumTitle);
            viewHolder.detail = (TextView) convertView.findViewById(R.id.textAlbumArtist);
            viewHolder.parent = (ViewGroup) convertView.findViewById(R.id.albumBackground);
            convertView.setTag(viewHolder);
        } else {
            // recycle the already inflated view
            viewHolder = (AlbumViewHolder) convertView.getTag();
        }

        viewHolder.title.setText(a.albumName);
        viewHolder.detail.setText(a.artistName);

        // Cancel any previous Picasso requests on this view
        Picasso.with(context).cancelRequest(viewHolder.art);

        // Cancel any previous color animations
        if (viewHolder.backgroundColorFade != null) viewHolder.backgroundColorFade.cancel();
        if (viewHolder.titleColorFade != null) viewHolder.titleColorFade.cancel();
        if (viewHolder.detailColorFade != null) viewHolder.detailColorFade.cancel();

        viewHolder.backgroundColorFade = null;
        viewHolder.titleColorFade = null;
        viewHolder.detailColorFade = null;

        // Load the album art into the layout's ImageView if this album art has a cover
        if (a.artUri != null && !a.artUri.equals("")) {
            // If the album's palette has already been generated, update the view's colors and begin to load the image
            if (a.artPrimaryPalette != 0 &&  a.artPrimaryTextPalette != 0 && a.artDetailTextPalette != 0) {
                Picasso.with(context).load("file://" + a.artUri).placeholder(R.drawable.art_default).resizeDimen(R.dimen.grid_art_size, R.dimen.grid_art_size).into(viewHolder.art);
                viewHolder.parent.setBackgroundColor(a.artPrimaryPalette);
                viewHolder.title.setTextColor(a.artPrimaryTextPalette);
                viewHolder.detail.setTextColor(a.artDetailTextPalette);
            }
            // If the album's palette hasn't already been generated, set the view's colors to the default,
            // load the image, find the colors, save the colors to the album, then update the view's colors the generated ones
            else {
                viewHolder.parent.setBackgroundColor(context.getResources().getColor(R.color.grid_background_default));
                viewHolder.title.setTextColor(context.getResources().getColor(R.color.grid_text));
                viewHolder.detail.setTextColor(context.getResources().getColor(R.color.grid_detail_text));
                Picasso.with(context).load("file://" + a.artUri).placeholder(R.drawable.art_default).resizeDimen(R.dimen.grid_art_size, R.dimen.grid_art_size).into(viewHolder.art, new Callback() {
                    @Override
                    public void onSuccess() {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                Bitmap loadedImage = ((BitmapDrawable) viewHolder.art.getDrawable()).getBitmap();
                                if (loadedImage != null) {
                                    Fetch.buildAlbumPalette(loadedImage,
                                            context.getResources().getColor(R.color.grid_background_default),
                                            context.getResources().getColor(R.color.grid_background_default),
                                            context.getResources().getColor(R.color.grid_background_default),
                                            a);

                                    Handler handler = new Handler(Looper.getMainLooper());

                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            viewHolder.backgroundColorFade = ObjectAnimator.ofObject(viewHolder.parent,
                                                    "backgroundColor",
                                                    new ArgbEvaluator(),
                                                    context.getResources().getColor(R.color.grid_background_default),
                                                    a.artPrimaryPalette);
                                            viewHolder.backgroundColorFade.setDuration(300).start();

                                            viewHolder.titleColorFade = ObjectAnimator.ofObject(viewHolder.title,
                                                    "textColor",
                                                    new ArgbEvaluator(),
                                                    context.getResources().getColor(R.color.grid_text),
                                                    a.artPrimaryTextPalette);
                                            viewHolder.titleColorFade.setDuration(300).start();

                                            viewHolder.detailColorFade = ObjectAnimator.ofObject(viewHolder.detail,
                                                    "textColor",
                                                    new ArgbEvaluator(),
                                                    context.getResources().getColor(R.color.grid_detail_text),
                                                    a.artDetailTextPalette);
                                            viewHolder.detailColorFade.setDuration(300).start();
                                        }
                                    });
                                }
                            }
                        }).start();
                    }

                    @Override
                    public void onError() {

                    }
                });
            }
        }
        else {
            // if there isn't any art, just load the placeholder image into the view and reset the colors
            Picasso.with(context).load(R.drawable.art_default).into(viewHolder.art);
            viewHolder.parent.setBackgroundColor(context.getResources().getColor(R.color.grid_background_default));
            viewHolder.title.setTextColor(context.getResources().getColor(R.color.grid_text));
            viewHolder.detail.setTextColor(context.getResources().getColor(R.color.grid_detail_text));
        }

        return convertView;
    }

    @Override
    public void onClick(View v) {
        int position = ((GridView) v.getParent()).getPositionForView(v);
        Album album = data.get(position);
        Navigate.to(context, LibraryPageActivity.class, "entry", album);
    }

    @Override
    public boolean onLongClick(View view) {
        final Album item = data.get(((GridView) view.getParent()).getPositionForView(view));

        AlertDialog.Builder dialog = new AlertDialog.Builder(context);

        dialog.setTitle(item.albumName)
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // There's nothing to do here
                    }
                })
                .setItems(R.array.queue_options_album, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0: //Queue this artist next
                                PlayerController.queueNext(LibraryScanner.getAlbumEntries(item));
                                break;
                            case 1: //Queue this artist last
                                PlayerController.queueLast(LibraryScanner.getAlbumEntries(item));
                                break;
                            case 2: //Go to artist
                                Navigate.to(context, LibraryPageActivity.class, "entry", LibraryScanner.findArtistById(item.artistId));
                                break;
                            case 3: //Add to playlist...
                                ArrayList<Playlist> playlists = Library.getPlaylists();
                                String[] playlistNames = new String[playlists.size()];

                                for (int i = 0; i < playlists.size(); i++ ){
                                    playlistNames[i] = playlists.get(i).toString();
                                }

                                new AlertDialog.Builder(context).setTitle("Add songs by \"" + item.artistName + "\" to playlist")
                                        .setItems(playlistNames, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                LibraryScanner.addPlaylistEntries(
                                                        context,
                                                        Library.getPlaylists().get(which),
                                                        LibraryScanner.getAlbumEntries(item));
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

    public void updateData(ArrayList<Album> albumLibrary) {
        this.data = albumLibrary;
        notifyDataSetChanged();
    }

    public class AlbumViewHolder {
        public ImageView art;
        public TextView title;
        public TextView detail;
        public ViewGroup parent;

        public ObjectAnimator backgroundColorFade;
        public ObjectAnimator titleColorFade;
        public ObjectAnimator detailColorFade;
    }
}
