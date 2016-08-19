/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.integration;

import java.util.Collection;
import javax.swing.JFrame;
import org.janelia.it.jacs.integration.framework.compression.CompressedFileResolverI;
import org.janelia.it.jacs.integration.framework.session_mgr.ActivityLogging;
import org.janelia.it.jacs.integration.framework.session_mgr.ErrorHandler;
import org.janelia.it.jacs.integration.framework.session_mgr.ParentFrame;
import org.janelia.it.jacs.integration.framework.session_mgr.SettingsModel;
import org.openide.util.lookup.Lookups;

/**
 * The factory to return implementations from the framework.
 *
 * @author fosterl
 * @todo once the generic approach is proven, invert the commenting below.
 */
public class FrameworkImplProvider {
    public static ActivityLogging getSessionSupport() {
//        return get(ActivityLogging.LOOKUP_PATH, ActivityLogging.class);
        Collection<? extends ActivityLogging> candidates
                = Lookups.forPath(ActivityLogging.LOOKUP_PATH).lookupAll(ActivityLogging.class);
        if (candidates.size() > 0) {
            return candidates.iterator().next();
        }
        else {
            return null;
        }
    }
    
    public static CompressedFileResolverI getCompressedFileResolver() {
//        return get(CompressedFileResolverI.LOOKUP_PATH, CompressedFileResolverI.class);
        Collection<? extends CompressedFileResolverI> candidates
                = Lookups.forPath(CompressedFileResolverI.LOOKUP_PATH).lookupAll(CompressedFileResolverI.class);
        if (candidates.size() > 0) {
            return candidates.iterator().next();            
        }
        else {
            return null;
        }
    }
    
    public static ErrorHandler getErrorHandler() {
        return get(ErrorHandler.LOOKUP_PATH, ErrorHandler.class);
    }
    
    public static SettingsModel getSettingsModel() {
        return get(SettingsModel.LOOKUP_PATH, SettingsModel.class);
    }
    
    public static void handleException(Throwable th){
        ErrorHandler eh = getErrorHandler();
        if (eh == null) {
            th.printStackTrace();
        }
        else {
            eh.handleException(th);
        }
    }
    
    /** Convenience methods. */
    public static JFrame getMainFrame() {
        return getParentFrameProvider().getMainFrame();
    }

    public static ParentFrame getParentFrameProvider() {
        return get(ParentFrame.LOOKUP_PATH, ParentFrame.class);
    }
    
    public static Object getModelProperty(String propName) {
        SettingsModel model = getSettingsModel();
        if (model == null) {
            throw new RuntimeException("Failed to find settings model.  Cannot fetch " + propName);
        }
        else {
            return model.getModelProperty(propName);
        }
    }
    
    public static void setModelProperty(String propName, Object value) {
        SettingsModel model = getSettingsModel();
        if (model == null) {
            throw new RuntimeException("Failed to find settings model.  Cannot set " + propName);
        }
        else {
            model.setModelProperty(propName, value);
        }
    }
    
    private static <T> T get(String path, Class clazz) {
        Collection<? extends T> candidates
                = Lookups.forPath(path).lookupAll(clazz);
        if (candidates.size() > 0) {
            return candidates.iterator().next();
        }
        else {
            return null;
        }
    }
}
