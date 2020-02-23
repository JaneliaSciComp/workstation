package org.janelia.workstation.controller.action;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import org.janelia.workstation.controller.ComponentUtil;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.controller.AnnotationModel;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * this action pops a dialog to let the user choose a color for all
 * neurons currently visible in the neuron list
 */
public class BulkChangeNeuronColorAction extends AbstractAction {

    private static final Logger logger = LoggerFactory.getLogger(BulkChangeNeuronColorAction.class);

    private AnnotationModel annModel;
    private NeuronListProvider listProvider;

    public BulkChangeNeuronColorAction(AnnotationModel annotationModel, NeuronListProvider listProvider) {
        this.annModel = annotationModel;
        this.listProvider = listProvider;

        putValue(NAME, "Choose neuron color...");
        putValue(SHORT_DESCRIPTION, "Change color for all neurons in the list");
    }

    @Override
    public void actionPerformed(ActionEvent action) {
        //final Color color = AnnotationManager.askForNeuronColor(null);
        final Color color = new Color(0,0,0);
        if (color != null) {
            final List<TmNeuronMetadata> neurons = listProvider.getNeuronList();
            int ans = JOptionPane.showConfirmDialog(
                    null,
                    String.format("You are about to change the color for %d neurons. This action cannot be undone! Continue?",
                        neurons.size()),
                    "Change colors?",
                    JOptionPane.OK_CANCEL_OPTION
            );
            if (ans == JOptionPane.OK_OPTION) {
                SimpleWorker changer = new SimpleWorker() {
                    @Override
                    protected void doStuff() throws Exception {
                        // Stopwatch stopwatch = new Stopwatch();
                        // stopwatch.start();
                      //  annModel.setNeuronColors(neurons, color);
                        // System.out.println("changed style of " + neurons.size() + " neurons in " + stopwatch);
                        // stopwatch.stop();
                    }

                    @Override
                    protected void hadSuccess() {
                        // nothing
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        logger.error("error changing color for multiple neurons");
                        JOptionPane.showMessageDialog(
                                ComponentUtil.getMainWindow(),
                                "Error changing neuron color!",
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                };
                changer.execute();
            }
        }
    }
}
