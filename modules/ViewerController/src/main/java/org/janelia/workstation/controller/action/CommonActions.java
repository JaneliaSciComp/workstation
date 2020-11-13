package org.janelia.workstation.controller.action;

import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.model.domain.tiledMicroscope.TmStructuredTextAnnotation;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.workstation.controller.ComponentUtil;
import org.janelia.workstation.controller.NeuronManager;
import org.janelia.workstation.controller.dialog.AddEditNoteDialog;
import org.janelia.workstation.controller.model.TmModelManager;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

public class CommonActions {
    private static final Logger log = LoggerFactory.getLogger(CommonActions.class);

    /**
     * pop a dialog to add, edit, or delete note at the given annotation
     */
    public static void addEditNote(final Long neuronID, final Long annotationID) {
        NeuronManager neuronManager = NeuronManager.getInstance();
      //  if (!checkOwnership(neuronID))
      //      return;

        String noteText = neuronManager.getNote(neuronID, annotationID);
        TmGeoAnnotation annotation = neuronManager.getGeoAnnotationFromID(neuronID, annotationID);
        TmNeuronMetadata neuron = neuronManager.getNeuronFromNeuronID(neuronID);
        AddEditNoteDialog testDialog = new AddEditNoteDialog(
                null,
                noteText,
                neuronManager.getNeuronFromNeuronID(neuronID),
                annotationID);
        testDialog.setVisible(true);
        if (testDialog.isSuccess()) {
            SimpleWorker noteExecuter = new SimpleWorker() {
                @Override
                protected void doStuff() throws Exception {
                    String resultText = testDialog.getOutputText().trim();
                    if (resultText.length() > 0) {
                        neuronManager.setNote(annotation, resultText);
                    } else {
                        final TmStructuredTextAnnotation textAnnotation = neuron.getStructuredTextAnnotationMap().get(annotationID);
                        neuronManager.removeNote(neuronID, textAnnotation);
                    }
                }

                @Override
                protected void hadSuccess() {
                    // nothing to see
                }

                @Override
                protected void hadError(Throwable error) {
                    FrameworkAccess.handleException(error);
                }
            };
            noteExecuter.execute();

        } else {
            // canceled
            return;
        }
    }

    public static void presentError(String message, String title) throws HeadlessException {
        JOptionPane.showMessageDialog(
                null,
                message,
                title,
                JOptionPane.ERROR_MESSAGE);
    }
}
