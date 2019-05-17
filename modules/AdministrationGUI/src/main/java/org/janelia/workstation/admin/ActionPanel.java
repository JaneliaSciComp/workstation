package org.janelia.workstation.admin;

import javax.swing.JPanel;

import org.janelia.workstation.common.gui.support.WrapLayout;

/**
 * All screens in the AdministrationGUI can reuse this component in order to get a consistent GUI for action buttons.
 *
 * Uses a wrap layout to ensure that all actions are always displayed, no matter how narrow the panel is made.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ActionPanel extends JPanel {

    public ActionPanel() {
        super(new WrapLayout(false, WrapLayout.LEFT, 2, 2));
    }
}
