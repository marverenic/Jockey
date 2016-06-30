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
    public static final int ALWAYS_SHOWN = -1;

    private int mLinkedTypeId;

    public HeaderSection(String header, int linkedTypeId) {
        super(ID, header);
        mLinkedTypeId = linkedTypeId;
    }

    @Override
    public boolean showSection(HeterogeneousAdapter adapter) {
        if (mLinkedTypeId == ALWAYS_SHOWN) {
            return true;
        } else {
            HeterogeneousAdapter.Section dependency = adapter.getSectionById(mLinkedTypeId);
            return dependency == null || dependency.getSize(adapter) > 0;
        }
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
