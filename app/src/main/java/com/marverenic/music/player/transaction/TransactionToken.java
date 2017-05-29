package com.marverenic.music.player.transaction;

import android.os.Parcel;
import android.os.Parcelable;

public final class TransactionToken implements Parcelable {

    private final int mSize;

    public TransactionToken(int count) {
        mSize = count;
    }

    protected TransactionToken(Parcel in) {
        mSize = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mSize);
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
