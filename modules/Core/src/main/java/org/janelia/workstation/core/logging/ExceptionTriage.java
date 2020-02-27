package org.janelia.workstation.core.logging;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.janelia.workstation.core.api.exceptions.AuthenticationException;

import java.awt.*;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * This class can triage exceptions to determine if they fall into a known category of
 * states or bugs.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ExceptionTriage {

    public enum ExceptionCategory {
        AUTH,
        NETWORK,
        FILE_PERMISSIONS,
        OUT_OF_MEMORY,
        OUT_OF_DISK,
        MISSING_REMOTE_FILE,
        KNOWN_BUG,
        UNKNOWN
    }

    public static ExceptionCategory getExceptionCategory(Throwable e) {
        String stacktrace = ExceptionUtils.getStackTrace(e);
        return getExceptionCategory(e, stacktrace);
    }

    public static ExceptionCategory getExceptionCategory(Throwable e, String stacktrace) {

        Throwable rootCause = org.hibernate.exception.ExceptionUtils.getRootCause(e);

        // Ignore auth issues
        if (stacktrace.contains(AuthenticationException.class.getName()+": Invalid username or password")
                || "HTTP 401 Unauthorized".equalsIgnoreCase(e.getMessage())) {
            return ExceptionCategory.AUTH;
        }

        // Ignore network and data issues. If it's in fact a problem on our end, the user will let us know.
        if (stacktrace.contains("java.io.IOException: unexpected end of stream")
                || stacktrace.contains("java.net.ConnectException: Connection refused")
                || stacktrace.contains("java.net.ConnectException: Connection timed out")
                || stacktrace.contains("java.io.IOException: stream is closed")
                || rootCause instanceof java.net.ConnectException
                || rootCause instanceof java.net.SocketTimeoutException) {
            return ExceptionCategory.NETWORK;
        }

        // Ignore problems with local disks
        if (e instanceof java.nio.file.AccessDeniedException
                || rootCause instanceof java.nio.file.AccessDeniedException) {
            return ExceptionCategory.FILE_PERMISSIONS;
        }

        // Ignore memory issues, these do not represent bugs.
        if (e instanceof OutOfMemoryError || (e.getMessage()!=null && e.getMessage().contains("Java heap space"))) {
            return ExceptionCategory.OUT_OF_MEMORY;
        }

        // Ignore all disk space issues, these do not represent bugs.
        if (e.getMessage()!=null && (e.getMessage().contains("No space left on device") || e.getMessage().contains("There is not enough space on the disk"))) {
            return ExceptionCategory.OUT_OF_DISK;
        }

        // Ignore missing files on remote server
        if (e instanceof FileNotFoundException && e.getMessage()!=null && e.getMessage().contains("404")) {
            return ExceptionCategory.MISSING_REMOTE_FILE;
        }

        // Ignore all broken pipes, because these are usually caused by user initiated cancellation or network issues.
        if (stacktrace.contains("Caused by: java.io.IOException: Broken pipe")) {
            return ExceptionCategory.KNOWN_BUG;
        }

        // JDK bug: http://bugs.java.com/view_bug.do?bug_id=8003398
        if ((e instanceof IllegalArgumentException) && e.getMessage()!=null && "adding a container to a container on a different GraphicsDevice".equalsIgnoreCase(e.getMessage().trim())) {
            return ExceptionCategory.KNOWN_BUG;
        }

        // JDK bug: http://bugs.java.com/view_bug.do?bug_id=7117595
        if ((e instanceof ArrayIndexOutOfBoundsException) && "1".equals(e.getMessage())) {
            StackTraceElement ste = e.getStackTrace()[0];
            if ("sun.awt.Win32GraphicsEnvironment".equals(ste.getClassName()) && "getDefaultScreenDevice".equals(ste.getMethodName())) {
                return ExceptionCategory.KNOWN_BUG;
            }
        }

        // NetBeans bug: https://netbeans.org/bugzilla/show_bug.cgi?id=270487
        if ((e instanceof IllegalComponentStateException) && "component must be showing on the screen to determine its location".equals(e.getMessage())) {
            return ExceptionCategory.KNOWN_BUG;
        }

        // NetBeans bug: https://netbeans.org/bugzilla/show_bug.cgi?id=232389
        if (e instanceof IOException && "Cyclic reference. Somebody is trying to get value from FolderInstance (org.openide.awt.Toolbar$Folder) from the same thread that is processing the instance".equals(e.getMessage())) {
            return ExceptionCategory.KNOWN_BUG;
        }

        // Ignore older ArtifactDescriptor deserialization issues. These older ArtifactDescriptors are no longer usable, but the user can overwrite them with new preferences.
        if (stacktrace.contains("com.fasterxml.jackson.databind.JsonMappingException: Unexpected token") && stacktrace.contains("ArtifactDescriptor")) {
            return ExceptionCategory.KNOWN_BUG;
        }

        return ExceptionCategory.UNKNOWN;
    }
}
