package org.janelia.workstation.gui.large_volume_viewer.controller;

/**
 * Implement this to hear about anno-select events, such as in response to
 * database changes.
 * 
 * @author fosterl
 */
public interface AnnotationSelectionListener {
    void annotationSelected(Long id);
}
