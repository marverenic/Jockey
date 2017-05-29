package com.marverenic.music.player.transaction;

import java.util.UUID;

import static com.marverenic.music.player.transaction.TransactionChunk.MAX_ENTRIES;

public final class OutgoingTransaction<T> {

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

    public void send(StartFunction startFunction, SendFunction<T> sendFunction,
                     FinishFunction finishFunction) {

        begin(startFunction);
        send(sendFunction);
        finish(finishFunction);
    }

    private void begin(StartFunction startFunction) {
        TransactionToken token = new TransactionToken(mTransactionId, mSize);
        startFunction.start(token);
    }

    private void send(SendFunction<T> sendFunction) {
        int offset;
        for (offset = 0; offset + MAX_ENTRIES < mSize; offset += MAX_ENTRIES) {
            T subData = mSplitFunction.split(mData, offset, offset + MAX_ENTRIES);
            TransactionChunk<T> chunk = new TransactionChunk<>(
                    mTransactionId, offset, MAX_ENTRIES, subData);

            sendFunction.send(chunk);
        }

        if (offset < mSize) {
            T subData = mSplitFunction.split(mData, offset, mSize);
            TransactionChunk<T> chunk = new TransactionChunk<>(
                    mTransactionId, offset, mSize - offset, subData);

            sendFunction.send(chunk);
        }
    }

    private void finish(FinishFunction finishFunction) {
        finishFunction.finish();
    }

    interface SplitFunction<T> {
        T split(T data, int startPos, int endPos);
    }

    public interface StartFunction {
        void start(TransactionToken token);
    }

    public interface SendFunction<T> {
        void send(TransactionChunk<T> chunk);
    }

    public interface FinishFunction {
        void finish();
    }

}
