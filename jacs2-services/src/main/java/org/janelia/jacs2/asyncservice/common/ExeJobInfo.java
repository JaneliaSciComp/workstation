package org.janelia.jacs2.asyncservice.common;

public interface ExeJobInfo {
    String getScriptName();
    boolean isDone();
    boolean hasFailed();
    void terminate();
}
