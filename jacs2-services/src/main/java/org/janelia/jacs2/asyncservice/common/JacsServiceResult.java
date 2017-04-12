package org.janelia.jacs2.asyncservice.common;

import org.janelia.jacs2.model.jacsservice.JacsServiceData;

public class JacsServiceResult<T> {
    private final JacsServiceData jacsServiceData;
    private T result;

    public JacsServiceResult(JacsServiceData jacsServiceData) {
        this.jacsServiceData = jacsServiceData;
    }

    public JacsServiceResult(JacsServiceData jacsServiceData, T result) {
        this.jacsServiceData = jacsServiceData;
        this.result = result;
    }

    public JacsServiceData getJacsServiceData() {
        return jacsServiceData;
    }

    public T getResult() {
        return result;
    }

    public void setResult(T result) {
        this.result = result;
    }
}
