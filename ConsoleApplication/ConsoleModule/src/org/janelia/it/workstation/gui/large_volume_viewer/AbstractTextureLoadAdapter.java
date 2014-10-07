package org.janelia.it.workstation.gui.large_volume_viewer;

public abstract class AbstractTextureLoadAdapter 
{
	// private TextureCache textureCache = new TextureCache();

	public static class MissingTileException extends Exception {
		private static final long serialVersionUID = 1L;
        public MissingTileException() { super(); }
        public MissingTileException(String message) {
            super(message);
        }
        public MissingTileException(Exception ex) {
            super(ex);
        }
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
	
	protected TileFormat tileFormat = new TileFormat();

	abstract TextureData2dGL loadToRam(TileIndex tileIndex)
		throws TileLoadError, MissingTileException;

	public TileFormat getTileFormat() {
		return tileFormat;
	}

}
