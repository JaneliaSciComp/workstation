package org.janelia.it.workstation.gui.large_volume_viewer.action;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import org.janelia.it.jacs.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.janelia.it.workstation.gui.large_volume_viewer.ComponentUtil;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationManager;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.BasicAnnotationManager;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.NeuronListProvider;
import org.janelia.it.workstation.gui.large_volume_viewer.top_component.LargeVolumeViewerTopComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * this action pops a dialog to let the user choose a color for all
 * neurons currently visible in the neuron list
 */
public class BulkChangeNeuronColorAction extends AbstractAction {

    private static final Logger logger = LoggerFactory.getLogger(BulkChangeNeuronColorAction.class);

    private NeuronListProvider listProvider;

    public BulkChangeNeuronColorAction(NeuronListProvider listProvider) {
        this.listProvider = listProvider;

        putValue(NAME, "Choose neuron color...");
        putValue(SHORT_DESCRIPTION, "Change color for all neurons in the list");
    }

    @Override
    public void actionPerformed(ActionEvent action) {
        final Color color = BasicAnnotationManager.askForNeuronColor(null);
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
                        AnnotationManager annotationMgr = LargeVolumeViewerTopComponent.getInstance().getAnnotationMgr();
                        annotationMgr.setNeuronColors(neurons, color);
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
                                ComponentUtil.getLVVMainWindow(),
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
