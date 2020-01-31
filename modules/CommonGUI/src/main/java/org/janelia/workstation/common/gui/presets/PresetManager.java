package org.janelia.workstation.common.gui.presets;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import org.apache.commons.lang3.SerializationException;
import org.janelia.model.domain.Preference;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Presents GUI presets as Preferences.
 * A preset manager instance can be implemented to manage the presets for a particular widget.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class PresetManager<T> {

    private final static Logger log = LoggerFactory.getLogger(PresetManager.class);

    private static final String PRESET_CATEGORY_PREFIX = "Presets_";

    private final String preferenceCategory;

    public PresetManager(String presetName) {
        this.preferenceCategory = PRESET_CATEGORY_PREFIX + presetName;
    }

    public List<Preset> getPresets() {
        try {
            return DomainMgr.getDomainMgr().getPreferencesByCategory(preferenceCategory)
                    .stream()
                    .sorted((o1, o2) -> ComparisonChain.start()
                            .compareTrueFirst(ClientDomainUtils.isOwner(o2), ClientDomainUtils.isOwner(o1))
                            .compare(o1.getId(), o2.getId(), Ordering.natural())
                            .result())
                    .map(Preset::new)
                    .collect(Collectors.toList());
        }
        catch (Exception e) {
            log.error("Error getting preferences for category {}", preferenceCategory, e);
            return Collections.emptyList();
        }
    }

    public void saveCurrentPreset(String newName) {
        T currentSettings = getCurrentSettings();
        try {
            String serialized = serialize(currentSettings);
            DomainMgr.getDomainMgr().setPreference(preferenceCategory, newName, serialized);
        }
        catch (Exception e) {
            FrameworkAccess.handleException(e);
        }
    }

    public boolean deletePreset(Preset preset) {
        try {
            // Clear old preset
            Preference preference = preset.getPreference();
            DomainMgr.getDomainMgr().setPreference(preferenceCategory, preference.getKey(), null);
            log.info("Deleted preset {}", preference.getKey());
            return true;
        }
        catch (Exception e) {
            FrameworkAccess.handleException(e);
            return false;
        }
    }

    public boolean renamePreset(Preset preset, String newName) {
        try {
            Preference preference = preset.getPreference();
            String oldName = preference.getKey();
            // Save with new name first. In case something goes wrong the settings won't be lost.
            DomainMgr.getDomainMgr().setPreference(preferenceCategory, newName, preference.getValue());
            // Clear old preset
            DomainMgr.getDomainMgr().setPreference(preferenceCategory, oldName, null);
            log.info("Renamed preset {} to {}", oldName, newName);
            return true;
        }
        catch (Exception e) {
            FrameworkAccess.handleException(e);
            return false;
        }
    }

    /**
     * Implement this method to return the widget's current settings as an object that can be persisted as a preference.
     */
    protected abstract T getCurrentSettings();

    /**
     * Implement this method to load the given preset into the widget UI.
     * @param preset the preset
     */
    protected void loadPreset(Preset preset) {
        T settings;
        try {
            settings = deserialize(preset.getSerializedObject());
        }
        catch (Exception e) {
            throw new SerializationException("Problem deserializing preset", e);
        }
        loadSettings(settings);
    }

    protected abstract void loadSettings(T settings);

    protected abstract String serialize(T config) throws Exception;

    protected abstract T deserialize(String json) throws Exception;
}
