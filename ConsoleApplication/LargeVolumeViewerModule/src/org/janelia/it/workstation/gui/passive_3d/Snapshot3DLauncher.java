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
import javax.swing.JSeparator;
import org.janelia.it.workstation.geom.CoordinateAxis;
import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.camera.ObservableCamera3d;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.large_volume_viewer.ImageColorModel;
import org.janelia.it.workstation.gui.large_volume_viewer.SubvolumeProvider;
import org.janelia.it.workstation.gui.large_volume_viewer.TileServer;
import org.janelia.it.workstation.gui.passive_3d.top_component.Snapshot3dTopComponent;
import org.janelia.it.workstation.gui.viewer3d.BoundingBox3d;
import org.janelia.it.workstation.shared.workers.IndeterminateNoteProgressMonitor;
import org.openide.windows.WindowManager;

/**
 * Sources menu items for presenting the user with their 3D viewer.
 * 
 * @author fosterl
 */
public class Snapshot3DLauncher {
    private static final String RENDERED_VOLUME_TEXT_FORMAT = "Contains point [%3.1f,%3.1f,%3.1f].  %4$dx%5$dx%6$d.  Rendered Data.";
    private final static String RAW_VOLUME_TEXT_FORMAT = "Contains point [%3.1f,%3.1f,%3.1f].  %4$dx%5$dx%6$d.  Raw Data.";

    private CoordinateAxis sliceAxis;
    private TileServer tileServer;
    private SubvolumeProvider subvolumeProvider;
    private ObservableCamera3d camera;
    private URL dataUrl;
    private String basePath;
    private ImageColorModel sharedImageColorModel;
    private Integer maxIntensity;
    private Integer numberOfChannels;
    
    public Snapshot3DLauncher(
            TileServer tileServer,
            CoordinateAxis sliceAxis,
            ObservableCamera3d camera,
            SubvolumeProvider subvolumeProvider,
            String basePath,
            URL dataUrl,
            ImageColorModel imageColorModel
    ) {
        this.tileServer = tileServer;
        this.sliceAxis = sliceAxis;
        this.subvolumeProvider = subvolumeProvider;
        this.sharedImageColorModel = imageColorModel;
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
        snapShot3dSubMenu.add(new JSeparator());

        extents = new int[] {
            64, 128
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

        /*
        JMenuItem item = new JMenuItem( "Full raw" );
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                launchRaw3dViewer(-1);
            }
        });
        snapShot3dSubMenu.add( item );
        */
        
        rtnVal.add( snapShot3dSubMenu );

        return rtnVal;
    }    

    /** Launches a 3D popup containing raw data represented by camera position. */
    public void launchRaw3dViewer( int cubicDimension ) {
        int[] dimensions = new int[] { cubicDimension, cubicDimension, cubicDimension };
        launchRaw3dViewer( dimensions );
    }
    
    /** Launches a 3D popup containing raw data represented by camera position. */
    public void launchRaw3dViewer( int[] dimensions ) {
        try {            
            RawTiffVolumeSource collector = new RawTiffVolumeSource( 
                    tileServer.getLoadAdapter().getTileFormat(), camera, basePath 
            );
            if ( dimensions != null ) {
                collector.setDimensions(dimensions);
            }
            final String labelText = labelTextForRaw3d( dimensions );
            final String frameTitle = "Fetching raw data";
            makeAndLaunch(frameTitle, collector, labelText);

        } catch ( Exception ex ) {
            System.err.println("Failed to launch viewer: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public void launch3dViewer( int cubicDimension ) {
        int[] dimensions = new int[] { cubicDimension, cubicDimension, cubicDimension };
        launch3dViewer( dimensions );
    }
    
    /** Launches a 3D popup static-block viewer. */
    public void launch3dViewer( int[] dimensions ) {
        try {         
            BoundingBox3d bb = tileServer.getLoadAdapter().getTileFormat().calcBoundingBox();
            final String labelText = labelTextFor3d(dimensions);
            final String frameTitle = "Fetching tiles";
            MonitoredVolumeSource collector = new ViewTileManagerVolumeSource(
                    camera,
                    bb,
                    dimensions,
                    subvolumeProvider,
                    dataUrl
            );            

            makeAndLaunch(frameTitle, collector, labelText);
//            Snapshot3d snapshotViewer = Snapshot3d.getInstance();
//            IndeterminateNoteProgressMonitor monitor = 
//                    new IndeterminateNoteProgressMonitor(SessionMgr.getMainFrame(), frameTitle, collector.getInfo());
//            snapshotViewer.setLoadProgressMonitor( monitor );
//            establishColorControls( snapshotViewer );
//            snapshotViewer.setLabelText( labelText);
//            snapshotViewer.launch( collector );
//            makeViewerVisible(snapshotViewer);

        } catch ( Exception ex ) {
            System.err.println("Failed to launch viewer: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    public static void removeStaleViewer() {
        Snapshot3dTopComponent snapshotTopComponent =
                findComponent();
        if ( snapshotTopComponent != null ) {
            snapshotTopComponent.cleanupContent();
        }
    }

    /**
     * Common code for launching the viewer.
     * 
     * @param frameTitle over the viewer
     * @param collector for getting data.
     * @param labelText specific to input data.
     */
    private void makeAndLaunch(final String frameTitle, MonitoredVolumeSource collector, final String labelText) {
        Snapshot3d snapshotViewer = Snapshot3d.getInstance();
        IndeterminateNoteProgressMonitor monitor =
                new IndeterminateNoteProgressMonitor(SessionMgr.getMainFrame(), frameTitle, collector.getInfo());
        snapshotViewer.setLoadProgressMonitor( monitor );
        establishColorControls( snapshotViewer );
        snapshotViewer.setLabelText( labelText);
        snapshotViewer.launch( collector );
        makeViewerVisible(snapshotViewer);
    }
    
    /**
     * Give ourselves a separate set of color adjustments.
     * 
     * @param snapshotViewer will be given a color model.
     */
    private void establishColorControls( Snapshot3d snapshotViewer ) {
        ImageColorModel independentCM = new ImageColorModel(getMaxIntensity(), getNumberOfChannels());
        snapshotViewer.setIndependentImageColorModel( independentCM ); 
        snapshotViewer.setSharedImageColorModel( sharedImageColorModel );
        independentCM.fromString(sharedImageColorModel.asString());
    }
    
    private void makeViewerVisible(Snapshot3d snapshotViewer) {
        Snapshot3dTopComponent snapshotTopComponent =
                findComponent();
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

    private static Snapshot3dTopComponent findComponent() {
        Snapshot3dTopComponent snapshotTopComponent =
                (Snapshot3dTopComponent)WindowManager.getDefault()
                        .findTopComponent(
                                Snapshot3dTopComponent.SNAPSHOT3D_TOP_COMPONENT_PREFERRED_ID
                        );
        return snapshotTopComponent;
    }

    private String labelTextFor3d(int[] dimensions) {
        return getLabelText( dimensions, RENDERED_VOLUME_TEXT_FORMAT );
    }

    private String labelTextForRaw3d(int[] dimensions) {
        return getLabelText( dimensions, RAW_VOLUME_TEXT_FORMAT );
    }

    private String getLabelText(int[] dimensions, String format ) {
        final Vec3 focus = camera.getFocus();
        return String.format( format, focus.getX(), focus.getY(), focus.getZ(), dimensions[0], dimensions[1], dimensions[2] );
    }

}
