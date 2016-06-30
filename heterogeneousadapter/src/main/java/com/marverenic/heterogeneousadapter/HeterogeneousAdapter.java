package com.marverenic.heterogeneousadapter;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of {@link android.support.v7.widget.RecyclerView.Adapter} designed for data sets
 * that have different kinds of data that are grouped together into sections and displayed after
 * each other.
 *
 * To populate this adapter, use {@link #addSection(Section)}. All data lookup and ViewHolder
 * instantiation is handled by Sections. Sections appear in the order that they are added.
 */
public class HeterogeneousAdapter extends RecyclerView.Adapter<EnhancedViewHolder> {

    /**
     * Used in {@link #getItemViewType(int)} to denote that the empty state should be shown
     */
    private static final int EMPTY_TYPE = -2;

    private List<Section> mSections;
    private SparseArray<Section> mSectionIdMap;
    private EmptyState mEmptyState;

    /**
     * A reused Coordinate to avoid GC overhead when calling
     * {@link #lookupCoordinates(int, Coordinate))
     */
    private Coordinate mCoordinate;

    /**
     * Sets up a new HeterogeneousAdapter with no children
     */
    public HeterogeneousAdapter() {
        mSections = new ArrayList<>();
        mSectionIdMap = new SparseArray<>();
        mCoordinate = new Coordinate();
    }

    /**
     * @return The number of Sections currently attached to this Adapter
     */
    public int getSectionCount() {
        return mSections.size();
    }

    /**
     * Find the first section of a certain type in this Adapter
     * @param id The ID of the Section as set in the Section's constructor
     *           ({@link Section#Section(int)})
     * @return the Section in this Adapter with the same ID, or null if no Section with that ID
     *         was found
     */
    public Section getSectionById(int id) {
        return mSectionIdMap.get(id);
    }

    /**
     * Adds a {@link HeterogeneousAdapter.Section} to the bottom of this Adapter
     * @param section the Section to add
     * @return this Adapter, for chain building
     */
    public HeterogeneousAdapter addSection(@NonNull Section section) {
        return addSection(section, getSectionCount());
    }

    /**
     * Adds a {@link HeterogeneousAdapter.Section} to a specified index in this Adapter
     * @param section the Section to add
     * @param index the index to add this Section at
     * @return this Adapter, for chain building
     */
    public HeterogeneousAdapter addSection(@NonNull Section section, int index) {
        mSections.add(index, section);
        mSectionIdMap.put(section.getTypeId(), section);
        notifyDataSetChanged();
        return this;
    }

    /**
     * Removes a section in a specified index
     * @param index the index to remove
     */
    public void removeSection(int index) {
        Section removed = mSections.remove(index);
        mSectionIdMap.remove(removed.getTypeId());
        notifyDataSetChanged();
    }

    /**
     * Removes a section by its ID
     * @param id The ID of the Section as set in the Section's constructor
     *           ({@link Section#Section(int)})
     * @return true if the Sections in this adapter were changed, false otherwise
     */
    public boolean removeSectionById(int id) {
        Section requested = mSectionIdMap.get(id);
        if (requested != null && mSections.remove(requested)) {
            notifyDataSetChanged();
            return true;
        }
        return false;
    }

    public void setEmptyState(@Nullable EmptyState emptyState) {
        mEmptyState = emptyState;
    }

    /**
     * Converts a position in the entire data set to a Coordinate in the section list. This method
     * returns two values into the provided {@code Coordinate} so that it can be reused to save GC
     * overhead.
     *
     * @param position The position in the entire data set to lookup a coordinate of
     * @param coordinate @ {@code Coordinate} object to put the result into
     */
    private void lookupCoordinates(int position, Coordinate coordinate) {
        int runningTotal = 0;
        for (int i = 0; i < mSections.size(); i++) {
            int sectionTotal = mSections.get(i).getSize(this);
            if (position < runningTotal + sectionTotal) {
                coordinate.setSection(i);
                coordinate.setItemIndex(position - runningTotal);
                return;
            }
            runningTotal += sectionTotal;
        }
        coordinate.clear();
    }

    /**
     * Calculates the number of views contained in sections proceeding a given section
     * @param typeId The ID of the section to get the leading view count of
     * @return The number of views in this list that are above the first view in the given section
     */
    protected int getLeadingViewCount(int typeId) {
        int count = 0;
        for (Section section : mSections) {
            if (section.getTypeId() == typeId) {
                break;
            }
            count += section.getSize(this);
        }
        return count;
    }

    @Override
    public int getItemViewType(int position) {
        if (getDataSize() == 0) {
            return EMPTY_TYPE;
        }

        lookupCoordinates(position, mCoordinate);
        int section = mCoordinate.getSection();
        return mSections.get(section).getTypeId();
    }

    @Override
    public long getItemId(int position) {
        lookupCoordinates(position, mCoordinate);
        int section = mCoordinate.getSection();
        int item = mCoordinate.getItemIndex();
        return mSections.get(section).getId(item);
    }

    @Override
    public void onViewRecycled(EnhancedViewHolder holder) {
        // TODO determine where to delegate this
        super.onViewRecycled(holder);
    }

    @Override
    public EnhancedViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == EMPTY_TYPE) {
            return mEmptyState.createViewHolder(this, parent);
        }
        return mSectionIdMap.get(viewType).createViewHolder(this, parent);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onBindViewHolder(EnhancedViewHolder holder, int position) {
        if (holder instanceof EmptyState.EmptyViewHolder) {
            ((EmptyState.EmptyViewHolder) holder).onUpdate(null, position);
        } else {
            lookupCoordinates(position, mCoordinate);
            int section = mCoordinate.getSection();
            int item = mCoordinate.getItemIndex();
            holder.onUpdate(mSections.get(section).get(item), item);
        }
    }

    /**
     * @return The number of visible data entries in all sections. This value does not necessarily
     *         correspond to the value returned by {@link #getItemCount()}
     */
    private int getDataSize() {
        int sum = 0;
        for (Section s : mSections) {
            sum += s.getSize(this);
        }
        return sum;
    }

    @Override
    public final int getItemCount() {
        int count = getDataSize();

        if (count == 0 && mEmptyState != null) {
            return 1;
        } else {
            return count;
        }
    }

    /**
     * Holds a group of sequential items if the same type to be displayed in a
     * {@link HeterogeneousAdapter}. Sections act as {@link RecyclerView.Adapter}s with the
     * condition that they may only have one type of ItemView
     * @param <Type> The type of data that this Section holds
     * @see HeterogeneousAdapter.ListSection
     * @see HeterogeneousAdapter.SingletonSection
     */
    public static abstract class Section<Type> {

        private int mTypeId;

        /**
         * @param typeId The item type ID as used by
         *               {@link RecyclerView.Adapter#getItemViewType(int)}. This ID is constant
         *               for all items in this section. This value should be unique and constant
         *               to the each class that extends Section. This value MUST be unique among
         *               all Sections that are put in the same HeterogeneousAdapter.
         */
        public Section (int typeId) {
            mTypeId = typeId;
        }

        /**
         * Creates a ViewHolder for the {@link HeterogeneousAdapter} this Section is attached to
         * @param adapter the Adapter requesting a new ViewHolder
         * @param parent the ViewGroup that this ViewHolder will be placed into
         * @return A valid ViewHolder that may be used for items in this Section
         * @see {@link RecyclerView.Adapter#createViewHolder(ViewGroup, int)}
         */
        public abstract EnhancedViewHolder<Type> createViewHolder(HeterogeneousAdapter adapter,
                                                                  ViewGroup parent);

        /**
         * Get the ID of an item in the data set
         * @param position The index in the data set that an ID has been requested for
         * @return The ID of this item or {@link RecyclerView#NO_ID}
         * @see android.support.v7.widget.RecyclerView.Adapter#getItemId(int)
         */
        public long getId(int position) {
            return RecyclerView.NO_ID;
        }

        /**
         * Override this method to hide this Section if its visibility is dependent on another
         * external condition. The default implementation always shows this section.
         * @param adapter The adapter this Section is attached to
         * @return true if this section should be shown, false if it should be hidden
         */
        public boolean showSection(HeterogeneousAdapter adapter) {
            return true;
        }

        /**
         * @param adapter The adapter this Section is attached to
         * @return The number of items in this section
         */
        public abstract int getSize(HeterogeneousAdapter adapter);

        /**
         * Returns an item in the data set used to populate a ViewHolder
         * @param position The index of the item to return
         * @return The item at the specified index in this Section's data set
         */
        public abstract Type get(int position);

        /**
         * @return The item type ID as used by
         *         {@link RecyclerView.Adapter#getItemViewType(int)}. This ID is constant
         *         for all items in this section. This value should be unique and constant
         *         to the each class that extends Section. This value MUST be unique among
         *         all Sections that are put in the same HeterogeneousAdapter.
         */
        public final int getTypeId() {
            return mTypeId;
        }
    }

    /**
     * An extension of {@link HeterogeneousAdapter.Section} that always has exactly one item in
     * the set
     * @param <Type> The class of the item that this Section shows. You may use {@link Void}
     *              if this Section has no data
     */
    public static abstract class SingletonSection<Type> extends Section<Type> {

        private Type mData;

        /**
         * @param typeId The item type ID as used by
         *               {@link RecyclerView.Adapter#getItemViewType(int)}. This ID is constant
         *               for all items in this section. This value should be unique and constant
         *               to the each class that extends Section. This value MUST be unique among
         *               all Sections that are put in the same HeterogeneousAdapter.
         * @param data The item to show in this Section
         */
        public SingletonSection(int typeId, Type data) {
            super(typeId);
            mData = data;
        }

        @Override
        public final int getSize(HeterogeneousAdapter adapter) {
            return showSection(adapter) ? 1 : 0;
        }

        @Override
        public final Type get(int position) {
            return mData;
        }

    }

    /**
     * An extension of {@link .HeterogeneousAdapter.Section} used to show a list of items of the
     * same type
     * @param <Type> The class of the data that this Section shows.
     */
    public static abstract class ListSection<Type> extends Section<Type> {

        private List<Type> mData;

        /**
         * @param typeId The item type ID as used by
         *               {@link RecyclerView.Adapter#getItemViewType(int)}. This ID is constant
         *               for all items in this section. This value should be unique and constant
         *               to the each class that extends Section. This value MUST be unique among
         *               all Sections that are put in the same HeterogeneousAdapter.
         * @param data The data to populate this Section with
         */
        public ListSection(int typeId, @NonNull List<Type> data) {
            super(typeId);
            mData = data;
        }

        /**
         * @return the backing data set
         */
        public List<Type> getData() {
            return mData;
        }

        /**
         * Replace the active data set. Callers are responsible for calling
         * {@link RecyclerView.Adapter#notifyDataSetChanged()}
         * @param mData The new data set to back this Section
         */
        public void setData(@NonNull List<Type> mData) {
            this.mData = mData;
        }

        @Override
        public final int getSize(HeterogeneousAdapter adapter) {
            return showSection(adapter) ? mData.size() : 0;
        }

        @Override
        public final Type get(int position) {
            return mData.get(position);
        }
    }
}
