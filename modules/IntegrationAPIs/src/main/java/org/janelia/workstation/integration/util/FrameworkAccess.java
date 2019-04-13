package org.janelia.workstation.integration.util;

import java.util.Collection;

import javax.swing.JFrame;

import org.janelia.workstation.integration.api.FileAccessController;
import org.janelia.workstation.integration.api.FrameModel;
import org.janelia.workstation.integration.api.InspectionController;
import org.janelia.workstation.integration.api.PreferenceModel;
import org.janelia.workstation.integration.spi.compression.CompressedFileResolverI;
import org.janelia.workstation.integration.api.ActivityLogging;
import org.janelia.workstation.integration.api.BrowsingController;
import org.janelia.workstation.integration.api.ErrorHandler;
import org.janelia.workstation.integration.api.ProgressController;
import org.janelia.workstation.integration.api.SettingsModel;
import org.openide.util.Lookup;
import org.openide.util.NbPreferences;
import org.openide.util.lookup.Lookups;

/**
 * The factory to return implementations from the framework.
 *
 * @author fosterl
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class FrameworkAccess {

    public static FrameModel getFrameModel() {
        return getProvider(FrameModel.LOOKUP_PATH, FrameModel.class);
    }

    public static ErrorHandler getErrorHandler() {
        return getProvider(ErrorHandler.LOOKUP_PATH, ErrorHandler.class);
    }

    public static SettingsModel getSettingsModel() {
        return getProvider(SettingsModel.LOOKUP_PATH, SettingsModel.class);
    }
    
    public static PreferenceModel getPreferenceModel() {
        return getProvider(PreferenceModel.LOOKUP_PATH, PreferenceModel.class);
    }

    public static FileAccessController getFileAccessController() {
        return getProvider(FileAccessController.LOOKUP_PATH, FileAccessController.class);
    }
    
    public static CompressedFileResolverI getCompressedFileResolver() {
        return getProvider(CompressedFileResolverI.LOOKUP_PATH, CompressedFileResolverI.class);
    }
    
    public static ActivityLogging getActivityLogging() {
        return getProvider(ActivityLogging.LOOKUP_PATH, ActivityLogging.class);
    }

    public static InspectionController getInspectionController() {
        return getProvider(InspectionController.LOOKUP_PATH, InspectionController.class);
    }

    public static ProgressController getProgressController() {
        return getProvider(ProgressController.LOOKUP_PATH, ProgressController.class);
    }

    public static BrowsingController getBrowsingController() {
        return getProvider(BrowsingController.class);
    }

    private static <T> T getProvider(Class<T> clazz) {
        Collection<? extends T> candidates = Lookup.getDefault().lookupAll(clazz);
        for(T handler : candidates) {
            return handler;
        }
        throw new UnprovidedServiceException("No service provider found for "+clazz.getName());
    }

    private static <T> T getProvider(String path, Class<T> clazz) {
        Collection<? extends T> candidates = Lookups.forPath(path).lookupAll(clazz);
        for(T handler : candidates) {
            return handler;
        }
        throw new UnprovidedServiceException("No service provider found for "+path);
    }

    public static JFrame getMainFrame() {
        final FrameModel frameModel = getFrameModel();
        if (frameModel == null) {
            return null;
        }
        else {
            return frameModel.getMainFrame();
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
            throw new UnprovidedServiceException("Failed to find settings model.  Cannot set " + propName);
        }
        else {
            model.setModelProperty(propName, value);
        }
    }

    /**
     * Returns a preference value stored remotely on the server. This method should be called from a background thread.
     * @param category preference category
     * @param key preference key
     * @param defaultValue default value to return if no value is currently set
     * @param <T> the type of the value
     * @return preference value or default value if none
     * @throws Exception if there is any error communicating with the server
     */
    public static <T> T getRemotePreferenceValue(String category, String key, T defaultValue) throws Exception {
        PreferenceModel model = getPreferenceModel();
        if (model == null) {
            throw new UnprovidedServiceException("Failed to find preference handler.  Cannot fetch " + key);
        }
        else {
            return model.getPreferenceValue(category, key, defaultValue);
        }
    }

    /**
     * Sets a preference value remotely on the server. This method should be called from a background thread.
     * @param category preference category
     * @param key preference key
     * @param value preference value (must be serializable with Jackson)
     * @throws Exception if there is any error communicating with the server
     */
    public static void setRemotePreferenceValue(String category, String key, Object value) throws Exception {
        PreferenceModel model = getPreferenceModel();
        if (model == null) {
            throw new UnprovidedServiceException("Failed to find settings handler.  Cannot set " + key);
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
