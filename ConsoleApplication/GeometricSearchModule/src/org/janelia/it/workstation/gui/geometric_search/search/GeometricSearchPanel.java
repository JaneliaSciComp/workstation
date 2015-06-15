package org.janelia.it.workstation.gui.geometric_search.search;

import org.janelia.geometry3d.Matrix4;
import org.janelia.it.workstation.gui.framework.outline.Refreshable;

import javax.media.opengl.GL4;
import javax.swing.*;

import org.janelia.it.workstation.gui.geometric_search.gl.*;
import org.janelia.it.workstation.gui.geometric_search.viewer.GL4Viewer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.janelia.geometry3d.Vector4;


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

        setupOITMeshExperiment();

//        GL4ShaderActionSequence actionSequence = new GL4ShaderActionSequence("Experimental Shader Action Sequence");
//
//        setupTexelExperiment(actionSequence);
//
//        logger.info("Adding glSequence...");
//        viewer.addShaderAction(actionSequence);

        add(viewer, BorderLayout.CENTER);

    }

    public void displayReady() {
        if (viewer==null) {
            createGL4Viewer();
        }
        viewer.resetView();
        viewer.refresh();
    }

    private void setupOITMeshExperiment() {

        // First create the OITMeshDrawShader
        GL4ShaderActionSequence drawSequence = new GL4ShaderActionSequence("Draw Phase");
        GL4ShaderActionSequence sortSequence = new GL4ShaderActionSequence("Sort Phase");

        final OITMeshDrawShader drawShader = new OITMeshDrawShader();
        final OITMeshSortShader sortShader = new OITMeshSortShader();

        // Setup Draw Shader  //////////////////////////////


       drawShader.setUpdateCallback(new GLDisplayUpdateCallback() {
            @Override
            public void update(GL4 gl) {
                Matrix4 viewMatrix = viewer.getRenderer().getViewMatrix();
                drawShader.setView(gl, viewMatrix);
                Matrix4 projMatrix = viewer.getRenderer().getProjectionMatrix();
                drawShader.setProjection(gl, projMatrix);
            }
        });

        
        File meshDir = new File("U:\\meshes");
        
        File[] meshFiles = meshDir.listFiles();
        
        drawSequence.setShader(drawShader);
        
        Random rand = new Random();
        for (File meshFile : meshFiles) {
            if (meshFile.getName().endsWith(".obj")) {
                final MeshObjFileV2Actor ma = new MeshObjFileV2Actor(meshFile);
                ma.setColor(new Vector4(rand.nextFloat(), rand.nextFloat(), rand.nextFloat(), 0.5f));
                ma.setUpdateCallback(new GLDisplayUpdateCallback() {
                    @Override
                    public void update(GL4 gl) {
                        Matrix4 actorModel = ma.getModel();
                        drawShader.setModel(gl, actorModel);
                        drawShader.setDrawColor(gl, ma.getColor());
                    }
                });
                drawSequence.getActorSequence().add(ma);
            }
        }

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

    private void setupMeshExperiment(GL4ShaderActionSequence actionSequence) {
        final MeshObjFileV2Shader shader = new MeshObjFileV2Shader();

        shader.setUpdateCallback(new GLDisplayUpdateCallback() {
            @Override
            public void update(GL4 gl) {
                Matrix4 viewMatrix=viewer.getRenderer().getViewMatrix();
                shader.setView(gl, viewMatrix);
                Matrix4 projMatrix=viewer.getRenderer().getProjectionMatrix();
                shader.setProjection(gl, projMatrix);
            }
        });

        final MeshObjFileV2Actor meshActor1 = new MeshObjFileV2Actor(new File("/Users/murphys/meshes/compartment_62.obj"));

        meshActor1.setUpdateCallback(new GLDisplayUpdateCallback() {
            @Override
            public void update(GL4 gl) {
                Matrix4 actorModel = meshActor1.getModel();
                shader.setModel(gl, actorModel);
            }
        });

        final MeshObjFileV2Actor meshActor2 = new MeshObjFileV2Actor(new File("/Users/murphys/meshes/compartment_39.obj"));

        meshActor2.setUpdateCallback(new GLDisplayUpdateCallback() {
            @Override
            public void update(GL4 gl) {
                Matrix4 actorModel = meshActor2.getModel();
                shader.setModel(gl, actorModel);
            }
        });

        actionSequence.setShader(shader);
        actionSequence.getActorSequence().add(meshActor1);
        actionSequence.getActorSequence().add(meshActor2);
    }

}
