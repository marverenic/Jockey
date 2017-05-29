package com.marverenic.music.player.transaction;

public final class TransactionChunk<T> {

    static final int MAX_ENTRIES = 500;

    private final String mTransactionId;
    private final int mOffset;
    private final int mSize;
    private final T mData;

    TransactionChunk(String transactionId, int offset, int size, T data) {
        mTransactionId = transactionId;
        mOffset = offset;
        mSize = size;
        mData = data;
    }

    String getTransactionId() {
        return mTransactionId;
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
