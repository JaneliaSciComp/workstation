package org.janelia.it.workstation.browser.gui.options;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.prefs.Preferences;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.workstation.browser.util.BrandingConfig;
import org.openide.util.NbPreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Application options stored in NetBeans preferences.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ApplicationOptions {

    private static final Logger log = LoggerFactory.getLogger(ApplicationOptions.class);
    
    private static ApplicationOptions instance;

    public static synchronized ApplicationOptions getInstance() {
        if (null == instance) {
            instance = new ApplicationOptions();
        }
        return instance;
    }
        
    private PropertyChangeSupport propSupport;
    
    private ApplicationOptions() {
    }

    private Preferences prefs() {
        return NbPreferences.forModule(ApplicationOptions.class);
    }
    
    public void setShowStartPageOnStartup(boolean show) {
        boolean oldVal = isShowStartPageOnStartup();
        if (oldVal == show) {
            return;
        }
        prefs().putBoolean(OptionConstants.SHOW_START_PAGE_ON_STARTUP, show);
        log.info("Set show start page on startup = {}", show);
        if (null != propSupport)
            propSupport.firePropertyChange(OptionConstants.SHOW_START_PAGE_ON_STARTUP, oldVal, show);
    }

    public boolean isShowStartPageOnStartup() {
        return prefs().getBoolean(OptionConstants.SHOW_START_PAGE_ON_STARTUP, true);
    }

    public void setAutoDownloadUpdates(boolean autoDownload) {
        boolean oldVal = isAutoDownloadUpdates();
        if (oldVal == autoDownload) {
            return;
        }        
        
        prefs().putBoolean(OptionConstants.AUTO_DOWNLOAD_UPDATES, autoDownload);
        log.info("Set auto download updates = {}", autoDownload);
        
        if (null != propSupport)
            propSupport.firePropertyChange(OptionConstants.AUTO_DOWNLOAD_UPDATES, oldVal, autoDownload);
    }

    public boolean isAutoDownloadUpdates() {
        return prefs().getBoolean(OptionConstants.AUTO_DOWNLOAD_UPDATES, true);
    }
    
    public boolean isShowReleaseNotes() {
        Boolean value = (Boolean) FrameworkImplProvider.getModelProperty(OptionConstants.SHOW_RELEASE_NOTES);
        return value==null || value;
    }
    
    public void setShowReleaseNotes(boolean value) {
        boolean oldVal = isShowReleaseNotes();
        if (oldVal == value) {
            return;
        }
        
        FrameworkImplProvider.setModelProperty(OptionConstants.SHOW_RELEASE_NOTES, value);  
        log.info("Set show release notes = {}", value);
        
        if (null != propSupport)
            propSupport.firePropertyChange(OptionConstants.SHOW_RELEASE_NOTES, oldVal, value);
    }
    
    public boolean isUseRunAsUserPreferences() {
        Boolean value = (Boolean) FrameworkImplProvider.getModelProperty(OptionConstants.USE_RUN_AS_USER_PREFERENCES);
        return value!=null && value;
    }
    
    public void setUseRunAsUserPreferences(boolean value) {
        boolean oldVal = isUseRunAsUserPreferences();
        if (oldVal == value) {
            return;
        }
        
        FrameworkImplProvider.setModelProperty(OptionConstants.USE_RUN_AS_USER_PREFERENCES, value); 
        log.info("Set use run as user preferences = {}", value);
        
        if (null != propSupport)
            propSupport.firePropertyChange(OptionConstants.USE_RUN_AS_USER_PREFERENCES, oldVal, value); 
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
