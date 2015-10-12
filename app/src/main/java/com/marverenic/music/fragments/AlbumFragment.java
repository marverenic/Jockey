package com.marverenic.music.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.marverenic.music.Library;
import com.marverenic.music.R;
import com.marverenic.music.instances.viewholder.AlbumViewHolder;
import com.marverenic.music.instances.viewholder.EmptyStateViewHolder;
import com.marverenic.music.utils.Themes;
import com.marverenic.music.view.BackgroundDecoration;
import com.marverenic.music.view.GridSpacingDecoration;
import com.marverenic.music.view.ViewUtils;

public class AlbumFragment extends Fragment {

    private Adapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.list, container, false);
        RecyclerView albumRecyclerView = (RecyclerView) view.findViewById(R.id.list);
        albumRecyclerView.addItemDecoration(new BackgroundDecoration(Themes.getBackgroundElevated()));

        int paddingH =(int) getActivity().getResources().getDimension(R.dimen.global_padding);
        view.setPadding(paddingH, 0, paddingH, 0);

        adapter = new Adapter();
        albumRecyclerView.setAdapter(adapter);

        Library.addRefreshListener(adapter);

        final int numColumns = ViewUtils.getNumberOfGridColumns(getActivity());

        GridLayoutManager layoutManager = new GridLayoutManager(getActivity(), numColumns);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return (Library.getAlbums().isEmpty())? numColumns : 1;
            }
        });
        albumRecyclerView.setLayoutManager(layoutManager);

        albumRecyclerView.addItemDecoration(new GridSpacingDecoration((int) getResources().getDimension(R.dimen.grid_margin), numColumns));

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Library.addRefreshListener(adapter);
        // Assume this fragment's data has gone stale since it was last in the foreground
        adapter.onLibraryRefreshed();
    }

    @Override
    public void onPause() {
        super.onPause();
        Library.removeRefreshListener(adapter);
    }

    public class Adapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements Library.LibraryRefreshListener {

        public static final int EMPTY = 0;
        public static final int ALBUM = 1;

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            switch (viewType) {
                case EMPTY:
                    return new EmptyStateViewHolder(
                            LayoutInflater
                                    .from(viewGroup.getContext())
                                    .inflate(R.layout.instance_empty, viewGroup, false),
                            getActivity());
                case ALBUM:
                default:
                    return new AlbumViewHolder(
                            LayoutInflater
                                    .from(viewGroup.getContext())
                                    .inflate(R.layout.instance_album, viewGroup, false));
            }
        }

        @Override
        public int getItemViewType(int position){
            if (Library.getAlbums().isEmpty()) return EMPTY;
            return ALBUM;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
            if (getItemViewType(position) == ALBUM) {
                ((AlbumViewHolder) viewHolder).update(Library.getAlbums().get(position));
            }
            else if (viewHolder instanceof EmptyStateViewHolder &&
                    Library.hasRWPermission(getActivity())) {
                EmptyStateViewHolder emptyHolder = ((EmptyStateViewHolder) viewHolder);
                emptyHolder.setReason(R.string.empty);
                emptyHolder.setDetail(R.string.empty_detail);
                emptyHolder.setButton1(R.string.action_try_again);
            }
        }

        @Override
        public int getItemCount() {
            return (Library.getAlbums().isEmpty())? 1 : Library.getAlbums().size();
        }

        @Override
        public void onLibraryRefreshed() {
            notifyDataSetChanged();
        }
    }

}
