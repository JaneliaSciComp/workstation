package org.janelia.it.workstation.browser.gui.options;

import static org.janelia.it.workstation.browser.gui.options.OptionConstants.NUM_CONCURRENT_DOWNLOADS_PROPERTY;
import static org.janelia.it.workstation.browser.gui.options.OptionConstants.SANITIZE_FILENAMES_PROPERTY;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.workstation.browser.util.SystemInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Getters and setters for browser options.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DownloadOptions {

    private static final Logger log = LoggerFactory.getLogger(DownloadOptions.class);

    private static final int NUM_CONCURRENT_DOWNLOADS_DEFAULT = 1;
    private static final boolean SANITIZE_FILENAMES_DEFAULT = true;
    
    private static DownloadOptions instance;
    public static synchronized DownloadOptions getInstance() {
        if (null == instance) {
            instance = new DownloadOptions();
        }
        return instance;
    }

    private PropertyChangeSupport propSupport;
    
    private DownloadOptions() {
    }

    public String getDownloadsDir() {
        return SystemInfo.getDownloadsDir().toString();
    }
    
    public void setDownloadsDir(String newValue) {
        if (!newValue.equals(SystemInfo.getDownloadsDir().toString())) {
            SystemInfo.setDownloadsDir(newValue);
            log.info("Saved downloads dir: {}", newValue);
        }
    }
    
    public int getNumConcurrentDownloads() {
        return FrameworkImplProvider.getModelProperty(
                NUM_CONCURRENT_DOWNLOADS_PROPERTY, NUM_CONCURRENT_DOWNLOADS_DEFAULT);
    }
    
    public void setNumConcurrentDownloads(int newValue) {
        int currValue = getNumConcurrentDownloads();
        if (newValue == currValue) return;

        FrameworkImplProvider.setModelProperty(NUM_CONCURRENT_DOWNLOADS_PROPERTY, newValue);
        log.info("Saved num concurrent downloads: {}", newValue);

        if (null != propSupport)
            propSupport.firePropertyChange(NUM_CONCURRENT_DOWNLOADS_PROPERTY, currValue, newValue);
    }
    
    public boolean getSanitizeDownloads() {
        return FrameworkImplProvider.getModelProperty(
                SANITIZE_FILENAMES_PROPERTY, SANITIZE_FILENAMES_DEFAULT);
    }
    
    public void setSanitizeDownloads(boolean newValue) {
        boolean currValue = getSanitizeDownloads();
        if (newValue == currValue) return;
        
        FrameworkImplProvider.setModelProperty(SANITIZE_FILENAMES_PROPERTY, newValue);
        log.info("Saved show sanitize downloads: {}", newValue);
    }
    
    public void addPropertyChangeListener(PropertyChangeListener l) {
        if (null == propSupport)
            propSupport = new PropertyChangeSupport(this);
        propSupport.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        if (null == propSupport)
            return;
        propSupport.removePropertyChangeListener(l);
    }
    
    
}
