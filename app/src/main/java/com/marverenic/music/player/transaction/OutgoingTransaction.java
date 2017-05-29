package com.marverenic.music.player.transaction;

import static com.marverenic.music.player.transaction.TransactionChunk.MAX_ENTRIES;

public final class OutgoingTransaction<T> {

    private SplitFunction<T> mSplitFunction;
    private int mSize;
    private T mData;

    OutgoingTransaction(T data, int size, SplitFunction<T> splitFunction) {
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
        TransactionToken token = new TransactionToken(mSize);
        startFunction.start(token);
    }

    private void send(SendFunction<T> sendFunction) {
        int offset;
        for (offset = 0; offset + MAX_ENTRIES < mSize; offset += MAX_ENTRIES) {
            T subData = mSplitFunction.split(mData, offset, offset + MAX_ENTRIES);
            TransactionChunk<T> chunk = new TransactionChunk<>(offset, MAX_ENTRIES, subData);

            sendFunction.send(chunk);
        }

        if (offset < mSize) {
            T subData = mSplitFunction.split(mData, offset, mSize);
            TransactionChunk<T> chunk = new TransactionChunk<>(offset, mSize - offset, subData);

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
