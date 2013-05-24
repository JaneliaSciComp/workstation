package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import javax.media.opengl.GL2;

import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.AbstractTextureLoadAdapter.MissingTileException;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.AbstractTextureLoadAdapter.TileLoadError;

/*
 * Note the subtle distinction between Tile2d and TileTexture
 */
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
	
	private Stage stage = Stage.UNINITIALIZED;
	private TileIndex index;
	// private URL url;
	private TextureData2dGL textureData;
	private PyramidTexture texture;
	private AbstractTextureLoadAdapter loadAdapter;
	
	// time stamps for performance measurement
	private long constructTime = System.nanoTime();
	// Treat constructTime - 1 as "invalid" time
	private long invalidTime = constructTime - 1;
	private long downloadDataTime = invalidTime;
	private long parseImageTime = invalidTime;
	private long convertToGlTime = invalidTime;
	private long uploadTextureTime = invalidTime;
	private long firstDisplayTime = invalidTime;

	private Signal1<TileIndex> ramLoadedSignal = new Signal1<TileIndex>();

	public TileTexture(TileIndex index, AbstractTextureLoadAdapter loadAdapter) {
		this.index = index;
		this.loadAdapter = loadAdapter;
	}
	
	public long getDownloadDataTime() {
		return downloadDataTime;
	}

	public long getFirstDisplayTime() {
		return firstDisplayTime;
	}

	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((index == null) ? 0 : index.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TileTexture other = (TileTexture) obj;
		if (index == null) {
			if (other.index != null)
				return false;
		} else if (!index.equals(other.index))
			return false;
		return true;
	}

	public void releaseMemory() {
		if (textureData != null)
			textureData.releaseMemory();
		textureData = null;
		setStage(Stage.UNINITIALIZED);
		texture = null;
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

	public TileIndex getIndex() {
		return index;
	}

	public Signal1<TileIndex> getRamLoadedSignal() {
		return ramLoadedSignal;
	}

	public PyramidTexture getTexture() {
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
		texture = textureData.createTexture(gl);
		setStage(Stage.GL_LOADED);
		uploadTextureTime = System.nanoTime();
	}

	public synchronized boolean loadImageToRam() {
		setStage(Stage.RAM_LOADING);
		try {
			textureData = loadAdapter.loadToRam(index);
		} catch (TileLoadError e) {
			setStage(Stage.LOAD_FAILED); // error
			return false;
		} catch (MissingTileException e) { // texture correctly has no data
			setStage(Stage.MISSING);
			return true; // because missing is a valid state
		}
		setStage(Stage.RAM_LOADED); // Yay!
		return true;
	}

	public void setIndex(TileIndex index) {
		this.index = index;
	}

	public Stage getStage() {
		return stage;
	}

	public void setStage(Stage stage) {
		this.stage = stage;
	}

	public ImageBrightnessStats getBrightnessStats() {
		if (stage.ordinal() < Stage.RAM_LOADED.ordinal())
			return null;
		return textureData.getBrightnessStats();
	}

}
