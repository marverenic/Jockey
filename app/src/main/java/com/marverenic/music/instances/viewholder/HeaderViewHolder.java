package com.marverenic.music.instances.viewholder;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.marverenic.music.R;

public class HeaderViewHolder extends RecyclerView.ViewHolder {

    private TextView subheaderText;

    public HeaderViewHolder(View itemView) {
        super(itemView);
        subheaderText = (TextView) itemView.findViewById(R.id.subheader);
    }

    public void update(String sectionName) {
        subheaderText.setText(sectionName);
    }
}
