package org.janelia.jacs2.service.impl;

import java.io.InputStream;

@FunctionalInterface
public interface ExternalProcessOutputHandler {
    String handle(InputStream stream);
}
