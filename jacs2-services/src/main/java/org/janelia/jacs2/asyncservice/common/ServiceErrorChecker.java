package org.janelia.jacs2.asyncservice.common;

import org.janelia.jacs2.model.jacsservice.JacsServiceData;

import java.util.List;

public interface ServiceErrorChecker {
    List<String> collectErrors(JacsServiceData jacsServiceData);
}
