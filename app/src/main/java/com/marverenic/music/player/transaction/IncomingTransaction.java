package com.marverenic.music.player.transaction;

import android.support.annotation.NonNull;

public final class IncomingTransaction<T> {

    private Aggregator<T> mAggregator;

    private String mTransactionId;
    private int mOffset;
    private int mSize;
    private T mAggregate;

    IncomingTransaction(TransactionToken token, T emptyAggregate, Aggregator<T> aggregator) {
        if (token == null) {
            throw new IllegalArgumentException("Token cannot be null");
        } else if (aggregator == null) {
            throw new IllegalArgumentException("Aggregator cannot be null");
        }

        mTransactionId = token.getTransactionId();
        mSize = token.getSize();
        mAggregator = aggregator;
        mAggregate = emptyAggregate;
    }

    public void receive(@NonNull ChunkHeader header, T data) {
        if (!mTransactionId.equals(header.getTransactionId())) {
            throw new IllegalArgumentException("Chunk contains data from a different transaction");
        }

        if (header.getOffset() > mOffset) {
            throw new IllegalArgumentException("Chunk arrived early");
        } else if (header.getOffset() < mOffset) {
            throw new IllegalArgumentException("Duplicate chunk arrived");
        } else if (mOffset + header.getSize() > mSize) {
            throw new IllegalArgumentException("Chunk has too much data");
        }

        mAggregate = mAggregator.aggregate(mAggregate, data, header.getOffset());
        mOffset += header.getSize();
    }

    public boolean isTransmissionComplete() {
        return mOffset >= mSize;
    }

    public T getData() {
        if (!isTransmissionComplete()) {
            throw new IllegalStateException("Not all data has been received");
        }
        return mAggregate;
    }

    interface Aggregator<T> {
        T aggregate(T aggregate, T toAppend, int offset);
    }

}
