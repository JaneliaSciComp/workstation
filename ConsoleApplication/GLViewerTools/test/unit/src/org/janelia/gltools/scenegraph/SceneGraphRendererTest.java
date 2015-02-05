/*
 * Licensed under the Janelia Farm Research Campus Software Copyright 1.1
 * 
 * Copyright (c) 2014, Howard Hughes Medical Institute, All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *     1. Redistributions of source code must retain the above copyright notice, 
 *        this list of conditions and the following disclaimer.
 *     2. Redistributions in binary form must reproduce the above copyright 
 *        notice, this list of conditions and the following disclaimer in the 
 *        documentation and/or other materials provided with the distribution.
 *     3. Neither the name of the Howard Hughes Medical Institute nor the names 
 *        of its contributors may be used to endorse or promote products derived 
 *        from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, ANY 
 * IMPLIED WARRANTIES OF MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A 
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * REASONABLE ROYALTIES; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

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
        renderer = new SceneGraphRenderer();
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
        offscreenDrawable.setSize(5, 20);
    }
    
}
