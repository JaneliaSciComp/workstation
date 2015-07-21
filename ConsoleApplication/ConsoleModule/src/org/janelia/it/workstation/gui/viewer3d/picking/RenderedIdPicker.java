/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.gui.viewer3d.picking;

//import javax.media.opengl.GL2GL3;
import java.nio.ByteBuffer;
import javax.media.opengl.GL3;
import javax.media.opengl.GLAutoDrawable;

import java.nio.IntBuffer;
import static org.janelia.it.workstation.gui.viewer3d.OpenGLUtils.reportError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Setup a buffer for IDs-as-renders, object selection.  Support
 * carrying out the prep, teardown, and actual selection.  Meant
 * to be called from a GLActor.
 * 
 * @see http://www.opengl-tutorial.org/intermediate-tutorials/tutorial-14-render-to-texture/
 *
 * @author fosterl
 */
public class RenderedIdPicker {
	public static final int BYTES_PER_PIXEL = 4;
	public static final int BYTE_MULT = 256;
	public static final int WORD_MULT = 256 * 256;
	private static final int UNSET_COORD = -1;

	private int frameBufId;
    private int colorTextureId_0;
    private int colorTextureId_1;
    private int depthBufferId;
    private int viewportWidth;
    private int viewportHeight;
    private IdCoderProvider idCoderProvider;
	private PixelListener listener;
	
	private int x = UNSET_COORD;
	private int y = UNSET_COORD;
	
	private Logger logger = LoggerFactory.getLogger(RenderedIdPicker.class);

    public RenderedIdPicker(IdCoderProvider idCoderProvider) {
        this.idCoderProvider = idCoderProvider;
    }
    
    /**
     * Call this for the one-time-only steps.
	 * @see https://www.opengl.org/wiki/Framebuffer_Object_Examples
     * @param glDrawable 
     */
    public void init(GLAutoDrawable glDrawable) {        
		this.viewportWidth = glDrawable.getWidth();
		this.viewportHeight = glDrawable.getHeight();				

        logger.info("Establishing width={}, and height={}.", viewportWidth, viewportHeight);
		GL3 gl = (GL3)glDrawable.getGL().getGL2();
        
		// Test the version.
		logger.info("OpenGL version {}.", gl.glGetString(GL3.GL_VERSION));

		/*
		  The steps:
		  - get an FBO
		  - bind to FBO
		  - set its viewport
		  - get attachments: 2 for color, 1 for depth.
		  - attach all said attachments to the framebuffer.
		  - check status of whole FBO: is it complete?
		*/
        // The framebuffer, which regroups 0, 1, or more textures, and 0 or 1 depth buffer.
        IntBuffer exchange = IntBuffer.allocate(3);
        exchange.rewind();
        gl.glGenFramebuffers(1, exchange);
        exchange.rewind();
        frameBufId = exchange.get();
        		
        gl.glBindFramebuffer(GL3.GL_FRAMEBUFFER, frameBufId);
		reportError(gl, "Binding Framebuffer");
		
		gl.glViewport(0, 0, viewportWidth, viewportHeight);
		reportError(gl, "Viewport for Framebuffer");

		// Attach the renderbuffers.
		exchange.rewind();
		gl.glGenRenderbuffers(1, exchange);
		reportError(gl, "Gen render Buffer");
		depthBufferId = exchange.get();
		
		exchange.rewind();
		gl.glGenTextures(2, exchange);
		reportError(gl, "Gen render textures");
		colorTextureId_0 = exchange.get();
		colorTextureId_1 = exchange.get();
		
		// Establish the color buffer at attachment 0.
	    gl.glBindTexture(GL3.GL_TEXTURE_2D, colorTextureId_0);
		reportError(gl, "Bind Color-0 Render Buffer");
		gl.glTexParameteri(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_WRAP_S, GL3.GL_REPEAT);
		gl.glTexParameteri(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_WRAP_T, GL3.GL_REPEAT);
		gl.glTexParameteri(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_MIN_FILTER, GL3.GL_NEAREST);
		gl.glTexParameteri(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_MAG_FILTER, GL3.GL_NEAREST);
		reportError(gl, "Render-Buffer Color-0 parameters");		
		//NULL means reserve texture memory, but texels are undefined
		gl.glTexImage2D(GL3.GL_TEXTURE_2D, 0, GL3.GL_RGBA8, viewportWidth, viewportHeight, 0, GL3.GL_BGRA, GL3.GL_UNSIGNED_BYTE, null);
		reportError(gl, "Render-Buffer Color-0 teximage");		
        gl.glFramebufferTexture2D(GL3.GL_FRAMEBUFFER, GL3.GL_COLOR_ATTACHMENT0, GL3.GL_TEXTURE_2D, colorTextureId_0, 0);
		reportError(gl, "Render-Buffer Color-0 Attachment");		
		
		// Establish depth render buffer.
		gl.glBindRenderbuffer(GL3.GL_RENDERBUFFER, depthBufferId);
		reportError(gl, "Bind Depth Render Buffer");
        gl.glRenderbufferStorage(GL3.GL_RENDERBUFFER, GL3.GL_DEPTH_COMPONENT24, viewportWidth, viewportHeight);
		reportError(gl, "Render-Buffer Depth Attachment");		
		gl.glFramebufferRenderbuffer(GL3.GL_FRAMEBUFFER, GL3.GL_DEPTH_ATTACHMENT, GL3.GL_RENDERBUFFER, depthBufferId);
		reportError(gl, "Render Buffer Storage-0");
		
        // Establish the color buffer at attachment 1.
        gl.glBindTexture(GL3.GL_TEXTURE_2D, colorTextureId_1);
        reportError(gl, "Bind Color-1 Render Buffer");
        gl.glTexParameteri(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_WRAP_S, GL3.GL_REPEAT);
        gl.glTexParameteri(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_WRAP_T, GL3.GL_REPEAT);
        gl.glTexParameteri(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_MIN_FILTER, GL3.GL_NEAREST);
        gl.glTexParameteri(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_MAG_FILTER, GL3.GL_NEAREST);
        reportError(gl, "Render-Buffer Color-1 parameters");
        //NULL means reserve texture memory, but texels are undefined
        gl.glTexImage2D(GL3.GL_TEXTURE_2D, 0, GL3.GL_RGBA8, viewportWidth, viewportHeight, 0, GL3.GL_BGRA, GL3.GL_UNSIGNED_BYTE, null);
        reportError(gl, "Render-Buffer Color-1 teximage");
        gl.glFramebufferTexture2D(GL3.GL_FRAMEBUFFER, GL3.GL_COLOR_ATTACHMENT1, GL3.GL_TEXTURE_2D, colorTextureId_1, 0);
        reportError(gl, "Render-Buffer Color-1 Attachment");
		
		int status = gl.glCheckFramebufferStatus(GL3.GL_FRAMEBUFFER);
		if (status != GL3.GL_FRAMEBUFFER_COMPLETE) {
			logger.error("Failed to establish framebuffer: {}", decodeFramebufferStatus(status));
		}
		else {
			logger.info("Framebuffer complete.");
		}
		reportError(gl, "Frame Render Buffer");
        
		gl.glBindFramebuffer(GL3.GL_FRAMEBUFFER, 0);
        
	}
	
	public void setPickCoords(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public void prePick(GLAutoDrawable glDrawable) {
		if (! inPick()) {
			return;
		}
        init(glDrawable);
		/*
		   Bind the FBO, so that all drawing is done to IT, not usual
		   default framebuffer.
		*/
		GL3 gl = (GL3)glDrawable.getGL().getGL2();
        gl.glBindFramebuffer(GL3.GL_FRAMEBUFFER, frameBufId);
		reportError(gl, "Binding Frame Buffer");

		// A last clear op.
		//  PROOF: something going into the buffer. gl.glClearColor(1.0f, 0.5f, 0.25f, 1.0f);
		gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		gl.glClearDepth(1.0f);
		gl.glClear(GL3.GL_COLOR_BUFFER_BIT | GL3.GL_DEPTH_BUFFER_BIT);
		reportError(gl, "Clearing FBO");

		gl.glViewport(0, 0, viewportWidth, viewportHeight);
		reportError(gl, "Viewport for Framebuffer");
		
        // Setting up the draw-buffers.
        //   This step is what associates "gl_FragData[n]" at the GLSL
        //   shader, with targets.  By default gl_FragData[0] is the
        //   attachment0 color buffer.  Here, we add gl_FragData[1]'s
        //   association with attachment1.
        int[] drawBuffersTargets = new int[]{
            GL3.GL_COLOR_ATTACHMENT0,
            GL3.GL_COLOR_ATTACHMENT1,
        };
        IntBuffer exchange = IntBuffer.allocate(2);
        exchange.rewind();
        exchange.put(drawBuffersTargets);
        exchange.rewind();
        gl.glDrawBuffers(drawBuffersTargets.length, exchange);

	}
	
    public void postPick(GLAutoDrawable glDrawable) {
		if (! inPick()) {
			return;
		}
		GL3 gl = (GL3)glDrawable.getGL().getGL2();
		gl.glBindFramebuffer(GL3.GL_READ_FRAMEBUFFER, frameBufId);
		//pixelReadTest(gl, GL3.GL_COLOR_ATTACHMENT0, colorTextureId_0);
        //pixelReadTest(gl, GL3.GL_COLOR_ATTACHMENT1, colorTextureId_1);
        int yPos = viewportHeight - y;
		byte[] pixels = readPixels(gl, colorTextureId_1, GL3.GL_COLOR_ATTACHMENT1, x, yPos, 1, 1);
		int id = getId(pixels);
        gl.glBindFramebuffer(GL3.GL_FRAMEBUFFER, 0);
		reportError(gl, "Unbind Frame Buffer");
		
		if (listener != null) {
			x = UNSET_COORD;
			y = UNSET_COORD;
			listener.setPixel(id);
		}
		
    }

	public void dispose(GLAutoDrawable glDrawable) {
		if (! inPick()) {
			return;
		}
		GL3 gl = (GL3)glDrawable.getGL().getGL2();
		IntBuffer exchange = IntBuffer.allocate(2);

		exchange.rewind();
		exchange.put(colorTextureId_0);
		exchange.put(colorTextureId_1);
		exchange.rewind();
		gl.glDeleteTextures(2, exchange);
		reportError(gl, "Delete Textures");

		exchange.rewind();
		exchange.put(depthBufferId);
		exchange.rewind();
		gl.glDeleteRenderbuffers(1, exchange);
		reportError(gl, "Delete Render Buffers");

	    exchange.rewind();
		exchange.put(frameBufId);
		exchange.rewind();
		gl.glDeleteFramebuffers(1, exchange);
		reportError(gl, "Delete Frame Buffers");
	}
	
	public void setPixelListener(PixelListener listener) {
		this.listener = listener;
	}
	
	private boolean inPick() {
		return x != UNSET_COORD  &&  y != UNSET_COORD;
	}
	
	private void prepareTexture(GL3 gl, int texId) {
		// "Bind" the newly created texture : all future texture functions will modify this texture
		gl.glBindTexture(GL3.GL_TEXTURE_2D, texId);
		reportError("Binding Texture ", gl.getGL2(), texId);
		gl.glTexParameteri(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_WRAP_S, GL3.GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_WRAP_T, GL3.GL_CLAMP_TO_BORDER);
		gl.glTexParameteri(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_MIN_FILTER, GL3.GL_NEAREST);
		gl.glTexParameteri(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_MAG_FILTER, GL3.GL_NEAREST);
		reportError(gl, "Texture Characteristics");
		// Give an empty image to OpenGL ( the last "0" )
		gl.glTexImage2D(GL3.GL_TEXTURE_2D, 0, GL3.GL_RGBA, viewportWidth, viewportHeight, 0, GL3.GL_RGBA, GL3.GL_UNSIGNED_BYTE, null);
		reportError("Frame Buffer Texture", gl.getGL2(), texId);
	}

	private void pixelReadTest(GL3 gl, int attachment, int textureId) {
        System.out.println("Pixel Read Test for " + attachment);
		byte[] rawBuffer = readPixels(gl, textureId, attachment);
		int[] freq = new int[256];
		for (int i = 0; i < rawBuffer.length; i++) {
			int b = rawBuffer[i];
			if (b < 0) {
				b += 256;
			}
			freq[b]++;
		}
		for (int i = 0; i < freq.length; i+=BYTES_PER_PIXEL) {
			if (freq[i] > 0) {
                // Dump the identifier implied here, and its frequency.
                IdCoder idCoder = idCoderProvider.getIdCoder();
                float idFloat = i / 256.0f;
                int id = idCoder.decode(idFloat);
                System.out.println("ID=" + id + ", frequency of character " + i + "=" + freq[i]);
			}
		}
		reportError(gl, "Pixel Read Test.");
	}

	private byte[] readPixels(GL3 gl, int textureId, int attachment) {
        return readPixels(gl, textureId, attachment, 0, 0, viewportWidth, viewportHeight);
	}
	
    private byte[] readPixels(GL3 gl, int textureId, int attachment, int startX, int startY, int width, int height) {
        gl.glBindTexture(GL3.GL_TEXTURE_2D, textureId);
        int pixelSize = BYTES_PER_PIXEL; // * (Float.SIZE / Byte.SIZE)
        int bufferSize = width * height * pixelSize;
        byte[] rawBuffer = new byte[bufferSize];
        ByteBuffer buffer = ByteBuffer.wrap(rawBuffer);
        gl.glReadBuffer(attachment);
        gl.glReadPixels(startX, startY, width, height, GL3.GL_BGRA, GL3.GL_UNSIGNED_BYTE, buffer);
        return rawBuffer;
    }

	private int getId(int x, int y, byte[] rawBuffer) {
        int vertPos = viewportHeight - y;
		int startOfPixel = (vertPos * viewportWidth + x) * BYTES_PER_PIXEL;
	    // Using BGRA order.
		return rawBuffer[startOfPixel] + BYTE_MULT * rawBuffer[startOfPixel + 1] + WORD_MULT * rawBuffer[startOfPixel + 2];
	}
	
    private int getId(byte[] rawBuffer) {
        // Using BGRA order.
        return rawBuffer[1] + BYTE_MULT * rawBuffer[2] + WORD_MULT * rawBuffer[3];
    }

	private String decodeFramebufferStatus( int status ) {
		String rtnVal = null;
		switch (status) {			
			case GL3.GL_FRAMEBUFFER_UNDEFINED:
				rtnVal = "GL_FRAMEBUFFER_UNDEFINED means target is the default framebuffer, but the default framebuffer does not exist.";
				break;
			case GL3.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT :
				rtnVal = "GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT is returned if any of the framebuffer attachment points are framebuffer incomplete.";
				break;
			case GL3.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT:
				rtnVal = "GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT is returned if the framebuffer does not have at least one image attached to it.";
				break;
			case GL3.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER:
				rtnVal = "GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER is returned if the value of GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE\n" +
"		 is GL_NONE for any color attachment point(s) named by GL_DRAW_BUFFERi.";
				break;
			case GL3.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER:
				rtnVal = "GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER is returned if GL_READ_BUFFER is not GL_NONE\n" +
"		 and the value of GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE is GL_NONE for the color attachment point named\n" +
"		 by GL_READ_BUFFER.";
				break;
			case GL3.GL_FRAMEBUFFER_UNSUPPORTED:
				rtnVal = "GL_FRAMEBUFFER_UNSUPPORTED is returned if the combination of internal formats of the attached images violates\n" +
"		 an implementation-dependent set of restrictions.";
				break;
			case GL3.GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE:
				rtnVal = "GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE is returned if the value of GL_RENDERBUFFER_SAMPLES is not the same\n" +
"		 for all attached renderbuffers; if the value of GL_TEXTURE_SAMPLES is the not same for all attached textures; or, if the attached\n" +
"		 images are a mix of renderbuffers and textures, the value of GL_RENDERBUFFER_SAMPLES does not match the value of\n" +
"		 GL_TEXTURE_SAMPLES.  GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE  also returned if the value of GL_TEXTURE_FIXED_SAMPLE_LOCATIONS is\n" +
"		 not the same for all attached textures; or, if the attached images are a mix of renderbuffers and textures, the value of GL_TEXTURE_FIXED_SAMPLE_LOCATIONS\n" +
"		 is not GL_TRUE for all attached textures.";
				break;
			case GL3.GL_FRAMEBUFFER_INCOMPLETE_LAYER_TARGETS:
				rtnVal = " is returned if any framebuffer attachment is layered, and any populated attachment is not layered,\n" +
"		 or if all populated color attachments are not from textures of the same target.";
				break;
			default:
				rtnVal = "--Message not decoded: " + status;
		}
		return rtnVal;
	}
	
	/**
	 * Implement this and set it, to be informed of the captured pixel value.
	 */
	public static interface PixelListener {
		void setPixel(int pixel);
	}	
}
