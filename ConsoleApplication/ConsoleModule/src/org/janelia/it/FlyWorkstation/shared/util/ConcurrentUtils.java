package org.janelia.it.FlyWorkstation.shared.util;

import java.util.concurrent.Callable;

import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;

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
                SessionMgr.getSessionMgr().handleException(e); 
            }
        }
    }
    
    public static void invoke(Callable<?> callback) throws Exception {
        if (callback!=null) {
            callback.call();
        }
    }
}
