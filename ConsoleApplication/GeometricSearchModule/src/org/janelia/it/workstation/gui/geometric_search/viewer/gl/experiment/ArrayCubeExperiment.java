package org.janelia.it.workstation.gui.geometric_search.viewer.gl.experiment;

import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.Vector3;
import org.janelia.geometry3d.Vector4;
import org.janelia.it.workstation.gui.geometric_search.viewer.gl.GL4ShaderActionSequence;
import org.janelia.it.workstation.gui.geometric_search.viewer.gl.GLDisplayUpdateCallback;
import org.janelia.it.workstation.gui.geometric_search.viewer.gl.oitarr.*;
import org.janelia.it.workstation.gui.geometric_search.viewer.VoxelViewerGLPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.opengl.GL4;
import java.io.File;
import java.util.Random;

/**
 * Created by murphys on 7/28/2015.
 */
public class ArrayCubeExperiment implements GL4Experiment {

    private final Logger logger = LoggerFactory.getLogger(ArrayCubeExperiment.class);

    public void setup(final VoxelViewerGLPanel viewer) {

        final int DEPTH = 135;

        GL4ShaderActionSequence cubeSequence = new GL4ShaderActionSequence("ArrayCube");
        GL4ShaderActionSequence meshSequence = new GL4ShaderActionSequence("Meshes");
        GL4ShaderActionSequence sortSequence = new GL4ShaderActionSequence("Sort Phase");

        // VOLUME //////////////////////////////////////////////////////////////

        final ArrayCubeShader cubeShader = new ArrayCubeShader(viewer.getProperties());

        cubeShader.setUpdateCallback(new GLDisplayUpdateCallback() {
            @Override
            public void update(GL4 gl) {
                logger.info("Calling cubeShader.set* with depth="+DEPTH);
                cubeShader.setWidth(gl, viewer.getWidth());
                cubeShader.setHeight(gl, viewer.getHeight());
                cubeShader.setDepth(gl, DEPTH);            }
        });

        File testHomeFile = new File("C:\\cygwin64\\home\\murphys\\volumes\\GMR_40B09_AE_01_06-fA01b_C091216_20100427171414198.reg.local.v3dpbd");
        File testJaneliaFile = new File("U:\\volumes\\GMR_40B09_AE_01_06-fA01b_C091216_20100427171414198.reg.local.v3dpbd");
        File testFile = null;

        if (testHomeFile.exists()) {
            testFile = testHomeFile;
        } else {
            testFile = testJaneliaFile;
        }

        final ArrayCubeGLActor pa = new ArrayCubeGLActor(testFile, 1, 0.20f, 100000);

        Matrix4 gal4Rotation=new Matrix4();

        // Empirically derived - for GAL4 samples
        gal4Rotation.setTranspose(-1.0f, 0.0f, 0.0f, 0.5f,
                0.0f, -1.0f, 0.0f, 0.25f,
                0.0f, 0.0f, -1.0f, 0.625f,
                0.0f, 0.0f, 0.0f, 1.0f);
        pa.setModel(gal4Rotation);

        pa.setColor(new Vector4(1.0f, 0.0f, 0.0f, 0.005f));
        pa.setUpdateCallback(new GLDisplayUpdateCallback() {
            @Override
            public void update(GL4 gl) {
                Matrix4 view = viewer.getRenderer().getViewMatrix();
                Matrix4 proj = viewer.getRenderer().getProjectionMatrix();
                Matrix4 model = pa.getModel();

                Matrix4 viewCopy = new Matrix4(view);
                Matrix4 projCopy = new Matrix4(proj);
                Matrix4 modelCopy = new Matrix4(model);

                Matrix4 vp = viewCopy.multiply(projCopy);
                Matrix4 mvp = modelCopy.multiply(vp);

                cubeShader.setMVP(gl, mvp);
                cubeShader.setProjection(gl, projCopy);
                cubeShader.setWidth(gl, viewer.getWidth());
                cubeShader.setHeight(gl, viewer.getHeight());
                cubeShader.setDepth(gl, DEPTH);

                float voxelUnitSize = pa.getVoxelUnitSize();
                //float voxelUnitSize = 0.01f;
                cubeShader.setVoxelUnitSize(gl, new Vector3(voxelUnitSize, voxelUnitSize, voxelUnitSize));

                cubeShader.setDrawColor(gl, pa.getColor());
            }
        });
        cubeSequence.getActorSequence().add(pa);
        cubeSequence.setShader(cubeShader);

        // MESHES //////////////////////////////////////////////////////////////

        final ArrayMeshShader meshShader = new ArrayMeshShader(viewer.getProperties());

        meshShader.setUpdateCallback(new GLDisplayUpdateCallback() {
            @Override
            public void update(GL4 gl) {
                Matrix4 viewMatrix = viewer.getRenderer().getViewMatrix();
                meshShader.setView(gl, viewMatrix);
                Matrix4 projMatrix = viewer.getRenderer().getProjectionMatrix();
                meshShader.setProjection(gl, projMatrix);

                meshShader.setWidth(gl, viewer.getWidth());
                meshShader.setHeight(gl, viewer.getHeight());
                meshShader.setDepth(gl, DEPTH);

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

        Random rand = new Random();
        Matrix4 vertexRotation=new Matrix4();

        // Empirically derived - compatible with results of MeshLab import/export from normalized compartment coordinates
        vertexRotation.setTranspose(-1.0f,   0.0f,   0.0f,   0.5f,
                0.0f,  -1.0f,   0.0f,   0.25f,
                0.0f,   0.0f,  -1.0f,   0.625f,
                0.0f,   0.0f,   0.0f,   1.0f);

        for (File meshFile : meshFiles) {
            if (meshFile.getName().endsWith(".obj")) {
                final ArrayMeshGLActor ma = new ArrayMeshGLActor(meshFile);
                ma.setVertexRotation(vertexRotation);
                ma.setColor(new Vector4(rand.nextFloat(), rand.nextFloat(), rand.nextFloat(), 0.2f));
                ma.setUpdateCallback(new GLDisplayUpdateCallback() {
                    @Override
                    public void update(GL4 gl) {
                        Matrix4 actorModel = ma.getModel();
                        meshShader.setModel(gl, actorModel);
                        meshShader.setDrawColor(gl, ma.getColor());
                    }
                });
                logger.info("Adding actor to meshSequence for file="+meshFile.getName());
                meshSequence.getActorSequence().add(ma);
            }
        }
        meshSequence.setShader(meshShader);

        /////////////////////////////////////////////////////////////////////////


        final ArraySortShader sortShader = new ArraySortShader(viewer.getProperties());
        sortShader.setUpdateCallback(new GLDisplayUpdateCallback() {
            @Override
            public void update(GL4 gl) {
                logger.info("Calling sortShader.set* with depth="+DEPTH);
                sortShader.setWidth(gl, viewer.getWidth());
                sortShader.setHeight(gl, viewer.getHeight());
                sortShader.setDepth(gl, DEPTH);
            }
        });

        sortSequence.setShader(sortShader);

        /////////////////////////////////////////////////////////////////////////

//        viewer.addShaderAction(cubeSequence);
//        viewer.addShaderAction(meshSequence);
//        viewer.addShaderAction(sortSequence);
    }

}

