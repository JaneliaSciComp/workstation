package org.janelia.it.workstation.browser.gui.options;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.prefs.Preferences;

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
