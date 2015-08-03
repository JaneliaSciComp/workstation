package org.janelia.it.workstation.gui.geometric_search.viewer.gl.experiment;

import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.Vector4;
import org.janelia.it.workstation.gui.geometric_search.viewer.gl.GL4ShaderActionSequence;
import org.janelia.it.workstation.gui.geometric_search.viewer.gl.GLDisplayUpdateCallback;
import org.janelia.it.workstation.gui.geometric_search.viewer.gl.OITSortShader;
import org.janelia.it.workstation.gui.geometric_search.viewer.gl.mesh.OITMeshDrawShader;
import org.janelia.it.workstation.gui.geometric_search.viewer.gl.volume.SparseVolumeCubeActor;
import org.janelia.it.workstation.gui.geometric_search.viewer.VoxelViewerGLPanel;

import javax.media.opengl.GL4;
import java.io.File;
import java.util.Random;

/**
 * Created by murphys on 7/29/2015.
 */
public class OITMeshExperiment implements GL4Experiment {

    public void setup(final VoxelViewerGLPanel viewer) {
        // First create the OITMeshDrawShader
        GL4ShaderActionSequence drawSequence = new GL4ShaderActionSequence("Draw Phase");
        GL4ShaderActionSequence sortSequence = new GL4ShaderActionSequence("Sort Phase");

        final OITMeshDrawShader drawShader = new OITMeshDrawShader();
        final OITSortShader sortShader = new OITSortShader();

        // Setup Draw Shader  //////////////////////////////


        drawShader.setUpdateCallback(new GLDisplayUpdateCallback() {
            @Override
            public void update(GL4 gl) {
                Matrix4 viewMatrix = viewer.getRenderer().getViewMatrix();
                drawShader.setView(gl, viewMatrix);
                //logger.info("View Matrix:\n"+viewMatrix.toString()+"\n");
                Matrix4 projMatrix = viewer.getRenderer().getProjectionMatrix();
                drawShader.setProjection(gl, projMatrix);
                //logger.info("Projection Matrix:\n"+projMatrix.toString()+"\n");
            }
        });


        File testHomeMeshDir = new File("C:\\cygwin64\\home\\murphys\\meshes");
        File testJaneliaMeshDir = new File("U:\\meshes");
        File testMeshDir = null;

        if (testHomeMeshDir.exists()) {
            testMeshDir = testHomeMeshDir;
        } else {
            testMeshDir = testJaneliaMeshDir;
        }

        File[] meshFiles = testMeshDir.listFiles();

        drawSequence.setShader(drawShader);

        Random rand = new Random();

        Matrix4 vertexRotation=new Matrix4();

        // Empirically derived - compatible with results of MeshLab import/export from normalized compartment coordinates
        vertexRotation.setTranspose(-1.0f,   0.0f,   0.0f,   0.5f,
                0.0f,  -1.0f,   0.0f,   0.25f,
                0.0f,   0.0f,  -1.0f,   0.625f,
                0.0f,   0.0f,   0.0f,   1.0f);

//        for (File meshFile : meshFiles) {
//            if (meshFile.getName().endsWith(".obj")) {
//                final MeshObjActor ma = new MeshObjActor(meshFile);
//                ma.setVertexRotation(vertexRotation);
//                ma.setColor(new Vector4(rand.nextFloat(), rand.nextFloat(), rand.nextFloat(), 0.5f));
//                ma.setUpdateCallback(new GLDisplayUpdateCallback() {
//                    @Override
//                    public void update(GL4 gl) {
//                        Matrix4 actorModel = ma.getModel();
//                        drawShader.setModel(gl, actorModel);
//                        drawShader.setDrawColor(gl, ma.getColor());
//                    }
//                });
//                drawSequence.getActorSequence().add(ma);
//            }
//        }

        File testHomeFile = new File("C:\\cygwin64\\home\\murphys\\volumes\\GMR_40B09_AE_01_06-fA01b_C091216_20100427171414198.reg.local.v3dpbd");
        File testJaneliaFile = new File("U:\\volumes\\GMR_40B09_AE_01_06-fA01b_C091216_20100427171414198.reg.local.v3dpbd");
        File testFile = null;

        if (testHomeFile.exists()) {
            testFile = testHomeFile;
        } else {
            testFile = testJaneliaFile;
        }

        final SparseVolumeCubeActor pa = new SparseVolumeCubeActor(testFile, 0, 0.2f);

        //final SparseVolumePointActor pa = new SparseVolumePointActor(new File("U:\\volumes\\GMR_40B09_AE_01_06-fA01b_C091216_20100427171414198.reg.local.v3dpbd"), 0, 0.2f);
        //final SparseVolumePointActor pa = new SparseVolumePointActor(new File("C:\\cygwin64\\home\\murphys\\volumes\\GMR_40B09_AE_01_06-fA01b_C091216_20100427171414198.reg.local.v3dpbd"), 1);


        //pa.setVertexRotation(vertexRotation);
        pa.setColor(new Vector4(0.0f, 1.0f, 0.0f, 1.0f));
        pa.setUpdateCallback(new GLDisplayUpdateCallback() {
            @Override
            public void update(GL4 gl) {
                Matrix4 actorModel = pa.getModel();
                drawShader.setModel(gl, actorModel);
                drawShader.setDrawColor(gl, pa.getColor());
            }
        });
        drawSequence.getActorSequence().add(pa);


        viewer.addShaderAction(drawSequence);

        // Setup Sort Shader ///////////////////////////////////////

        sortSequence.setShader(sortShader);
        viewer.addShaderAction(sortSequence); //DEBUG AND SEE WHAT HAPPENS
    }

}
