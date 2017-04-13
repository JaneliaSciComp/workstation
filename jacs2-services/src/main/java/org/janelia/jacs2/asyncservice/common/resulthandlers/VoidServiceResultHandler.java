package org.janelia.jacs2.asyncservice.common.resulthandlers;

import org.janelia.jacs2.asyncservice.common.JacsServiceResult;
import org.janelia.jacs2.asyncservice.common.ServiceResultHandler;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;

public class VoidServiceResultHandler implements ServiceResultHandler<Void> {
    @Override
    public boolean isResultReady(JacsServiceResult<?> depResults) {
        return true;
    }

    @Override
    public Void collectResult(JacsServiceResult<?> depResults) {
        return null;
    }

    @Override
    public void updateServiceDataResult(JacsServiceData jacsServiceData, Void result) {
        // do nothing
    }

    @Override
    public Void getServiceDataResult(JacsServiceData jacsServiceData) {
        return null;
    }
}
