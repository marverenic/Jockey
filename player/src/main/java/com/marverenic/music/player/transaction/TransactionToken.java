package com.marverenic.music.player.transaction;

import android.os.Parcel;
import android.os.Parcelable;

public final class TransactionToken implements Parcelable {

    private final String mId;
    private final int mSize;

    public TransactionToken(String transactionId, int count) {
        mId = transactionId;
        mSize = count;
    }

    protected TransactionToken(Parcel in) {
        mId = in.readString();
        mSize = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mId);
        dest.writeInt(mSize);
    }

    String getTransactionId() {
        return mId;
    }

    int getSize() {
        return mSize;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<TransactionToken> CREATOR = new Creator<TransactionToken>() {
        @Override
        public TransactionToken createFromParcel(Parcel in) {
            return new TransactionToken(in);
        }

        @Override
        public TransactionToken[] newArray(int size) {
            return new TransactionToken[size];
        }
    };

}
