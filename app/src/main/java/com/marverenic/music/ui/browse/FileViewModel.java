package com.marverenic.music.ui.browse;

import android.content.Context;
import androidx.databinding.Bindable;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import androidx.annotation.Nullable;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import com.marverenic.music.BR;
import com.marverenic.music.R;
import com.marverenic.music.ui.BaseViewModel;

import java.io.File;

import rx.Subscription;
import timber.log.Timber;

public class FileViewModel extends BaseViewModel {

    private ThumbnailLoader mThumbnailLoader;

    private File mFile;
    private Drawable mThumbnail;
    private Drawable mDefaultThumbnail;

    private boolean mUsingDefaultArtwork;
    private Subscription mArtworkSubscription;

    @Nullable
    private OnFileSelectedListener mSelectionListener;

    public FileViewModel(Context context, ThumbnailLoader thumbnailLoader) {
        super(context);
        mThumbnailLoader = thumbnailLoader;

        Bitmap defaultArt = BitmapFactory.decodeResource(getResources(), R.drawable.art_default);
        mDefaultThumbnail = makeCircular(defaultArt);
    }

    private Drawable makeCircular(Bitmap image) {
        RoundedBitmapDrawable drawable = RoundedBitmapDrawableFactory.create(getResources(), image);
        drawable.setCircular(true);
        return drawable;
    }

    public void setFileSelectionListener(OnFileSelectedListener selectionListener) {
        mSelectionListener = selectionListener;
    }

    public void setFile(File file) {
        mFile = file;
        mThumbnail = null;
        mUsingDefaultArtwork = false;

        if (mArtworkSubscription != null) {
            mArtworkSubscription.unsubscribe();
        }

        mArtworkSubscription = mThumbnailLoader.getThumbnail(mFile)
                .map(this::makeCircular)
                .subscribe(artwork -> {
                    mThumbnail = artwork;
                    notifyPropertyChanged(BR.thumbnail);
                }, throwable -> {
                    Timber.e(throwable, "Failed to load artwork thumbnail");
                });

        notifyPropertyChanged(BR.fileName);
        notifyPropertyChanged(BR.thumbnail);
    }

    @Bindable
    public String getFileName() {
        return mFile.getName();
    }

    @Bindable
    public Drawable getThumbnail() {
        if (mThumbnail == null) {
            mUsingDefaultArtwork = true;
            return mDefaultThumbnail;
        } else if (mUsingDefaultArtwork) {
            // If the default artwork was shown, but a replacement image was loaded later,
            // fade it in
            mUsingDefaultArtwork = false;
            TransitionDrawable crossFade = new TransitionDrawable(new Drawable[] {
                    mDefaultThumbnail,
                    mThumbnail
            });
            crossFade.setCrossFadeEnabled(true);
            crossFade.startTransition(getResources()
                    .getInteger(R.integer.file_thumbnail_crossfade_duration_ms));
            return crossFade;
        } else {
            return mThumbnail;
        }
    }

    public void onClickFile() {
        if (mSelectionListener != null) {
            mSelectionListener.onFileSelected(mFile);
        }
    }

    interface OnFileSelectedListener {
        void onFileSelected(File file);
    }

}
