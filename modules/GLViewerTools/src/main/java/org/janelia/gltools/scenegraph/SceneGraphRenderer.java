package org.janelia.gltools.scenegraph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.media.opengl.DebugGL3;
import javax.media.opengl.GL;
import javax.media.opengl.GL3;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;

/**
 *
 * @author Christopher Bruns
 * TODO - use this scene-graph based rendering approach for more scalable scene rendering
 * , as opposed to SceneRenderer
 */
public class SceneGraphRenderer implements GLEventListener
{
    // Handle for the scene graph itself
    private SceneNode rootNode;
    // Viewport objects: multiple, for use in stereo and minimap modes
    private List<RenderViewport> viewports = new ArrayList<RenderViewport>();

    public SceneGraphRenderer(SceneNode rootNode, RenderViewport viewport) 
    {
        this.rootNode = rootNode;
        this.viewports.add(viewport);
    }
    
    public SceneGraphRenderer(SceneNode rootNode, Collection<RenderViewport> viewports) 
    {
        this.rootNode = rootNode;
        this.viewports.addAll(viewports);
    }
    
    // getGL3 delegate method, so we can turn debug on/off here
    private static GL3 getGL3(GLAutoDrawable drawable) {
        if (drawable == null)
            return null;
        GL gl = drawable.getGL();
        if (gl == null) 
            return null;
        GL3 gl3 = gl.getGL3();
        if (gl3 == null)
            return null;
        return new DebugGL3(gl3);
    };

    public SceneNode getRootNode()
    {
        return rootNode;
    }

    public List<RenderViewport> getViewports()
    {
        return viewports;
    }
    
    @Override
    public void init(GLAutoDrawable drawable)
    {
        // For GL resources, use lazy initialization elsewhere (e.g. display()), rather than monolithic initialization here.
    }

    @Override
    public void dispose(GLAutoDrawable drawable)
    {
        if (drawable == null)
            return;
        GL3 gl = getGL3(drawable);
        // TODO - what about nodes that have been deleted previously?
        new DisposeGlVisitor(gl).visit(rootNode);
    }

    @Override
    public void display(GLAutoDrawable drawable)
    {
        GL3 gl = getGL3(drawable);
        for (RenderViewport v : viewports) { // ordinariy just one viewport
            if (v.getHeightPixels() * v.getWidthPixels() < 1)
                continue; // Too small to draw anything
            // TODO - compute viewport contribution to transform
            for (CameraNode c : v.getCameras()) { // ordinarily just one camera
                // TODO - walk camera to root node, to finish View and Projection matrices
                // TODO - cull nodes based on view frustum
                // TODO - assort all Drawables into RenderPasses
                // TODO - execute RenderPasses
            }
        }
    }
    
    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height)
    {
        for (RenderViewport v : viewports) {
            v.setHeightPixels( (int)(height*v.getHeightRelative()) );
            v.setWidthPixels( (int)(width * v.getWidthRelative()) );
            v.setOriginBottomPixels( y + (int)(height * v.getOriginBottomRelative()) );
            v.setOriginLeftPixels( x + (int)(width * v.getOriginLeftRelative()) );
        }
        // TODO - does anyone need to be notified? Or is display called automatically?
    }
    
}
