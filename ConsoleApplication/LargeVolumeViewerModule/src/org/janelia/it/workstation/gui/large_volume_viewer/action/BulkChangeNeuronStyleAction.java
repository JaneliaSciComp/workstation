package org.janelia.it.workstation.gui.large_volume_viewer.action;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmNeuron;
import org.janelia.it.workstation.gui.large_volume_viewer.ComponentUtil;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationManager;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationModel;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.NeuronListProvider;
import org.janelia.it.workstation.gui.large_volume_viewer.style.NeuronStyle;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * this action pops a dialog to let the user choose a style for all
 * neurons currently visible in the neuron list
 */
public class BulkChangeNeuronStyleAction extends AbstractAction {

    private static final Logger logger = LoggerFactory.getLogger(BulkChangeNeuronStyleAction.class);

    private AnnotationModel annModel;
    private NeuronListProvider listProvider;

    public BulkChangeNeuronStyleAction(AnnotationModel annotationModel, NeuronListProvider listProvider) {
        this.annModel = annotationModel;
        this.listProvider = listProvider;

        putValue(NAME, "Bulk change neuron style...");
        putValue(SHORT_DESCRIPTION, "Change neuron style for all visible neurons");
    }

    @Override
    public void actionPerformed(ActionEvent action) {
        final NeuronStyle style = AnnotationManager.askForNeuronStyle(null);
        if (style != null) {
            final List<TmNeuron> neurons = listProvider.getNeuronList();
            int ans = JOptionPane.showConfirmDialog(
                    null,
                    String.format("You are about to change the style for %d neurons. This action cannot be undone! Continue?",
                        neurons.size()),
                    "Change styles?",
                    JOptionPane.OK_CANCEL_OPTION
            );
            if (ans == JOptionPane.OK_OPTION) {
                SimpleWorker changer = new SimpleWorker() {
                    @Override
                    protected void doStuff() throws Exception {
                        annModel.setNeuronStyles(neurons, style);
                    }

                    @Override
                    protected void hadSuccess() {
                        // nothing
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        logger.error("error changing style for multiple neurons");
                        JOptionPane.showMessageDialog(
                                ComponentUtil.getLVVMainWindow(),
                                "Error changing neuron styles!",
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                };
                changer.execute();
            }
        }
    }
}
