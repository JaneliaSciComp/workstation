package org.janelia.it.workstation.gui.large_volume_viewer.controller;

import java.util.List;

import org.janelia.it.jacs.model.domain.tiledMicroscope.TmAnchoredPath;

/**
 * Implement this to hear about changes to anchored paths at the model level.
 * @author fosterl
 */
public interface TmAnchoredPathListener {
    void addAnchoredPath(TmAnchoredPath path);
    void removeAnchoredPaths(List<TmAnchoredPath> paths);
}
