package com.marverenic.music.fragments;

import android.content.res.Configuration;
import android.graphics.drawable.NinePatchDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.marverenic.music.PlayerController;
import com.marverenic.music.R;
import com.marverenic.music.instances.Song;
import com.marverenic.music.instances.section.LibraryEmptyState;
import com.marverenic.music.instances.section.QueueSection;
import com.marverenic.music.instances.section.SpacerSingleton;
import com.marverenic.music.instances.viewholder.DragDropSongViewHolder;
import com.marverenic.music.utils.Themes;
import com.marverenic.music.view.EnhancedAdapters.DragBackgroundDecoration;
import com.marverenic.music.view.EnhancedAdapters.DragDividerDecoration;
import com.marverenic.music.view.EnhancedAdapters.DragDropAdapter;
import com.marverenic.music.view.EnhancedAdapters.DragDropDecoration;

import java.util.List;

public class QueueFragment extends Fragment implements PlayerController.UpdateListener,
        DragDropSongViewHolder.OnRemovedListener {

    private final List<Song> queue = PlayerController.getQueue();
    private int lastPlayIndex;
    private RecyclerView list;
    private DragDropAdapter adapter;
    private SpacerSingleton bottomSpacer;
    private int itemHeight;
    private int dividerHeight;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.list, container, false);

        // Remove the list padding on landscape tablets
        Configuration config = getResources().getConfiguration();
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE
                && config.smallestScreenWidthDp >= 600) {
            view.setPadding(0, 0, 0, 0);
        }

        itemHeight = (int) getResources().getDimension(R.dimen.list_height);
        dividerHeight = (int) getResources().getDisplayMetrics().density;
        bottomSpacer = new SpacerSingleton(QueueSection.ID, 0);

        adapter = new DragDropAdapter();
        adapter.setDragSection(new QueueSection(queue));
        adapter.addSection(bottomSpacer);
        adapter.setEmptyState(new LibraryEmptyState(getActivity()) {
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

        list = (RecyclerView) view.findViewById(R.id.list);
        adapter.attach(list);
        list.addItemDecoration(new DragBackgroundDecoration(Themes.getBackgroundElevated()));
        list.addItemDecoration(new DragDividerDecoration(getActivity(), R.id.empty_layout));
        //noinspection deprecation
        list.addItemDecoration(new DragDropDecoration(
                (NinePatchDrawable) getResources().getDrawable((Themes.isLight(getContext()))
                        ? R.drawable.list_drag_shadow_light
                        : R.drawable.list_drag_shadow_dark)));

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        list.setLayoutManager(layoutManager);

        /*
            Because of the way that CoordinatorLayout lays out children, there isn't a way to get
            the height of this list until it's about to be shown. Since this fragment is dependent
            on having an accurate height of the list (in order to pad the bottom of the list so that
            the playing song is always at the top of the list), we need to have a way to be informed
            when the list has a valid height before it's shown to the user.

            This post request will be run after the layout has been assigned a height and before
            it's shown to the user so that we can set the bottom padding correctly.
         */
        view.post(new Runnable() {
            @Override
            public void run() {
                scrollToNowPlaying();
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        PlayerController.registerUpdateListener(this);
        // Assume this fragment's data has gone stale since it was last in the foreground
        onUpdate();
    }

    @Override
    public void onPause() {
        super.onPause();
        PlayerController.unregisterUpdateListener(this);
    }

    @Override
    public void onUpdate() {
        int currentIndex = PlayerController.getQueuePosition();
        if (currentIndex != lastPlayIndex) {
            adapter.notifyItemChanged(lastPlayIndex);
            adapter.notifyItemChanged(currentIndex);

            lastPlayIndex = currentIndex;
            scrollToNowPlaying();
        }
    }

    private void scrollToNowPlaying() {
        int padding = (lastPlayIndex - queue.size()) * (itemHeight + dividerHeight) - dividerHeight;
        bottomSpacer.setHeight(padding);

        adapter.notifyItemChanged(queue.size());
        ((LinearLayoutManager) list.getLayoutManager())
                .scrollToPositionWithOffset(lastPlayIndex, 0);
    }

    public void updateShuffle() {
        queue.clear();
        queue.addAll(PlayerController.getQueue());
        adapter.notifyDataSetChanged();
        lastPlayIndex = PlayerController.getQueuePosition();
        scrollToNowPlaying();
    }

    @Override
    public void onItemRemoved(int index) {
        queue.remove(index);
        adapter.notifyItemRemoved(index);
    }
}
