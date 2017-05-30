package com.marverenic.music.player.transaction;

import android.os.Parcel;
import android.os.Parcelable;

public final class ChunkHeader implements Parcelable {

    public static final int MAX_ENTRIES = 500;

    private final String mTransactionId;
    private final int mOffset;
    private final int mSize;

    ChunkHeader(String transactionId, int offset, int size) {
        mTransactionId = transactionId;
        mOffset = offset;
        mSize = size;
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

    protected ChunkHeader(Parcel in) {
        mTransactionId = in.readString();
        mOffset = in.readInt();
        mSize = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mTransactionId);
        dest.writeInt(mOffset);
        dest.writeInt(mSize);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ChunkHeader> CREATOR = new Creator<ChunkHeader>() {
        @Override
        public ChunkHeader createFromParcel(Parcel in) {
            return new ChunkHeader(in);
        }

        @Override
        public ChunkHeader[] newArray(int size) {
            return new ChunkHeader[size];
        }
    };

}
