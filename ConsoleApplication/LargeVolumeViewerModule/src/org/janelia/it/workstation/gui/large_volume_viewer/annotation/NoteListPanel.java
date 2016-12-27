package org.janelia.it.workstation.gui.large_volume_viewer.annotation;

import com.fasterxml.jackson.databind.JsonNode;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmGeoAnnotation;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmNeuron;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmStructuredTextAnnotation;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmWorkspace;
import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.CameraPanToListener;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.EditNoteRequestedListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * this class displays notes placed on annotations in a clickable list
 *
 * djo, 10/14
 */
public class NoteListPanel extends JPanel {

    private TmWorkspace workspace;

    private DefaultListModel<NoteProxy> noteListModel;
    private CameraPanToListener panListener;
    private EditNoteRequestedListener editNoteRequestListener;

    private int width;
    private static final int height = AnnotationPanel.SUBPANEL_STD_HEIGHT;

    public NoteListPanel(int width) {
        this.width = width;
        setupUI();
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(width, height);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }

    /**
     * Method to set the panListener
     */
    public void setPanListener(CameraPanToListener panListener) {
        this.panListener = panListener;
    }

    private void setupUI() {

        setLayout(new GridBagLayout());

        // list of neurons
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.anchor = GridBagConstraints.PAGE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(10, 0, 0, 0);
        add(new JLabel("Notes", JLabel.LEADING), c);

        noteListModel = new DefaultListModel<>();
        JList noteListBox = new JList(noteListModel) {
            public String getToolTipText(MouseEvent event) {
                int location = locationToIndex(event.getPoint());
                if (location >= 0) {
                    return getModel().getElementAt(location).toString();
                } else {
                    return null;
                }
            }
        };

        JScrollPane noteScrollPane = new JScrollPane(noteListBox);
        noteListBox.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // listen to clicks, not selection changes!
        // single-click = go to
        // double-click = edit/delete dialog
        noteListBox.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                JList list = (JList) evt.getSource();
                int index = list.locationToIndex(evt.getPoint());
                NoteProxy selectedNote;
                if (index >= 0) {
                    selectedNote = noteListModel.getElementAt(index);
                    TmNeuron foundNeuron = null;
                    for (TmNeuron neuron : workspace.getNeuronList()) {
                        if (neuron.getId().equals(selectedNote.neuronID)) {
                            foundNeuron = neuron;
                        }
                    }
                    TmGeoAnnotation ann = null;
                    if (foundNeuron != null) {
                        ann = foundNeuron.getGeoAnnotationMap().get(selectedNote.parentID);
                    }
                    if (evt.getClickCount() == 1) {
                        if (ann != null) {
                            if (panListener != null) {
                                panListener.cameraPanTo(new Vec3(ann.getX(), ann.getY(), ann.getZ()));
                            }
                        }
                    } else if (evt.getClickCount() == 2) {
                        if (ann != null) {
                            if (editNoteRequestListener != null) {
                                editNoteRequestListener.editNote(ann);
                            }
                        }
                    }
                }
            }
        });

        GridBagConstraints c2 = new GridBagConstraints();
        c2.gridx = 0;
        c2.gridy = GridBagConstraints.RELATIVE;
        c2.weighty = 1.0;
        c2.anchor = GridBagConstraints.PAGE_START;
        c2.fill = GridBagConstraints.BOTH;
        add(noteScrollPane, c2);

        loadWorkspace(null);

    }

    public void loadWorkspace(TmWorkspace workspace) {

        this.workspace = workspace;
        noteListModel.clear();
        if (workspace != null) {
            // repopulate the notes list, brute force
            // unsorted; if you want to sort, add note proxies to a list, sort the list,
            // then add from the list
            ArrayList<NoteProxy> noteList = new ArrayList<>();
            for (TmNeuron neuron: workspace.getNeuronList()) {
                for (TmStructuredTextAnnotation note: neuron.getStructuredTextAnnotationMap().values()) {
                    JsonNode noteNode = note.getData().path("note");
                    if (!noteNode.isMissingNode()) {
                        noteList.add(new NoteProxy(neuron.getId(), note.getParentId(), noteNode.asText()));
                        //noteListModel.addElement(new NoteProxy(neuron.getId(), note.getParentId(), noteNode.asText()));
                    }
                }
            }
            // sort, then insert into model; alphabetic for now
            Collections.sort(noteList, new Comparator<NoteProxy>() {
                @Override
                public int compare(NoteProxy np1, NoteProxy np2) {
                    return np1.note.compareToIgnoreCase(np2.note);
                }
            });
            for (NoteProxy np: noteList) {
                noteListModel.addElement(np);
            }
        }
    }

    /**
     * @param editNoteRequestListener the editNoteRequestListener to set
     */
    public void setEditNoteRequestListener(EditNoteRequestedListener editNoteRequestListener) {
        this.editNoteRequestListener = editNoteRequestListener;
    }

}

/**
 * this class holds just the "note" part of the structured annotation,
 * which is the part we intend to display
 */
class NoteProxy {
    Long neuronID;
    Long parentID;
    String note;

    NoteProxy(Long neuronID, Long parentID, String note) {
        this.neuronID = neuronID;
        this.parentID = parentID;
        this.note = note;
    }

    @Override
    public String toString() {
        return note;
    }
}