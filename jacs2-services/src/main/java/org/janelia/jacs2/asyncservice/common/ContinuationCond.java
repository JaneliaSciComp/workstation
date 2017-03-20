package org.janelia.jacs2.asyncservice.common;

@FunctionalInterface
public interface ContinuationCond {
    boolean checkCond();
}
