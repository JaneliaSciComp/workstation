package org.janelia.it.workstation.gui.large_volume_viewer.annotation;

import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmGeoAnnotation;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmNeuron;

import java.util.ArrayList;
import java.util.List;

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

    // end of branch that can't be traced further but
    //  is clearly not complete (biologically)
    PROBLEM_END     ("problem end")
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

    PredefinedNote(String noteText) {
        this.noteText = noteText;
    }

    /**
     * this is where it gets interesting; not all notes can be applied
     * to every annotation; we don't allow "traced end" to be applied
     * to an annotation that's not an end, for example
     */
    public boolean isValid(TmNeuron neuron, Long annotationID) {
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
        }
        return true;
    }

    public String getNoteText() {
        return noteText;
    }
}
