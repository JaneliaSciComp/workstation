package org.janelia.workstation.controller.listener;

import java.awt.Color;

/**
 * Implement this to hear about changes of color.
 *
 * @author fosterl
 */
public interface ColorListener {
    void color(Color color);
}
