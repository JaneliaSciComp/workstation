package org.janelia.workstation.common.gui.presets;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;

import org.apache.commons.lang3.StringUtils;
import org.janelia.workstation.common.gui.dialogs.ModalDialog;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.integration.util.FrameworkAccess;

/**
 * A dialog which shows a list of presets and allows the user to delete and rename them. This is a generic component
 * and not bound to a particular widget. The PresetManager implementation accomplishes the actual
 * widget binding.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ManagePresetsDialog extends ModalDialog {

    private final PresetManager<?> manager;
    private final DefaultListModel<Preset> listModel;
    private final JList<Preset> presetList;
    private final JPanel buttonPane;
    private final JButton deleteButton;
    private final JButton renameButton;
    private final JButton doneButton;

    public ManagePresetsDialog(PresetManager<?> manager) {

        this.manager = manager;
        setTitle("Connect to Workstation Server");

        deleteButton = new JButton("Delete");
        deleteButton.setToolTipText("Delete the selected preset");
        deleteButton.setEnabled(false);
        deleteButton.addActionListener(e -> deleteCurrent());

        renameButton = new JButton("Rename");
        renameButton.setToolTipText("Rename the selected preset");
        renameButton.setEnabled(false);
        renameButton.addActionListener(e -> renameCurrent());

        doneButton = new JButton("Done");
        doneButton.setToolTipText("Close this window");
        doneButton.addActionListener(e -> saveAndClose());

        buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.PAGE_AXIS));
        buttonPane.add(deleteButton);
        buttonPane.add(Box.createVerticalStrut(5));
        buttonPane.add(renameButton);
        buttonPane.add(Box.createVerticalGlue());
        buttonPane.add(doneButton);

        listModel = new DefaultListModel<>();
        presetList = new JList<>(listModel);
        presetList.setPreferredSize(new Dimension(300, 250));
        presetList.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));

        presetList.addListSelectionListener(e -> {
            Preset selectedValue = presetList.getSelectedValue();
            if (selectedValue!=null) {
                boolean editable = selectedValue.getOwnerKey().equals(AccessManager.getSubjectKey());
                deleteButton.setEnabled(editable);
                renameButton.setEnabled(editable);
            }
        });

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.add(new JLabel("Manage Presets:"), BorderLayout.NORTH);
        mainPanel.add(presetList, BorderLayout.CENTER);
        mainPanel.add(buttonPane, BorderLayout.EAST);

        add(mainPanel, BorderLayout.CENTER);
    }

    private void load() {

        listModel.removeAllElements();
        for (Preset preset : manager.getPresets()) {
            boolean editable = preset.getOwnerKey().equals(AccessManager.getSubjectKey());
            if (editable) {
                listModel.addElement(preset);
            }
        }

    }

    public void showDialog() {
        load();
        packAndShow();
    }

    private void deleteCurrent() {
        Preset selectedValue = presetList.getSelectedValue();
        if (manager.deletePreset(selectedValue)) {
            listModel.removeElement(selectedValue);
        }
    }

    private void renameCurrent() {
        Preset selectedValue = presetList.getSelectedValue();

        String newName = (String) JOptionPane.showInputDialog(FrameworkAccess.getMainFrame(),
                "Preset Name:\n", "Rename Preset", JOptionPane.PLAIN_MESSAGE, null,
                null, selectedValue.getName());
        if (StringUtils.isBlank(newName)) {
            return;
        }

        if (manager.renamePreset(selectedValue, newName)) {
            load();
        }
    }

    private void saveAndClose() {
        setVisible(false);
    }

}
