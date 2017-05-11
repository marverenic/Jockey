package com.marverenic.music.fragments;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ItemDecoration;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.marverenic.adapter.DragDropAdapter;
import com.marverenic.adapter.DragDropDecoration;
import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.adapter.LibraryEmptyState;
import com.marverenic.music.adapter.QueueSection;
import com.marverenic.music.adapter.SpacerSingleton;
import com.marverenic.music.model.Song;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.view.DragBackgroundDecoration;
import com.marverenic.music.view.DragDividerDecoration;
import com.marverenic.music.view.InsetDecoration;
import com.marverenic.music.view.QueueAnimator;
import com.marverenic.music.view.SnappingScroller;
import com.trello.rxlifecycle.FragmentEvent;

import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;

public class QueueFragment extends BaseFragment {

    @Inject PlayerController mPlayerController;

    private int lastPlayIndex;

    private RecyclerView mRecyclerView;
    private DragDropAdapter mAdapter;
    private QueueSection mQueueSection;
    private SpacerSingleton[] mBottomSpacers;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        JockeyApplication.getComponent(this).inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.list, container, false);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.list);

        boolean isLandscape = getContext().getResources().getConfiguration().orientation
                == ORIENTATION_LANDSCAPE;
        mRecyclerView.setNestedScrollingEnabled(!isLandscape);

        setupRecyclerView();

        mPlayerController.getQueue()
                .compose(bindToLifecycle())
                .subscribe(this::setupAdapter, throwable -> {
                    Timber.e(throwable, "Failed to update queue");
                });

        // Remove the list padding on landscape tablets
        Configuration config = getResources().getConfiguration();
        if (config.orientation == ORIENTATION_LANDSCAPE
                && config.smallestScreenWidthDp >= 600) {
            view.setPadding(0, 0, 0, 0);
        }

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (mQueueSection != null) mRecyclerView.post(this::setupSpacers);
    }

    private void setupAdapter(List<Song> queue) {
        if (mQueueSection == null) {
            mAdapter = new DragDropAdapter();
            mAdapter.setHasStableIds(true);
            mAdapter.attach(mRecyclerView);

            mRecyclerView.setItemAnimator(new QueueAnimator());
            mQueueSection = new QueueSection(this, mPlayerController, queue);
            mAdapter.setDragSection(mQueueSection);

            // Wait for a layout pass before calculating bottom spacing since it is dependent on the
            // height of the RecyclerView (which has not been computed yet)
            if (isAdded()) mRecyclerView.post(this::setupSpacers);

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

            mPlayerController.getQueuePosition()
                    .take(1)
                    .compose(bindUntilEvent(FragmentEvent.DESTROY_VIEW))
                    .subscribe(this::setQueuePosition, throwable -> {
                        Timber.e(throwable, "Failed to scroll to queue position");
                    });

            mPlayerController.getQueuePosition()
                    .skip(1)
                    .compose(bindUntilEvent(FragmentEvent.DESTROY_VIEW))
                    .subscribe(this::onQueuePositionChanged, throwable -> {
                        Timber.e(throwable, "Failed to scroll to queue position");
                    });

        } else if (!mQueueSection.getData().equals(queue)) {
            mQueueSection.setData(queue);
            mAdapter.notifyDataSetChanged();

            mPlayerController.getQueuePosition()
                    .skip(1)
                    .take(1)
                    .compose(bindUntilEvent(FragmentEvent.DESTROY_VIEW))
                    .subscribe(this::setQueuePosition, throwable -> {
                        Timber.e(throwable, "Failed to scroll to queue position");
                    });
        }
    }

    private void setupSpacers() {
        if (mBottomSpacers != null || !isAdded()) {
            return;
        }

        int itemHeight = (int) getResources().getDimension(R.dimen.list_height);
        int dividerHeight = (int) getResources().getDisplayMetrics().density;

        int listHeight = mRecyclerView.getMeasuredHeight();
        int listItemHeight = itemHeight + dividerHeight;
        int numberOfSpacers = (int) Math.ceil(listHeight / (float) listItemHeight) - 1;
        numberOfSpacers = Math.max(0, numberOfSpacers);

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

        ItemDecoration background = new DragBackgroundDecoration(R.id.song_drag_root);
        ItemDecoration divider = new DragDividerDecoration(getContext(), true, R.id.instance_blank);
        ItemDecoration dragShadow = new DragDropDecoration((NinePatchDrawable) shadow);

        mRecyclerView.addItemDecoration(background);
        mRecyclerView.addItemDecoration(divider);
        mRecyclerView.addItemDecoration(dragShadow);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        boolean portrait = getResources().getConfiguration().orientation != ORIENTATION_LANDSCAPE;
        boolean tablet = getResources().getConfiguration().smallestScreenWidthDp >= 600;
        if (portrait || !tablet) {
            // Add an inner shadow at the top of the list
            mRecyclerView.addItemDecoration(new InsetDecoration(
                    ContextCompat.getDrawable(getContext(), R.drawable.inset_top_shadow),
                    (int) getResources().getDimension(R.dimen.inset_top_shadow_height),
                    Gravity.TOP));
        } else {
            // Add an inner shadow at the bottom of the list
            mRecyclerView.addItemDecoration(new InsetDecoration(
                    ContextCompat.getDrawable(getContext(), R.drawable.inset_bottom_shadow),
                    getResources().getDimensionPixelSize(R.dimen.inset_bottom_shadow_height),
                    Gravity.BOTTOM));
        }
    }

    private void setQueuePosition(int currentIndex) {
        lastPlayIndex = currentIndex;
        scrollToNowPlaying();
    }

    private void onQueuePositionChanged(int currentIndex) {
        lastPlayIndex = currentIndex;
        if (shouldScrollToCurrent()) {
            smoothScrollToNowPlaying();
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

        RecyclerView.SmoothScroller scroller =
                new SnappingScroller(getContext(), SnappingScroller.SNAP_TO_START);
        scroller.setTargetPosition(lastPlayIndex);
        mRecyclerView.getLayoutManager().startSmoothScroll(scroller);
    }

    private void scrollToNowPlaying() {
        updateSpacers();

        ((LinearLayoutManager) mRecyclerView.getLayoutManager())
                .scrollToPositionWithOffset(lastPlayIndex, 0);
    }

}
