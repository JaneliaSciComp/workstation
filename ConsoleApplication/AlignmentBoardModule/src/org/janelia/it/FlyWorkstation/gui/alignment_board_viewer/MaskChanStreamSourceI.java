package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 10/8/13
 * Time: 2:04 PM
 * To change this template use File | Settings | File Templates.
 */
public interface MaskChanStreamSourceI {
    InputStream getMaskInputStream() throws IOException;

    InputStream getChannelInputStream() throws IOException;
}
