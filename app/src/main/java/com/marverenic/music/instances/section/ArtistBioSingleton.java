package com.marverenic.music.instances.section;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.widget.CardView;
import android.support.v7.widget.GridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.marverenic.music.R;
import com.marverenic.music.lastfm.LArtist;
import com.marverenic.music.lastfm.Tag;
import com.marverenic.music.utils.Themes;
import com.marverenic.music.view.EnhancedAdapters.EnhancedViewHolder;
import com.marverenic.music.view.EnhancedAdapters.HeterogeneousAdapter;

public class ArtistBioSingleton extends HeterogeneousAdapter.SingletonSection<LArtist> {

    public static final int ID = 28;

    private boolean mHasRelatedArtists;

    public ArtistBioSingleton(LArtist data, boolean hasRelatedArtists) {
        super(ID, data);
        mHasRelatedArtists = hasRelatedArtists;
    }

    @Override
    public EnhancedViewHolder<LArtist> createViewHolder(HeterogeneousAdapter adapter,
                                                        ViewGroup parent) {
        return new ViewHolder(
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.instance_artist_bio, parent, false),
                mHasRelatedArtists);
    }

    public static class ViewHolder extends EnhancedViewHolder<LArtist>
            implements View.OnClickListener {

        private CardView cardView;
        private TextView bioText;
        private FrameLayout lfmButton;
        private String artistURL;

        public ViewHolder(View itemView, boolean hasRelatedArtists) {
            super(itemView);

            cardView = (CardView) itemView.findViewById(R.id.infoCard);
            bioText = (TextView) itemView.findViewById(R.id.infoText);
            lfmButton = (FrameLayout) itemView.findViewById(R.id.openLFMButton);
            lfmButton.setOnClickListener(this);

            cardView.setCardBackgroundColor(Themes.getBackgroundElevated());
            if (hasRelatedArtists) {
                ((GridLayoutManager.LayoutParams) itemView.getLayoutParams()).bottomMargin = 0;
            }
        }

        @Override
        @SuppressLint("SetTextI18n")
        public void update(LArtist item, int sectionPosition) {
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
