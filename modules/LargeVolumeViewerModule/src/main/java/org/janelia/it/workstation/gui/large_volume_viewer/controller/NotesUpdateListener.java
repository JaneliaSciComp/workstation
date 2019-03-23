package org.janelia.it.workstation.gui.large_volume_viewer.controller;

import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;

/**
 * When notes are changed, implement this to hear about it.
 * 
 * @author fosterl
 */
public interface NotesUpdateListener {
    void notesUpdated(TmGeoAnnotation ann);
}
