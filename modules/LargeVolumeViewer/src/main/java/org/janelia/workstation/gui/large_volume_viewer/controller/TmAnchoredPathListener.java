package org.janelia.workstation.gui.large_volume_viewer.controller;

import java.util.List;

import org.janelia.model.domain.tiledMicroscope.TmAnchoredPath;

/**
 * Implement this to hear about changes to anchored paths at the model level.
 * @author fosterl
 */
public interface TmAnchoredPathListener {
    void addAnchoredPath(Long neuronID, TmAnchoredPath path);
    void removeAnchoredPaths(Long neuronID, List<TmAnchoredPath> paths);
    void removeAnchoredPathsByNeuronID(Long neuronID);
}
