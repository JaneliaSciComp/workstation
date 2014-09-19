/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.passive_3d;

import java.net.URL;
import org.janelia.it.workstation.geom.CoordinateAxis;
import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.camera.ObservableCamera3d;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.large_volume_viewer.ImageColorModel;
import org.janelia.it.workstation.gui.large_volume_viewer.SubvolumeProvider;
import org.janelia.it.workstation.shared.workers.IndeterminateNoteProgressMonitor;

/**
 *
 * @author fosterl
 */
public class Snapshot3DLauncher {
    private CoordinateAxis sliceAxis;
    private SubvolumeProvider subvolumeProvider;
    private ObservableCamera3d camera;
    private URL dataUrl;
    private ImageColorModel imageColorModel;
    
    public Snapshot3DLauncher(
            CoordinateAxis sliceAxis,
            ObservableCamera3d camera,
            SubvolumeProvider subvolumeProvider,
            URL dataUrl,
            ImageColorModel imageColorModel
    ) {
        this.sliceAxis = sliceAxis;
        this.subvolumeProvider = subvolumeProvider;
        this.imageColorModel = imageColorModel;
        this.dataUrl = dataUrl;
        this.camera = camera;
    }
    
    /** Launches a 3D popup static-block viewer. */
    public void launch3dViewer( int cubicDimension ) {
        try {            
            ViewTileManagerVolumeSource collector = new ViewTileManagerVolumeSource(
                    camera,
                    sliceAxis,
                    cubicDimension,
                    subvolumeProvider
            );            
            collector.setDataUrl(dataUrl);

            Snapshot3d snapshotViewer = Snapshot3d.getInstance();
            IndeterminateNoteProgressMonitor monitor = 
                    new IndeterminateNoteProgressMonitor(SessionMgr.getMainFrame(), "Fetching tiles", collector.getInfo());
            snapshotViewer.setLoadProgressMonitor( monitor );
            snapshotViewer.setImageColorModel( imageColorModel );
            snapshotViewer.setLabelText( labelTextFor3d(cubicDimension) );
            snapshotViewer.launch( collector );

        } catch ( Exception ex ) {
            System.err.println("Failed to launch viewer: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private String labelTextFor3d(int cubicDimension) {
        final Vec3 focus = camera.getFocus();
        String LABEL3d_FORMAT = "Centered at [%3.1f,%3.1f,%3.1f].  %4$dx%4$dx%4$d.";
        return String.format( LABEL3d_FORMAT, focus.getX(), focus.getY(), focus.getZ(), cubicDimension );
    }

}
