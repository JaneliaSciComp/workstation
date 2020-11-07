package org.janelia.workstation.controller.listener;

/**
 * Implement this to hear about changes to the color-model.  Not to be confused
 * with changes to color itself.
 *
 * @author fosterl
 */
public interface ColorModelListener {
    void colorModelChanged();
}
