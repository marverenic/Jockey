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

import com.marverenic.heterogeneousadapter.DragDropAdapter;
import com.marverenic.heterogeneousadapter.DragDropDecoration;
import com.marverenic.music.R;
import com.marverenic.music.instances.section.LibraryEmptyState;
import com.marverenic.music.instances.section.QueueSection;
import com.marverenic.music.instances.section.SpacerSingleton;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.view.DragBackgroundDecoration;
import com.marverenic.music.view.DragDividerDecoration;
import com.marverenic.music.view.InsetDecoration;
import com.marverenic.music.view.QueueAnimator;
import com.marverenic.music.view.SnappingScroller;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;

public class QueueFragment extends Fragment implements PlayerController.UpdateListener {

    private int lastPlayIndex;

    private RecyclerView mRecyclerView;
    private DragDropAdapter mAdapter;
    private QueueSection mQueueSection;
    private SpacerSingleton[] mBottomSpacers;

    private SnappingScroller mScroller;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.list, container, false);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.list);
        mScroller = new SnappingScroller(getContext(), SnappingScroller.SNAP_TO_START);

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

            mRecyclerView.setItemAnimator(new QueueAnimator());
            mQueueSection = new QueueSection(this, PlayerController.getQueue());
            mAdapter.setDragSection(mQueueSection);

            // Wait for a layout pass before calculating bottom spacing since it is dependent on the
            // height of the RecyclerView (which has not been computed yet)
            mRecyclerView.post(this::setupSpacers);

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
        } else if (!mQueueSection.getData().equals(PlayerController.getQueue())) {
            mQueueSection.setData(PlayerController.getQueue());
            mAdapter.notifyDataSetChanged();
        }
    }

    private void setupSpacers() {
        if (mBottomSpacers != null) {
            return;
        }

        int itemHeight = (int) getResources().getDimension(R.dimen.list_height);
        int dividerHeight = (int) getResources().getDisplayMetrics().density;

        int listHeight = mRecyclerView.getMeasuredHeight();
        int listItemHeight = itemHeight + dividerHeight;
        int numberOfSpacers = (int) Math.ceil(listHeight / (float) listItemHeight) - 1;

        int remainingListHeight = listHeight - listItemHeight;
        mBottomSpacers = new SpacerSingleton[numberOfSpacers];
        for (int i = 0; i < numberOfSpacers; i++) {
            int height;
            if (remainingListHeight % listItemHeight > 0) {
                height = remainingListHeight % listItemHeight;
            } else {
                height = listItemHeight;
            }
            remainingListHeight -= height;

            mBottomSpacers[i] = new SpacerSingleton(height);
            mAdapter.addSection(mBottomSpacers[i]);
        }

        mAdapter.notifyDataSetChanged();
        smoothScrollToNowPlaying();
    }

    private void setupRecyclerView() {
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

            mAdapter.notifyItemChanged(previousIndex);
            mAdapter.notifyItemChanged(currentIndex);

            if (shouldScrollToCurrent()) {
                smoothScrollToNowPlaying();
            }
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

    private void updateSpacers() {
        if (mBottomSpacers == null) {
            return;
        }

        int queueSize = mQueueSection.getData().size();
        int visibleSpacers = mBottomSpacers.length - (queueSize - lastPlayIndex) + 1;
        visibleSpacers = Math.max(0, visibleSpacers);
        int prevVisibleSpacers = 0;

        for (int i = 0; i < mBottomSpacers.length; i++) {
            if (mBottomSpacers[i].showSection()) {
                prevVisibleSpacers++;
            }
            mBottomSpacers[i].setShowSection(i < visibleSpacers);
        }

        if (visibleSpacers > prevVisibleSpacers) {
            int addedSpacers = visibleSpacers - prevVisibleSpacers;
            mAdapter.notifyItemRangeInserted(queueSize, addedSpacers);
        } else if (visibleSpacers < prevVisibleSpacers) {
            int firstSpacerIndex = queueSize + visibleSpacers;
            int removedSpacers = prevVisibleSpacers - visibleSpacers;
            mAdapter.notifyItemRangeRemoved(firstSpacerIndex, removedSpacers);
        }
    }

    private void smoothScrollToNowPlaying() {
        updateSpacers();

        mScroller.setTargetPosition(lastPlayIndex);
        mRecyclerView.getLayoutManager().startSmoothScroll(mScroller);
    }

    private void scrollToNowPlaying() {
        updateSpacers();

        ((LinearLayoutManager) mRecyclerView.getLayoutManager())
                .scrollToPositionWithOffset(lastPlayIndex, 0);
    }

    public void updateShuffle() {
        setupAdapter();
        lastPlayIndex = PlayerController.getQueuePosition();
        scrollToNowPlaying();
    }
}
