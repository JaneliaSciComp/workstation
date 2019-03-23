/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.large_volume_viewer.controller;

/**
 * Implement this to hear about anno-select events, such as in response to
 * database changes.
 * 
 * @author fosterl
 */
public interface AnnotationSelectionListener {
    void annotationSelected(Long id);
}
