package org.janelia.workstation.core.logging;

import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.core.util.SystemInfo;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class LoggingUtils {

    public static String getReportEmailSubject(boolean isAutoReport) {

        String username = AccessManager.getAccessManager().getAuthenticatedSubject().getName();
        String version = SystemInfo.appVersion;

        StringBuilder sb = new StringBuilder();
        sb.append(isAutoReport?"Auto-report":"User-report");
        sb.append(" from ");
        sb.append(username);
        if (!username.equals(AccessManager.getSubjectName())) {
            sb.append("(running as ").append(AccessManager.getSubjectName()).append(")");
        }
        sb.append(" -- ").append(version);

        return sb.toString();
    }
}
