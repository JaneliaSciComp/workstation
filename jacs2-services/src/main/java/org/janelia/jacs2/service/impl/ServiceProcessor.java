package org.janelia.jacs2.service.impl;

import org.janelia.jacs2.model.service.JacsServiceData;

public interface ServiceProcessor<R> {
    ServiceComputation<ServiceProcessor<R>, ? extends R> process(JacsService<R> jacsServiceData);
}
