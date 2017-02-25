package com.marverenic.music.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.marverenic.adapter.EnhancedViewHolder;
import com.marverenic.adapter.HeterogeneousAdapter;
import com.marverenic.music.R;
import com.marverenic.music.activity.instance.ArtistActivity;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.lastfm.model.Image;
import com.marverenic.music.lastfm.model.LfmArtist;
import com.marverenic.music.model.Artist;

import java.util.List;

import timber.log.Timber;

public class RelatedArtistSection extends HeterogeneousAdapter.ListSection<LfmArtist> {

    private MusicStore mMusicStore;

    public RelatedArtistSection(MusicStore musicStore, @NonNull List<LfmArtist> data) {
        super(data);
        mMusicStore = musicStore;
    }

    @Override
    public EnhancedViewHolder<LfmArtist> createViewHolder(HeterogeneousAdapter adapter,
                                                          ViewGroup parent) {
        return new ViewHolder(
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.instance_artist_suggested, parent, false));
    }

    @Override
    public int getId(int position) {
        return get(position).getMbid().hashCode();
    }

    private class ViewHolder extends EnhancedViewHolder<LfmArtist>
            implements View.OnClickListener {

        private Artist localReference;

        private Context context;
        private ImageView artwork;
        private TextView artistName;

        public ViewHolder(View itemView) {
            super(itemView);
            context = itemView.getContext();

            itemView.setOnClickListener(this);

            artwork = (ImageView) itemView.findViewById(R.id.artist_suggested_artwork);
            artistName = (TextView) itemView.findViewById(R.id.artist_suggested_name);
        }

        @Override
        public void onUpdate(LfmArtist item, int sectionPosition) {
            mMusicStore.findArtistByName(item.getName())
                    .take(1)
                    .subscribe(
                            artist -> {
                                localReference = artist;
                            }, throwable -> {
                                Timber.e(throwable, "Failed to get local reference");
                            });

            Image image = item.getImageBySize(Image.Size.MEDIUM);
            String artUrl = (image == null) ? null : image.getUrl();

            Glide.with(context)
                    .load(artUrl)
                    .asBitmap()
                    .error(R.drawable.art_default)
                    .into(new BitmapImageViewTarget(artwork) {
                        @Override
                        protected void setResource(Bitmap resource) {
                            RoundedBitmapDrawable circularBitmapDrawable =
                                    RoundedBitmapDrawableFactory.create(
                                            context.getResources(),
                                            resource);
                            circularBitmapDrawable.setCircular(true);
                            artwork.setImageDrawable(circularBitmapDrawable);
                        }

                        @Override
                        public void onLoadFailed(Exception e, Drawable errorDrawable) {
                            RoundedBitmapDrawable circularBitmapDrawable =
                                    RoundedBitmapDrawableFactory.create(
                                            context.getResources(),
                                            ((BitmapDrawable) errorDrawable).getBitmap());
                            circularBitmapDrawable.setCircular(true);
                            artwork.setImageDrawable(circularBitmapDrawable);
                        }
                    });

            artistName.setText(item.getName());
        }

        @Override
        public void onClick(View v) {
            if (localReference != null) {
                context.startActivity(ArtistActivity.newIntent(context, localReference));
            }
        }
    }
}
