/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.passive_3d;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import org.janelia.it.workstation.geom.CoordinateAxis;
import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.camera.ObservableCamera3d;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.large_volume_viewer.ImageColorModel;
import org.janelia.it.workstation.gui.large_volume_viewer.SubvolumeProvider;
import org.janelia.it.workstation.shared.workers.IndeterminateNoteProgressMonitor;

/**
 * Sources menu items for presenting the user with their 3D viewer.
 * 
 * @author fosterl
 */
public class Snapshot3DLauncher {
    private CoordinateAxis sliceAxis;
    private SubvolumeProvider subvolumeProvider;
    private ObservableCamera3d camera;
    private URL dataUrl;
    private String basePath;
    private ImageColorModel imageColorModel;
    
    public Snapshot3DLauncher(
            CoordinateAxis sliceAxis,
            ObservableCamera3d camera,
            SubvolumeProvider subvolumeProvider,
            String basePath,
            URL dataUrl,
            ImageColorModel imageColorModel
    ) {
        this.sliceAxis = sliceAxis;
        this.subvolumeProvider = subvolumeProvider;
        this.imageColorModel = imageColorModel;
        this.basePath = basePath;
        this.dataUrl = dataUrl;
        this.camera = camera;
    }
    
    public List<JMenuItem> getSnapshotMenuItems() {
        JMenu snapShot3dSubMenu = new JMenu("3D Snapshot");

        List<JMenuItem> rtnVal = new ArrayList<>();
        int[] extents = new int[] {
            64, 128, 512
        };

        for (final int extent : extents) {
            JMenuItem item = new JMenuItem( extent + " cubed" );
            item.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    launch3dViewer( extent );
                }

            });
            snapShot3dSubMenu.add( item );
        }
        JMenuItem item = new JMenuItem( "Raw" );
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                launchRaw3dViewer();
            }
        });
        snapShot3dSubMenu.add( item );
        
        rtnVal.add( snapShot3dSubMenu );

        return rtnVal;
    }    

    /** Launches a 3D popup containing raw data represented by camera position. */
    public void launchRaw3dViewer() {
        try {            
            MonitoredVolumeSource collector = new RawTiffVolumeSource( camera, basePath );
            
            Snapshot3d snapshotViewer = Snapshot3d.getInstance();
            IndeterminateNoteProgressMonitor monitor = 
                    new IndeterminateNoteProgressMonitor(SessionMgr.getMainFrame(), "Fetching raw data", collector.getInfo());
            snapshotViewer.setLoadProgressMonitor( monitor );
            snapshotViewer.setImageColorModel( imageColorModel );
            snapshotViewer.setLabelText( labelTextFor3d() );
            snapshotViewer.launch( collector );

        } catch ( Exception ex ) {
            System.err.println("Failed to launch viewer: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    /** Launches a 3D popup static-block viewer. */
    public void launch3dViewer( int cubicDimension ) {
        try {            
            MonitoredVolumeSource collector = new ViewTileManagerVolumeSource(
                    camera,
                    sliceAxis,
                    cubicDimension,
                    subvolumeProvider,
                    dataUrl
            );            

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

    private String labelTextFor3d() {
        final Vec3 focus = camera.getFocus();
        String LABEL3d_FORMAT = "Centered at [%3.1f,%3.1f,%3.1f].  Raw Data.";
        return String.format( LABEL3d_FORMAT, focus.getX(), focus.getY(), focus.getZ() );
    }

}
