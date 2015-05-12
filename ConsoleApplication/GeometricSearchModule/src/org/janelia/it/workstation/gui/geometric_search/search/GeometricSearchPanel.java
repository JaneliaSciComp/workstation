package org.janelia.it.workstation.gui.geometric_search.search;

import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.Rotation;
import org.janelia.it.workstation.gui.framework.outline.Refreshable;

import javax.media.opengl.GL3;
import javax.swing.*;

import org.janelia.it.workstation.gui.geometric_search.gl.*;
import org.janelia.it.workstation.gui.geometric_search.viewer.GL3Viewer;
import org.janelia.it.workstation.gui.viewer3d.Mip3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;


/**
 * Created by murphys on 3/9/15.
 */
public class GeometricSearchPanel extends JPanel implements Refreshable {

    private final Logger logger = LoggerFactory.getLogger(GeometricSearchPanel.class);
    GL3Viewer viewer;

    @Override
    public void refresh() {

        if ( viewer == null ) {
            createGL3Viewer();
        }

        viewer.refresh();
    }

    @Override
    public void totalRefresh() {
        refresh();
    }

    private void createGL3Viewer() {

        if ( viewer != null ) {
            viewer.releaseMenuActions();
        }
        viewer = new GL3Viewer();
        viewer.setPreferredSize(new Dimension(1200, 900));
        viewer.setVisible(true);
        viewer.setResetFirstRedraw(true);

        GL3ShaderActionSequence glSequence = new GL3ShaderActionSequence("Lattice");

        //final PassthroughShader passthroughShader = new PassthroughShader();
        final MeshObjFileV2Shader shader = new MeshObjFileV2Shader();

        shader.setUpdateCallback(new GLDisplayUpdateCallback() {
            @Override
            public void update(GL3 gl) {
                Matrix4 viewMatrix=viewer.getRenderer().getViewMatrix();
                shader.setView(gl, viewMatrix);
                Matrix4 projMatrix=viewer.getRenderer().getProjectionMatrix();
                shader.setProjection(gl, projMatrix);
            }
        });

//        final LatticeActor latticeActor = new LatticeActor();
//
//        latticeActor.setUpdateCallback(new GLDisplayUpdateCallback() {
//            @Override
//            public void update(GL3 gl) {
//                Matrix4 actorModel=latticeActor.getModel();
//                passthroughShader.setModel(gl, actorModel);
//            }
//        });

        final MeshObjFileV2Actor meshActor = new MeshObjFileV2Actor(new File("/Users/murphys/compartment_62.obj"));

        meshActor.setUpdateCallback(new GLDisplayUpdateCallback() {
            @Override
            public void update(GL3 gl) {
                Matrix4 actorModel = meshActor.getModel();
                shader.setModel(gl, actorModel);
            }
        });


        glSequence.setShader(shader);
//        glSequence.getActorSequence().add(latticeActor);
        glSequence.getActorSequence().add(meshActor);

        logger.info("Adding glSequence...");
        viewer.addShaderAction(glSequence);


        ///////////////////////////////////////////////////////////////////////////////

//        GL3ShaderActionSequence meshSequence=new GL3ShaderActionSequence("Mesh");
//        final MeshObjFileV2Shader meshShader = new MeshObjFileV2Shader();
//
//        meshShader.setUpdateCallback(new GLDisplayUpdateCallback() {
//            @Override
//            public void update(GL3 gl) {
//                Matrix4 viewMatrix=viewer.getRenderer().getViewMatrix();
//                meshShader.setView(gl, viewMatrix);
//                Matrix4 projMatrix=viewer.getRenderer().getProjectionMatrix();
//                meshShader.setProjection(gl, projMatrix);
//            }
//        });
//
//        final MeshObjFileV2Actor meshActor =  new MeshObjFileV2Actor(new File("/Users/murphys/simple_cube.obj"));
//
//        meshActor.setUpdateCallback(new GLDisplayUpdateCallback() {
//            @Override
//            public void update(GL3 gl) {
//                Matrix4 actorModel=meshActor.getModel();
//                meshShader.setModel(gl, actorModel);
//            }
//        });
//
//        //MeshObjFileActor meshActor =  new MeshObjFileActor(new File("/Users/murphys/compartment_62.obj"));
//        meshActor.setDrawLines(false);
//
//        meshSequence.setShader(meshShader);
//        meshSequence.getActorSequence().add(meshActor);
//
//        viewer.addShaderAction(meshSequence);

        add(viewer, BorderLayout.CENTER);

    }

    public void displayReady() {
        if (viewer==null) {
            createGL3Viewer();
        }
        viewer.resetView();
        viewer.refresh();
    }

}
