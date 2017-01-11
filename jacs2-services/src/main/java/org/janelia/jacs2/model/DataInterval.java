package org.janelia.jacs2.model;

import java.util.Optional;

public class DataInterval<T> {
    private final Optional<T> from, to;

    public DataInterval(T from, T to) {
        this.from = Optional.ofNullable(from);
        this.to = Optional.ofNullable(to);
    }

    public boolean hasFrom() {
        return from.isPresent();
    }

    public boolean hasTo() {
        return to.isPresent();
    }

    public T getFrom() {
        return from.get();
    }

    public T getTo() {
        return to.get();
    }
}
