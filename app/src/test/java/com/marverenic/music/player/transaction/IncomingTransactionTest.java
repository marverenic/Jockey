package com.marverenic.music.player.transaction;

import org.junit.Before;
import org.junit.Test;

public class IncomingTransactionTest {

    // chosen by fair dice roll. guaranteed to be random.
    private static final String TRANS_ID = "4";
    private static final int TRANS_SIZE = 20;

    private IncomingTransaction<Object> mSubject;

    @Before
    public void setUp() {
        mSubject = new IncomingTransaction<>(
                new TransactionToken(TRANS_ID, TRANS_SIZE),
                null,
                (aggregate, toAppend, offset) -> null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrows_nullToken() {
        new IncomingTransaction<>(null, null, (aggregate, toAppend, offset) -> null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrows_nullAggregator() {
        new IncomingTransaction<>(new TransactionToken(TRANS_ID, 0), null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReceiveFails_earlyChunk() {
        ChunkHeader header = new ChunkHeader(TRANS_ID, 10, 1);
        mSubject.receive(header, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReceiveFails_duplicateChunk() {
        ChunkHeader chunk = new ChunkHeader(TRANS_ID, 0, 1);
        mSubject.receive(chunk, null);
        mSubject.receive(chunk, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReceiveFails_largeChunk() {
        ChunkHeader chunk = new ChunkHeader(TRANS_ID, 0, TRANS_SIZE + 1);
        mSubject.receive(chunk, null);
    }

}
