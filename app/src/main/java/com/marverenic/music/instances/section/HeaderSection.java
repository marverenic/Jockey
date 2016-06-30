package com.marverenic.music.instances.section;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.marverenic.music.R;
import com.marverenic.heterogeneousadapter.EnhancedViewHolder;
import com.marverenic.heterogeneousadapter.HeterogeneousAdapter;

public class HeaderSection extends HeterogeneousAdapter.SingletonSection<String> {

    public static final int ID = 60;
    public HeaderSection(String header) {
        super(ID, header);
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
            subheaderText = (TextView) itemView.findViewById(R.id.subheader);
        }

        @Override
        public void onUpdate(String sectionName, int sectionPosition) {
            subheaderText.setText(sectionName);
        }
    }
}
