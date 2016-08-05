package org.janelia.it.workstation.gui.large_volume_viewer.action;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmNeuron;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationModel;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.NeuronListProvider;

/**
 * Created by olbrisd on 8/5/16.
 */
public class BulkAddNeuronTagAction extends AbstractAction {

    private AnnotationModel annModel;
    private NeuronListProvider listProvider;

    public BulkAddNeuronTagAction(AnnotationModel annotationModel, NeuronListProvider listProvider) {
        this.annModel = annotationModel;
        this.listProvider = listProvider;

        putValue(NAME, "Bulk add neuron tags...");
        putValue(SHORT_DESCRIPTION, "Add neuron tag to all visible neurons");
    }


    @Override
    public void actionPerformed(ActionEvent action) {
        List<TmNeuron> neurons = listProvider.getNeuronList();
        if (neurons.size() == 0) {
            return;
        }

        Object[] options = {"Add", "Cancel"};
        JOptionPane.showOptionDialog(null,
                getInterface(),
                "Add tags to " + neurons.size() + " neurons",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                options,
                options[1]
        );
    }

    private JPanel getInterface() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));


        panel.add(new JLabel("placeholder"));


        return panel;
    }
}
