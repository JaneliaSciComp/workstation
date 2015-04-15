/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.full_skeleton_view.viewer;

import java.awt.BorderLayout;
import java.awt.Graphics;
import javax.swing.JPanel;
import org.janelia.it.workstation.gui.full_skeleton_view.data_source.AnnotationSkeletonDataSourceI;
import org.janelia.it.workstation.gui.large_volume_viewer.TileFormat;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.SkeletonController;
import org.janelia.it.workstation.gui.large_volume_viewer.skeleton.SkeletonActor;
import org.janelia.it.workstation.gui.viewer3d.BoundingBox3d;
import org.janelia.it.workstation.gui.viewer3d.Mip3d;

/**
 * This panel holds all relevant components for showing the skeleton of
 * an annotation made in the Large Volume Viewer.
 * 
 * @author fosterl
 */
public class AnnotationSkeletonPanel extends JPanel {
    private final AnnotationSkeletonDataSourceI dataSource;
    private Mip3d mip3d;
    
    public AnnotationSkeletonPanel(AnnotationSkeletonDataSourceI dataSource) {
        this.dataSource = dataSource;
        this.setLayout(new BorderLayout());
    }

    public void establish3D() {
        if (mip3d == null  &&  dataSource.getSkeleton() != null) {
            SkeletonActor actor = new SkeletonActor();
            actor.setNeuronStyleModel(dataSource.getNeuronStyleModel());
            actor.setShowOnlyParentAnchors(true);
            actor.setAnchorsVisible(true);
            TileFormat tileFormat = dataSource.getSkeleton().getTileFormat();
            final BoundingBox3d boundingBox = tileFormat.calcBoundingBox();
            actor.getBoundingBox3d().setMax( boundingBox.getMax() );
            actor.getBoundingBox3d().setMin( boundingBox.getMin() );
            mip3d = new Mip3d();
            actor.setSkeleton(dataSource.getSkeleton());
            actor.setCamera(mip3d.getVolumeModel().getCamera3d());
            actor.setTileFormat(tileFormat);
            // Set maximal thickness.  Z-fade is not practical for 3D rotations.
            actor.setZThicknessInPixels( Long.MAX_VALUE );
            actor.updateAnchors();

            // This should be done after establishing the skeleton.
            SkeletonController controller = SkeletonController.getInstance();
            controller.registerForEvents(actor);
//            controller.skeletonChanged();

            mip3d.addActor(actor);    
            mip3d.setResetFirstRedraw(true);
            this.add(mip3d, BorderLayout.CENTER);
            validate();
            repaint();
        }
    }
    
    public void close() {
        if (mip3d != null ) {
            mip3d.clear();
            mip3d = null;
        }
    }
    
    public void paint(Graphics g) {
        establish3D();
        super.paint(g);
    }
}
