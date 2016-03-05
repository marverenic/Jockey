package com.marverenic.music.instances.section;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.PopupMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.R;
import com.marverenic.music.activity.instance.AlbumActivity;
import com.marverenic.music.activity.instance.ArtistActivity;
import com.marverenic.music.instances.Album;
import com.marverenic.music.instances.Library;
import com.marverenic.music.instances.PlaylistDialog;
import com.marverenic.music.utils.Navigate;
import com.marverenic.music.view.EnhancedAdapters.EnhancedViewHolder;
import com.marverenic.music.view.EnhancedAdapters.HeterogeneousAdapter;
import com.marverenic.music.view.ViewUtils;

import java.util.HashMap;
import java.util.List;

public class AlbumSection extends HeterogeneousAdapter.ListSection<Album> {

    public static final int ID = 7804;

    public AlbumSection(@NonNull List<Album> data) {
        super(ID, data);
    }

    @Override
    public EnhancedViewHolder<Album> createViewHolder(HeterogeneousAdapter adapter,
                                                      ViewGroup parent) {
        return new ViewHolder(
                LayoutInflater
                        .from(parent.getContext())
                        .inflate(R.layout.instance_album, parent, false));
    }

    public static class ViewHolder extends EnhancedViewHolder<Album>
            implements View.OnClickListener, Palette.PaletteAsyncListener,
            PopupMenu.OnMenuItemClickListener, RequestListener<String, GlideDrawable> {

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
        private ImageView artwork;
        private Album reference;

        private AsyncTask<Bitmap, Void, Palette> paletteTask;
        private ObjectAnimator backgroundAnimator;
        private ObjectAnimator titleAnimator;
        private ObjectAnimator detailAnimator;

        public ViewHolder(View itemView) {
            super(itemView);
            this.itemView = itemView;

            defaultFrameColor = itemView.getResources().getColor(R.color.grid_background_default);
            defaultTitleColor = itemView.getResources().getColor(R.color.grid_text);
            defaultDetailColor = itemView.getResources().getColor(R.color.grid_detail_text);

            container = (FrameLayout) itemView;
            albumName = (TextView) itemView.findViewById(R.id.instanceTitle);
            artistName = (TextView) itemView.findViewById(R.id.instanceDetail);
            ImageView moreButton = (ImageView) itemView.findViewById(R.id.instanceMore);
            artwork = (ImageView) itemView.findViewById(R.id.instanceArt);

            itemView.setOnClickListener(this);
            moreButton.setOnClickListener(this);
        }

        @Override
        public void update(Album item, int sectionPosition) {
            if (paletteTask != null && !paletteTask.isCancelled()) {
                paletteTask.cancel(true);
            }

            reference = item;
            albumName.setText(item.getAlbumName());
            artistName.setText(item.getArtistName());

            resetPalette();

            Glide.with(itemView.getContext())
                    .load("file://" + item.getArtUri())
                    .placeholder(R.drawable.art_default)
                    .animate(android.R.anim.fade_in)
                    .crossFade()
                    .listener(this)
                    .into(artwork);
        }

        @Override
        public boolean onException(Exception e, String model, Target<GlideDrawable> target,
                                   boolean isFirstResource) {
            return false;
        }

        @Override
        public boolean onResourceReady(GlideDrawable resource, String model,
                                       Target<GlideDrawable> target, boolean isFromMemoryCache,
                                       boolean isFirstResource) {
            if (isFromMemoryCache) {
                updatePalette(resource);
            } else {
                animatePalette(resource);
            }
            return false;
        }

        private void generatePalette(Drawable drawable) {
            if (colorCache.get(reference) == null) {
                paletteTask = Palette.from(ViewUtils.drawableToBitmap(drawable)).generate(this);
            }
        }

        private void resetPalette() {
            if (paletteTask != null && !paletteTask.isCancelled()) {
                paletteTask.cancel(true);
            }

            if (backgroundAnimator != null) {
                backgroundAnimator.setDuration(0);
                backgroundAnimator.cancel();
            }
            if (titleAnimator != null) {
                titleAnimator.setDuration(0);
                titleAnimator.cancel();
            }
            if (detailAnimator != null) {
                detailAnimator.setDuration(0);
                detailAnimator.cancel();
            }

            container.setBackgroundColor(defaultFrameColor);
            albumName.setTextColor(defaultTitleColor);
            artistName.setTextColor(defaultDetailColor);
        }

        private void updatePalette(Drawable drawable) {
            int[] colors = colorCache.get(reference);

            if (colors != null) {
                container.setBackgroundColor(colors[FRAME_COLOR]);
                albumName.setTextColor(colors[TITLE_COLOR]);
                artistName.setTextColor(colors[DETAIL_COLOR]);
            } else {
                resetPalette();
                generatePalette(drawable);
            }
        }

        private void animatePalette(Drawable drawable) {
            int[] colors = colorCache.get(reference);

            if (colors != null) {
                backgroundAnimator = ObjectAnimator.ofObject(
                        container,
                        "backgroundColor",
                        new ArgbEvaluator(),
                        defaultFrameColor,
                        colors[FRAME_COLOR]);
                backgroundAnimator.setDuration(300).start();

                titleAnimator = ObjectAnimator.ofObject(
                        albumName,
                        "textColor",
                        new ArgbEvaluator(),
                        defaultTitleColor,
                        colors[TITLE_COLOR]);
                titleAnimator.setDuration(300).start();

                detailAnimator = ObjectAnimator.ofObject(
                        artistName,
                        "textColor",
                        new ArgbEvaluator(),
                        defaultDetailColor,
                        colors[DETAIL_COLOR]);
                detailAnimator.setDuration(300).start();
            } else {
                generatePalette(drawable);
            }
        }

        @Override
        public void onGenerated(Palette palette) {
            int frameColor = palette.getVibrantColor(Color.TRANSPARENT);
            Palette.Swatch swatch = palette.getVibrantSwatch();

            if (swatch == null || frameColor == Color.TRANSPARENT) {
                frameColor = palette.getLightVibrantColor(Color.TRANSPARENT);
                swatch = palette.getLightVibrantSwatch();
            }
            if (swatch == null || frameColor == Color.TRANSPARENT) {
                frameColor = palette.getDarkVibrantColor(Color.TRANSPARENT);
                swatch = palette.getDarkVibrantSwatch();
            }
            if (swatch == null || frameColor == Color.TRANSPARENT) {
                frameColor = palette.getLightMutedColor(Color.TRANSPARENT);
                swatch = palette.getLightMutedSwatch();
            }
            if (swatch == null || frameColor == Color.TRANSPARENT) {
                frameColor = palette.getDarkMutedColor(Color.TRANSPARENT);
                swatch = palette.getDarkMutedSwatch();
            }

            int titleColor = defaultTitleColor;
            int detailColor = defaultDetailColor;

            if (swatch != null && frameColor != Color.TRANSPARENT) {
                titleColor = swatch.getTitleTextColor();
                detailColor = swatch.getBodyTextColor();
            } else {
                frameColor = defaultFrameColor;
            }

            colorCache.put(reference, new int[]{frameColor, titleColor, detailColor});
            animatePalette(null);
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.instanceMore:
                    final PopupMenu menu = new PopupMenu(itemView.getContext(), v, Gravity.END);
                    String[] options =
                            itemView.getResources().getStringArray(R.array.queue_options_album);
                    for (int i = 0; i < options.length; i++) {
                        menu.getMenu().add(Menu.NONE, i, i, options[i]);
                    }
                    menu.setOnMenuItemClickListener(this);
                    menu.show();
                    break;
                default:
                    Navigate.to(itemView.getContext(), AlbumActivity.class,
                            AlbumActivity.ALBUM_EXTRA, reference);
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
                            Library.findArtistById(reference.getArtistId()));
                    return true;
                case 3: //Add to playlist...
                    PlaylistDialog.AddToNormal.alert(
                            itemView,
                            Library.getAlbumEntries(reference),
                            itemView.getContext().getString(
                                            R.string.header_add_song_name_to_playlist, reference));
                    return true;
            }
            return false;
        }
    }
}
