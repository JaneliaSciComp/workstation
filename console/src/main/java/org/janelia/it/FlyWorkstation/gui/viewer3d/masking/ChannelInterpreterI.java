package org.janelia.it.FlyWorkstation.gui.viewer3d.masking;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 4/10/13
 * Time: 4:17 PM
 *
 * Implement this to help figure out what to do, as an acceptor of channel bytes read from input files.
 */
public interface ChannelInterpreterI {
    void interpretChannelBytes(byte[] channelData, int targetPos);
    void close();
}
