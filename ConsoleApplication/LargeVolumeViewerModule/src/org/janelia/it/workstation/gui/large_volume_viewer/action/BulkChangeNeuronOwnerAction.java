package org.janelia.it.workstation.gui.large_volume_viewer.action;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.janelia.it.workstation.browser.api.AccessManager;
import org.janelia.it.workstation.browser.util.ConsoleProperties;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.janelia.it.workstation.gui.large_volume_viewer.ComponentUtil;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationManager;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationModel;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.NeuronListProvider;
import org.janelia.it.workstation.gui.large_volume_viewer.dialogs.ChangeNeuronOwnerDialog;
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

    private AnnotationManager annMgr;
    private AnnotationModel annModel;
    private NeuronListProvider listProvider;

    private static final String COMMON_USER_KEY = ConsoleProperties.getInstance().getProperty("domain.msgserver.systemowner").trim();

    public BulkChangeNeuronOwnerAction(AnnotationManager annMgr, AnnotationModel annModel, NeuronListProvider listProvider) {
        this.annMgr = annMgr;
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
                    ComponentUtil.getLVVMainWindow(),
                    "No neurons chosen!",
                    "Can't change owner",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // check that the user is allowed to change owner for all neurons in list
        boolean allowed = false;
        if (annMgr.isOwnershipAdmin()) {
            allowed = true;
        } else {
            String username = AccessManager.getAccessManager().getActualSubject().getName();
            for (TmNeuronMetadata neuron: neurons) {
                if (neuron.getOwnerName().equals(username) || neuron.getOwnerKey().equals(COMMON_USER_KEY)) {
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
                    ComponentUtil.getLVVMainWindow(),
                    "You do not have permission to change one or more of the currently displayed neurons.",
                    "Can't change owner",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // user is allowed; ask them who the new owner should be:
        ChangeNeuronOwnerDialog dialog = new ChangeNeuronOwnerDialog((Frame) SwingUtilities.windowForComponent(ComponentUtil.getLVVMainWindow()));
        dialog.setVisible(true);
        if (!dialog.isSuccess()) {
            return;
        }

        int ans = JOptionPane.showConfirmDialog(
            null,
            String.format("You are about to change the owner for %d neurons. This action cannot be undone. Continue?",
                    neurons.size()),
            "Change owner?",
            JOptionPane.OK_CANCEL_OPTION
        );
        if (ans == JOptionPane.OK_OPTION) {
            SimpleWorker changer = new SimpleWorker() {
                @Override
                protected void doStuff() throws Exception {


                    // we really need to do a bulk change operation here; this could be
                    //  slow, because every neuron owner change does its own completable future
                    //  and waits for it to finish


                    Subject newOwner = dialog.getNewOwnerKey();
                    for (TmNeuronMetadata neuron: neurons) {
                        annMgr.changeNeuronOwner(neuron, newOwner);
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
                            ComponentUtil.getLVVMainWindow(),
                            "Error changing neuron owner!",
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            };
            changer.execute();
        }
    }
}
