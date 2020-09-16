package org.janelia.workstation.controller.action;

import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.workstation.controller.NeuronManager;
import org.janelia.workstation.controller.TmViewerManager;
import org.janelia.workstation.controller.dialog.ChangeNeuronOwnerDialog;
import org.janelia.workstation.controller.model.TmModelManager;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.core.util.ConsoleProperties;
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
import java.awt.event.ActionEvent;

/**
 * pop up a dialog so the user can choose a new owner for the target neuron, which is
 * passed in, or if not, is the currently selected neuron
 */
public class ChangeNeuronOwnerAction extends AbstractAction {
    private static final Logger log = LoggerFactory.getLogger(ChangeNeuronOwnerAction.class);

    private TmNeuronMetadata targetNeuron;

    private static final String TRACERS_GROUP = ConsoleProperties.getInstance().getProperty("console.LVVHorta.tracersgroup").trim();

    public ChangeNeuronOwnerAction() {
        super("Change Neuron Owner");
        targetNeuron = null;
    }

    public ChangeNeuronOwnerAction(TmNeuronMetadata targetNeuron) {
        super("Change Neuron Owner");
        this.targetNeuron = targetNeuron;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        changeNeuronOwner();
    }

    public void execute() {
        changeNeuronOwner();
    }

    private void changeNeuronOwner() {
        // if we aren't given a neuron, use the currently selected neuron
        if (targetNeuron == null) {
            TmNeuronMetadata currentNeuron = TmModelManager.getInstance().getCurrentSelections().getCurrentNeuron();
            if (currentNeuron != null) {
                targetNeuron = currentNeuron;
            }
        }
        if (targetNeuron == null) {
            return;
        }

        String owner = targetNeuron.getOwnerName();
        String ownerKey = targetNeuron.getOwnerKey();
        String username = AccessManager.getAccessManager().getActualSubject().getName();

        if (owner.equals(username) ||
                ownerKey.equals(TRACERS_GROUP) ||
                // admins can change ownership on any neuron
                TmViewerManager.getInstance().isOwnershipAdmin()) {

            // pop up a dialog so the user can request to change the ownership
            //  of the neuron
            ChangeNeuronOwnerDialog dialog = new ChangeNeuronOwnerDialog(null);
            dialog.setVisible(true);
            if (dialog.isSuccess()) {
                SimpleWorker changer = new SimpleWorker() {
                    @Override
                    protected void doStuff() throws Exception {
                        NeuronManager.getInstance().changeNeuronOwner(targetNeuron.getId(), dialog.getNewOwnerKey());
                    }

                    @Override
                    protected void hadSuccess() {

                    }

                    @Override
                    protected void hadError(Throwable error) {
                        FrameworkAccess.handleException("Could not change neuron owner!", error);
                    }
                };
                changer.execute();
            }

        } else {
            // submit a request to take ownership

            // for now:
            JOptionPane.showMessageDialog(null,
                    owner + " owns this neuron. You need to ask them or an admin to give this neuron to you.");
        }
    }
}
