package org.janelia.it.jacs.integration;

import java.util.Collection;

import javax.swing.JFrame;

import org.janelia.it.jacs.integration.framework.compression.CompressedFileResolverI;
import org.janelia.it.jacs.integration.framework.domain.PreferenceHandler;
import org.janelia.it.jacs.integration.framework.exceptions.UnprovidedServiceException;
import org.janelia.it.jacs.integration.framework.system.ActivityLogging;
import org.janelia.it.jacs.integration.framework.system.ErrorHandler;
import org.janelia.it.jacs.integration.framework.system.FileAccess;
import org.janelia.it.jacs.integration.framework.system.ParentFrame;
import org.janelia.it.jacs.integration.framework.system.SettingsModel;
import org.openide.util.lookup.Lookups;

/**
 * The factory to return implementations from the framework.
 *
 * @author fosterl
 */
public class FrameworkImplProvider {

    public static ParentFrame getAppHandler() {
        return get(ParentFrame.LOOKUP_PATH, ParentFrame.class);
    }

    public static ErrorHandler getErrorHandler() {
        return get(ErrorHandler.LOOKUP_PATH, ErrorHandler.class);
    }

    public static SettingsModel getSettingsModel() {
        return get(SettingsModel.LOOKUP_PATH, SettingsModel.class);
    }
    
    public static PreferenceHandler getPreferenceHandler() {
        return get(PreferenceHandler.LOOKUP_PATH, PreferenceHandler.class);
    }

    public static FileAccess getFileAccess() {
        return get(FileAccess.LOOKUP_PATH, FileAccess.class);
    }
    
    public static CompressedFileResolverI getCompressedFileResolver() {
        return get(CompressedFileResolverI.LOOKUP_PATH, CompressedFileResolverI.class);
    }
    
    public static ActivityLogging getSessionSupport() {
        return get(ActivityLogging.LOOKUP_PATH, ActivityLogging.class);
    }
    
    public static ParentFrame getParentFrameProvider() {
        return get(ParentFrame.LOOKUP_PATH, ParentFrame.class);
    }

    // Convenience methods

    public static JFrame getMainFrame() {
        final ParentFrame parentFrameProvider = getParentFrameProvider();
        if (parentFrameProvider == null) {
            return null;
        }
        else {
            return parentFrameProvider.getMainFrame();
        }
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
    
    private static <T> T get(String path, Class<T> clazz) {
        Collection<? extends T> candidates = Lookups.forPath(path).lookupAll(clazz);
        for(T handler : candidates) {
            return handler;
        }
        throw new UnprovidedServiceException("No service provider found for "+path);
    }
}
