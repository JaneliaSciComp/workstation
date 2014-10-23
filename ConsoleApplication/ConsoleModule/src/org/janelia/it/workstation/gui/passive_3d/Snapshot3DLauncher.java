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
import org.janelia.it.workstation.gui.passive_3d.top_component.Snapshot3dTopComponent;
import org.janelia.it.workstation.shared.workers.IndeterminateNoteProgressMonitor;
import org.openide.windows.WindowManager;

/**
 * Sources menu items for presenting the user with their 3D viewer.
 * 
 * @author fosterl
 */
public class Snapshot3DLauncher {
    private static final String RENDERED_VOLUME_TEXT_FORMAT = "Contains point [%3.1f,%3.1f,%3.1f].  %4$dx%4$dx%4$d.  Rendered Data.";
    private final static String RAW_VOLUME_TEXT_FORMAT = "Contains point [%3.1f,%3.1f,%3.1f].  %4$dx%4$dx%4$d.  Raw Data.";

    private CoordinateAxis sliceAxis;
    private SubvolumeProvider subvolumeProvider;
    private ObservableCamera3d camera;
    private URL dataUrl;
    private String basePath;
    private ImageColorModel imageColorModel;
    private Integer maxIntensity;
    private Integer numberOfChannels;
    
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
    
    /**
     * @return the maxIntensity
     */
    public Integer getMaxIntensity() {
        return maxIntensity;
    }

    /**
     * @param maxIntensity the maxIntensity to set
     */
    public void setMaxIntensity(Integer maxIntensity) {
        this.maxIntensity = maxIntensity;
    }

    /**
     * @return the numberOfChannels
     */
    public Integer getNumberOfChannels() {
        return numberOfChannels;
    }

    /**
     * @param numberOfChannels the numberOfChannels to set
     */
    public void setNumberOfChannels(Integer numberOfChannels) {
        this.numberOfChannels = numberOfChannels;
    }

    public List<JMenuItem> getSnapshotMenuItems() {
        JMenu snapShot3dSubMenu = new JMenu("3D Snapshot");

        List<JMenuItem> rtnVal = new ArrayList<>();

        int[] extents = new int[] {
            64, 128
        };
        for (final int extent : extents) {
            JMenuItem item = new JMenuItem("Raw sub-volume: " + extent + " cubic voxels");
            item.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    launchRaw3dViewer(extent);
                }
            });
            snapShot3dSubMenu.add(item);
        }

        extents = new int[] {
            64, 128, 512
        };
        for (final int extent : extents) {
            JMenuItem item = new JMenuItem( "Rendered sub-volume: " + extent + " cubed" );
            item.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    launch3dViewer( extent );
                }

            });
            snapShot3dSubMenu.add( item );
        }

        JMenuItem item = new JMenuItem( "Full raw" );
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                launchRaw3dViewer(-1);
            }
        });
        snapShot3dSubMenu.add( item );

        
        rtnVal.add( snapShot3dSubMenu );

        return rtnVal;
    }    

    /** Launches a 3D popup containing raw data represented by camera position. */
    public void launchRaw3dViewer( int cubicDimension ) {
        try {            
            RawTiffVolumeSource collector = new RawTiffVolumeSource( camera, basePath );
            if ( cubicDimension > -1 ) {
                collector.setCubicDimension( cubicDimension );
            }
            Snapshot3d snapshotViewer = Snapshot3d.getInstance();
            IndeterminateNoteProgressMonitor monitor = 
                    new IndeterminateNoteProgressMonitor(SessionMgr.getMainFrame(), "Fetching raw data", collector.getInfo());
            snapshotViewer.setLoadProgressMonitor( monitor );
            establishColorControls( snapshotViewer );
            snapshotViewer.setLabelText( labelTextForRaw3d( cubicDimension ) );
            snapshotViewer.launch( collector );
            makeViewerVisible(snapshotViewer);

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
            establishColorControls( snapshotViewer );
            snapshotViewer.setLabelText( labelTextFor3d(cubicDimension) );
            snapshotViewer.launch( collector );
            makeViewerVisible(snapshotViewer);

        } catch ( Exception ex ) {
            System.err.println("Failed to launch viewer: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Give ourselves a separate set of color adjustements.
     * 
     * @param snapshotViewer will be given a color model.
     */
    private void establishColorControls( Snapshot3d snapshotViewer ) {
        ImageColorModel independentCM = new ImageColorModel(getMaxIntensity(), getNumberOfChannels());
        snapshotViewer.setImageColorModel( independentCM );        
        // TODO: find way to use either new or parent-sourced image color model.
        //        snapshotViewer.setImageColorModel( imageColorModel );        
    }
    
    private void makeViewerVisible(Snapshot3d snapshotViewer) {
        Snapshot3dTopComponent snapshotTopComponent =
                (Snapshot3dTopComponent)WindowManager.getDefault()
                        .findTopComponent(
                                Snapshot3dTopComponent.SNAPSHOT3D_TOP_COMPONENT_PREFERRED_ID
                        );
        if ( snapshotTopComponent != null ) {
            if ( ! snapshotTopComponent.isOpened() ) {
                snapshotTopComponent.open();
            }
            if ( snapshotTopComponent.isOpened() ) {
                snapshotTopComponent.requestActive();
            }
            snapshotTopComponent.setSnapshotComponent(snapshotViewer);
            snapshotTopComponent.setVisible(true);
        }
    }

    private String labelTextFor3d(int cubicDimension) {
        return getLabelText( cubicDimension, RENDERED_VOLUME_TEXT_FORMAT );
    }

    private String labelTextForRaw3d(int cubicDimension) {
        return getLabelText( cubicDimension, RAW_VOLUME_TEXT_FORMAT );
    }

    private String getLabelText(int cubicDimension, String format ) {
        final Vec3 focus = camera.getFocus();
        return String.format( format, focus.getX(), focus.getY(), focus.getZ(), cubicDimension );
    }

}
