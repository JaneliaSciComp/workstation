package org.janelia.jacs2.asyncservice.utils;

public class DataHolder<T> {
    private T data;

    public DataHolder() {
    }

    public DataHolder(T data) {
        this.data = data;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
