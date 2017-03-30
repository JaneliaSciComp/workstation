package org.janelia.jacs2.asyncservice.common;

import org.ggf.drmaa.DrmaaException;
import org.ggf.drmaa.Session;

public class DrmaaJobInfo implements ExeJobInfo {
    private final Session drmaaSession;
    private final String jobId;
    private final String scriptName;
    private boolean done;
    private boolean failed;

    DrmaaJobInfo(Session drmaaSession, String jobId, String scriptName) {
        this.drmaaSession = drmaaSession;
        this.jobId = jobId;
        this.scriptName = scriptName;
        this.done = false;
        this.failed = false;
    }

    @Override
    public String getScriptName() {
        return scriptName;
    }

    @Override
    public boolean isDone() {
        if (done) return done;
        try {
            int status = drmaaSession.getJobProgramStatus(jobId);
            switch (status) {
                case Session.UNDETERMINED:
                    break;
                case Session.QUEUED_ACTIVE:
                    done = false;
                    break;
                case Session.SYSTEM_ON_HOLD:
                case Session.USER_ON_HOLD:
                case Session.USER_SYSTEM_ON_HOLD:
                case Session.RUNNING:
                case Session.SYSTEM_SUSPENDED:
                case Session.USER_SUSPENDED:
                case Session.USER_SYSTEM_SUSPENDED:
                    done = false;
                    break;
                case Session.DONE:
                    done = true;
                    break;
                case Session.FAILED:
                    done = true;
                    failed = true;
                    break;
                default:
                    break;
            }
            return done;
        } catch (DrmaaException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public boolean hasFailed() {
        return done && failed;
    }

    @Override
    public void terminate() {
        if (!done) {
            try {
                drmaaSession.control(jobId, Session.TERMINATE);
            } catch (DrmaaException e) {
                throw new IllegalStateException(e);
            }
        }
    }

}
