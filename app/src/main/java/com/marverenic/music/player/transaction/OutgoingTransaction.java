package com.marverenic.music.player.transaction;

import java.util.UUID;

import static com.marverenic.music.player.transaction.ChunkHeader.MAX_ENTRIES;

public final class OutgoingTransaction<T, E extends Throwable> {

    private final String mTransactionId;
    private final SplitFunction<T> mSplitFunction;
    private final int mSize;
    private final T mData;

    OutgoingTransaction(T data, int size, SplitFunction<T> splitFunction) {
        mTransactionId = UUID.randomUUID().toString();

        mSplitFunction = splitFunction;
        mSize = size;
        mData = data;
    }

    public void transmit(StartFunction<E> startFunction, SendFunction<T, E> sendFunction,
                         FinishFunction<E> finishFunction) throws E {

        begin(startFunction);
        send(sendFunction);
        finish(finishFunction);
    }

    private void begin(StartFunction<E> startFunction) throws E {
        TransactionToken token = new TransactionToken(mTransactionId, mSize);
        startFunction.start(token);
    }

    private void send(SendFunction<T, E> sendFunction) throws E {
        int offset;
        for (offset = 0; offset + MAX_ENTRIES < mSize; offset += MAX_ENTRIES) {
            T chunk = mSplitFunction.split(mData, offset, offset + MAX_ENTRIES);
            ChunkHeader header = new ChunkHeader(mTransactionId, offset, MAX_ENTRIES);

            sendFunction.send(header, chunk);
        }

        if (offset < mSize) {
            T chunk = mSplitFunction.split(mData, offset, mSize);
            ChunkHeader header = new ChunkHeader(mTransactionId, offset, mSize - offset);

            sendFunction.send(header, chunk);
        }
    }

    private void finish(FinishFunction<E> finishFunction) throws E {
        finishFunction.finish();
    }

    interface SplitFunction<T> {
        T split(T data, int startPos, int endPos);
    }

    public interface StartFunction<E extends Throwable> {
        void start(TransactionToken token) throws E;
    }

    public interface SendFunction<T, E extends Throwable> {
        void send(ChunkHeader header, T chunk) throws E;
    }

    public interface FinishFunction<E extends Throwable> {
        void finish() throws E;
    }

}
