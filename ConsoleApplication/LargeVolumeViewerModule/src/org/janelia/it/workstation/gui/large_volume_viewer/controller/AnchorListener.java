/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.large_volume_viewer.controller;

import org.janelia.it.workstation.gui.large_volume_viewer.skeleton.Anchor;

/**
 * Implement this to pickup changes to a skeleton, or other events related to
 * anchors.
 *
 * @author fosterl
 */
public interface AnchorListener {
    void deleteSubtreeRequested(Anchor anchor);
    void splitAnchorRequested(Anchor anchor);
    void rerootNeuriteRequested(Anchor anchor);
    void splitNeuriteRequested(Anchor anchor);
    void deleteLinkRequested(Anchor anchor);
    void addEditNoteRequested(Anchor anchor);

}
