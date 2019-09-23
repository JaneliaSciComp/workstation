package org.janelia.workstation.common.gui.presets;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;

import org.apache.commons.lang3.StringUtils;
import org.janelia.workstation.common.gui.support.buttons.DropDownButton;
import org.janelia.workstation.integration.util.FrameworkAccess;

/**
 * Allows the user to load various presets for a GUI widget, and to manage those presets. This is a generic
 * component which is not bound to a particular widget. The PresetManager implementation accomplishes the actual
 * widget binding.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class PresetSelectionButton extends DropDownButton {

    private final PresetManager<?> manager;

    public PresetSelectionButton(PresetManager<?> manager) {
        setText("Presets");
        this.manager = manager;
    }

    @Override
    public List<Component> getMenuComponents() {

        List<Component> components = new ArrayList<>();
        List<Preset> presets = manager.getPresets();

        if (!presets.isEmpty()) {
            JMenuItem menuItem = new JMenuItem("Load preset:");
            menuItem.setEnabled(false);
            components.add(menuItem);
        }

        for (Preset preset : presets) {
            components.add(createPrefixItem(preset));
        }

        if (!presets.isEmpty()) {
            components.add(new JSeparator());
        }

        JMenuItem manageMenuItem = new JMenuItem("Manage Presets...");
        manageMenuItem.addActionListener(e -> {
            ManagePresetsDialog dialog = new ManagePresetsDialog(manager);
            dialog.showDialog();
        });
        components.add(manageMenuItem);

        JMenuItem saveMenuItem = new JMenuItem("Save Current Settings As...");
        saveMenuItem.addActionListener(e -> {
            try {

                String newName = (String) JOptionPane.showInputDialog(this,
                        "Preset Name:\n", "Create New Preset", JOptionPane.PLAIN_MESSAGE, null, null, null);
                if (StringUtils.isBlank(newName)) {
                    return;
                }

                manager.saveCurrentPreset(newName);
            }
            catch (Exception ex) {
                FrameworkAccess.handleException(ex);
            }
        });
        components.add(saveMenuItem);

        return components;
    }

    private JMenuItem createPrefixItem(Preset preset) {
        JMenuItem menuItem = new JMenuItem(preset.getName());
        menuItem.addActionListener(e -> {
            manager.loadPreset(preset);
        });
        return menuItem;
    }
}
