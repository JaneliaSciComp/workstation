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
import org.janelia.it.workstation.gui.large_volume_viewer.skeleton.SkeletonActor;
import org.janelia.it.workstation.gui.viewer3d.Mip3d;

/**
 * This panel holds all relevant components for showing the skeleton of
 * an annotation made in the Large Volume Viewer.
 * 
 * @author fosterl
 */
public class AnnotationSkeletonPanel extends JPanel {
    private AnnotationSkeletonDataSourceI dataSource;
    private Mip3d mip3d;
    
    public AnnotationSkeletonPanel(AnnotationSkeletonDataSourceI dataSource) {
        this.dataSource = dataSource;
        this.setLayout(new BorderLayout());
    }

    public void establish3D() {
        if (mip3d == null  &&  dataSource.getSkeleton() != null) {
            SkeletonActor actor = new SkeletonActor();
            mip3d = new Mip3d();
            mip3d.clear();
            actor.setSkeleton(dataSource.getSkeleton());
            actor.setCamera(mip3d.getVolumeModel().getCamera3d());
            mip3d.addActor(actor);
            this.add(mip3d, BorderLayout.CENTER);
        }
    }
    
    public void paint(Graphics g) {
        establish3D();
    }
}
