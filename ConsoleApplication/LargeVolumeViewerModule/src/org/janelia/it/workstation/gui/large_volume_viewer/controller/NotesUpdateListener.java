package org.janelia.it.workstation.gui.large_volume_viewer.controller;

import org.janelia.it.jacs.model.domain.tiledMicroscope.TmWorkspace;

/**
 * When notes are changed, implement this to hear about it.
 * 
 * @author fosterl
 */
public interface NotesUpdateListener {
    void notesUpdated(TmWorkspace workspace);
}
