package org.janelia.jacs2.asyncservice.common;

import org.janelia.jacs2.model.jacsservice.JacsServiceData;

public interface ServiceCommand {
    void execute(JacsServiceData jacsServiceData);
}
