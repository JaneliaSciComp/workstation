/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.full_skeleton_view.viewer;

import java.awt.BorderLayout;
import java.awt.Graphics;
import javax.media.opengl.GLAutoDrawable;
import javax.swing.JPanel;
import org.janelia.it.workstation.gui.full_skeleton_view.data_source.AnnotationSkeletonDataSourceI;
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
//            {
//                //Override, so can selectively breakpoint only this instance.
//                @Override
//                public void display(GLAutoDrawable glDrawable) {
//                    super.display(glDrawable);
//                } 
//                
//                @Override
//                public void updateAnchors() {
//                    super.updateAnchors();
//                }
//            };
            final BoundingBox3d boundingBox = dataSource.getTileFormat().calcBoundingBox();
            actor.getBoundingBox3d().setMax( boundingBox.getMax() );
            actor.getBoundingBox3d().setMin( boundingBox.getMin() );
//            actor.getBoundingBox3d().setMin( 70000, 43000, 15000 );
//            actor.getBoundingBox3d().setMax( 79000, 50000, 29000 );            
            mip3d = new Mip3d();
            actor.setSkeleton(dataSource.getSkeleton());
            actor.setCamera(mip3d.getVolumeModel().getCamera3d());
            actor.setAnchorsVisible(false);
            actor.setTileFormat(dataSource.getTileFormat());
            actor.setZThicknessInPixels( 29000 - 15000 );
            actor.updateAnchors();
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
