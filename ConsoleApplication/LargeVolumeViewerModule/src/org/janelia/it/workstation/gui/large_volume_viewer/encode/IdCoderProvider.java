package org.janelia.it.workstation.gui.large_volume_viewer.encode;

/**
 * Lazily-retrievable source for an id coder.
 *
 * @author fosterl
 */
public interface IdCoderProvider {
    IdCoder getIdCoder();
}
