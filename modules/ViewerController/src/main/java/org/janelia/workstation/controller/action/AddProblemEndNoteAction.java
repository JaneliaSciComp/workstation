package org.janelia.workstation.controller.action;

import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.workstation.controller.NeuronManager;
import org.janelia.workstation.controller.model.TmModelManager;
import org.janelia.workstation.controller.model.TmSelectionState;
import org.janelia.workstation.controller.model.annotations.neuron.PredefinedNote;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.awt.ActionID;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


@ActionID(
        category = "Horta",
        id = "org.janelia.horta.actions.AddProblemEndNoteAction"
)
@ActionRegistration(
        displayName = "#CTL_AddProblemEndNote",
        lazy = true
)
@NbBundle.Messages("CTL_AddProblemEndNote=Add Problem End Note")


public class AddProblemEndNoteAction
        extends AbstractAction
        implements ActionListener
{
    public AddProblemEndNoteAction() {
        super("Add Problem End Note");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        TmModelManager modelManager = TmModelManager.getInstance();
        if (modelManager.getCurrentWorkspace()==null ||
                modelManager.getCurrentSelections().getCurrentVertex()==null)
            return;
        NeuronManager neuronManager = NeuronManager.getInstance();
        TmSelectionState state = TmSelectionState.getInstance();
        TmGeoAnnotation ann = state.getCurrentVertex();
        if (ann == null) {
            // no vertex selected
            return;
        }
        TmNeuronMetadata neuron = neuronManager.getNeuronFromNeuronID(ann.getNeuronId());
        if (neuron == null) {
            return;
        }
        if (!PredefinedNote.TRACED_END.isValid(neuron, ann.getId())) {
            JOptionPane.showMessageDialog(
                    null,
                    "Can't apply 'Problem end' note to an annotation that is not an end!",
                    "Not an end!",
                    JOptionPane.OK_OPTION);
            return;
        }

        SimpleWorker setter = new SimpleWorker() {
            @Override
            protected void doStuff() throws Exception {
                neuronManager.setNote(state.getCurrentVertex(), PredefinedNote.PROBLEM_END.getNoteText());
            }

            @Override
            protected void hadSuccess() {
                // nothing to see here
            }

            @Override
            protected void hadError(Throwable error) {
                FrameworkAccess.handleException(error);
            }
        };
        setter.execute();
    }
}

