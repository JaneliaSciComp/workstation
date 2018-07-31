package org.janelia.it.jacs.integration;

import java.util.Collection;

import javax.swing.JFrame;

import org.janelia.it.jacs.integration.framework.compression.CompressedFileResolverI;
import org.janelia.it.jacs.integration.framework.domain.PreferenceHandler;
import org.janelia.it.jacs.integration.framework.exceptions.UnprovidedServiceException;
import org.janelia.it.jacs.integration.framework.system.ActivityLogging;
import org.janelia.it.jacs.integration.framework.system.ErrorHandler;
import org.janelia.it.jacs.integration.framework.system.FileAccess;
import org.janelia.it.jacs.integration.framework.system.InspectionHandler;
import org.janelia.it.jacs.integration.framework.system.ParentFrame;
import org.janelia.it.jacs.integration.framework.system.SettingsModel;
import org.openide.util.NbPreferences;
import org.openide.util.lookup.Lookups;

/**
 * The factory to return implementations from the framework.
 *
 * @author fosterl
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class FrameworkImplProvider {

    public static ParentFrame getAppHandler() {
        return getProvider(ParentFrame.LOOKUP_PATH, ParentFrame.class);
    }

    public static ErrorHandler getErrorHandler() {
        return getProvider(ErrorHandler.LOOKUP_PATH, ErrorHandler.class);
    }

    public static SettingsModel getSettingsModel() {
        return getProvider(SettingsModel.LOOKUP_PATH, SettingsModel.class);
    }
    
    public static PreferenceHandler getPreferenceHandler() {
        return getProvider(PreferenceHandler.LOOKUP_PATH, PreferenceHandler.class);
    }

    public static FileAccess getFileAccess() {
        return getProvider(FileAccess.LOOKUP_PATH, FileAccess.class);
    }
    
    public static CompressedFileResolverI getCompressedFileResolver() {
        return getProvider(CompressedFileResolverI.LOOKUP_PATH, CompressedFileResolverI.class);
    }
    
    public static ActivityLogging getSessionSupport() {
        return getProvider(ActivityLogging.LOOKUP_PATH, ActivityLogging.class);
    }
    
    public static ParentFrame getParentFrameProvider() {
        return getProvider(ParentFrame.LOOKUP_PATH, ParentFrame.class);
    }
    
    public static InspectionHandler getInspectionHandler() {
        return getProvider(InspectionHandler.LOOKUP_PATH, InspectionHandler.class);
    }

    private static <T> T getProvider(String path, Class<T> clazz) {
        Collection<? extends T> candidates = Lookups.forPath(path).lookupAll(clazz);
        for(T handler : candidates) {
            return handler;
        }
        throw new UnprovidedServiceException("No service provider found for "+path);
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

    public static void handleException(Throwable th) {
        handleException(null, th);
    }

    public static void handleException(String message, Throwable th) {
        ErrorHandler eh = getErrorHandler();
        if (eh == null) {
            th.printStackTrace(); // If all else fails. 
        }
        else {
            eh.handleException(message, th);
        }
    }

    public static void handleExceptionQuietly(Throwable th) {
        handleExceptionQuietly(null, th);
    }

    public static void handleExceptionQuietly(String message, Throwable th) {
        ErrorHandler eh = getErrorHandler();
        if (eh == null) {
            th.printStackTrace(); // If all else fails.
        }
        else {
            eh.handleExceptionQuietly(message, th);
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

    public static <T> T getModelProperty(String propName, T defaultValue) {
        @SuppressWarnings("unchecked")
        T value = (T)getModelProperty(propName);
        if (value==null) return defaultValue;
        return value;
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

    public static <T> T getRemotePreferenceValue(String category, String key, T defaultValue) throws Exception {
        PreferenceHandler model = getPreferenceHandler();
        if (model == null) {
            throw new RuntimeException("Failed to find preference handler.  Cannot fetch " + key);
        }
        else {
            return model.getPreferenceValue(category, key, defaultValue);
        }
    }
    
    public static void setRemotePreferenceValue(String category, String key, Object value) throws Exception {
        PreferenceHandler model = getPreferenceHandler();
        if (model == null) {
            throw new RuntimeException("Failed to find settings handler.  Cannot set " + key);
        }
        else {
            model.setPreferenceValue(category, key, value);
        }
    }
    
    public static String getLocalPreferenceValue(Class<?> moduleClass, String key, String defaultValue) {
        return NbPreferences.forModule(moduleClass).get(key, defaultValue);   
    }

    public static boolean getLocalPreferenceValue(Class<?> moduleClass, String key, boolean defaultValue) {
        return NbPreferences.forModule(moduleClass).getBoolean(key, defaultValue);   
    }

    public static int getLocalPreferenceValue(Class<?> moduleClass, String key, int defaultValue) {
        return NbPreferences.forModule(moduleClass).getInt(key, defaultValue);   
    }

    public static double getLocalPreferenceValue(Class<?> moduleClass, String key, double defaultValue) {
        return NbPreferences.forModule(moduleClass).getDouble(key, defaultValue);   
    }
    
    public static void setLocalPreferenceValue(Class<?> moduleClass, String key, String value) {
        if (value==null) {
            NbPreferences.forModule(moduleClass).remove(key);
        }
        else {
            NbPreferences.forModule(moduleClass).put(key, value);
        }
    }

    public static void setLocalPreferenceValue(Class<?> moduleClass, String key, boolean value) {
        NbPreferences.forModule(moduleClass).putBoolean(key, value);
    }

    public static void setLocalPreferenceValue(Class<?> moduleClass, String key, int value) {
        NbPreferences.forModule(moduleClass).putInt(key, value);
    }
    
    public static void setLocalPreferenceValue(Class<?> moduleClass, String key, double value) {
        NbPreferences.forModule(moduleClass).putDouble(key, value);
    }
}
