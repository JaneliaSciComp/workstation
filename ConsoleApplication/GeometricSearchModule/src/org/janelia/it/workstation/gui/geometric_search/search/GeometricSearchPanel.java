package org.janelia.it.workstation.gui.geometric_search.search;

import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.Vector3;
import org.janelia.it.workstation.gui.framework.outline.Refreshable;

import javax.media.opengl.GL4;
import javax.swing.*;

import org.janelia.it.workstation.gui.geometric_search.gl.*;
import org.janelia.it.workstation.gui.geometric_search.gl.mesh.OITMeshDrawShader;
import org.janelia.it.workstation.gui.geometric_search.gl.volume.SparseVolumeCubeActor;
import org.janelia.it.workstation.gui.geometric_search.viewer.GL4Viewer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.util.Random;
import org.janelia.geometry3d.Vector4;
import org.janelia.it.workstation.gui.geometric_search.gl.mesh.MeshObjActor;
import org.janelia.it.workstation.gui.geometric_search.gl.volume.OITCubeShader;


/**
 * Created by murphys on 3/9/15.
 */
public class GeometricSearchPanel extends JPanel implements Refreshable {

    private final Logger logger = LoggerFactory.getLogger(GeometricSearchPanel.class);
    GL4Viewer viewer;

    @Override
    public void refresh() {

        if ( viewer == null ) {
            createGL4Viewer();
        }

        viewer.refresh();
    }

    @Override
    public void totalRefresh() {
        refresh();
    }

    private void createGL4Viewer() {

        if ( viewer != null ) {
            viewer.releaseMenuActions();
        }
        viewer = new GL4Viewer();
        viewer.setPreferredSize(new Dimension(1600, 1200));
        viewer.setVisible(true);
        viewer.setResetFirstRedraw(true);
        
        //setupOITMeshExperiment();
        setupCubeExperiment();

        add(viewer, BorderLayout.CENTER);

    }

    public void displayReady() {
        if (viewer==null) {
            createGL4Viewer();
        }
        viewer.resetView();
        viewer.refresh();
    }
    
    private void setupCubeExperiment() {
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

        final SparseVolumeCubeActor pa = new SparseVolumeCubeActor(testFile, 0, 0.1f);
        
        Matrix4 gal4Rotation=new Matrix4();
      
        // Empirically derived - for GAL4 samples
        gal4Rotation.setTranspose(-1.0f,   0.0f,   0.0f,   0.5f,
                                     0.0f,  -1.0f,   0.0f,   0.25f,
                                     0.0f,   0.0f,  -1.0f,   0.625f,
                                     0.0f,   0.0f,   0.0f,   1.0f);
        pa.setModel(gal4Rotation);
        
        pa.setColor(new Vector4(1.0f, 0.0f, 0.0f, 0.2f));
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

                //float voxelUnitSize = pa.getVoxelUnitSize();
                float voxelUnitSize = 0.2f;
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
        //viewer.addShaderAction(meshSequence);
        sortSequence.setShader(new OITSortShader());
        viewer.addShaderAction(sortSequence);      
    }
    
    private void setupVolumeExperiment() {
        GL4ShaderActionSequence volumeSequence = new GL4ShaderActionSequence("Volume");
        final VolumeShader volumeShader = new VolumeShader();
        
        volumeShader.setUpdateCallback(new GLDisplayUpdateCallback() {
            @Override
            public void update(GL4 gl) {
                Matrix4 viewMatrix = viewer.getRenderer().getViewMatrix();
                volumeShader.setView(gl, viewMatrix);
                logger.info("View Matrix:\n"+viewMatrix.toString()+"\n");
                Matrix4 projMatrix = viewer.getRenderer().getProjectionMatrix();
                volumeShader.setProjection(gl, projMatrix);
                logger.info("Projection Matrix:\n"+projMatrix.toString()+"\n");               
            }
        });
                
        volumeSequence.setShader(volumeShader);
        
        //final VolumeActor volumeActor = new VolumeActor(new File("U:\\volumes\\GMR_40B09_AE_01_06-fA01b_C091216_20100427171414198.reg.local.v3dpbd"));
 
        final VolumeActor volumeActor = new VolumeActor(new File("C:\\cygwin64\\home\\murphys\\volumes\\GMR_40B09_AE_01_06-fA01b_C091216_20100427171414198.reg.local.v3dpbd"));

        volumeSequence.getActorSequence().add(volumeActor);
        
        viewer.addShaderAction(volumeSequence);
    }

    private void setupOITMeshExperiment() {

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

    private void setupTexelExperiment(GL4ShaderActionSequence actionSequence) {
        final TexelShader shader = new TexelShader();

        shader.setUpdateCallback(new GLDisplayUpdateCallback() {
            @Override
            public void update(GL4 gl) {
                int uniformLoc = gl.glGetUniformLocation(shader.getShaderProgram(), "tex");
                gl.glUniform1i(uniformLoc, 0);
            }
        });

        final TexelActor texelActor = new TexelActor();

        actionSequence.setShader(shader);
        actionSequence.getActorSequence().add(texelActor);
    }

}
