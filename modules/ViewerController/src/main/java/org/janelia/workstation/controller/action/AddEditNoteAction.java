package org.janelia.workstation.controller.action;

import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.model.domain.tiledMicroscope.TmStructuredTextAnnotation;
import org.janelia.workstation.controller.NeuronManager;
import org.janelia.workstation.controller.dialog.AddEditNoteDialog;
import org.janelia.workstation.controller.model.TmModelManager;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

@ActionID(
        category = "actions",
        id = "AddEditNoteAction"
)
@ActionRegistration(
        displayName = "#CTL_AddEditNoteAction",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/actions/LVV-Horta", position = 1510, separatorBefore = 1499)
})
@NbBundle.Messages("CTL_AddEditNoteAction=Add/Edit Note")
/**
 * delete the annotation with the input ID; the annotation must be a "link",
 * which is an annotation that is not a root (no parent) or branch point
 * (many children); in other words, it's an end point, or an annotation with
 * a parent and single child that can be connected up unambiguously
 */
public class AddEditNoteAction extends AbstractAction {
    private static final Logger log = LoggerFactory.getLogger(AddEditNoteAction.class);

    public AddEditNoteAction() {
        super("Add/Edit Note");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // get current neuron and selected annotation
        TmNeuronMetadata currNeuron = TmModelManager.getInstance().getCurrentSelections().getCurrentNeuron();
        TmGeoAnnotation currVertex = TmModelManager.getInstance().getCurrentSelections().getCurrentVertex();
        if (currNeuron==null || currVertex==null)
            return;

        addEditNote(currNeuron, currVertex);
    }

    public void execute(Long neuronID, Long vertexID) {
        NeuronManager manager = NeuronManager.getInstance();
        TmGeoAnnotation vertex = manager.getGeoAnnotationFromID(neuronID, vertexID);
        TmNeuronMetadata neuron = manager.getNeuronFromNeuronID(neuronID);
        addEditNote(neuron, vertex);
    }

    public void addEditNote(TmNeuronMetadata neuron, TmGeoAnnotation vertex) {
        Long neuronID = neuron.getId();
        Long vertexID = vertex.getId();
        NeuronManager manager = NeuronManager.getInstance();
        if (!TmModelManager.getInstance().checkOwnership(neuronID))
            return;

        String noteText = getNote(neuronID, vertexID);

        AddEditNoteDialog testDialog = new AddEditNoteDialog(
                null,
                noteText,
                neuron,
                vertexID);
        testDialog.setVisible(true);
        if (testDialog.isSuccess()) {
            String resultText = testDialog.getOutputText().trim();
            if (resultText.length() > 0) {
                setNote(neuronID, vertexID, resultText);
            } else {
                // empty string means delete note
                clearNote(neuronID, vertexID);
            }
        } else {
            // canceled
            return;
        }
    }

    public void clearNote(final Long neuronID, final Long annotationID) {
        TmNeuronMetadata neuron = NeuronManager.getInstance().getNeuronFromNeuronID(neuronID);
        if (!TmModelManager.getInstance().checkOwnership(neuronID))
            return;

        final TmStructuredTextAnnotation textAnnotation = neuron.getStructuredTextAnnotationMap().get(annotationID);
        if (textAnnotation != null) {
            SimpleWorker deleter = new SimpleWorker() {
                @Override
                protected void doStuff() throws Exception {
                    NeuronManager.getInstance().removeNote(neuronID, textAnnotation);
                }

                @Override
                protected void hadSuccess() {
                    // nothing to see
                }

                @Override
                protected void hadError(Throwable error) {
                    FrameworkAccess.handleException(new Throwable("Could not remove note!"));
                }
            };
            deleter.execute();
        }
    }

    /**
     * returns the note attached to a given annotation; returns empty
     * string if there is no note; you'll get an exception if the
     * annotation ID doesn't exist
     */
    public String getNote(final Long neuronID, Long annotationID) {
        return NeuronManager.getInstance().getNote(neuronID, annotationID);
    }

    public void setNote(final Long neuronID, final Long annotationID, final String noteText) {
        SimpleWorker setter = new SimpleWorker() {
            @Override
            protected void doStuff() throws Exception {
                NeuronManager.getInstance().setNote(NeuronManager.getInstance()
                        .getGeoAnnotationFromID(neuronID, annotationID), noteText);
            }

            @Override
            protected void hadSuccess() {
                // nothing here
            }

            @Override
            protected void hadError(Throwable error) {
                FrameworkAccess.handleException(new Throwable("Could not set note!"));
            }
        };
        setter.execute();
    }


}
