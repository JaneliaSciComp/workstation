package org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board;

import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureDataI;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 4/13/13
 * Time: 2:49 PM
 *
 * A controller for an alignment board viewer may target this interface.  Its purpose is to make
 * an abstraction for such callbacks, and allow for broader applicability, specifically testing.
 */
public interface AlignmentBoardControllable {

    void clearDisplay();

    /**
     * Callback from loader threads to control loading information.
     *
     * @param signalTexture for the signal
     * @param maskTexture for the mask
     */
    void loadVolume( TextureDataI signalTexture, TextureDataI maskTexture );

    void dataLoadComplete();

    void loadCompletion( boolean successful, boolean loadFiles, Throwable error );

}
