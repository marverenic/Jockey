package com.marverenic.music.adapter;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.widget.GridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.marverenic.adapter.EnhancedViewHolder;
import com.marverenic.adapter.HeterogeneousAdapter;
import com.marverenic.music.R;
import com.marverenic.music.lastfm.model.LfmArtist;
import com.marverenic.music.lastfm.model.Tag;

public class ArtistBioSingleton extends HeterogeneousAdapter.SingletonSection<LfmArtist> {

    private boolean mHasRelatedArtists;

    public ArtistBioSingleton(LfmArtist data, boolean hasRelatedArtists) {
        super(data);
        mHasRelatedArtists = hasRelatedArtists;
    }

    @Override
    public EnhancedViewHolder<LfmArtist> createViewHolder(HeterogeneousAdapter adapter,
                                                          ViewGroup parent) {
        return new ViewHolder(
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.instance_artist_bio, parent, false),
                mHasRelatedArtists);
    }

    public static class ViewHolder extends EnhancedViewHolder<LfmArtist>
            implements View.OnClickListener {

        private TextView bioText;
        private Button lfmButton;
        private String artistURL;

        public ViewHolder(View itemView, boolean hasRelatedArtists) {
            super(itemView);

            bioText = (TextView) itemView.findViewById(R.id.artist_bio_content);
            lfmButton = (Button) itemView.findViewById(R.id.artist_bio_lfm_link);
            lfmButton.setOnClickListener(this);

            if (hasRelatedArtists) {
                ((GridLayoutManager.LayoutParams) itemView.getLayoutParams()).bottomMargin = 0;
            }
        }

        @Override
        @SuppressLint("SetTextI18n")
        public void onUpdate(LfmArtist item, int sectionPosition) {
            Tag[] tags = item.getTags();
            String[] tagNames = new String[tags.length];

            for (int i = 0; i < tags.length; i++) {
                tagNames[i] = tags[i].getName();
            }

            String tagList = ""; // A list of tags to display in the card

            if (tags.length > 0) {
                // Capitalize the first letter of the tag
                tagList = tagNames[0].substring(0, 1).toUpperCase() + tagNames[0].substring(1);
                // Add up to 4 more tags (separated by commas)
                int tagCount = (tags.length < 5) ? tags.length : 5;
                for (int i = 1; i < tagCount; i++) {
                    tagList += ", " + tagNames[i].substring(0, 1).toUpperCase()
                            + tagNames[i].substring(1);
                }
            }

            String summary = item.getBio().getSummary();
            if (!summary.isEmpty()) {
                // Trim the "read more" attribution since there's already a button
                // linking to Last.fm
                summary = summary.substring(0, summary.lastIndexOf("<a href=\""));
            }

            bioText.setText(tagList
                    + ((!tagList.trim().isEmpty() && !summary.trim().isEmpty()) ? "\n\n" : "")
                    + summary);

            artistURL = item.getUrl();
        }

        @Override
        public void onClick(View v) {
            if (v.equals(lfmButton)) {
                Intent openLFMIntent = new Intent(Intent.ACTION_VIEW);
                if (artistURL == null) {
                    openLFMIntent.setData(Uri.parse("http://www.last.fm/home"));
                } else {
                    openLFMIntent.setData(Uri.parse(artistURL));
                }
                itemView.getContext().startActivity(openLFMIntent);
            }
        }
    }
}
