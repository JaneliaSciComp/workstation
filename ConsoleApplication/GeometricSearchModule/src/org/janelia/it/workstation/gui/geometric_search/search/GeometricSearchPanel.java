package org.janelia.it.workstation.gui.geometric_search.search;

import org.janelia.it.workstation.gui.framework.outline.Refreshable;

import javax.swing.*;

import org.janelia.it.workstation.gui.geometric_search.gl.DepthShader;
import org.janelia.it.workstation.gui.geometric_search.gl.MeshObjFileActor;
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
    Mip3d mip3d;
    //TestGLJPanel testGLJPanel;

    @Override
    public void refresh() {
        logger.info("*** refresh()");

        if ( mip3d == null ) {
            logger.warn("Have to create a new mip3d on refresh.");
            createMip3d();
        }

        mip3d.refresh();

//        if (testGLJPanel==null) {
//            createMip3d();
//        }
        //testGLJPanel.display();

    }

    @Override
    public void totalRefresh() {
        logger.info("*** totalRefresh()");
        refresh();
    }

    private void createMip3d() {
        logger.info("*** createMip3d()");

        if ( mip3d != null ) {
            mip3d.releaseMenuActions();
        }
        mip3d = new Mip3d();
        mip3d.setPreferredSize(new Dimension(1200, 900));
        mip3d.setVisible(true);
        mip3d.setResetFirstRedraw(true);

        //XrayMeshShader shader=new XrayMeshShader();
        //DepthShader shader=new DepthShader();
        //shader.addActor(new MeshObjFileActor(new File("/Users/murphys/compartment_62.obj")));


        //mip3d.addActor(shader);

        MeshObjFileActor meshActor =  new MeshObjFileActor(new File("/Users/murphys/simple_cube.obj"));
        meshActor.setDrawLines(false);
        mip3d.addActor(meshActor);

        add(mip3d, BorderLayout.CENTER);

//        if (testGLJPanel==null) {
//            testGLJPanel=new TestGLJPanel();
//            testGLJPanel.setVisible(true);
//            add(testGLJPanel, BorderLayout.CENTER);
//        }

    }

    public void displayReady() {
        logger.info("*** displayReady()");
        if (mip3d==null) {
            createMip3d();
        }
        mip3d.resetView();
        mip3d.refresh();
        logger.info("*** displayReady() done");
    }


//    protected void render( GL2 gl2, int width, int height ) {
//        gl2.glClear( gl2.GL_COLOR_BUFFER_BIT );
//
//        // draw a triangle filling the window
//        gl2.glLoadIdentity();
//        gl2.glBegin( gl2.GL_TRIANGLES );
//        gl2.glColor3f( 1, 0, 0 );
//        gl2.glVertex2f( 0, 0 );
//        gl2.glColor3f( 0, 1, 0 );
//        gl2.glVertex2f( width, 0 );
//        gl2.glColor3f( 0, 0, 1 );
//        gl2.glVertex2f( width / 2, height );
//        gl2.glEnd();
//    }


}
