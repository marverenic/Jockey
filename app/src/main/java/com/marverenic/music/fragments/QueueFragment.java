package com.marverenic.music.fragments;

import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ItemDecoration;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.marverenic.music.R;
import com.marverenic.music.instances.section.LibraryEmptyState;
import com.marverenic.music.instances.section.QueueSection;
import com.marverenic.music.instances.section.SpacerSingleton;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.view.DragBackgroundDecoration;
import com.marverenic.music.view.DragDividerDecoration;
import com.marverenic.heterogeneousadapter.DragDropAdapter;
import com.marverenic.heterogeneousadapter.DragDropDecoration;
import com.marverenic.music.view.InsetDecoration;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;

public class QueueFragment extends Fragment implements PlayerController.UpdateListener {

    private int lastPlayIndex;

    private RecyclerView mRecyclerView;
    private DragDropAdapter mAdapter;
    private QueueSection mQueueSection;
    private SpacerSingleton mBottomSpacer;

    private int itemHeight;
    private int dividerHeight;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.list, container, false);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.list);

        setupAdapter();
        setupRecyclerView();

        // Remove the list padding on landscape tablets
        Configuration config = getResources().getConfiguration();
        if (config.orientation == ORIENTATION_LANDSCAPE
                && config.smallestScreenWidthDp >= 600) {
            view.setPadding(0, 0, 0, 0);
        }

        /*
            Because of the way that CoordinatorLayout lays out children, there isn't a way to get
            the height of this list until it's about to be shown. Since this fragment is dependent
            on having an accurate height of the list (in order to pad the bottom of the list so that
            the playing song is always at the top of the list), we need to have a way to be informed
            when the list has a valid height before it's shown to the user.

            This post request will be run after the layout has been assigned a height and before
            it's shown to the user so that we can set the bottom padding correctly.
         */
        view.post(this::scrollToNowPlaying);

        return view;
    }

    private void setupAdapter() {
        if (mQueueSection == null) {
            mAdapter = new DragDropAdapter();
            mAdapter.setHasStableIds(true);
            mAdapter.attach(mRecyclerView);

            mQueueSection = new QueueSection(this, PlayerController.getQueue());
            mBottomSpacer = new SpacerSingleton(0);

            mAdapter.setDragSection(mQueueSection);
            mAdapter.addSection(mBottomSpacer);

            mAdapter.setEmptyState(new LibraryEmptyState(getActivity()) {
                @Override
                public String getEmptyMessage() {
                    return getString(R.string.empty_queue);
                }

                @Override
                public String getEmptyMessageDetail() {
                    return getString(R.string.empty_queue_detail);
                }

                @Override
                public String getEmptyAction1Label() {
                    return "";
                }

                @Override
                public String getEmptyAction2Label() {
                    return "";
                }
            });
        } else {
            mQueueSection.setData(PlayerController.getQueue());
            mAdapter.notifyDataSetChanged();
        }
    }

    private void setupRecyclerView() {
        itemHeight = (int) getResources().getDimension(R.dimen.list_height);
        dividerHeight = (int) getResources().getDisplayMetrics().density;

        Drawable shadow = ContextCompat.getDrawable(getContext(), R.drawable.list_drag_shadow);

        ItemDecoration background = new DragBackgroundDecoration();
        ItemDecoration divider = new DragDividerDecoration(getContext(), true, R.id.instance_blank);
        ItemDecoration dragShadow = new DragDropDecoration((NinePatchDrawable) shadow);

        mRecyclerView.addItemDecoration(background);
        mRecyclerView.addItemDecoration(divider);
        mRecyclerView.addItemDecoration(dragShadow);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        boolean portrait = getResources().getConfiguration().orientation != ORIENTATION_LANDSCAPE;
        boolean tablet = getResources().getConfiguration().smallestScreenWidthDp < 600;
        if (portrait || tablet) {
            // Add an inner shadow on phones and portrait tablets
            mRecyclerView.addItemDecoration(new InsetDecoration(
                    ContextCompat.getDrawable(getContext(), R.drawable.inset_shadow),
                    (int) getResources().getDimension(R.dimen.inset_shadow_height)));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        PlayerController.registerUpdateListener(this);
        // Assume this fragment's data has gone stale since it was last in the foreground
        onUpdate();
        scrollToNowPlaying();
    }

    @Override
    public void onPause() {
        super.onPause();
        PlayerController.unregisterUpdateListener(this);
    }

    @Override
    public void onUpdate() {
        setupAdapter();

        int currentIndex = PlayerController.getQueuePosition();
        int previousIndex = lastPlayIndex;

        if (currentIndex != lastPlayIndex) {
            lastPlayIndex = currentIndex;

            updateView(previousIndex);
            updateView(currentIndex);

            if (shouldScrollToCurrent()) {
                scrollToNowPlaying();
            }
        }
    }

    /**
     * When views are being updated and scrolled passed at the same time, the attached
     * {@link ItemDecoration}s will not appear on the
     * changed item because of its animation.
     *
     * Because this animation implies that items are being removed from the queue, this method
     * will manually update a specific view in a RecyclerView if it's visible. If it's not visible,
     * {@link android.support.v7.widget.RecyclerView.Adapter#notifyItemChanged(int)} will be
     * called instead.
     * @param index The index of the item in the attached RecyclerView adapter to be updated
     */
    private void updateView(int index) {
        View topView = mRecyclerView.getChildAt(0);
        View bottomView = mRecyclerView.getChildAt(mRecyclerView.getChildCount() - 1);

        int start = mRecyclerView.getChildAdapterPosition(topView);
        int end = mRecyclerView.getChildAdapterPosition(bottomView);

        if (index - start >= 0 && index - start < end) {
            ViewGroup itemView = (ViewGroup) mRecyclerView.getChildAt(index - start);
            if (itemView != null) {
                itemView.findViewById(R.id.instancePlayingIndicator)
                        .setVisibility(index == lastPlayIndex
                                ? View.VISIBLE
                                : View.GONE);
            }
        } else {
            mAdapter.notifyItemChanged(index);
        }
    }

    /**
     * @return true if the currently playing song is above or below the current item by the
     *         list's height, if the queue has been restarted, or if repeat all is enabled and
     *         the user wrapped from the front of the queue to the end of the queue
     */
    private boolean shouldScrollToCurrent() {
        int queueSize = mQueueSection.getData().size();

        View topView = mRecyclerView.getChildAt(0);
        View bottomView = mRecyclerView.getChildAt(mRecyclerView.getChildCount() - 1);

        int topIndex = mRecyclerView.getChildAdapterPosition(topView);
        int bottomIndex = mRecyclerView.getChildAdapterPosition(bottomView);

        return Math.abs(topIndex - lastPlayIndex) <= (bottomIndex - topIndex)
                || (queueSize - bottomIndex <= 2 && lastPlayIndex == 0)
                || (bottomIndex - queueSize <= 2 && lastPlayIndex == queueSize - 1);
    }

    private void scrollToNowPlaying() {
        int queueSize = mQueueSection.getData().size();

        int padding = (lastPlayIndex - queueSize) * (itemHeight + dividerHeight) - dividerHeight;
        mBottomSpacer.setHeight(padding);

        mAdapter.notifyItemChanged(queueSize);
        ((LinearLayoutManager) mRecyclerView.getLayoutManager())
                .scrollToPositionWithOffset(lastPlayIndex, 0);
    }

    public void updateShuffle() {
        setupAdapter();
        lastPlayIndex = PlayerController.getQueuePosition();
        scrollToNowPlaying();
    }
}
