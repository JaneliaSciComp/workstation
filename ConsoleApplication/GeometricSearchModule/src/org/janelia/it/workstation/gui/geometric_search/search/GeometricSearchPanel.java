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
        viewer.setPreferredSize(new Dimension(1200, 900));
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
                sortShader.setHeadPointerTextureId(drawShader.getHeadPointerTextureId());
                sortShader.setFragmentStorageBufferId(drawShader.getFragmentStorageBufferId());

                Matrix4 viewMatrix = viewer.getRenderer().getViewMatrix();
                drawShader.setView(gl, viewMatrix);
                Matrix4 projMatrix = viewer.getRenderer().getProjectionMatrix();
                drawShader.setProjection(gl, projMatrix);
            }
        });

        final MeshObjFileV2Actor meshActor1 = new MeshObjFileV2Actor(new File("/Users/murphys/meshes/compartment_62.obj"));

        meshActor1.setUpdateCallback(new GLDisplayUpdateCallback() {
            @Override
            public void update(GL4 gl) {
                Matrix4 actorModel = meshActor1.getModel();
                drawShader.setModel(gl, actorModel);
            }
        });

        final MeshObjFileV2Actor meshActor2 = new MeshObjFileV2Actor(new File("/Users/murphys/meshes/compartment_39.obj"));

        meshActor2.setUpdateCallback(new GLDisplayUpdateCallback() {
            @Override
            public void update(GL4 gl) {
                Matrix4 actorModel = meshActor2.getModel();
                drawShader.setModel(gl, actorModel);
            }
        });

        drawSequence.setShader(drawShader);
        drawSequence.getActorSequence().add(meshActor1);
        drawSequence.getActorSequence().add(meshActor2);

        viewer.addShaderAction(drawSequence);

        // Setup Sort Shader ///////////////////////////////////////

        sortSequence.setShader(sortShader);
        viewer.addShaderAction(sortSequence);

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
