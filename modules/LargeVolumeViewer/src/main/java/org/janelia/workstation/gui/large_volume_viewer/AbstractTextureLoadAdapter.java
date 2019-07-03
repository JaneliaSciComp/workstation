package org.janelia.workstation.gui.large_volume_viewer;

public abstract class AbstractTextureLoadAdapter {

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
	
	private final TileFormat tileFormat;

	public AbstractTextureLoadAdapter(TileFormat tileFormat) {
		this.tileFormat = tileFormat;
	}

	public abstract TextureData2d loadToRam(TileIndex tileIndex)
		throws TileLoadError, MissingTileException;

	public TileFormat getTileFormat() {
		return tileFormat;
	}
}
