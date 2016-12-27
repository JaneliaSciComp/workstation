package org.janelia.it.workstation.gui.viewer3d;

import org.janelia.it.workstation.gui.viewer3d.mesh.actor.FewVoxelVtxAttribMgr;
import org.janelia.it.workstation.gui.viewer3d.mesh.MeshViewer;
import org.janelia.it.workstation.geom.Rotation3d;
import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.WorkstationEnvironment;
import org.janelia.it.workstation.gui.viewer3d.matrix_support.ViewMatrixSupport;
import org.janelia.it.workstation.gui.viewer3d.mesh.actor.MeshDrawActor;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.janelia.it.jacs.model.TestCategories;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import org.janelia.it.jacs.shared.mesh_loader.VertexAttributeSourceI;

/**
 * Testing class - a regression test vehicle, for looking at mesh rendering over existing volumes.
 *
 * Created by fosterl on 4/14/14.
 */
@Category(TestCategories.InteractiveTests.class)
public class TestMeshRender {
    private static Logger logger = LoggerFactory.getLogger(TestMeshRender.class);
    public static final Long COMPARTMENT_RENDERABLE_ID = 5555L;
    private static long renderId;

    public static void main( final String[] args ) throws Exception {
        // Prepare the environment.  Now we can work like the workstation.
        WorkstationEnvironment we = new WorkstationEnvironment();
        we.invoke();
        // Fire a thread to build data for presentation, and show that.
        PresWorker loadAndPresentWorker = new PresWorker();
        loadAndPresentWorker.execute();
        Thread.sleep( 3000 );
    }

    private static class PresWorker extends SimpleWorker {
        private MeshViewer viewerWidget;
        private VertexAttributeSourceI attribMgr;

        public PresWorker() {
            viewerWidget = new MeshViewer();
            Action dumpAction = new AbstractAction("Dump"){

                @Override
                public void actionPerformed(ActionEvent e) {
                    Rotation3d rotation = viewerWidget.getVolumeModel().getCamera3d().getRotation();
                    System.out.println("Rotation = " + rotation);
                    float[] mvm = ((MeshViewContext)viewerWidget.getVolumeModel()).getModelViewMatrix();
                    float[] pm = ((MeshViewContext)viewerWidget.getVolumeModel()).getPerspectiveMatrix();
                    ViewMatrixSupport vms = new ViewMatrixSupport();
                    vms.dumpMatrices( mvm, pm );
                }

            };
            viewerWidget.addMenuAction(dumpAction);
            viewerWidget.getVolumeModel().setGammaAdjustment( 1.0f );
            viewerWidget.getVolumeModel().setCameraDepth( new Vec3( 0.0, 0.0, 0.0 ) );

        }

        @Override
        protected void doStuff() throws Exception {
            logger.info("Doing atttribute creation in thread {}", Thread.currentThread().getName());
            //renderId = MeshRenderTestFacilities.NEURON_RENDERABLE_ID;
            renderId = COMPARTMENT_RENDERABLE_ID;
            attribMgr =
                    //new VtxAttribMgr( MeshRenderTestFacilities.getCompartmentMaskChanRenderableDatas() );
                    //new VtxAttribMgr( MeshRenderTestFacilities.getNeuronMaskChanRenderableDatas() );
                    new FewVoxelVtxAttribMgr( renderId );
            //attribMgr.execute();

        }

        @Override
        protected void hadSuccess() {
            logger.info("Successful vtx attrib manager.");
            MeshDrawActor.MeshDrawActorConfigurator configurator = new MeshDrawActor.MeshDrawActorConfigurator();
            configurator.setAxisLengths( new double[] {200, 200, 200} );
            configurator.setContext( viewerWidget.getMeshViewContext() ); 
            configurator.setMatrixScope(MeshDrawActor.MatrixScope.EXTERNAL);
            /*
             *  Ordinarily, the bounding box will be set from without, to
             *  correspond to the caller's data space.  This test does not
             *  have such a natural source.
             */
            // NOTE: bounding box must match conditions in attributes mgr.
            BoundingBox3d bb = new BoundingBox3d();
            bb.setMin( 73000, 47000, 17000 );
            bb.setMax( 75000, 49000, 21000 );
            configurator.setBoundingBox( bb );

            configurator.setVertexAttributeManager(attribMgr);
            //configurator.setRenderableId( MeshRenderTestFacilities.COMPARTMENT_RENDERABLE_ID);
            configurator.setRenderableId( renderId );
            configurator.setColoringStrategy(MeshDrawActor.ColoringStrategy.ATTRIBUTE);
            MeshDrawActor actor = new MeshDrawActor( configurator );
            viewerWidget.clear();
            viewerWidget.addActor( actor );            
            final Vec3 center = actor.getBoundingBox3d().getCenter();
            viewerWidget.getMeshViewContext().getCamera3d().setFocus(center);

            JFrame frame = new JFrame("Test Mesh Render");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            JLabel label = new JLabel("Test Mesh Render");
            frame.getContentPane().add(label);

            frame.getContentPane().add(viewerWidget);

            //Display the window.
            frame.pack();
            frame.setSize( frame.getContentPane().getPreferredSize() );
            frame.setVisible(true);
        }

        @Override
        protected void hadError(Throwable error) {
            error.printStackTrace();
            JOptionPane.showMessageDialog( null, "Failed to launch");
        }
    }
}
