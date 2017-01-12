package com.marverenic.music.utils;

import java.util.NoSuchElementException;

public final class Optional<T> {

    private final boolean mPresent;
    private final T mValue;

    private Optional() {
        mPresent = false;
        mValue = null;
    }

    private Optional(T value) {
        mPresent = true;
        mValue = value;
    }

    public static<T> Optional<T> empty() {
        return new Optional<>();
    }

    public static <T> Optional<T> of(T value) {
        if (value == null) {
            throw new IllegalArgumentException("value cannot be null");
        }
        return ofNullable(value);
    }

    public static <T> Optional<T> ofNullable(T value) {
        return new Optional<>(value);
    }

    public T getValue() {
        if (!isPresent()) {
            throw new NoSuchElementException("No value present");
        }
        return mValue;
    }

    public boolean isPresent() {
        return mPresent;
    }

    @Override
    @SuppressWarnings("SimplifiableIfStatement")
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Optional<?> optional = (Optional<?>) o;

        if (mPresent != optional.mPresent) return false;
        return (mValue != null) ? mValue.equals(optional.mValue) : optional.mValue == null;

    }

    @Override
    public int hashCode() {
        return (mValue != null) ? mValue.hashCode() : 0;
    }

    @Override
    public String toString() {
        return isPresent() ? "Optional (" + mValue + ")" : "Optional (Empty)";
    }
}
