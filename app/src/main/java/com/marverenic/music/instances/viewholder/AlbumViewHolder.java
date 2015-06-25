package com.marverenic.music.instances.viewholder;

import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.support.v7.app.AlertDialog;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.marverenic.music.PlayerController;
import com.marverenic.music.R;
import com.marverenic.music.activity.instance.AlbumActivity;
import com.marverenic.music.activity.instance.ArtistActivity;
import com.marverenic.music.instances.Album;
import com.marverenic.music.Library;
import com.marverenic.music.instances.Playlist;
import com.marverenic.music.utils.Navigate;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;

public class AlbumViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, Callback, Palette.PaletteAsyncListener{

    // Used to cache Palette values in memory
    private static HashMap<Album, int[]> colorCache = new HashMap<>();
    private static final int FRAME_COLOR = 0;
    private static final int TITLE_COLOR = 1;
    private static final int DETAIL_COLOR = 2;

    private static int defaultFrameColor;
    private static int defaultTitleColor;
    private static int defaultDetailColor;

    private View itemView;
    private FrameLayout container;
    private TextView albumName;
    private TextView artistName;
    private ImageView moreButton;
    private ImageView artwork;
    private Album reference;

    public AlbumViewHolder(View itemView) {
        super(itemView);
        this.itemView = itemView;

        defaultFrameColor = itemView.getResources().getColor(R.color.grid_background_default);
        defaultTitleColor = itemView.getResources().getColor(R.color.grid_text);
        defaultDetailColor = itemView.getResources().getColor(R.color.grid_detail_text);

        container = (FrameLayout) itemView;
        albumName = (TextView) itemView.findViewById(R.id.instanceTitle);
        artistName = (TextView) itemView.findViewById(R.id.instanceDetail);
        moreButton = (ImageView) itemView.findViewById(R.id.instanceMore);
        artwork = (ImageView) itemView.findViewById(R.id.instanceArt);

        itemView.setOnClickListener(this);
        moreButton.setOnClickListener(this);
    }

    public void update(Album a){
        reference = a;
        albumName.setText(a.albumName);
        artistName.setText(a.artistName);

        container.setBackgroundColor(defaultFrameColor);
        albumName.setTextColor(defaultTitleColor);
        artistName.setTextColor(defaultDetailColor);
        moreButton.setColorFilter(defaultDetailColor);

        Picasso.with(itemView.getContext()).load("file://" + a.artUri)
                .placeholder(R.drawable.art_default)
                .resizeDimen(R.dimen.grid_art_size, R.dimen.grid_art_size)
                .into(artwork, this);
    }

    @Override
    public void onSuccess() {
        // Update the colors of the text when album art is loaded
        Bitmap image = ((BitmapDrawable) artwork.getDrawable()).getBitmap();

        int[] colors = colorCache.get(reference);
        if (colors == null)
            Palette.from(image).generate(this);
        else{
            //TODO Fade colors in
            container.setBackgroundColor(colors[FRAME_COLOR]);
            albumName.setTextColor(colors[TITLE_COLOR]);
            artistName.setTextColor(colors[DETAIL_COLOR]);
            moreButton.setColorFilter(colors[DETAIL_COLOR]);
        }
    }

    @Override
    public void onGenerated(Palette palette) {
        int frameColor = defaultFrameColor;
        int titleColor = defaultTitleColor;
        int detailColor = defaultDetailColor;

        if (palette.getVibrantSwatch() != null && palette.getVibrantColor(-1) != -1) {
            frameColor = palette.getVibrantColor(0);
            titleColor = palette.getVibrantSwatch().getTitleTextColor();
            detailColor = palette.getVibrantSwatch().getBodyTextColor();
        } else if (palette.getLightVibrantSwatch() != null && palette.getLightVibrantColor(-1) != -1) {
            frameColor = palette.getLightVibrantColor(0);
            titleColor = palette.getLightVibrantSwatch().getTitleTextColor();
            detailColor = palette.getLightVibrantSwatch().getBodyTextColor();
        } else if (palette.getDarkVibrantSwatch() != null && palette.getDarkVibrantColor(-1) != -1) {
            frameColor = palette.getDarkVibrantColor(0);
            titleColor = palette.getDarkVibrantSwatch().getTitleTextColor();
            detailColor = palette.getDarkVibrantSwatch().getBodyTextColor();
        } else if (palette.getLightMutedSwatch() != null && palette.getLightMutedColor(-1) != -1) {
            frameColor = palette.getLightMutedColor(0);
            titleColor = palette.getLightMutedSwatch().getTitleTextColor();
            detailColor = palette.getLightMutedSwatch().getBodyTextColor();
        } else if (palette.getDarkMutedSwatch() != null && palette.getDarkMutedColor(-1) != -1) {
            frameColor = palette.getDarkMutedColor(0);
            titleColor = palette.getDarkMutedSwatch().getTitleTextColor();
            detailColor = palette.getDarkMutedSwatch().getBodyTextColor();
        }

        //TODO fade colors in
        container.setBackgroundColor(frameColor);
        albumName.setTextColor(titleColor);
        artistName.setTextColor(detailColor);
        moreButton.setColorFilter(detailColor);

        colorCache.put(reference, new int[]{frameColor, titleColor, detailColor});
    }

    @Override
    public void onError() {
        container.setBackgroundColor(defaultFrameColor);
        albumName.setTextColor(defaultTitleColor);
        artistName.setTextColor(defaultDetailColor);
        moreButton.setColorFilter(defaultDetailColor);
    }

    @Override
    public void onClick(View v){
        switch (v.getId()){
            case R.id.instanceMore:
                final PopupMenu menu = new PopupMenu(itemView.getContext(), v, Gravity.END);
                String[] options = itemView.getResources().getStringArray(R.array.queue_options_album);
                for (int i = 0; i < options.length; i++) {
                    menu.getMenu().add(Menu.NONE, i, i, options[i]);
                }
                menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        switch (menuItem.getItemId()) {
                            case 0: //Queue this album next
                                PlayerController.queueNext(Library.getAlbumEntries(reference));
                                return true;
                            case 1: //Queue this album last
                                PlayerController.queueLast(Library.getAlbumEntries(reference));
                                return true;
                            case 2: //Go to artist
                                Navigate.to(
                                        itemView.getContext(),
                                        ArtistActivity.class,
                                        ArtistActivity.ARTIST_EXTRA,
                                        Library.findArtistById(reference.artistId));
                                return true;
                            case 3: //Add to playlist...
                                ArrayList<Playlist> playlists = Library.getPlaylists();
                                String[] playlistNames = new String[playlists.size()];

                                for (int i = 0; i < playlists.size(); i++ ){
                                    playlistNames[i] = playlists.get(i).toString();
                                }

                                new AlertDialog.Builder(itemView.getContext())
                                        .setTitle("Add songs on \"" + reference.albumName + "\" to playlist")
                                        .setItems(playlistNames, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                Library.addPlaylistEntries(
                                                        itemView.getContext(),
                                                        Library.getPlaylists().get(which),
                                                        Library.getAlbumEntries(reference));
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
                break;
            default:
                Navigate.to(itemView.getContext(), AlbumActivity.class, AlbumActivity.ALBUM_EXTRA, reference);
                break;
        }
    }
}