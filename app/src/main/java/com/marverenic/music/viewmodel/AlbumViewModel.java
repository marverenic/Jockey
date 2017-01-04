package com.marverenic.music.viewmodel;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.databinding.BaseObservable;
import android.databinding.ObservableField;
import android.databinding.ObservableInt;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.support.annotation.ColorInt;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.PopupMenu;
import android.view.Gravity;
import android.view.View;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.activity.instance.AlbumActivity;
import com.marverenic.music.activity.instance.ArtistActivity;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.dialog.AppendPlaylistDialogFragment;
import com.marverenic.music.model.Album;
import com.marverenic.music.player.OldPlayerController;
import com.marverenic.music.view.ViewUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import timber.log.Timber;

public class AlbumViewModel extends BaseObservable {

    private static final String TAG_PLAYLIST_DIALOG = "SongViewModel.PlaylistDialog";

    @Inject MusicStore mMusicStore;

    private Context mContext;
    private FragmentManager mFragmentManager;
    private Album mAlbum;

    private ObservableField<Drawable> mArtistImage;
    private ObservableInt mTitleTextColor;
    private ObservableInt mArtistTextColor;
    private ObservableInt mBackgroundColor;

    public AlbumViewModel(Context context, FragmentManager fragmentManager) {
        mContext = context;
        mFragmentManager = fragmentManager;

        JockeyApplication.getComponent(mContext).inject(this);
    }

    public void setAlbum(Album album) {
        mAlbum = album;

        mArtistImage = new ObservableField<>();
        mTitleTextColor = new ObservableInt();
        mArtistTextColor = new ObservableInt();
        mBackgroundColor = new ObservableInt();

        defaultColors();

        if (mAlbum.getArtUri() != null) {
            int imageSize = mContext.getResources().getDimensionPixelSize(R.dimen.grid_width);

            Glide.with(mContext)
                    .load(new File(mAlbum.getArtUri()))
                    .placeholder(R.drawable.art_default)
                    .error(R.drawable.art_default)
                    .listener(new PaletteListener(mTitleTextColor, mArtistTextColor,
                            mBackgroundColor))
                    .into(new ObservableTarget(imageSize, mArtistImage));
        } else {
            Drawable fallback = ResourcesCompat.getDrawable(mContext.getResources(),
                    R.drawable.art_default, mContext.getTheme());

            mArtistImage.set(fallback);
        }

        notifyChange();
    }

    private void defaultColors() {
        defaultColors(mContext, mTitleTextColor, mArtistTextColor, mBackgroundColor);
    }

    private static void defaultColors(Context context, ObservableInt title, ObservableInt artist,
                                      ObservableInt background) {

        Resources res = context.getResources();
        Resources.Theme theme = context.getTheme();

        title.set(ResourcesCompat.getColor(res, R.color.grid_text, theme));
        artist.set(ResourcesCompat.getColor(res, R.color.grid_detail_text, theme));
        background.set(ResourcesCompat.getColor(res, R.color.grid_background_default, theme));
    }

    public String getAlbumTitle() {
        return mAlbum.getAlbumName();
    }

    public String getAlbumArtist() {
        return mAlbum.getArtistName();
    }

    public ObservableField<Drawable> getArtistImage() {
        return mArtistImage;
    }

    public ObservableInt getTitleTextColor() {
        return mTitleTextColor;
    }

    public ObservableInt getArtistTextColor() {
        return mArtistTextColor;
    }

    public ObservableInt getBackgroundColor() {
        return mBackgroundColor;
    }

    public View.OnClickListener onClickAlbum() {
        return v -> mContext.startActivity(AlbumActivity.newIntent(mContext, mAlbum));
    }

    public View.OnClickListener onClickMenu() {
        return v -> {
            PopupMenu menu = new android.support.v7.widget.PopupMenu(mContext, v, Gravity.END);
            menu.inflate(R.menu.instance_album);
            menu.setOnMenuItemClickListener(onMenuItemClick());
            menu.show();
        };
    }

    private PopupMenu.OnMenuItemClickListener onMenuItemClick() {
        return menuItem -> {
            switch (menuItem.getItemId()) {
                case R.id.menu_item_queue_item_next:
                    mMusicStore.getSongs(mAlbum).subscribe(
                            OldPlayerController::queueNext,
                            throwable -> {
                                Timber.e(throwable, "Failed to get songs");
                            });

                    return true;
                case R.id.menu_item_queue_item_last:
                    mMusicStore.getSongs(mAlbum).subscribe(
                            OldPlayerController::queueLast,
                            throwable -> {
                                Timber.e(throwable, "Failed to get songs");
                            });

                    return true;
                case R.id.menu_item_navigate_to_artist:
                    mMusicStore.findArtistById(mAlbum.getArtistId()).subscribe(
                            artist -> {
                                mContext.startActivity(ArtistActivity.newIntent(mContext, artist));
                            }, throwable -> {
                                Timber.e(throwable, "Failed to find artist");
                            });

                    return true;
                case R.id.menu_item_add_to_playlist:
                    mMusicStore.getSongs(mAlbum).subscribe(
                            songs -> {
                                new AppendPlaylistDialogFragment.Builder(mContext, mFragmentManager)
                                        .setSongs(songs, mAlbum.getAlbumName())
                                        .showSnackbarIn(R.id.list)
                                        .show(TAG_PLAYLIST_DIALOG);
                            }, throwable -> {
                                Timber.e(throwable, "Failed to get songs");
                            });

                    return true;
            }
            return false;
        };
    }

    private static class ObservableTarget extends SimpleTarget<GlideDrawable> {

        private ObservableField<Drawable> mTarget;

        public ObservableTarget(int size, ObservableField<Drawable> target) {
            super(size, size);
            mTarget = target;
        }

        @Override
        public void onLoadStarted(Drawable placeholder) {
            mTarget.set(placeholder);
        }

        @Override
        public void onLoadFailed(Exception e, Drawable errorDrawable) {
            mTarget.set(errorDrawable);
            Timber.e(e, "failed to load thumbnail");
        }

        @Override
        public void onResourceReady(GlideDrawable resource,
                                    GlideAnimation<? super GlideDrawable> glideAnimation) {

            Drawable start = mTarget.get();

            if (start != null) {
                setDrawableWithFade(start, resource);
            } else {
                setDrawable(resource);
            }
        }

        private void setDrawableWithFade(Drawable start, Drawable end) {
            TransitionDrawable transition = new TransitionDrawable(new Drawable[]{start, end});
            transition.setCrossFadeEnabled(true);
            transition.startTransition(300);

            setDrawable(transition);
        }

        private void setDrawable(Drawable resource) {
            mTarget.set(resource);
        }
    }

    private static class PaletteListener implements RequestListener<File, GlideDrawable> {

        private static Map<File, Palette.Swatch> sColorMap;

        private ObservableInt mTitleTextColor;
        private ObservableInt mArtistTextColor;
        private ObservableInt mBackgroundColor;

        static {
            sColorMap = new HashMap<>();
        }

        public PaletteListener(ObservableInt title, ObservableInt artist,
                               ObservableInt background) {
            mTitleTextColor = title;
            mArtistTextColor = artist;
            mBackgroundColor = background;
        }

        @Override
        public boolean onException(Exception e, File model, Target<GlideDrawable> target,
                                   boolean isFirstResource) {
            return false;
        }

        @Override
        public boolean onResourceReady(GlideDrawable resource, File model,
                                       Target<GlideDrawable> target, boolean isFromMemoryCache,
                                       boolean isFirstResource) {

            if (sColorMap.containsKey(model)) {
                Palette.Swatch swatch = sColorMap.get(model);
                if (isFromMemoryCache) {
                    setSwatch(swatch);
                } else {
                    animateSwatch(swatch);
                }
            } else {
                generateSwatch(model, resource);
            }

            return false;
        }

        private void generateSwatch(File source, Drawable image) {
            Palette.from(ViewUtils.drawableToBitmap(image)).generate(palette -> {
                Palette.Swatch swatch = pickSwatch(palette);

                sColorMap.put(source, swatch);
                animateSwatch(swatch);
            });
        }

        private void setSwatch(Palette.Swatch swatch) {
            if (swatch == null) {
                return;
            }

            mBackgroundColor.set(swatch.getRgb());
            mTitleTextColor.set(swatch.getTitleTextColor());
            mArtistTextColor.set(swatch.getBodyTextColor());
        }

        private void animateSwatch(Palette.Swatch swatch) {
            if (swatch == null) {
                return;
            }

            animateColorValue(mBackgroundColor, swatch.getRgb());
            animateColorValue(mTitleTextColor, swatch.getTitleTextColor());
            animateColorValue(mArtistTextColor, swatch.getBodyTextColor());
        }

        private void animateColorValue(ObservableInt target, @ColorInt int toColor) {
            ObjectAnimator.ofObject(target, "", new ArgbEvaluator(), target.get(), toColor)
                    .setDuration(300)
                    .start();
        }

        private Palette.Swatch pickSwatch(Palette palette) {
            if (palette.getVibrantSwatch() != null) {
                return palette.getVibrantSwatch();
            }
            if (palette.getLightVibrantSwatch() != null) {
                return palette.getLightVibrantSwatch();
            }
            if (palette.getDarkVibrantSwatch() != null) {
                return palette.getDarkVibrantSwatch();
            }
            if (palette.getLightMutedSwatch() != null) {
                return palette.getLightMutedSwatch();
            }
            if (palette.getDarkMutedSwatch() != null) {
                return palette.getDarkMutedSwatch();
            }
            return null;
        }
    }
}
