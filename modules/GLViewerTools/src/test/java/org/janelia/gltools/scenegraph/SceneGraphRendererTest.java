package org.janelia.gltools.scenegraph;

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLOffscreenAutoDrawable;
import javax.media.opengl.GLProfile;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.testng.Assert.assertEquals;

/**
 *
 * @author Christopher Bruns
 */
public class SceneGraphRendererTest
{
    private GLOffscreenAutoDrawable offscreenDrawable;
    private SceneGraphRenderer renderer;
    
    public SceneGraphRendererTest()
    {
    }
    
    @BeforeClass
    public static void setUpClass()
    {
    }
    
    @AfterClass
    public static void tearDownClass()
    {
    }
    
    @Before
    public void setUp()
    {
        // Create a small offscreen drawable buffer for rendering
        final GLCapabilities caps = 
                new GLCapabilities(GLProfile.get(GLProfile.GL3));
        GLDrawableFactory factory = 
                GLDrawableFactory.getFactory(caps.getGLProfile());
        offscreenDrawable = factory.createOffscreenAutoDrawable(
                null, caps, null, 
                10, 10);

        // Create a small scene graph, including a camera
        SceneNode sceneGraph = new BasicSceneNode(null);
        CameraNode cameraNode = new PerspectiveCameraNode(sceneGraph);
        
        // Create a scene graph renderer
        RenderViewport viewport = new RenderViewport(cameraNode);
        renderer = new SceneGraphRenderer(sceneGraph, viewport);
        offscreenDrawable.addGLEventListener(renderer);
    }
    
    @After
    public void tearDown()
    {
        renderer.dispose(offscreenDrawable);
    }

    /**
     * Test of init method, of class SceneGraphRenderer.
     */
    @Test
    public void testInit()
    {
        System.out.println("init");
        renderer.init(offscreenDrawable);
        // init does nothing, so this test does nothing
    }

    /**
     * Test of display method, of class SceneGraphRenderer.
     */
    @Test
    public void testDisplay()
    {
        offscreenDrawable.display();
    }

    /**
     * Test of reshape method, of class SceneGraphRenderer.
     */
    @Test
    public void testReshape()
    {
        offscreenDrawable.setSurfaceSize(10, 10);
        
        assertEquals(offscreenDrawable.getSurfaceHeight(), 10);
        assertEquals(renderer.getViewports().get(0).getHeightPixels(), 10);
        
        offscreenDrawable.setSurfaceSize(5, 20);

        assertEquals(offscreenDrawable.getSurfaceHeight(), 20);
        assertEquals(renderer.getViewports().get(0).getHeightPixels(), 20);
    }
    
}
