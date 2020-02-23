package org.janelia.workstation.controller.action;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.janelia.workstation.controller.ComponentUtil;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.core.util.ConsoleProperties;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.controller.AnnotationModel;
import org.janelia.console.viewerapi.dialogs.ChangeNeuronOwnerDialog;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.model.security.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * this action pops up a dialog prompting user to choose a new
 * owner for the currently visible neurons in the neuron list
 */
public class BulkChangeNeuronOwnerAction extends AbstractAction{

    private static final Logger logger = LoggerFactory.getLogger(BulkChangeNeuronColorAction.class);

    private AnnotationModel annModel;
    private NeuronListProvider listProvider;

    public BulkChangeNeuronOwnerAction(AnnotationModel annModel, NeuronListProvider listProvider) {
        this.annModel = annModel;
        this.listProvider = listProvider;

        putValue(NAME, "Choose neuron owner...");
        putValue(SHORT_DESCRIPTION, "Change owner for all neurons in the list");
    }

    @Override
    public void actionPerformed(ActionEvent action) {

        final List<TmNeuronMetadata> neurons = listProvider.getNeuronList();
        if (neurons.size() == 0) {
            JOptionPane.showMessageDialog(
                    ComponentUtil.getMainWindow(),
                    "No neurons chosen!",
                    "Can't change owner",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String tracersGroup = ConsoleProperties.getInstance().getProperty("console.LVVHorta.tracersgroup").trim();

        // check that the user is allowed to change owner for all neurons in list
        boolean allowed = false;
        boolean isOwnershipAdmin = false;
        // REFACTOR
        if (isOwnershipAdmin) {
            allowed = true;
        } else {
            String username = AccessManager.getAccessManager().getActualSubject().getName();
            for (TmNeuronMetadata neuron: neurons) {
                if (neuron.getOwnerName().equals(username) || neuron.getOwnerKey().equals(tracersGroup)) {
                    allowed = true;
                } else {
                    // at the first not-allowed, stop
                    allowed = false;
                    break;
                }
            }
        }

        if (!allowed) {
            JOptionPane.showMessageDialog(
                    ComponentUtil.getMainWindow(),
                    "You do not have permission to change one or more of the currently displayed neurons.",
                    "Can't change owner",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // user is allowed; ask them who the new owner should be:
        ChangeNeuronOwnerDialog dialog = new ChangeNeuronOwnerDialog((Frame) SwingUtilities.windowForComponent(ComponentUtil.getMainWindow()));
        dialog.setVisible(true);
        if (!dialog.isSuccess()) {
            return;
        }

        // quick timing test suggests it takes ~0.4s per neuron; estimate the time for
        //  the user and warn them!
        double seconds = 0.4 * neurons.size();
        String message;
        if (seconds < 5.0) {
            message = String.format("You are about to change the owner for %d neurons. This action cannot be undone. Continue?",
                    neurons.size());
        } else {
            message = String.format("You are about to change the owner for %d neurons. This action cannot be undone.",
                    neurons.size());
            if (seconds < 60.0) {
                message += String.format("\n\nThis operation is estimated to take about %d seconds. The UI will not be responsive during\nthis time. IMPORTANT: This will also affect anyone using the same workspace!\n\nUpdate ownership?", Math.round(seconds));
            } else if (seconds < 3600.0) {
                message += String.format("\n\nThis operation is estimated to take about %.1f minutes. The UI will not be responsive during\nthis time. IMPORTANT: This will also affect anyone using the same workspace!\n\nUpdate ownership?", seconds / 60.0);
            } else {
                message += String.format("\n\nThis operation is estimated to take about %.1f hours. The UI will not be responsive during\nthis time. IMPORTANT: This will also affect anyone using the same workspace!\n\nUpdate ownership?", seconds / 3600.0);
            }
        }

        int ans = JOptionPane.showConfirmDialog(
            null,
            message,
            "Change owner?",
            JOptionPane.OK_CANCEL_OPTION
        );
        if (ans == JOptionPane.OK_OPTION) {
            // implementation note: I tried using a BackgroundWorker instead of a SimpleWorker,
            //  but since the UI updates happen for each neuron, even though the actual
            //  operation happened in a different thread, all the UI update effectively
            //  locked the UI anyway, making it irrelevant to user the BG Worker;
            //  if we do a real bulk update, we should switch this to a BG Worker, though


            SimpleWorker changer = new SimpleWorker() {
                @Override
                protected void doStuff() throws Exception {
                    // we really need to do a bulk change operation here; this could be
                    //  slow, because every neuron owner change does its own completable future
                    //  and waits for it to finish; for now, rely on the time warning
                    //  in the dialog above
                    Subject newOwner = dialog.getNewOwnerKey();
                    for (TmNeuronMetadata neuron: neurons) {
                      //  annMgr.changeNeuronOwner(neuron, newOwner);
                    }
                }

                @Override
                protected void hadSuccess() {
                    // nothing
                }

                @Override
                protected void hadError(Throwable error) {
                    logger.error("error changing owner for multiple neurons");
                    JOptionPane.showMessageDialog(
                            ComponentUtil.getMainWindow(),
                            "Error changing neuron owner!",
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            };
            changer.execute();
        }
    }
}
