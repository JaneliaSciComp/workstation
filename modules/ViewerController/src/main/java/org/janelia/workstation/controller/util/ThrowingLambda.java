package org.janelia.workstation.controller.util;

@FunctionalInterface
public interface ThrowingLambda {
    void accept() throws Exception;
}
