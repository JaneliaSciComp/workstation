package org.janelia.workstation.core.logging;

import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.core.util.SystemInfo;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class LoggingUtils {

    public static String getReportEmailSubject(boolean isAutoReport) {

        AccessManager accessManager = AccessManager.getAccessManager();

        String username = "anonymous";
        String runAsUser = null;

        if (AccessManager.getAccessManager().isLoggedIn()) {
            username = accessManager.getAuthenticatedSubject().getName();
            if (!username.equals(AccessManager.getSubjectName())) {
                runAsUser = AccessManager.getSubjectName();
            }
        }

        String version = SystemInfo.appVersion;

        StringBuilder sb = new StringBuilder();
        sb.append(isAutoReport?"Auto-report":"User-report");
        sb.append(" from ");
        sb.append(username);
        if (runAsUser != null) {
            sb.append(" (running as ").append(runAsUser).append(")");
        }
        sb.append(" -- ").append(version);

        return sb.toString();
    }
}
