package com.marverenic.music.player.transaction;

public final class IncomingTransaction<T> {

    private Aggregator<T> mAggregator;
    private int mOffset;
    private int mSize;
    private T mAggregate;

    @SuppressWarnings("unchecked")
    IncomingTransaction(TransactionToken token, T emptyAggregate, Aggregator<T> aggregator) {
        mSize = token.getSize();
        mAggregator = aggregator;
        mAggregate = emptyAggregate;
    }

    public void receive(TransactionChunk<T> chunk) {
        if (chunk.getOffset() > mOffset) {
            throw new IllegalArgumentException("Chunk arrived early");
        } else if (chunk.getOffset() < mOffset) {
            throw new IllegalArgumentException("Duplicate chunk arrived");
        } else if (mOffset + chunk.getSize() > mSize) {
            throw new IllegalArgumentException("Chunk has too many entries");
        }

        mAggregate = mAggregator.aggregate(mAggregate, chunk.getData(), chunk.getOffset());
        mOffset += chunk.getSize();
    }

    public T getData() {
        if (mOffset < mSize) {
            throw new IllegalStateException("Not all data has been received");
        }
        return mAggregate;
    }

    interface Aggregator<T> {
        T aggregate(T aggregate, T toAppend, int offset);
    }

}
