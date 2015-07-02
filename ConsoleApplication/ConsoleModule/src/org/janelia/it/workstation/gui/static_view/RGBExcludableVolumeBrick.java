package org.janelia.it.workstation.gui.static_view;

import org.janelia.it.workstation.gui.static_view.shader.RGBExcludableShader;
import org.janelia.it.workstation.gui.viewer3d.VolumeModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;

/**
 * VolumeTexture class draws a transparent rectangular volume with a 3D opengl texture
 * @author brunsc
 *
 */
public class RGBExcludableVolumeBrick extends AbstractVolumeBrick
{
    public enum RenderMethod {MAXIMUM_INTENSITY, ALPHA_BLENDING}

    // Vary these parameters to taste
	// Rendering variables
	private RenderMethod renderMethod =
		RenderMethod.MAXIMUM_INTENSITY; // MIP
    private boolean bUseShader = true; // Controls whether to load and use shader program(s).

    // OpenGL state
    private boolean bSignalTextureNeedsUpload = false;

    private boolean bIsInitialized;

    private static Logger logger = LoggerFactory.getLogger( RGBExcludableVolumeBrick.class );

    public RGBExcludableVolumeBrick(VolumeModel volumeModel) {
        super( volumeModel );
        super.setShader( new RGBExcludableShader() );
    }

    @Override
	public void display(GLAutoDrawable glDrawable) {
        // Avoid carrying out operations if there is no data.
        if ( this.getTextureMediators() == null  ||  this.getTextureMediators().isEmpty() ) {
            logger.warn( "No texture for volume brick." );
            return;
        }

		if (! bIsInitialized)
			init(glDrawable);
        GL2 gl = glDrawable.getGL().getGL2();
		if (bSignalTextureNeedsUpload)
			uploadAllTextures(gl);

		// debugging objects showing useful boundaries of what we want to render
		//gl.glColor3d(1,1,1);
		// displayVoxelCenterBox(gl);
		//gl.glColor3d(1,1,0.3);
		// displayVoxelCornerBox(gl);
		// a stack of transparent slices looks like a volume
		gl.glShadeModel(GL2.GL_FLAT);
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glEnable(GL2.GL_TEXTURE_3D);

        // set blending to enable transparent voxels
        gl.glEnable(GL2.GL_BLEND);
        if (renderMethod == RenderMethod.ALPHA_BLENDING) {
            gl.glBlendEquation(GL2.GL_FUNC_ADD);
            // Weight source by GL_ONE because we are using premultiplied alpha.
            gl.glBlendFunc(GL2.GL_ONE, GL2.GL_ONE_MINUS_SRC_ALPHA);
        }
        else if (renderMethod == RenderMethod.MAXIMUM_INTENSITY) {
            gl.glBlendEquation(GL2.GL_MAX);
            gl.glBlendFunc(GL2.GL_ONE, GL2.GL_DST_ALPHA);
        }
        if (bUseShader) {
            RGBExcludableShader rgbShader = (RGBExcludableShader)getShader();
            rgbShader.setColorMask(getVolumeModel().getColorMask());
            rgbShader.load(gl);
            int vertexAttribLoc = rgbShader.getVertexAttribLoc();
            int texCoordAttribLoc = rgbShader.getTexCoordAttribLoc();
            getBufferManager().setCoordAttributeLocations( vertexAttribLoc, texCoordAttribLoc );
        }

        displayVolumeSlices(gl);
		if (bUseShader) {
            getShader().unload(gl);
        }

        gl.glDisable( GL2.GL_TEXTURE_3D );
        gl.glDisable( GL2.GL_BLEND );
	}

}
