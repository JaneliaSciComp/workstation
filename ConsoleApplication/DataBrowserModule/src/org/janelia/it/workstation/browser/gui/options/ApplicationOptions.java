package org.janelia.it.workstation.browser.gui.options;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.prefs.Preferences;

import org.janelia.it.workstation.browser.ConsoleApp;
import org.openide.util.NbPreferences;

/**
 * Application options stored in NetBeans preferences.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ApplicationOptions {

    private static ApplicationOptions instance;

    public static synchronized ApplicationOptions getInstance() {
        if (null == instance) {
            instance = new ApplicationOptions();
        }
        return instance;
    }
    
    public static final String PROP_SHOW_START_PAGE_ON_STARTUP = "showOnStartup";
    
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
        prefs().putBoolean(PROP_SHOW_START_PAGE_ON_STARTUP, show);
        if (null != propSupport)
            propSupport.firePropertyChange(PROP_SHOW_START_PAGE_ON_STARTUP, oldVal, show);
    }

    public boolean isShowStartPageOnStartup() {
        return prefs().getBoolean(PROP_SHOW_START_PAGE_ON_STARTUP, true);
    }

    public boolean isShowReleaseNotes() {
        Boolean value = (Boolean) ConsoleApp.getConsoleApp().getModelProperty(OptionConstants.SHOW_RELEASE_NOTES);
        return value==null || value;
    }
    
    public void setShowReleaseNotes(boolean value) {
        Object oldVal = ConsoleApp.getConsoleApp().getModelProperty(OptionConstants.SHOW_RELEASE_NOTES);  
        ConsoleApp.getConsoleApp().setModelProperty(OptionConstants.SHOW_RELEASE_NOTES, value);  
        if (null != propSupport)
            propSupport.firePropertyChange(OptionConstants.SHOW_RELEASE_NOTES, oldVal, value);
    }
    
    public boolean isUseRunAsUserPreferences() {
        Boolean value = (Boolean) ConsoleApp.getConsoleApp().getModelProperty(OptionConstants.USE_RUN_AS_USER_PREFERENCES);
        return value!=null && value;
    }
    
    public void setUseRunAsUserPreferences(boolean value) {
        Object oldVal = ConsoleApp.getConsoleApp().getModelProperty(OptionConstants.USE_RUN_AS_USER_PREFERENCES);  
        ConsoleApp.getConsoleApp().setModelProperty(OptionConstants.USE_RUN_AS_USER_PREFERENCES, value); 
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
