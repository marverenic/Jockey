package com.marverenic.music.ui.browse;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.collection.ArrayMap;

import com.marverenic.music.R;
import com.marverenic.music.utils.Util;

import java.io.File;
import java.lang.ref.WeakReference;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * A utility class that is shared among a list of file views in a recycler view, and is responsible
 * for caching and managing requests to load song thumbnails. Ideally, we'd just use Glide's
 * implementation, but we use {@link Util#fetchThumbnailFromFile(Context, File, int)}.
 * There's no way for Glide to skip the read step – the metadata is used as the primary source,
 * which can't be read from Glide.
 *
 * Other than delegating to the {@code fetchArtwork()} method, this class implements an LRU cache
 * using a timestamp. In addition, this class also keeps a weak reference to all the bitmaps it
 * returns. This means that if a bitmap is evicted from the cache but still in use somewhere else
 * (i.e. is still visible on screen), then the bitmap can be reused if it is requested before it
 * gets garbage collected.
 *
 * Because only thumbnails are loaded here, the cache size is fairly modest. At most, this cache
 * will use approximately 10% of the heap.
 */
class ThumbnailLoader {

    private final Object mLock = new Object();

    private Context mContext;
    private int mThumbnailResolution;
    private int mMaxCacheElements;
    private int mCurrentTimestamp = 0;

    private ArrayMap<File, ThumbnailCacheEntry> mCache;
    private ArrayMap<File, WeakReference<Bitmap>> mWeakCache;

    ThumbnailLoader(Context context) {
        mContext = context;
        mThumbnailResolution = context.getResources().getDimensionPixelSize(R.dimen.list_thumbnail_size);

        long maxMemoryBytes = Runtime.getRuntime().maxMemory();
        // estimate 4 bytes per pixel
        long estimatedImageSizeBytes = mThumbnailResolution * mThumbnailResolution * 4;
        mMaxCacheElements = (int) (maxMemoryBytes / estimatedImageSizeBytes) / 10;

        mCache = new ArrayMap<>();
        mWeakCache = new ArrayMap<>();
    }

    void clearCache() {
        synchronized (mLock) {
            mCache.clear();
            mWeakCache.clear();
        }
    }

    Observable<Bitmap> getThumbnail(File file) {
        synchronized (mLock) {
            if (mCache.containsKey(file)) {
                // Bitmap is in cache. Return it and update its timestamp.
                ThumbnailCacheEntry cacheEntry = mCache.get(file);
                cacheEntry.timestamp = mCurrentTimestamp++;
                return cacheEntry.image;
            } else if (mWeakCache.containsKey(file)) {
                // Bitmap was evicted, but may still be in memory.
                WeakReference<Bitmap> reference = mWeakCache.get(file);
                if (reference != null) {
                    Bitmap restored = reference.get();
                    if (restored != null){
                        // Bitmap hasn't been GC'd. Restore it.
                        Observable<Bitmap> img = Observable.just(restored);
                        mCache.put(file, new ThumbnailCacheEntry(img, mCurrentTimestamp++));
                        evictOldestIfNecessary();
                        return img;
                    }
                }
            }

            Observable<Bitmap> bitmap = loadBitmap(file);
            evictOldestIfNecessary();
            return bitmap;
        }
    }

    private Observable<Bitmap> loadBitmap(File file) {
        Observable<Bitmap> image = Util.fetchThumbnailFromFile(mContext, file, mThumbnailResolution)
                .subscribeOn(Schedulers.io())
                .doOnNext(bitmap -> {
                    synchronized (mLock) {
                        mWeakCache.put(file, new WeakReference<>(bitmap));
                    }
                })
                .doOnError(throwable -> {
                    synchronized (mLock) {
                        mCache.remove(file);
                        mWeakCache.remove(file);
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .cache();

        mCache.put(file, new ThumbnailCacheEntry(image, mCurrentTimestamp++));
        return image;
    }

    private void evictOldestIfNecessary() {
        if (mCache.size() <= mMaxCacheElements) {
            return;
        }

        for (int i = 0; i < mWeakCache.size(); i++) {
            WeakReference<Bitmap> ref = mWeakCache.valueAt(i);
            if (ref == null || ref.get() == null) {
                mWeakCache.removeAt(i);
                i--;
            }
        }

        File lru = null;
        int lruTimestamp = Integer.MIN_VALUE;

        for (int i = 0; i < mCache.size(); i++) {
            File file = mCache.keyAt(i);
            ThumbnailCacheEntry cacheEntry = mCache.valueAt(i);
            if (cacheEntry.timestamp > lruTimestamp) {
                lruTimestamp = cacheEntry.timestamp;
                lru = file;
            }
        }

        if (lru != null) {
            mCache.remove(lru);
        }
    }

    private class ThumbnailCacheEntry {
        Observable<Bitmap> image;
        int timestamp;

        ThumbnailCacheEntry(Observable<Bitmap> image, int timestamp) {
            this.image = image;
            this.timestamp = timestamp;
        }
    }

}
