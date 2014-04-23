package org.janelia.it.FlyWorkstation.publication_quality.mesh;

import org.janelia.it.FlyWorkstation.geom.Rotation3d;
import org.janelia.it.FlyWorkstation.geom.Vec3;
import org.janelia.it.FlyWorkstation.gui.WorkstationEnvironment;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board.AlignmentBoardSettings;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Mip3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.matrix_support.ViewMatrixSupport;
import org.janelia.it.FlyWorkstation.publication_quality.mesh.actor.MeshDrawActor;
import org.janelia.it.FlyWorkstation.shared.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * Testing class - a regression test vehicle, for looking at mesh rendering over existing volumes.
 *
 * Created by fosterl on 4/14/14.
 */
public class TestMeshRender {
    private static Logger logger = LoggerFactory.getLogger(TestMeshRender.class);

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
        private Mip3d mipWidget;
        private VertexAttributeManagerI attribMgr;

        public PresWorker() {
            mipWidget = new Mip3d();
            Action dumpAction = new AbstractAction("Dump"){

                @Override
                public void actionPerformed(ActionEvent e) {
                    Rotation3d rotation = mipWidget.getVolumeModel().getCamera3d().getRotation();
                    System.out.println("Rotation = " + rotation);
                    float[] mvm = mipWidget.getVolumeModel().getModelViewMatrix();
                    float[] pm = mipWidget.getVolumeModel().getPerspectiveMatrix();
                    ViewMatrixSupport vms = new ViewMatrixSupport();
                    vms.dumpMatrices( mvm, pm );
                }

            };
            mipWidget.addMenuAction(dumpAction);
            mipWidget.getVolumeModel().setGammaAdjustment( (float) AlignmentBoardSettings.DEFAULT_GAMMA );
            mipWidget.getVolumeModel().setCameraDepth( new Vec3( 0.0, 0.0, 0.0 ) );

        }

        @Override
        protected void doStuff() throws Exception {
            logger.info("Doing atttribute creation in thread {}", Thread.currentThread().getName());
            //renderId = MeshRenderTestFacilities.NEURON_RENDERABLE_ID;
            renderId = MeshRenderTestFacilities.COMPARTMENT_RENDERABLE_ID;
            attribMgr =
                    new VtxAttribMgr( MeshRenderTestFacilities.getCompartmentMaskChanRenderableDatas() );
                    //new VtxAttribMgr( MeshRenderTestFacilities.getNeuronMaskChanRenderableDatas() );
                    //new FewVoxelVtxAttribMgr( renderId );
            attribMgr.execute();

        }

        @Override
        protected void hadSuccess() {
            logger.info("Successful vtx attrib manager.");
            MeshDrawActor.MeshDrawActorConfigurator configurator = new MeshDrawActor.MeshDrawActorConfigurator();
            configurator.setAxisLengths( new double[] {1024, 512, 218} );
            configurator.setVolumeModel( mipWidget.getVolumeModel() );

            configurator.setVertexAttributeManager(attribMgr);
            //configurator.setRenderableId( MeshRenderTestFacilities.COMPARTMENT_RENDERABLE_ID);
            configurator.setRenderableId( renderId );
            MeshDrawActor actor = new MeshDrawActor( configurator );
            mipWidget.clear();
            mipWidget.addActor( actor );

            JFrame frame = new JFrame("Test Mesh Render");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            JLabel label = new JLabel("Test Mesh Render");
            frame.getContentPane().add(label);

            frame.getContentPane().add(mipWidget);

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
