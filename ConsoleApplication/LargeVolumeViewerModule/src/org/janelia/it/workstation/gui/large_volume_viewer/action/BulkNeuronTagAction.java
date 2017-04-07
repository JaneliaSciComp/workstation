package org.janelia.it.workstation.gui.large_volume_viewer.action;

import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.janelia.it.jacs.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationManager;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.NeuronListProvider;
import org.janelia.it.workstation.gui.large_volume_viewer.top_component.LargeVolumeViewerTopComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;

/**
 * this action opens a dialog that allows the user to add or remove
 * a neuron tag to all the neurons that are visible in
 * the neuron list
 */
public class BulkNeuronTagAction extends AbstractAction {

    private static final Logger logger = LoggerFactory.getLogger(BulkNeuronTagAction.class);

    private NeuronListProvider listProvider;

    private JPanel mainPanel;
    private JComboBox<String> existingTagMenu;
    private JComboBox<String> removeTagMenu;
    private JTextField newTagField;


    public BulkNeuronTagAction(NeuronListProvider listProvider) {
        this.listProvider = listProvider;

        putValue(NAME, "Edit neuron tags...");
        putValue(SHORT_DESCRIPTION, "Edit neuron tag for all visible neurons");
    }


    @Override
    public void actionPerformed(ActionEvent action) {
        List<TmNeuronMetadata> neurons = listProvider.getNeuronList();
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
        if (tag==null) return;
        final List<TmNeuronMetadata> neuronList = listProvider.getNeuronList();
        if (neuronList.isEmpty()) return;
        SimpleWorker adder = new SimpleWorker() {
            @Override
            protected void doStuff() throws Exception {
                Stopwatch stopwatch = new Stopwatch();
                stopwatch.start();
                AnnotationManager annotationMgr = LargeVolumeViewerTopComponent.getInstance().getAnnotationMgr();
                annotationMgr.addNeuronTag(tag, neuronList);
                logger.info("added tag to " + neuronList.size() + " neurons in " + stopwatch);
                stopwatch.stop();
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
        if (tag==null) return;
        final List<TmNeuronMetadata> neuronList = listProvider.getNeuronList();
        if (neuronList.isEmpty()) return;
        SimpleWorker remover = new SimpleWorker() {
            @Override
            protected void doStuff() throws Exception {
                Stopwatch stopwatch = new Stopwatch();
                stopwatch.start();
                AnnotationManager annotationMgr = LargeVolumeViewerTopComponent.getInstance().getAnnotationMgr();
                annotationMgr.removeNeuronTag(tag, neuronList);
                logger.info("removed tag from " + neuronList.size() + " neurons in " + stopwatch);
                stopwatch.stop();
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

        AnnotationManager annotationMgr = LargeVolumeViewerTopComponent.getInstance().getAnnotationMgr();
        Set<String> availableNeuronTags = annotationMgr.getAvailableNeuronTags();
        String[] existingTags = availableNeuronTags.toArray(new String[availableNeuronTags.size()]);
        Arrays.sort(existingTags);
        existingTagMenu = new JComboBox<String>(existingTags);
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
        removeTagMenu = new JComboBox<String>(existingTags);
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
