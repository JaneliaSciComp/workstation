package org.janelia.it.workstation.gui.large_volume_viewer.action;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.List;

import javax.swing.*;

import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmNeuron;
import org.janelia.it.workstation.gui.large_volume_viewer.ComponentUtil;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationModel;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.NeuronListProvider;

/**
 * this action opens a dialog that allows the user to add
 * a neuron tag to all the neurons that are visible in
 * the neuron list
 */
public class BulkAddNeuronTagAction extends AbstractAction {

    private AnnotationModel annModel;
    private NeuronListProvider listProvider;

    private JPanel mainPanel;
    private JList existingList;
    private JTextField newTagField;


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

    private void requestAddTag(String tag) {
        // dialog: are you sure
        int ans = JOptionPane.showConfirmDialog(
                ComponentUtil.getLVVMainWindow(),
                String.format("You are about to add the tag '%s' to %d neurons. This action cannot be undone! Continue?",
                        tag, listProvider.getNeuronList().size()),
                "Add tags?",
                JOptionPane.OK_CANCEL_OPTION
                );
        if (ans == JOptionPane.OK_OPTION) {
            addTag(tag);
        }
    }

    private void addTag(String tag) {


        System.out.println("actually adding tag " + tag);


        // should I add a bulk add function annModel.addNeuronTags(String tag, List<TmNeuron> neuronList)?
        for (TmNeuron neuron: listProvider.getNeuronList()) {
            annModel.addNeuronTag(tag, neuron);
        }
    }

    private JPanel getInterface() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));

        JPanel existingPanel = new JPanel();
        existingPanel.setLayout(new BoxLayout(existingPanel, BoxLayout.LINE_AXIS));

        JButton existingButton = new JButton("Add");
        existingButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                requestAddTag((String) existingList.getSelectedValue());
            }
        });
        existingPanel.add(existingButton);

        existingPanel.add(new JLabel("existing tag"));

        existingList = new JList();
        List<TmNeuron> neurons = listProvider.getNeuronList();
        String[] existingTags = neurons.toArray(new String[neurons.size()]);
        Arrays.sort(existingTags);
        existingList.setListData(existingTags);
        existingPanel.add(existingList);

        existingPanel.add(Box.createHorizontalGlue());
        mainPanel.add(existingPanel);


        JPanel newTagPanel = new JPanel();
        newTagPanel.setLayout(new BoxLayout(newTagPanel, BoxLayout.LINE_AXIS));

        JButton newTagButton = new JButton("Add");
        newTagButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                requestAddTag(newTagField.getText());
            }
        });
        newTagPanel.add(newTagButton);
        newTagPanel.add(new JLabel("new tag"));
        newTagField = new JTextField();
        newTagPanel.add(newTagField);

        newTagPanel.add(Box.createHorizontalGlue());
        mainPanel.add(newTagPanel);

        ButtonGroup group = new ButtonGroup();
        group.add(existingButton);
        group.add(newTagButton);

        return mainPanel;
    }
}
