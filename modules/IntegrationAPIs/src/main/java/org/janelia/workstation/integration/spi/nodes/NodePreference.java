package org.janelia.workstation.integration.spi.nodes;

/**
 * Allows user to control whether or node is visible in the Data Explorer by providing an interface to an
 * underlying preference mechanism.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface NodePreference {

    String getNodeName();

    boolean isNodeShown();

    void setNodeShown(boolean value);
}
