package org.janelia.it.workstation.gui.geometric_search.gl.experiment;

import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.Vector3;
import org.janelia.geometry3d.Vector4;
import org.janelia.it.workstation.gui.geometric_search.gl.GL4ShaderActionSequence;
import org.janelia.it.workstation.gui.geometric_search.gl.GLDisplayUpdateCallback;
import org.janelia.it.workstation.gui.geometric_search.gl.OITSortShader;
import org.janelia.it.workstation.gui.geometric_search.gl.mesh.MeshObjActor;
import org.janelia.it.workstation.gui.geometric_search.gl.mesh.OITMeshDrawShader;
import org.janelia.it.workstation.gui.geometric_search.gl.volume.OITCubeShader;
import org.janelia.it.workstation.gui.geometric_search.gl.volume.SparseVolumeCubeActor;
import org.janelia.it.workstation.gui.geometric_search.viewer.VoxelViewerGLPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.opengl.GL4;
import java.io.File;
import java.util.Random;

/**
 * Created by murphys on 7/29/2015.
 */
public class OITCubeExperiment implements GL4Experiment {
    private final Logger logger = LoggerFactory.getLogger(OITCubeExperiment.class);

    public void setup(final VoxelViewerGLPanel viewer) {
        GL4ShaderActionSequence cubeSequence = new GL4ShaderActionSequence("Cube");
        GL4ShaderActionSequence meshSequence = new GL4ShaderActionSequence("Meshes");
        GL4ShaderActionSequence sortSequence = new GL4ShaderActionSequence("Sort Phase");

        // VOLUME //////////////////////////////////////////////////////////////

        final OITCubeShader cubeShader = new OITCubeShader();

        cubeShader.setUpdateCallback(new GLDisplayUpdateCallback() {
            @Override
            public void update(GL4 gl) {
                // Do nothing since we want to update MVP at model level
            }
        });

        File testHomeFile = new File("C:\\cygwin64\\home\\murphys\\volumes\\GMR_40B09_AE_01_06-fA01b_C091216_20100427171414198.reg.local.v3dpbd");
        File testJaneliaFile = new File("U:\\volumes\\GMR_40B09_AE_01_06-fA01b_C091216_20100427171414198.reg.local.v3dpbd");
        File testFile = null;

        if (testHomeFile.exists()) {
            testFile = testHomeFile;
        } else {
            testFile = testJaneliaFile;
        }

        final SparseVolumeCubeActor pa = new SparseVolumeCubeActor(testFile, 1, 0.35f);

        Matrix4 gal4Rotation=new Matrix4();

        // Empirically derived - for GAL4 samples
        gal4Rotation.setTranspose(-1.0f,   0.0f,   0.0f,   0.5f,
                0.0f,  -1.0f,   0.0f,   0.25f,
                0.0f,   0.0f,  -1.0f,   0.625f,
                0.0f,   0.0f,   0.0f,   1.0f);
        pa.setModel(gal4Rotation);

        pa.setColor(new Vector4(1.0f, 0.0f, 0.0f, 0.01f));
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

                float voxelUnitSize = pa.getVoxelUnitSize();
                //float voxelUnitSize = 0.2f;
                cubeShader.setVoxelUnitSize(gl, new Vector3(voxelUnitSize, voxelUnitSize, voxelUnitSize));

                cubeShader.setDrawColor(gl, pa.getColor());
            }
        });
        cubeSequence.getActorSequence().add(pa);
        cubeSequence.setShader(cubeShader);


        // MESHES //////////////////////////////////////////////////////////////

        final OITMeshDrawShader drawShader = new OITMeshDrawShader();

        drawShader.setUpdateCallback(new GLDisplayUpdateCallback() {
            @Override
            public void update(GL4 gl) {
                Matrix4 viewMatrix = viewer.getRenderer().getViewMatrix();
                drawShader.setView(gl, viewMatrix);
                Matrix4 projMatrix = viewer.getRenderer().getProjectionMatrix();
                drawShader.setProjection(gl, projMatrix);
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
                final MeshObjActor ma = new MeshObjActor(meshFile);
                ma.setVertexRotation(vertexRotation);
                ma.setColor(new Vector4(rand.nextFloat(), rand.nextFloat(), rand.nextFloat(), 0.5f));
                ma.setUpdateCallback(new GLDisplayUpdateCallback() {
                    @Override
                    public void update(GL4 gl) {
                        Matrix4 actorModel = ma.getModel();
                        drawShader.setModel(gl, actorModel);
                        drawShader.setDrawColor(gl, ma.getColor());
                    }
                });
                meshSequence.getActorSequence().add(ma);
            }
        }
        meshSequence.setShader(drawShader);

        /////////////////////////////////////////////////////////////////////////

        viewer.addShaderAction(cubeSequence);
        viewer.addShaderAction(meshSequence);
        sortSequence.setShader(new OITSortShader());
        viewer.addShaderAction(sortSequence);
    }


}
