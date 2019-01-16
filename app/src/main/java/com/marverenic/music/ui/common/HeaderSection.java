package com.marverenic.music.ui.common;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.marverenic.adapter.EnhancedViewHolder;
import com.marverenic.adapter.HeterogeneousAdapter;
import com.marverenic.music.R;

public class HeaderSection extends HeterogeneousAdapter.SingletonSection<String> {

    public HeaderSection(String header) {
        super(header);
    }

    @Override
    public boolean showSection(HeterogeneousAdapter adapter) {
        int thisIndex = adapter.getSectionIndex(this);
        return adapter.getSection(thisIndex + 1).getItemCount(adapter) > 0;
    }

    @Override
    public EnhancedViewHolder<String> createViewHolder(HeterogeneousAdapter adapter,
                                                       ViewGroup parent) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.subheader, parent, false));
    }

    public static final class ViewHolder extends EnhancedViewHolder<String> {

        private TextView subheaderText;

        public ViewHolder(View itemView) {
            super(itemView);
            subheaderText = itemView.findViewById(R.id.subheader);
        }

        @Override
        public void onUpdate(String sectionName, int sectionPosition) {
            subheaderText.setText(sectionName);
        }
    }
}
