package org.janelia.console.viewerapi;

/**
 *
 * @author brunsc
 */
public interface Command {
    // returns false if command execution fails
    boolean execute();
}
