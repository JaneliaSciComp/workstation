package org.janelia.workstation.common.gui.presets;

import org.janelia.model.domain.Preference;

/**
 * A preset is an owned, named object which contains some parameters that can be loaded into the GUI. It is implemented
 * using the Preference mechanism.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class Preset {

    private Preference preference;

    public Preset(Preference preference) {
        this.preference = preference;
    }

    Preference getPreference() {
        return preference;
    }

    public String getOwnerKey() {
        return preference.getSubjectKey();
    }

    public String getName() {
        return preference.getKey();
    }

    public String getSerializedObject() {
        return (String)preference.getValue();
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Preset preset = (Preset) o;

        return preference.getId().equals(preset.preference.getId());
    }

    @Override
    public int hashCode() {
        return preference.getId().hashCode();
    }
}
