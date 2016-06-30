package com.marverenic.heterogeneousadapter;

/**
 * An ordered pair of integers used by {@link HeterogeneousAdapter} to lookup the index of data
 * items within sections.
 *
 * @see HeterogeneousAdapter#lookupPos(int)
 */
final class Coordinate {

    public static final int UNKNOWN_POSITION = -1;

    private int mSection;
    private int mItemIndex;

    public Coordinate() {
        this(UNKNOWN_POSITION, UNKNOWN_POSITION);
    }

    public Coordinate(int section, int itemIndex) {
        mSection = section;
        mItemIndex = itemIndex;
    }

    /**
     * Resets this Coordinate's values to the default {@link #UNKNOWN_POSITION}
     */
    public void clear() {
        setSection(UNKNOWN_POSITION);
        setItemIndex(UNKNOWN_POSITION);
    }

    /**
     * @param section The section index to update the Coordinate to point to
     */
    public void setSection(int section) {
        mSection = section;
    }

    /**
     * @param itemIndex The item within a section's data to set this Coordinate to point to
     */
    public void setItemIndex(int itemIndex) {
        mItemIndex = itemIndex;
    }

    /**
     * @return The section that this coordinate points to
     */
    public int getSection() {
        return mSection;
    }

    /**
     * @return The item index within a section's data that this coordinate points to
     */
    public int getItemIndex() {
        return mItemIndex;
    }
}
