package org.janelia.it.workstation.gui.large_volume_viewer.action;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.swing.*;

import com.google.common.base.Stopwatch;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmNeuron;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationModel;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.NeuronListProvider;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * this action opens a dialog that allows the user to add or remove
 * a neuron tag to all the neurons that are visible in
 * the neuron list
 */
public class BulkNeuronTagAction extends AbstractAction {

    private static final Logger logger = LoggerFactory.getLogger(BulkNeuronTagAction.class);

    private AnnotationModel annModel;
    private NeuronListProvider listProvider;

    private JPanel mainPanel;
    private JComboBox existingTagMenu;
    private JComboBox removeTagMenu;
    private JTextField newTagField;


    public BulkNeuronTagAction(AnnotationModel annotationModel, NeuronListProvider listProvider) {
        this.annModel = annotationModel;
        this.listProvider = listProvider;

        putValue(NAME, "Bulk edit neuron tags...");
        putValue(SHORT_DESCRIPTION, "Edit neuron tag for all visible neurons");
    }


    @Override
    public void actionPerformed(ActionEvent action) {
        List<TmNeuron> neurons = listProvider.getNeuronList();
        if (neurons.size() == 0) {
            return;
        }

        Object[] options = {"Done"};
        JOptionPane.showOptionDialog(null,
                getInterface(),
                "Edit tags for " + neurons.size() + " neurons",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                options,
                options[0]
        );
    }

    private void requestAddTag(String tag) {
        // dialog: are you sure
        int ans = JOptionPane.showConfirmDialog(
                null,
                String.format("You are about to add the tag '%s' to %d neurons. This action cannot be undone! Continue?",
                        tag, listProvider.getNeuronList().size()),
                "Add tags?",
                JOptionPane.OK_CANCEL_OPTION
                );
        if (ans == JOptionPane.OK_OPTION) {
            addTag(tag);
        }
    }

    private void addTag(final String tag) {
        final List<TmNeuron> neuronList = listProvider.getNeuronList();
        SimpleWorker adder = new SimpleWorker() {
            @Override
            protected void doStuff() throws Exception {
                // Stopwatch stopwatch = new Stopwatch();
                // stopwatch.start();
                annModel.addNeuronTag(tag, neuronList);
                // System.out.println("added tag to " + neuronList.size() + " neurons in " + stopwatch);
                // stopwatch.stop();
            }

            @Override
            protected void hadSuccess() {
                // nothing to do here
            }

            @Override
            protected void hadError(Throwable error) {
                logger.error("error adding tag " + tag + " to multiple neurons");
                showError("There was an error adding the tag!", "Error");
            }
        };
        adder.execute();
    }

    private void requestClearTag(String tag) {
        // dialog: are you sure
        int ans = JOptionPane.showConfirmDialog(
                null,
                String.format("You are about to remove the tag '%s' from the visible neurons that have it. This action cannot be undone! Continue?", tag),
                "Clear tags?",
                JOptionPane.OK_CANCEL_OPTION
        );
        if (ans == JOptionPane.OK_OPTION) {
            clearTag(tag);
        }
    }

    private void clearTag(final String tag) {
        final List<TmNeuron> neuronList = new ArrayList<>();
        Set<Long> taggedNeurons = annModel.getNeuronIDsForTag(tag);
        for (TmNeuron neuron: listProvider.getNeuronList()) {
            if (taggedNeurons.contains(neuron.getId())) {
                neuronList.add(neuron);
            }
        }

        SimpleWorker remover = new SimpleWorker() {
            @Override
            protected void doStuff() throws Exception {
                // Stopwatch stopwatch = new Stopwatch();
                // stopwatch.start();
                annModel.removeNeuronTag(tag, neuronList);
                // System.out.println("removed tag from " + neuronList.size() + " neurons in " + stopwatch);
                // stopwatch.stop();
            }

            @Override
            protected void hadSuccess() {
                // nothing
            }

            @Override
            protected void hadError(Throwable error) {
                logger.error("error removing tag " + tag + " from multiple neurons");
                showError("There was an error removing the tags!", "Error");
            }
        };
        remover.execute();
    }

    private void showError(String message, String title) {
        JOptionPane.showMessageDialog(
                mainPanel,
                message,
                title,
                JOptionPane.ERROR_MESSAGE);
    }

    private JPanel getInterface() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));

        // add existing tag
        JPanel existingPanel = new JPanel();
        existingPanel.setLayout(new BoxLayout(existingPanel, BoxLayout.LINE_AXIS));

        JButton existingButton = new JButton("Add");
        existingButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                requestAddTag((String) existingTagMenu.getSelectedItem());
            }
        });
        existingPanel.add(existingButton);
        existingPanel.add(new JLabel("existing tag "));

        String[] existingTags = annModel.getAllNeuronTags().toArray(new String[annModel.getAllNeuronTags().size()]);
        Arrays.sort(existingTags);
        existingTagMenu = new JComboBox(existingTags);
        existingPanel.add(existingTagMenu);
        existingPanel.add(Box.createHorizontalGlue());
        mainPanel.add(existingPanel);


        // remove existing tag
        JPanel removePanel = new JPanel();
        removePanel.setLayout(new BoxLayout(removePanel, BoxLayout.LINE_AXIS));

        JButton removeButton = new JButton("Remove");
        removeButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                requestClearTag((String) removeTagMenu.getSelectedItem());
            }
        });
        removePanel.add(removeButton);
        removePanel.add(new JLabel("existing tag "));
        removeTagMenu = new JComboBox(existingTags);
        removePanel.add(removeTagMenu);
        removePanel.add(Box.createHorizontalGlue());
        mainPanel.add(removePanel);


        // add new tag
        JPanel newTagPanel = new JPanel();
        newTagPanel.setLayout(new BoxLayout(newTagPanel, BoxLayout.LINE_AXIS));

        JButton newTagButton = new JButton("Add");
        newTagButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (newTagField.getText().length() > 0) {
                    requestAddTag(newTagField.getText());
                }
            }
        });
        newTagPanel.add(newTagButton);
        newTagPanel.add(new JLabel("new tag "));
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
