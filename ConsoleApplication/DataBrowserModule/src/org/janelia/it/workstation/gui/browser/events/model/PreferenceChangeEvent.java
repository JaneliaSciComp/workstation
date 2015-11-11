package org.janelia.it.workstation.gui.browser.events.model;

import org.janelia.it.jacs.model.domain.Preference;

/**
 * A user preference has changed. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class PreferenceChangeEvent {

    private Preference preference;
    
    public PreferenceChangeEvent(Preference preference) {
        this.preference = preference;
    }
           
    public Preference getPreference() {
        return preference;
    }
}
