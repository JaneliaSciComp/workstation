package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
	/**
	 * Sequence of texture stages toward readiness for use.
	 * 
	 * @author brunsc
	 *
	 */
	public static enum Stage 
	{
		LOAD_FAILED, // worst
	    UNINITIALIZED, // initial state
	    LOAD_QUEUED, // waiting in load queue
	    MISSING, // No such file; assume it's a no-data tile.
	    RAM_LOADING, // actively loading
	    RAM_LOADED, // in memory
	    GL_LOADED // best; in texture memory
	}
	
	private static final Logger log = LoggerFactory.getLogger(TileTexture.class);
	
	private Stage stage = Stage.UNINITIALIZED;
	private RavelerZTileIndex index;
	private URL url;
	private int channelCount = 0;
	private int maxIntensity = 255;
	private boolean isSrgb = false;
	private boolean isSrgbApplied = false;
	private TextureData textureData;
	private Signal ramLoaded = new Signal();
	private Texture texture;
	
	// time stamps for performance measurement
	private long constructTime = System.nanoTime();
	// Treat constructTime - 1 as "invalid" time
	private long invalidTime = constructTime - 1;
	private long downloadDataTime = invalidTime;
	private long parseImageTime = invalidTime;
	private long convertToGlTime = invalidTime;
	private long uploadTextureTime = invalidTime;
	private long firstDisplayTime = invalidTime;

	public TileTexture(RavelerZTileIndex index, URL urlStalk) {
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
			log.info("Texture URL = " + url);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public long getDownloadDataTime() {
		return downloadDataTime;
	}

	public long getFirstDisplayTime() {
		return firstDisplayTime;
	}

	public void setFirstDisplayTime(long firstDisplayTime) {
		this.firstDisplayTime = firstDisplayTime;
	}

	public long getConstructTime() {
		return constructTime;
	}

	public long getInvalidTime() {
		return invalidTime;
	}

	public long getParseImageTime() {
		return parseImageTime;
	}

	public long getConvertToGlTime() {
		return convertToGlTime;
	}

	public long getUploadTextureTime() {
		return uploadTextureTime;
	}

	public RavelerZTileIndex getIndex() {
		return index;
	}

	public Signal getRamLoadedSignal() {
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
		uploadTextureTime = System.nanoTime();
	}

	public synchronized boolean loadImageToRam() {
		setStage(Stage.RAM_LOADING);
		try {
			// log.info("Loading texture from " + url);
			// TODO - download and parse simultaneously to save time
			// AFTER performance optimization is complete
			ByteArrayInputStream byteStream = downloadBytes();
			
			BufferedImage image = decodeImage(byteStream);
			
			if (! convertToGlFormat(image))
				return false;
		} catch (IOException e) {
			// e.printStackTrace();
			log.error("Tile load to RAM failed " + getIndex());
			setStage(Stage.LOAD_FAILED);
			return false;
		}
		setStage(Stage.RAM_LOADED); // Yay!
		return true;
	}

	private boolean convertToGlFormat(BufferedImage image) {
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
		if (bitDepth > 8)
			textureData.setPixelType(GL2.GL_UNSIGNED_SHORT);
		convertToGlTime = System.nanoTime();

		return true;
	}

	private BufferedImage decodeImage(ByteArrayInputStream byteStream)
			throws IOException {
		// BufferedImage image = ImageIO.read(url);
		BufferedImage image = ImageIO.read(byteStream);
		parseImageTime = System.nanoTime();
		return image;
	}

	private ByteArrayInputStream downloadBytes() throws IOException {
		//
		// First load bytes, THEN parse image (for more surgical timing measurements)
		// http://stackoverflow.com/questions/2295221/java-net-url-read-stream-to-byte
		// TODO - speed things up by combining download and decompress
		ByteArrayOutputStream byteStream0 = new ByteArrayOutputStream();
		byte[] chunk = new byte[32768];
		int bytesRead;
		InputStream stream = new BufferedInputStream(url.openStream());
		while ((bytesRead = stream.read(chunk)) > 0) {
			byteStream0.write(chunk, 0, bytesRead);
		}
		byte[] byteArray = byteStream0.toByteArray();
		ByteArrayInputStream byteStream = new ByteArrayInputStream(byteArray);
		downloadDataTime = System.nanoTime();
		return byteStream;
	}
	
	public void setIndex(RavelerZTileIndex index) {
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
