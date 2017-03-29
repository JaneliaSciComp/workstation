package org.janelia.jacs2.asyncservice.common.resulthandlers;

import org.janelia.jacs2.asyncservice.common.ServiceResultHandler;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;

public class VoidServiceResultHandler implements ServiceResultHandler<Void> {
    @Override
    public boolean isResultReady(JacsServiceData jacsServiceData) {
        return true;
    }

    @Override
    public Void collectResult(JacsServiceData jacsServiceData) {
        return null;
    }

    @Override
    public void updateServiceDataResult(JacsServiceData jacsServiceData, Void result) {
    }

    @Override
    public Void getServiceDataResult(JacsServiceData jacsServiceData) {
        return null;
    }
}
