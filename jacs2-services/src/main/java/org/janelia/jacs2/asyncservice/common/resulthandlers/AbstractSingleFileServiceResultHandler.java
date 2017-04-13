package org.janelia.jacs2.asyncservice.common.resulthandlers;

import org.janelia.jacs2.asyncservice.common.ServiceDataUtils;
import org.janelia.jacs2.asyncservice.common.ServiceResultHandler;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;

import java.io.File;

public abstract class AbstractSingleFileServiceResultHandler implements ServiceResultHandler<File> {

    public void updateServiceDataResult(JacsServiceData jacsServiceData, File result) {
        jacsServiceData.setStringifiedResult(ServiceDataUtils.fileToString(result));
    }

    public File getServiceDataResult(JacsServiceData jacsServiceData) {
        return ServiceDataUtils.stringToFile(jacsServiceData.getStringifiedResult());
    }
}
