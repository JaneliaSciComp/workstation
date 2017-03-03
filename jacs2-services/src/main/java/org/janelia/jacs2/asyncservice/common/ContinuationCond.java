package org.janelia.jacs2.asyncservice.common;

@FunctionalInterface
interface ContinuationCond {
    boolean checkCond();
}
