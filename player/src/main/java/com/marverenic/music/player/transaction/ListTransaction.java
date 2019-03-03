package com.marverenic.music.player.transaction;

import java.util.ArrayList;
import java.util.List;

public final class ListTransaction {

    public static <T, E extends Throwable> OutgoingTransaction<List<T>, E> send(List<T> data) {
        return new OutgoingTransaction<>(data, data.size(), List::subList);
    }

    public static <T> IncomingTransaction<List<T>> receive(TransactionToken token) {
        return new IncomingTransaction<>(token, new ArrayList<>(),
                (List<T> aggregate, List<T> toAppend, int offset) -> {
                    aggregate.addAll(toAppend);
                    return aggregate;
                });
    }

}
