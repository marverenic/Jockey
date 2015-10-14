package com.marverenic.music.instances.viewholder;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
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

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.marverenic.music.Library;
import com.marverenic.music.PlayerController;
import com.marverenic.music.R;
import com.marverenic.music.activity.instance.AlbumActivity;
import com.marverenic.music.activity.instance.ArtistActivity;
import com.marverenic.music.instances.Album;
import com.marverenic.music.utils.Navigate;
import com.marverenic.music.utils.PlaylistDialog;

import java.util.HashMap;

public class AlbumViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, Palette.PaletteAsyncListener, PopupMenu.OnMenuItemClickListener{

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

    private AsyncTask<Bitmap, Void, Palette> paletteTask;
    private ObjectAnimator backgroundAnimator;
    private ObjectAnimator titleAnimator;
    private ObjectAnimator detailAnimator;

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
        if (paletteTask != null && !paletteTask.isCancelled()) paletteTask.cancel(true);

        reference = a;
        albumName.setText(a.albumName);
        artistName.setText(a.artistName);

        if (backgroundAnimator != null){
            backgroundAnimator.setDuration(0);
            backgroundAnimator.cancel();
        }
        if (titleAnimator != null){
            titleAnimator.setDuration(0);
            titleAnimator.cancel();
        }
        if (detailAnimator != null){
            detailAnimator.setDuration(0);
            detailAnimator.cancel();
        }

        container.setBackgroundColor(defaultFrameColor);
        albumName.setTextColor(defaultTitleColor);
        artistName.setTextColor(defaultDetailColor);

        Glide.with(itemView.getContext())
                .load("file://" + a.artUri)
                .asBitmap()
                .diskCacheStrategy(DiskCacheStrategy.RESULT)
                .placeholder(R.drawable.art_default)
                .into(new BitmapImageViewTarget(artwork) {
                    @Override
                    @SuppressWarnings("unchecked")
                    public void onResourceReady(Bitmap bitmap, GlideAnimation anim) {
                        super.onResourceReady(bitmap, anim);
                        updatePalette();
                    }
                });
    }

    private void updatePalette() {
        if (paletteTask != null && !paletteTask.isCancelled()) paletteTask.cancel(true);

        Bitmap bitmap = ((BitmapDrawable) artwork.getDrawable()).getBitmap();
        int[] colors = colorCache.get(reference);
        if (colors == null)
            paletteTask = Palette.from(bitmap).generate(this);
        else{
            final ObjectAnimator backgroundAnimator = ObjectAnimator.ofObject(
                    container,
                    "backgroundColor",
                    new ArgbEvaluator(),
                    defaultFrameColor,
                    colors[FRAME_COLOR]);
            backgroundAnimator.setDuration(300).start();

            final ObjectAnimator titleAnimator = ObjectAnimator.ofObject(
                    albumName,
                    "textColor",
                    new ArgbEvaluator(),
                    defaultTitleColor,
                    colors[TITLE_COLOR]);
            titleAnimator.setDuration(300).start();

            final ObjectAnimator artistAnimator = ObjectAnimator.ofObject(
                    artistName,
                    "textColor",
                    new ArgbEvaluator(),
                    defaultDetailColor,
                    colors[DETAIL_COLOR]);
            artistAnimator.setDuration(300).start();
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

        colorCache.put(reference, new int[]{frameColor, titleColor, detailColor});

        if (backgroundAnimator != null) {
            backgroundAnimator.setDuration(0);
            backgroundAnimator.cancel();
        }
        if (titleAnimator != null){
            titleAnimator.setDuration(0);
            titleAnimator.cancel();
        }
        if (detailAnimator != null){
            detailAnimator.setDuration(0);
            detailAnimator.cancel();
        }

        backgroundAnimator = ObjectAnimator.ofObject(
                container,
                "backgroundColor",
                new ArgbEvaluator(),
                defaultFrameColor,
                frameColor);
        backgroundAnimator.setDuration(300).start();

        titleAnimator = ObjectAnimator.ofObject(
                albumName,
                "textColor",
                new ArgbEvaluator(),
                defaultTitleColor,
                titleColor);
        titleAnimator.setDuration(300).start();

        detailAnimator = ObjectAnimator.ofObject(
                artistName,
                "textColor",
                new ArgbEvaluator(),
                defaultDetailColor,
                detailColor);
        detailAnimator.setDuration(300).start();
    }

    private void resetPalette() {
        container.setBackgroundColor(defaultFrameColor);
        albumName.setTextColor(defaultTitleColor);
        artistName.setTextColor(defaultDetailColor);
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
                menu.setOnMenuItemClickListener(this);
                menu.show();
                break;
            default:
                Navigate.to(itemView.getContext(), AlbumActivity.class, AlbumActivity.ALBUM_EXTRA, reference);
                break;
        }
    }

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
                PlaylistDialog.AddToNormal.alert(
                        itemView,
                        Library.getAlbumEntries(reference),
                        itemView.getContext()
                                .getString(R.string.header_add_song_name_to_playlist, reference));
                return true;
        }
        return false;
    }
}