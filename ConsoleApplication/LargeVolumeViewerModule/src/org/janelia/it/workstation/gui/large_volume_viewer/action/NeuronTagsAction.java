package org.janelia.it.workstation.gui.large_volume_viewer.action;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmNeuron;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationModel;
import org.jdesktop.swingx.HorizontalLayout;

/**
 * this action opens a dialog that allows the user to edit
 * the tags on a neuron
 */
public class NeuronTagsAction extends AbstractAction {

    private AnnotationModel annModel;
    private TmNeuron currentNeuron;

    private JList appliedTagsList;
    private JList availableTagsList;
    private JTextField newTagField;

    public NeuronTagsAction(AnnotationModel annotationModel) {
        this.annModel = annotationModel;

        putValue(NAME, "Edit tags...");
        putValue(SHORT_DESCRIPTION, "Edit tags for neuron");
    }

    @Override
    public void actionPerformed(ActionEvent action) {

        // check for selected neuron
        currentNeuron = annModel.getCurrentNeuron();
        if (currentNeuron == null) {
            return;
        }

        Object[] options = {"Close"};
        JOptionPane.showOptionDialog(null,
            getInterface(),
            "Edit tags for " + currentNeuron.getName(),
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.PLAIN_MESSAGE,
            null,
            options,
            options[0]
            );
    }

    /**
     * add given tag to current neuron
     */
    private void addTag(String tag) {
        // if you change the tag map, you have to save the tag map
        // not entirely happy about how it's structured like that...but I
        //  decided I didn't want to delegate all its methods to AnnModel
        //  and handle the saves there, so for now, stuck with it
        annModel.getCurrentTagMap().addTag(tag, annModel.getCurrentNeuron());
        annModel.saveNeuronTagMap();
        fillTagLists();
    }

    /**
     * remove given tag from current neuron
     */
    private void removeTag(String tag) {
        annModel.getCurrentTagMap().removeTag(tag, annModel.getCurrentNeuron());
        annModel.saveNeuronTagMap();
        fillTagLists();
    }

    /**
     * create a new tag and apply it to the current neuron
     */
    private void onAddNewTag() {
        addTag(newTagField.getText());
    }

    /**
     * clear all the tags from the currently selected neuron
     */
    private void onClearAllButton() {


        System.out.println("Clear all button clicked");



    }

    /**
     * populate both of the tag lists in the UI
     */
    private void fillTagLists() {


        System.out.println("in fillTagLists");


        // note: I've learned that people like things to be in order;
        //  so even though tags are inherently unordered, alphabetize
        //  them in the lists

        // OMG Java makes everything hard...JList can't take List<>, only
        //  arrays and vectors
        Set<String> appliedTagSet = annModel.getCurrentTagMap().getTags(currentNeuron);
        String[] appliedTags = appliedTagSet.toArray(new String[appliedTagSet.size()]);
        Arrays.sort(appliedTags);
        appliedTagsList.setListData(appliedTags);


        // available tag list; be careful, we're mutating:
        Set<String> availableTagSet = new HashSet<>(annModel.getCurrentTagMap().getAllTags());
        availableTagSet.removeAll(appliedTagSet);
        String [] availableTags = availableTagSet.toArray(new String[availableTagSet.size()]);
        Arrays.sort(availableTags);
        availableTagsList.setListData(availableTags);


    }

    private JPanel getInterface() {

        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        // two columns; applied tags on the left, other available
        //  tags on the right

        // I hate GridBagLayout's verbosity, but it's the only way
        //  in Java to get the control over the UI that I want

        // left column = applied tags
        GridBagConstraints cTopLeft = new GridBagConstraints();
        cTopLeft.gridx = 0;
        cTopLeft.gridy = 0;
        cTopLeft.anchor = GridBagConstraints.PAGE_START;
        cTopLeft.fill = GridBagConstraints.HORIZONTAL;
        cTopLeft.weighty = 0.0;
        JLabel appliedLabel = new JLabel("Applied");
        appliedLabel.setHorizontalAlignment(JLabel.CENTER);
        panel.add(appliedLabel, cTopLeft);

        GridBagConstraints cFlowLeft = new GridBagConstraints();
        cFlowLeft.gridx = 0;
        cFlowLeft.gridy = GridBagConstraints.RELATIVE;
        cFlowLeft.anchor = GridBagConstraints.PAGE_START;
        cFlowLeft.fill = GridBagConstraints.HORIZONTAL;
        cFlowLeft.weighty = 0.0;
        JLabel appliedLabel2 = new JLabel("(click to remove)");
        appliedLabel2.setHorizontalAlignment(JLabel.CENTER);
        panel.add(appliedLabel2, cFlowLeft);

        // the dialog isn't currently resizeable, but I want
        //  to be ready in case I make it so
        GridBagConstraints cGrowLeft = new GridBagConstraints();
        cGrowLeft.gridx = 0;
        cGrowLeft.gridy = GridBagConstraints.RELATIVE;
        cGrowLeft.anchor = GridBagConstraints.PAGE_START;
        cGrowLeft.fill = GridBagConstraints.HORIZONTAL;
        cGrowLeft.weighty = 1.0;
        appliedTagsList = new JList();
        JScrollPane appliedScroller = new JScrollPane(appliedTagsList);
        panel.add(appliedScroller, cGrowLeft);

        JButton clearAllButton = new JButton("Clear all");
        clearAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onClearAllButton();
            }
        });
        panel.add(clearAllButton, cFlowLeft);


        // right column = available tags
        GridBagConstraints cTopRight = new GridBagConstraints();
        cTopRight.gridx = 1;
        cTopRight.gridy = 0;
        cTopRight.anchor = GridBagConstraints.PAGE_START;
        cTopRight.fill = GridBagConstraints.HORIZONTAL;
        cTopRight.weighty = 0.0;
        JLabel availableLabel = new JLabel("Available");
        availableLabel.setHorizontalAlignment(JLabel.CENTER);
        panel.add(availableLabel, cTopRight);

        GridBagConstraints cFlowRight = new GridBagConstraints();
        cFlowRight.gridx = 1;
        cFlowRight.gridy = GridBagConstraints.RELATIVE;
        cFlowRight.anchor = GridBagConstraints.PAGE_START;
        cFlowRight.fill = GridBagConstraints.HORIZONTAL;
        cFlowRight.weighty = 0.0;
        JLabel availableLabel2 = new JLabel("(click to add)");
        availableLabel2.setHorizontalAlignment(JLabel.CENTER);
        panel.add(availableLabel2, cFlowRight);

        GridBagConstraints cGrowRight = new GridBagConstraints();
        cGrowRight.gridx = 1;
        cGrowRight.gridy = GridBagConstraints.RELATIVE;
        cGrowRight.anchor = GridBagConstraints.PAGE_START;
        cGrowRight.fill = GridBagConstraints.HORIZONTAL;
        cGrowRight.weighty = 1.0;
        availableTagsList = new JList();
        JScrollPane availableScroller = new JScrollPane(availableTagsList);
        panel.add(availableScroller, cFlowRight);

        JPanel newTagPanel = new JPanel();
        newTagPanel.setLayout(new HorizontalLayout());
        newTagPanel.add(new JLabel("New tag:"));
        newTagField = new JTextField("", 10);
        newTagPanel.add(newTagField);
        JButton newTagButton = new JButton("Add");
        newTagButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onAddNewTag();
            }
        });
        newTagPanel.add(newTagButton);
        panel.add(newTagPanel, cFlowRight);

        // applied tags list behavior
        appliedTagsList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    int index = appliedTagsList.locationToIndex(e.getPoint());
                    removeTag((String) appliedTagsList.getModel().getElementAt(index));
                }
            }
        });

        // available tags list behavior
        availableTagsList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    int index = availableTagsList.locationToIndex(e.getPoint());
                    addTag((String) availableTagsList.getModel().getElementAt(index));
                }
            }
        });

        fillTagLists();

        return panel;
    }

}
