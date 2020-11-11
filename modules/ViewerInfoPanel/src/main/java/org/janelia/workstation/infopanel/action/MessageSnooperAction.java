package org.janelia.workstation.infopanel.action;

import Jama.Matrix;
import org.janelia.it.jacs.shared.swc.MatrixDrivenSWCExchanger;
import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.model.util.MatrixUtilities;
import org.janelia.workstation.controller.model.TmModelManager;
import org.janelia.workstation.geom.Vec3;
import org.janelia.workstation.infopanel.dialog.MessageSnooperDialog;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * this action opens a dialog in which information on the neurons
 * in the current workspace is displayed; currently we limit to the
 * neurons visible in the neuron list
 */
public class MessageSnooperAction extends AbstractAction {
    MessageSnooperDialog dialog;
    public MessageSnooperAction() {
        putValue(NAME, "Show message traffic...");
        putValue(SHORT_DESCRIPTION, "Show message traffic");
    }


    @Override
    public void actionPerformed(ActionEvent action) {
        dialog = new MessageSnooperDialog();
        dialog.pack();
        dialog.setSize(500, 300);
        dialog.setVisible(true);
    }
}