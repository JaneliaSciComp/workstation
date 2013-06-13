package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import javax.media.opengl.DefaultGLCapabilitiesChooser;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesChooser;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLPbuffer;
import javax.media.opengl.GLProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to help multiple GLJPanels share textures.
 * See http://www.java-gaming.org/index.php?topic=12781.0
 * 
 * @author brunsc
 *
 */
public class GLContextSharer
implements GLEventListener
{
	private static final Logger log = LoggerFactory.getLogger(GLContextSharer.class);

	protected GLProfile profile = null;
	protected GLCapabilities capabilities = null;
	protected GLCapabilitiesChooser chooser = null;

	// Non-resizing GLPbuffer will hold shared context
	// See http://www.java-gaming.org/index.php?topic=12781.0
	private GLPbuffer masterContextPbuffer;
	
	// OpenGL version is setup via profile
	public GLContextSharer(GLProfile profile) {
        try {
            this.profile = profile;
            capabilities = new GLCapabilities(profile);
            this.chooser = new DefaultGLCapabilitiesChooser();
        } catch ( Throwable th ) {
            profile = null;
            capabilities = null;
            log.warn("JOGL is unavailable. No viewer images will be shown.");
        }
	}
	
	GLContext getContext() {
		instantiateMaybe();
		if (masterContextPbuffer == null)
			return null;
		return masterContextPbuffer.getContext();
	}
	
	private void instantiateMaybe() {
		// Create a new context the first time this method is called
		if (masterContextPbuffer == null) {
            GLDrawableFactory factory = GLDrawableFactory.getFactory(profile);
			masterContextPbuffer = factory.createGLPbuffer(
					factory.getDefaultDevice(),
					capabilities,
					chooser,
					1, 1, // Small size that never changes
					null); // null means create a new context
			masterContextPbuffer.addGLEventListener(this);
			masterContextPbuffer.display(); // Attempt render, to force context creation.
		}		
	}

	@Override
	public void display(GLAutoDrawable arg0) {
		log.info("display");
	}

	@Override
	public void dispose(GLAutoDrawable arg0) {
		log.info("dispose");
	}

	@Override
	public void init(GLAutoDrawable arg0) {
		log.info("init");
	}

	@Override
	public void reshape(GLAutoDrawable arg0, int arg1, int arg2, int arg3,
			int arg4) {
		log.info("reshape");
	}

	public GLCapabilities getCapabilities() {
		instantiateMaybe();
		return capabilities;
	}

	public GLCapabilitiesChooser getChooser() {
		instantiateMaybe();
		return chooser;
	}
}
