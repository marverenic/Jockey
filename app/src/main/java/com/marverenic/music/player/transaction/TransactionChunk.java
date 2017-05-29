package com.marverenic.music.player.transaction;

public final class TransactionChunk<T> {

    static final int MAX_ENTRIES = 500;

    private final int mOffset;
    private final int mSize;
    private final T mData;

    TransactionChunk(int offset, int size, T data) {
        mOffset = offset;
        mSize = size;
        mData = data;
    }

    int getOffset() {
        return mOffset;
    }

    int getSize() {
        return mSize;
    }

    T getData() {
        return mData;
    }

}
