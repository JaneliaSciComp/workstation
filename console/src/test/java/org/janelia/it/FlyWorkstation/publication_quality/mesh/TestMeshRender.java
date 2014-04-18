package org.janelia.it.FlyWorkstation.publication_quality.mesh;

import org.janelia.it.FlyWorkstation.geom.Vec3;
import org.janelia.it.FlyWorkstation.gui.WorkstationEnvironment;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board.AlignmentBoardSettings;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Mip3d;
import org.janelia.it.FlyWorkstation.publication_quality.mesh.actor.MeshDrawActor;
import org.janelia.it.FlyWorkstation.shared.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

/**
 * Testing class - a regression test vehicle, for looking at mesh rendering over existing volumes.
 *
 * Created by fosterl on 4/14/14.
 */
public class TestMeshRender {
    private static Logger logger = LoggerFactory.getLogger(TestMeshRender.class);
    public static void main( final String[] args ) throws Exception {
        // Prepare th eenvironment.  Now we can work like the workstation.
        WorkstationEnvironment we = new WorkstationEnvironment();
        we.invoke();
        // Fire a thread to build data for presentation, and show that.
        PresWorker loadAndPresentWorker = new PresWorker();
        loadAndPresentWorker.execute();
        Thread.sleep( 3000 );
    }

    private static class PresWorker extends SimpleWorker {
        private Mip3d mipWidget;
        private VtxAttribMgr attribMgr;

        public PresWorker() {
            mipWidget = new Mip3d();
            mipWidget.getVolumeModel().setGammaAdjustment( (float) AlignmentBoardSettings.DEFAULT_GAMMA );
            mipWidget.getVolumeModel().setCameraDepth( new Vec3( 0.0, 0.0, 0.0 ) );

        }

        @Override
        protected void doStuff() throws Exception {
            logger.info("Doing atttribute creation in thread {}", Thread.currentThread().getName());
            attribMgr = new VtxAttribMgr( MeshRenderTestFacilities.getNeuronMaskChanRenderableDatas() );
            attribMgr.execute();

        }

        @Override
        protected void hadSuccess() {
            logger.info("Successful vtx attrib manager.");
            MeshDrawActor.MeshDrawActorConfigurator configurator = new MeshDrawActor.MeshDrawActorConfigurator();
            configurator.setAxisLengths( new double[] {1024, 512, 218} );
            configurator.setVolumeModel( mipWidget.getVolumeModel() );

            configurator.setVertexAttribMgr( attribMgr );
            configurator.setRenderableId( MeshRenderTestFacilities.NEURON_RENDERABLE_ID);
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
