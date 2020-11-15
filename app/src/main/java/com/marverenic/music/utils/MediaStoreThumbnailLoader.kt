package com.marverenic.music.utils

import android.annotation.TargetApi
import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.CancellationSignal
import android.util.Size
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.ResourceDecoder
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.model.ImageVideoWrapper
import com.bumptech.glide.load.model.stream.StreamModelLoader
import com.bumptech.glide.load.resource.bitmap.BitmapResource
import com.bumptech.glide.load.resource.gifbitmap.GifBitmapWrapper
import com.bumptech.glide.load.resource.gifbitmap.GifBitmapWrapperResource
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import java.io.InputStream

@TargetApi(29)
class MediaStoreThumbnailLoader(
    private val context: Context
) : StreamModelLoader<Uri> {

    override fun getResourceFetcher(
        model: Uri?,
        width: Int,
        height: Int
    ): DataFetcher<InputStream>? {
        return model?.let {
            MediaStoreThumbnailDataFetcher(
                contentResolver = context.contentResolver,
                uri = it,
                width = width,
                height = height
            )
        }
    }

    class Decoder private constructor(
        private val bitmapPool: BitmapPool
    ) : ResourceDecoder<ImageVideoWrapper, GifBitmapWrapper> {

        constructor(context: Context) : this(Glide.get(context).bitmapPool)

        override fun decode(
            source: ImageVideoWrapper?,
            width: Int,
            height: Int
        ): Resource<GifBitmapWrapper>? {
            val inputStream = source?.stream
            require(inputStream is MediaStoreThumbnailInputStream) {
                "This decoder can only be used with MediaStoreThumbnailLoader."
            }

            val bitmap = inputStream.bitmap ?: return null
            val bitmapResource = BitmapResource(bitmap, bitmapPool)
            return GifBitmapWrapperResource(GifBitmapWrapper(bitmapResource, null))
        }

        override fun getId() = ""
    }

    class BitmapDecoder private constructor(
        private val bitmapPool: BitmapPool
    ) : ResourceDecoder<ImageVideoWrapper, Bitmap> {

        constructor(context: Context) : this(Glide.get(context).bitmapPool)

        override fun decode(
            source: ImageVideoWrapper?,
            width: Int,
            height: Int
        ): Resource<Bitmap>? {
            val inputStream = source?.stream
            require(inputStream is MediaStoreThumbnailInputStream) {
                "This decoder can only be used with MediaStoreThumbnailLoader."
            }

            val bitmap = inputStream.bitmap ?: return null
            return BitmapResource(bitmap, bitmapPool)
        }

        override fun getId() = ""
    }
}

@TargetApi(29)
private class MediaStoreThumbnailDataFetcher(
    private val contentResolver: ContentResolver,
    private val uri: Uri,
    private val width: Int,
    private val height: Int
) : DataFetcher<InputStream> {

    private val cancellationSignal = CancellationSignal()

    override fun loadData(priority: Priority?): InputStream {
        return try {
            val image = contentResolver.loadThumbnail(uri, Size(width, height), cancellationSignal)
            MediaStoreThumbnailInputStream(image)
        } catch (e: FileNotFoundException) {
            MediaStoreThumbnailInputStream(null)
        }
    }

    override fun cleanup() {
    }

    override fun getId(): String {
        return "$uri[$width, $height]"
    }

    override fun cancel() {
        cancellationSignal.cancel()
    }
}

// This is a complete hack to make Glide play nicely with what I'm trying to get it to do.
private class MediaStoreThumbnailInputStream(
    val bitmap: Bitmap?
) : InputStream() {

    // Not actually used. The reader will cast the IS to this class and read the bitmap directly
    // to avoid a pointless in-memory copy.
    override fun read() = -1
}
