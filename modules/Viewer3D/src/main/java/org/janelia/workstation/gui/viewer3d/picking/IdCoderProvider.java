package org.janelia.workstation.gui.viewer3d.picking;

/**
 * Lazily-retrievable source for an id coder.
 *
 * @author fosterl
 */
public interface IdCoderProvider {
    IdCoder getIdCoder();
}
