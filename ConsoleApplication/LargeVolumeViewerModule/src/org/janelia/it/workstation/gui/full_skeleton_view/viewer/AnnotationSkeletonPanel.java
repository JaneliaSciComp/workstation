/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.full_skeleton_view.viewer;

import java.awt.BorderLayout;
import java.awt.Graphics;
import javax.swing.JPanel;
import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.full_skeleton_view.data_source.AnnotationSkeletonDataSourceI;
import org.janelia.it.workstation.gui.large_volume_viewer.TileFormat;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.SkeletonController;
import org.janelia.it.workstation.gui.large_volume_viewer.skeleton.SkeletonActor;
import org.janelia.it.workstation.gui.opengl.GLActor;
import org.janelia.it.workstation.gui.viewer3d.BoundingBox3d;
import org.janelia.it.workstation.gui.viewer3d.OcclusiveViewer;
import org.janelia.it.workstation.gui.viewer3d.VolumeModel;
import org.janelia.it.workstation.gui.viewer3d.axes.AxesActor;

/**
 * This panel holds all relevant components for showing the skeleton of
 * an annotation made in the Large Volume Viewer.
 * 
 * @author fosterl
 */
public class AnnotationSkeletonPanel extends JPanel {
    private final AnnotationSkeletonDataSourceI dataSource;
    private OcclusiveViewer viewer;
    
    public AnnotationSkeletonPanel(AnnotationSkeletonDataSourceI dataSource) {
        this.dataSource = dataSource;
        this.setLayout(new BorderLayout());
    }

    public void establish3D() {
        if (viewer == null  &&  dataSource.getSkeleton() != null) {
            SkeletonActor actor = new SkeletonActor();
            actor.setNeuronStyleModel(dataSource.getNeuronStyleModel());
            actor.setShowOnlyParentAnchors(true);
            actor.setAnchorsVisible(true);
            TileFormat tileFormat = dataSource.getSkeleton().getTileFormat();
            final BoundingBox3d boundingBox = tileFormat.calcBoundingBox();
            Vec3 yExtender = new Vec3(0, 0.75 * boundingBox.getHeight(), 0);
            actor.getBoundingBox3d().setMax( boundingBox.getMax().plus( yExtender ) );
            actor.getBoundingBox3d().setMin( boundingBox.getMin().minus( yExtender ) );
            viewer = new OcclusiveViewer();
            VolumeModel volumeModel = viewer.getVolumeModel();
            actor.setSkeleton(dataSource.getSkeleton());
            actor.setCamera(volumeModel.getCamera3d());
            actor.setTileFormat(tileFormat);
            actor.setRenderInterpositionMethod(
                    SkeletonActor.RenderInterpositionMethod.Occlusion
            );
            volumeModel.setBackgroundColor(new float[] {
                0.999f, 0.999f, 0.999f
//                0.0f, 0.0f, 0.0f
            });
            // Set maximal thickness.  Z-fade is not practical for 3D rotations.
            actor.setZThicknessInPixels( Long.MAX_VALUE );
            actor.updateAnchors();

            // This should be done after establishing the skeleton.
            SkeletonController controller = SkeletonController.getInstance();
            controller.registerForEvents(actor);

            viewer.setResetFirstRedraw(true);
            final BoundingBox3d originalBoundingBox = tileFormat.calcBoundingBox();
            GLActor axesActor = buildAxesActor( originalBoundingBox, 1.0, volumeModel );
//            mip3d.addActor(axesActor);
            viewer.addActor(actor);
            this.add(viewer, BorderLayout.CENTER);
            validate();
            repaint();
        }
    }
    
    public void close() {
        if (viewer != null ) {
            viewer.clear();
            viewer = null;
        }
    }
    
    public void paint(Graphics g) {
        establish3D();
        super.paint(g);
    }

    /**
     * Creates the actor to draw the axes on the screen.
     *
     * @param boundingBox tells extrema for the axes.
     * @param axisLengthDivisor applies downsampling abbreviation of axes.
     * @param volumeModel tells the axes actor whether its background will be white.
     * @return the actor.
     */
    public GLActor buildAxesActor(BoundingBox3d boundingBox, double axisLengthDivisor, VolumeModel volumeModel) {
        AxesActor axes = new AxesActor();
        axes.setVolumeModel(volumeModel);
        axes.setBoundingBox(boundingBox);
        axes.setAxisLengthDivisor( axisLengthDivisor );
        axes.setFullAxes( true );
        return axes;
    }
}
