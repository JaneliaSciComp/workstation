package org.janelia.workstation.controller.listener;

import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;

/**
 * Implement this to hear about note-edit requests.
 * 
 * @author fosterl
 */
public interface EditNoteRequestedListener {
    void editNote(TmGeoAnnotation annotation);
}
