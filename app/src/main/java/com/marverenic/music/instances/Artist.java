package com.marverenic.music.instances;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

public class Artist implements Parcelable {
    public static final Parcelable.Creator<Artist> CREATOR = new Parcelable.Creator<Artist>() {
        public Artist createFromParcel(Parcel in) {
            return new Artist(in);
        }

        public Artist[] newArray(int size) {
            return new Artist[size];
        }
    };
    public long artistId;
    public String artistName;

    public Artist(final long artistId, final String artistName) {
        this.artistId = artistId;
        this.artistName = artistName;
    }

    private Artist(Parcel in) {
        artistId = in.readLong();
        artistName = in.readString();
    }

    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Artist other = (Artist) obj;
        return (artistId == other.artistId) && TextUtils.equals(artistName, other.artistName);
    }

    public String toString() {
        return artistName;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(artistId);
        dest.writeString(artistName);
    }

}
