package org.janelia.workstation.core.util;

import org.janelia.workstation.integration.util.FrameworkAccess;

import java.util.concurrent.Callable;

/**
 * Utilities for dealing with the Java concurrent library.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ConcurrentUtils {

    public static void invokeAndHandleExceptions(Callable<?> callback) {
        if (callback!=null) {
            try {
                callback.call();
            }
            catch (Exception e) {
                FrameworkAccess.handleException(e);
            }
        }
    }
    
    public static void invoke(Callable<?> callback) throws Exception {
        if (callback!=null) {
            callback.call();
        }
    }
}
