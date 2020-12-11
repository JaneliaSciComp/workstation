package org.janelia.horta.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.KeyStroke;
import org.janelia.horta.NeuronTracerTopComponent;
import org.janelia.workstation.controller.NeuronManager;
import org.janelia.workstation.controller.model.TmSelectionState;
import org.janelia.workstation.controller.model.annotations.neuron.PredefinedNote;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ActionID(
        category = "Horta",
        id = "org.janelia.horta.actions.AddTracedEndNoteAction"
)
@ActionRegistration(
        displayName = "#CTL_AddTracedEndNote",
        lazy = true
)
@ActionReferences({
    @ActionReference(path = "Shortcuts", name = "T")
})
@Messages("CTL_AddTracedEndNote=Add Traced End Note")

public final class AddTracedEndNoteAction
extends AbstractAction
implements ActionListener
{
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public AddTracedEndNoteAction() {
        super("Add Traced End Note");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        NeuronManager neuronManager = NeuronManager.getInstance();
        TmSelectionState state = TmSelectionState.getInstance();
        SimpleWorker setter = new SimpleWorker() {
            @Override
            protected void doStuff() throws Exception {
                neuronManager.setNote(state.getCurrentVertex(), PredefinedNote.TRACED_END.getNoteText());
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
