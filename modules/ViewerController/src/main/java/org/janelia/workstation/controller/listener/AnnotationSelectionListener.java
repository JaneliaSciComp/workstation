package org.janelia.workstation.controller.listener;

/**
 * Implement this to hear about anno-select events, such as in response to
 * database changes.
 * 
 * @author fosterl
 */
public interface AnnotationSelectionListener {
    void annotationSelected(Long id);
}
