package org.janelia.it.workstation.gui.passive_3d;

import org.janelia.it.workstation.gui.static_view.*;
import org.janelia.it.workstation.gui.passive_3d.shader.SnapshotShader;
import org.janelia.it.workstation.gui.viewer3d.VolumeModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;

import java.awt.Color;
import java.util.Collection;
import java.util.Iterator;
import org.janelia.it.workstation.gui.large_volume_viewer.ChannelColorModel;
import org.janelia.it.workstation.gui.large_volume_viewer.ImageColorModel;
import org.janelia.it.workstation.gui.viewer3d.texture.TextureDataI;
import org.janelia.it.workstation.gui.viewer3d.texture.TextureMediator;

/**
 * VolumeTexture class draws a transparent rectangular volume with a 3D opengl texture
 * @author brunsc
 *
 */
public class SnapshotVolumeBrick extends AbstractVolumeBrick
{
    public enum RenderMethod {MAXIMUM_INTENSITY, ALPHA_BLENDING}

	private ImageColorModel imageColorModel;
    private TextureMediator interleavedTextureMediator;
    private boolean itmNeedsUploaded = false;
    
    // Vary these parameters to taste
	// Rendering variables
	private RenderMethod renderMethod =
		RenderMethod.MAXIMUM_INTENSITY; // MIP
    private boolean bUseShader = true; // Controls whether to load and use shader program(s).
    private int[] svbTextureIds = null;

    /**
     * Size of our opengl texture, which might be padded with extra voxels
     * to reach a multiple of 8
     */
    private boolean bIsInitialized;    

    private static Logger logger = LoggerFactory.getLogger( SnapshotVolumeBrick.class );

    public SnapshotVolumeBrick(VolumeModel volumeModel) {
        super( volumeModel );
        super.setShader( new SnapshotShader() );
    }
    
    public void setImageColorModel( ImageColorModel imageColorModel ) {
        this.imageColorModel = imageColorModel;
    }
    
    public void setTextureDatas(Collection<TextureDataI> textureDatas) {
        if ( textureDatas == null ) {
            return;
        }
        Iterator<TextureDataI> textureDataIterator = textureDatas.iterator();
        if ( textureDatas.size() >= 1 ) {
            setTextureData( textureDataIterator.next() );
        }
        if ( textureDatas.size() >= 2 ) {
            svbTextureIds = new int[1];
            interleavedTextureMediator = new TextureMediator();
            interleavedTextureMediator.setTextureData( textureDataIterator.next() );
            itmNeedsUploaded = true;
            super.textureMediators.add( interleavedTextureMediator );
        }
    }    
    
    @Override
    public void init( GLAutoDrawable glDrawable ) {
        getShader().setSignalTextureMediator(getSignalTextureMediator());
        super.init( glDrawable );
        logger.info("Initializing....");
        if ( interleavedTextureMediator != null ) {
            final GL2 gl = glDrawable.getGL().getGL2();
            svbTextureIds = TextureMediator.genTextureIds( gl, textureMediators.size() );
            // NOTE: wish to avoid pushing texture ids past gaps.
            // Therefore, using signal-tex + 1, here. This brick
            // is not using the masking texture.
            interleavedTextureMediator.init( svbTextureIds[ 0 ], TextureMediator.SIGNAL_TEXTURE_OFFSET+1 );
            uploadInterleavedTexture(gl);
        }
        bIsInitialized = true;
    }

    private void uploadInterleavedTexture(final GL2 gl) {
        if ( itmNeedsUploaded ) {
            interleavedTextureMediator.deleteTexture(gl);
            interleavedTextureMediator.uploadTexture(gl);
            itmNeedsUploaded = false;
        }
    }

    @Override
	public void display(GLAutoDrawable glDrawable) {
        // Avoid carrying out operations if there is no data.
        if ( getSignalTextureMediator() == null ) {
            logger.warn( "No texture for volume brick." );
            return;
        }

		if (! bIsInitialized)
			init(glDrawable);
        GL2 gl = glDrawable.getGL().getGL2();
		if (bSignalTextureNeedsUpload)
			uploadSignalTexture(gl);
        uploadInterleavedTexture(gl);

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
            SnapshotShader snapshotShader = (SnapshotShader) getShader();
            snapshotShader.load(gl);
            pushValuesToShader( gl, snapshotShader );

            int vertexAttribLoc = snapshotShader.getVertexAttribLoc();
            int texCoordAttribLoc = snapshotShader.getTexCoordAttribLoc();
            getBufferManager().setCoordAttributeLocations(vertexAttribLoc, texCoordAttribLoc);
        }

        displayVolumeSlices(gl);
		if (bUseShader) {
            getShader().unload(gl);
        }

        gl.glDisable( GL2.GL_TEXTURE_3D );
        gl.glDisable( GL2.GL_BLEND );
	}
    
    @Override
	public void dispose(GLAutoDrawable glDrawable) {
        if ( interleavedTextureMediator != null ) {
            interleavedTextureMediator.deleteTexture(glDrawable.getGL().getGL2());
        }
        svbTextureIds = null;
    }

    private void pushValuesToShader(GL2 gl, SnapshotShader snapshotShader) {
		int sc = imageColorModel.getChannelCount();

        float[] channelColor
                = {0, 0, 0,
                   0, 0, 0,
                   0, 0, 0,
                   0, 0, 0};
        float[] channelGamma = {1, 1, 1, 1};
        float[] channelMin = {0, 0, 0, 0};
        float[] channelScale = {1, 1, 1, 1};
        for (int c = 0; c < sc; ++c) {
            int offset = 3 * c;
            ChannelColorModel ccm = imageColorModel.getChannel(c);
            Color col = ccm.getColor();
            channelColor[offset + 0] = col.getRed() / 255.0f;
            channelColor[offset + 1] = col.getGreen() / 255.0f;
            channelColor[offset + 2] = col.getBlue() / 255.0f;
            int b = ccm.getBlackLevel();
            int w = ccm.getWhiteLevel();
            float nrange = (float) Math.pow(2.0, ccm.getBitDepth()) - 1;
            channelMin[c] = b / nrange;
            float crange = (float) Math.max(1.0, w - b); // avoid divide by zero
            channelScale[c] = nrange / crange;
            if (!ccm.isVisible()) {
                channelMin[c] = 0f;
                channelScale[c] = 0f;
            }
            channelGamma[c] = (float) ccm.getGamma();
        }
        
        reportError(gl, "before setting shader values");
        snapshotShader.setChannelCount( gl, 2 );
        reportError(gl, "after pushing channel count.");
        
        snapshotShader.setExplicitInterleave( gl, interleavedTextureMediator != null );
        snapshotShader.setChannelGamma( gl, channelGamma );
        reportError(gl, "after setting channel gamma");
        snapshotShader.setChannelMin( gl, channelMin );
        reportError(gl, "after setting channel min");
        snapshotShader.setChannelScale( gl, channelScale );
        reportError(gl, "after setting channel scale");
        snapshotShader.setChannelColor( gl, channelColor );
        reportError(gl, "after setting shader values");
    }

}
