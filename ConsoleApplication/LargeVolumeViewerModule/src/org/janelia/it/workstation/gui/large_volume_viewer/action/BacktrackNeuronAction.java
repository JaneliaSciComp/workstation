package org.janelia.it.workstation.gui.large_volume_viewer.action;

import org.janelia.it.workstation.gui.large_volume_viewer.QuadViewUi;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * this action moves the camera back along a neuron, typically
 * to the next branch point toward the neuron root
 */
public class BacktrackNeuronAction extends AbstractAction {

    private QuadViewUi ui;

    public BacktrackNeuronAction(QuadViewUi ui) {
        this.ui = ui;
        putValue(NAME, "Backtrack along neuron");
        String acc = "B";
        KeyStroke accelerator = KeyStroke.getKeyStroke(acc);
        putValue(ACCELERATOR_KEY, accelerator);
        putValue(SHORT_DESCRIPTION, "Move along the neuron backbone toward the root, stopping at branches");
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        ui.backtrackNeuronMicron();
    }

}
