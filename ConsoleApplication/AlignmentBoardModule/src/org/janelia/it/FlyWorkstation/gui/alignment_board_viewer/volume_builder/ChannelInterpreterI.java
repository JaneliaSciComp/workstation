package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.volume_builder;

import org.janelia.it.FlyWorkstation.gui.alignment_board.loader.ChannelMetaData;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 4/10/13
 * Time: 4:17 PM
 *
 * Implement this to help figure out what to do, as an acceptor of channel bytes read from input files.
 */
public interface ChannelInterpreterI {
    void interpretChannelBytes(ChannelMetaData srcChannelMetaData, ChannelMetaData targetChannelMetaData, int fileMaskNum, byte[] channelData, long targetPos);
    void close();
}
