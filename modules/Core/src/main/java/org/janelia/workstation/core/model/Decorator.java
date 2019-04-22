package org.janelia.workstation.core.model;

import javax.swing.ImageIcon;

/**
 * A named icon decorator.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface Decorator {

    String getLabel();

    ImageIcon getIcon();
}
