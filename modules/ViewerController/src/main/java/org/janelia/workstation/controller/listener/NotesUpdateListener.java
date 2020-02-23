package org.janelia.workstation.controller.listener;

import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;

/**
 * When notes are changed, implement this to hear about it.
 * 
 * @author fosterl
 */
public interface NotesUpdateListener {
    void notesUpdated(TmGeoAnnotation ann);
}
