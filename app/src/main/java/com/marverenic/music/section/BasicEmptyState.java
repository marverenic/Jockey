package com.marverenic.music.section;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.marverenic.music.R;
import com.marverenic.heterogeneousadapter.EmptyState;
import com.marverenic.heterogeneousadapter.EnhancedViewHolder;

public abstract class BasicEmptyState extends EmptyState
        implements View.OnClickListener {

    @Override
    public final View onCreateView(RecyclerView.Adapter<EnhancedViewHolder> adapter,
                                   final ViewGroup parent) {

        final View layout = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.instance_empty, parent, false);

        parent.requestLayout();
        layout.setMinimumHeight(parent.getHeight());

        layout.findViewById(R.id.empty_button).setOnClickListener(this);
        layout.findViewById(R.id.empty_button2).setOnClickListener(this);

        return layout;
    }

    @Override
    public final void onUpdate(View emptyStateView) {
        TextView message = ((TextView) emptyStateView.findViewById(R.id.empty_message));
        TextView detail = ((TextView) emptyStateView.findViewById(R.id.empty_message_detail));
        TextView button1Label = ((TextView) emptyStateView.findViewById(R.id.empty_button_label));
        TextView button2Label = ((TextView) emptyStateView.findViewById(R.id.empty_button2_label));
        View button1 = emptyStateView.findViewById(R.id.empty_button);
        View button2 = emptyStateView.findViewById(R.id.empty_button2);

        message.setText(getMessage());
        detail.setText(getDetail());

        String b1Label = getAction1Label();
        String b2Label = getAction2Label();

        if (b1Label.isEmpty()) {
            button1.setVisibility(View.GONE);
        } else {
            button1.setVisibility(View.VISIBLE);
            button1Label.setText(b1Label);
        }

        if (b2Label.isEmpty()) {
            button2.setVisibility(View.GONE);
        } else {
            button2.setVisibility(View.VISIBLE);
            button2Label.setText(b2Label);
        }
    }

    public String getMessage() {
        return "";
    }

    public String getDetail() {
        return "";
    }

    public String getAction1Label() {
        return "";
    }

    public String getAction2Label() {
        return "";
    }

    public void onAction1() {

    }

    public void onAction2() {

    }

    @Override
    public final void onClick(View v) {
        if (v.getId() == R.id.empty_button) {
            onAction1();
        } else if (v.getId() == R.id.empty_button2) {
            onAction2();
        }
    }
}
