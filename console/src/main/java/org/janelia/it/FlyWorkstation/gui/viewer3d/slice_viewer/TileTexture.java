package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.media.opengl.GL2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;

public class TileTexture 
{
	public static enum Stage 
	{
		LOAD_FAILED, // worst
	    UNINITIALIZED, // initial state
	    RAM_LOADING,
	    RAM_LOADED,
	    GL_LOADED // best
	}
	
	private static final Logger log = LoggerFactory.getLogger(TileTexture.class);
	
	private Stage stage = Stage.UNINITIALIZED;
	private TileIndex index;
	private URL url;
	private int channelCount = 0;
	private int maxIntensity = 255;
	private boolean isSrgb = false;
	private boolean isSrgbApplied = false;
	private TextureData textureData;
	private QtSignal ramLoaded = new QtSignal();
	private Texture texture;

	public TileTexture(TileIndex index, URL urlStalk) {
		this.index = index;
		int z = index.getZ();
		// Raveler uses a separate directory for each group of 1000 slices
		String thousands_dir = "";
		if (z >= 1000) {
			thousands_dir = Integer.toString(z/1000) + "000/";
		}
		String path = String.format(
				"tiles/1024/%d/%d/%d/g/%s%03d.png",
				index.getZoom(),
				index.getY(),
				index.getX(),
				thousands_dir, z);
		try {
			url = new URL(urlStalk, path);
			// log.info("Texture URL = " + url);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public TileIndex getIndex() {
		return index;
	}

	public QtSignal getRamLoadedSignal() {
		return ramLoaded;
	}

	public Texture getTexture() {
		return texture;
	}

	public void init(GL2 gl) 
	{
		if (textureData == null)
			return;
		if (getStage().ordinal() < Stage.RAM_LOADED.ordinal())
			return; // not ready to init
		if (getStage().ordinal() >= Stage.GL_LOADED.ordinal())
			return; // already initialized
		texture = TextureIO.newTexture(gl, textureData);
		setStage(Stage.GL_LOADED);
	}

	public synchronized boolean loadImageToRam() {
		setStage(Stage.RAM_LOADING);
		try {
			// log.info("Loading texture from " + url);
			BufferedImage image = ImageIO.read(url);
			ColorModel colorModel = image.getColorModel();
			// NOT getNumColorComponents(), because we count alpha channel as data.
			channelCount = colorModel.getNumComponents();
			int bitDepth = colorModel.getPixelSize() / channelCount;
			if (bitDepth > 8)
				maxIntensity = 65535;
			isSrgb = colorModel.getColorSpace().isCS_sRGB();
			// Determine correct OpenGL texture type, based on bit-depth, number of colors, and srgb
			int internalFormat, pixelFormat;
			if (channelCount == 1) {
				internalFormat = pixelFormat = GL2.GL_LUMINANCE; // default for single gray channel
				if (bitDepth > 8)
					internalFormat = GL2.GL_LUMINANCE16;
			}
			else if (channelCount == 2) {
				internalFormat = pixelFormat = GL2.GL_LUMINANCE_ALPHA;
				if (bitDepth > 8)
					internalFormat = GL2.GL_LUMINANCE16_ALPHA16;
			}
			else if (channelCount == 3) {
				internalFormat = pixelFormat = GL2.GL_RGB;
				if (bitDepth > 8)
					internalFormat = GL2.GL_RGB16;
				else if (isSrgb) {
					internalFormat = GL2.GL_SRGB;
					isSrgbApplied = true;
				}
			}
			else if (channelCount == 4) {
				internalFormat = pixelFormat = GL2.GL_RGB8;
				if (bitDepth > 8)
					internalFormat = GL2.GL_RGB16;				
				else if (isSrgb) {
					internalFormat = GL2.GL_SRGB_ALPHA;
					isSrgbApplied = true;
				}
			}
			else {
				log.error("Unsupported number of channels: " + channelCount);
				return false;
			}
			textureData = AWTTextureIO.newTextureData(
					SliceViewer.glProfile,
					image,
					internalFormat,
					pixelFormat,
					false); // mipmap
		} catch (IOException e) {
			e.printStackTrace();
			setStage(Stage.LOAD_FAILED);
			return false;
		}
		setStage(Stage.RAM_LOADED); // Yay!
		return true;
	}
	
	public void setIndex(TileIndex index) {
		this.index = index;
	}

	public URL getUrl() {
		return url;
	}

	public void setUrl(URL url) {
		this.url = url;
	}

	public Stage getStage() {
		return stage;
	}

	public void setStage(Stage stage) {
		this.stage = stage;
	}

}
