package org.janelia.workstation.controller.model.annotations.neuron;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;

/**
 * this enum defines the predefined notes that a user can add to
 * our geometric annotations
 *
 * djo, 4/15
 */
public enum PredefinedNote {
    // keep these in the desired button display order!

    // end of branch that has been fully traced
    TRACED_END      ("traced end"),

    // point at which a branch should be placed (but hasn't been)
    FUTURE_BRANCH   ("branch"),

    // interesting point to return to later
    POINT_OF_INTEREST ("interesting"),

    // something that needs review
    REVIEW          ("review"),

    // end of branch that can't be traced further but
    //  is clearly not complete (biologically)
    PROBLEM_END     ("problem end"),

    // requested for new workflow in second half 2019
    UNIQUE_1        ("unique 1"),
    UNIQUE_2        ("unique 2"),
    // deprecated, replaced by unique 1 and 2:
    UNIQUE          ("unique")
    ;

    private final String noteText;

    /**
     * given a string, identify any PredefinedNotes whose text
     * is included in the string; returns a list of such notes
     */
    public static List<PredefinedNote> findNotes(String noteText) {
        List<PredefinedNote> foundNotes = new ArrayList<>();
        for (PredefinedNote note: PredefinedNote.values()) {
            if (noteText.contains(note.getNoteText())) {
                foundNotes.add(note);
            }
        }
        return foundNotes;
    }

    public static List<PredefinedNote> getButtonList() {
        // return a list of predefined notes that should have dedicated buttons
        //  in the add/edit note dialog box; for example, there's a deprecated note now
        // the order of this list is the order in which the buttons will appear
        List<PredefinedNote> buttons = Arrays.asList(
                TRACED_END,
                FUTURE_BRANCH,
                POINT_OF_INTEREST,
                REVIEW,
                PROBLEM_END,
                UNIQUE_1,
                UNIQUE_2
                );
        return buttons;
    }

    PredefinedNote(String noteText) {
        this.noteText = noteText;
    }

    /**
     * this is where it gets interesting; not all notes can be applied
     * to every annotation; we don't allow "traced end" to be applied
     * to an annotation that's not an end, for example
     */
    public boolean isValid(TmNeuronMetadata neuron, Long annotationID) {
        TmGeoAnnotation ann = neuron.getGeoAnnotationMap().get(annotationID);
        switch (this) {
            // these guys need to be actual ends:
            case TRACED_END:
            case PROBLEM_END:
                return !(ann.getChildIds().size() > 0);

            // future branches can't already be branches:
            case FUTURE_BRANCH:
                return !(ann.getChildIds().size() > 1);

            default:
                // everybody else is OK
                return true;
        }
    }

    public String getNoteText() {
        return noteText;
    }
}
