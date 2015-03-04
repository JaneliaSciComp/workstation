/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.large_volume_viewer.controller;

import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmGeoAnnotation;

/**
 * Implement this to hear about note-edit requests.
 * 
 * @author fosterl
 */
public interface EditNoteRequestedListener {
    void editNote(TmGeoAnnotation annotation);
}
