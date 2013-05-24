package org.janelia.it.FlyWorkstation.gui.framework.viewer;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 1/4/13
 * Time: 9:19 AM
 *
 * A swing worker to load the volume from the filename, in background.
 */
import org.janelia.it.FlyWorkstation.gui.viewer3d.Mip3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.resolver.CacheFileResolver;

import javax.swing.*;

public class Load3dSwingWorker extends SwingWorker<Boolean,Boolean> {
    private Mip3d mip3d;
    private String filename;
    public Load3dSwingWorker( Mip3d mip3d, String filename ) {
        this.mip3d = mip3d;
        this.filename = filename;
    }

    /**
     * This "background-thread" method of the worker will return false if the entity has NOT been properly
     * shown, indicating it is still 'dirty' (as in not fully interpreted as current).
     *
     * @return False to indicate that the entity has been processed and made current.
     * @throws Exception
     */
    @Override
    protected Boolean doInBackground() throws Exception {
        return false; // Not dirty
    }

    /** This is done in the event thread. */
    @Override
    protected void done() {
        if ( filename != null ) {
            mip3d.clear();
            mip3d.loadVolume(filename, new CacheFileResolver());
            filenameSufficient();
        }
        else {
            filenameUnavailable();
        }
    }

    public void filenameSufficient() {}
    public void filenameUnavailable() {}
}

