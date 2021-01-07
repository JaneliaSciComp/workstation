package org.janelia.workstation.controller.action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.AbstractAction;
import org.janelia.workstation.controller.NeuronManager;
import org.janelia.workstation.controller.model.TmSelectionState;
import org.janelia.workstation.controller.model.annotations.neuron.PredefinedNote;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.awt.ActionID;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "Horta",
        id = "org.janelia.horta.actions.AddUnique2NoteAction"
)
@ActionRegistration(
        displayName = "#CTL_AddUnique2Note",
        lazy = true
)
@Messages("CTL_AddUnique2Note=Add Unique 2 Note")
public final class AddUnique2NoteAction
extends AbstractAction
implements ActionListener
{
    public AddUnique2NoteAction() {
        super("Add Unique 2 Note");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        NeuronManager neuronManager = NeuronManager.getInstance();
        TmSelectionState state = TmSelectionState.getInstance();
        SimpleWorker setter = new SimpleWorker() {
            @Override
            protected void doStuff() throws Exception {
                neuronManager.setNote(state.getCurrentVertex(), PredefinedNote.UNIQUE_2.getNoteText());
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
