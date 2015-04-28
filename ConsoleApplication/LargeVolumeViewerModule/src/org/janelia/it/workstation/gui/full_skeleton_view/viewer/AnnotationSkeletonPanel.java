/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.full_skeleton_view.viewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JColorChooser;
import javax.swing.JPanel;
import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.full_skeleton_view.data_source.AnnotationSkeletonDataSourceI;
import org.janelia.it.workstation.gui.large_volume_viewer.TileFormat;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.SkeletonController;
import org.janelia.it.workstation.gui.large_volume_viewer.skeleton.Anchor;
import org.janelia.it.workstation.gui.large_volume_viewer.skeleton.DirectionalReferenceAxesActor;
import org.janelia.it.workstation.gui.large_volume_viewer.skeleton.Skeleton;
import org.janelia.it.workstation.gui.large_volume_viewer.skeleton.SkeletonActor;
import org.janelia.it.workstation.gui.opengl.GLActor;
import org.janelia.it.workstation.gui.viewer3d.BoundingBox3d;
import org.janelia.it.workstation.gui.viewer3d.MeshViewContext;
import org.janelia.it.workstation.gui.viewer3d.OcclusiveViewer;
import org.janelia.it.workstation.gui.viewer3d.OcclusiveRenderer;
import org.janelia.it.workstation.gui.viewer3d.ResetPositionerI;
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
        if (viewer == null  &&  dataSource.getSkeleton() != null  &&  dataSource.getSkeleton().getTileFormat() != null) {
            SkeletonActor actor = new SkeletonActor();
            actor.setParentAnchorImageName( SkeletonActor.ParentAnchorImage.LARGE );
            actor.setNeuronStyleModel( dataSource.getNeuronStyleModel() );
            actor.setShowOnlyParentAnchors( true );
            actor.setAnchorsVisible(true);
            actor.setFocusOnNextParent(true);
            TileFormat tileFormat = dataSource.getSkeleton().getTileFormat();
            final BoundingBox3d boundingBox = tileFormat.calcBoundingBox();
            Vec3 yExtender = new Vec3(0, 0.75 * boundingBox.getHeight(), 0);
            actor.getBoundingBox3d().setMax( boundingBox.getMax().plus( yExtender ) );
            actor.getBoundingBox3d().setMin( boundingBox.getMin().minus( yExtender ) );
            OcclusiveRenderer renderer = new OcclusiveRenderer();
            final SkeletalBoundsResetPositioner skeletalBoundsResetPositioner = new SkeletalBoundsResetPositioner(dataSource.getSkeleton());
            renderer.setResetPositioner( skeletalBoundsResetPositioner);
            viewer = new OcclusiveViewer(renderer);
            skeletalBoundsResetPositioner.setViewer(viewer);
            skeletalBoundsResetPositioner.setRenderer(renderer);
            skeletalBoundsResetPositioner.setActor(actor);
            MeshViewContext context = new MeshViewContext();
            viewer.setVolumeModel(context);
            VolumeModel volumeModel = viewer.getVolumeModel();
            actor.setSkeleton(dataSource.getSkeleton());
            actor.setCamera(volumeModel.getCamera3d());
            actor.setTileFormat(tileFormat);
            actor.setRenderInterpositionMethod(
                    SkeletonActor.RenderInterpositionMethod.Occlusion
            );
            volumeModel.setBackgroundColor(new float[] {
                0.0f, 0.0f, 0.0f
            });
            // Set maximal thickness.  Z-fade is not practical for 3D rotations.
            actor.setZThicknessInPixels( Long.MAX_VALUE );
            actor.updateAnchors();

            // This should be done after establishing the skeleton.
            SkeletonController controller = SkeletonController.getInstance();
            controller.registerForEvents(actor);

            DirectionalReferenceAxesActor refAxisActor = new DirectionalReferenceAxesActor(
                    new float[] { 100.0f, 100.0f, 100.0f },
                    boundingBox,
                    context,
                    DirectionalReferenceAxesActor.Placement.BOTTOM_LEFT                    
            );
            
            viewer.setResetFirstRedraw(true);
            final BoundingBox3d originalBoundingBox = tileFormat.calcBoundingBox();
            GLActor axesActor = buildAxesActor( originalBoundingBox, 1.0, volumeModel );
            viewer.addActor(axesActor);
            viewer.addActor(actor);
            viewer.addActor(refAxisActor);
            viewer.addMenuAction(new BackgroundPickAction(viewer));
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
    
    @Override
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
        axes.setAxisLengths( boundingBox.getWidth(), boundingBox.getHeight(), boundingBox.getDepth() );
        axes.setRenderMethod(AxesActor.RenderMethod.MESH);
        axes.setAxisLengthDivisor( axisLengthDivisor );
        axes.setFullAxes( true );
        return axes;
    }
    
    public static class BackgroundPickAction extends AbstractAction {

        private OcclusiveViewer viewer;
        public BackgroundPickAction( OcclusiveViewer viewer ) {
            this.viewer = viewer;
            putValue(Action.NAME, "Background Color");
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            float[] background = viewer.getVolumeModel().getBackgroundColorFArr();
            Color oldBackground = new Color( background[0], background[1], background[2] );
            Color newBackground = JColorChooser.showDialog(
                    viewer, 
                    "Annotation Skeleton Background Color", 
                    oldBackground
            );
            if ( newBackground != null ) {
                newBackground.getColorComponents(background);
                viewer.getVolumeModel().setBackgroundColor(background);
            }
        }
        
    }
    
    public static class SkeletalBoundsResetPositioner implements ResetPositionerI {
        private Skeleton skeleton;
        private OcclusiveViewer viewer;
        private OcclusiveRenderer renderer;
        private SkeletonActor actor;
        public SkeletalBoundsResetPositioner( Skeleton skeleton ) {
            this.skeleton = skeleton;
        }
        
        public void setViewer(OcclusiveViewer viewer) {
            this.viewer = viewer;
        }
        
        public void setRenderer(OcclusiveRenderer renderer) {
            this.renderer = renderer;
        }
        
        public void setActor(SkeletonActor actor) {
            this.actor = actor;
        }
        
        @Override
        public void resetView() {
            // Compute a bounding box out of the skeleton.
            BoundingBox3d boundingBox = getInclusiveBox();            
            viewer.getVolumeModel().getCamera3d().setFocus(boundingBox.getCenter());
            viewer.getVolumeModel().getCamera3d().resetRotation();
            renderer.resetCameraDepth(actor.getBoundingBox3d());
        }
        
        private BoundingBox3d getInclusiveBox() {
            Vec3 minimum = null;
            Vec3 maximum = null;
            for (Anchor anchor : skeleton.getAnchors()) {
                if (minimum == null) {
                    minimum = anchor.getLocation().clone();
                } else {
                    minimum.setX(Math.min(minimum.getX(), anchor.getLocation().getX()));
                    minimum.setY(Math.min(minimum.getY(), anchor.getLocation().getY()));
                    minimum.setZ(Math.min(minimum.getZ(), anchor.getLocation().getZ()));
                }

                if (maximum == null) {
                    maximum = anchor.getLocation().clone();
                } else {
                    maximum.setX(Math.max(maximum.getX(), anchor.getLocation().getX()));
                    maximum.setY(Math.max(maximum.getY(), anchor.getLocation().getY()));
                    maximum.setZ(Math.max(maximum.getZ(), anchor.getLocation().getZ()));
                }
            }
            BoundingBox3d box = new BoundingBox3d();
            box.include(minimum);
            box.include(maximum);
            return box;
        }
        
    }
    
}
