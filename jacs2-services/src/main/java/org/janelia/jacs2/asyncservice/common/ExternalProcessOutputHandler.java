package org.janelia.jacs2.asyncservice.common;

import java.io.InputStream;

@FunctionalInterface
public interface ExternalProcessOutputHandler {
    String handle(InputStream stream);
}
