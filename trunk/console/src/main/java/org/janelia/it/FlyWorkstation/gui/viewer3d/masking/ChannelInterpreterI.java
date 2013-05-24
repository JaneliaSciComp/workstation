package org.janelia.it.FlyWorkstation.gui.viewer3d.masking;

import org.janelia.it.FlyWorkstation.gui.viewer3d.loader.ChannelMetaData;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 4/10/13
 * Time: 4:17 PM
 *
 * Implement this to help figure out what to do, as an acceptor of channel bytes read from input files.
 */
public interface ChannelInterpreterI {
    void interpretChannelBytes(ChannelMetaData srcChannelMetaData, ChannelMetaData targetChannelMetaData, byte[] channelData, int targetPos);
    void close();
}
