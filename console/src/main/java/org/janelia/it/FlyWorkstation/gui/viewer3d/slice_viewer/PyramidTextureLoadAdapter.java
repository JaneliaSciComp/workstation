package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

public abstract class PyramidTextureLoadAdapter 
{
	private TextureCache textureCache = new TextureCache();

	public static class MissingTileException extends Exception {
		private static final long serialVersionUID = 1L;
	};
	
	public static class TileLoadError extends Exception {
		private static final long serialVersionUID = 1L;
		public TileLoadError(Throwable e) {
			super(e);
		}
		public TileLoadError(String string) {
			super(string);
		}
	};
	
	protected PyramidTileFormat tileFormat = new PyramidTileFormat();

	abstract TextureData2dGL loadToRam(PyramidTileIndex tileIndex)
		throws TileLoadError, MissingTileException;

	public PyramidTileFormat getTileFormat() {
		return tileFormat;
	}

	public TextureCache getTextureCache() {
		return textureCache;
	}

}
