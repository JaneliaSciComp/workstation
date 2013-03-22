package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

public interface PyramidTextureLoadAdapter 
{
	public class MissingTileException extends Exception {
		private static final long serialVersionUID = 1L;
	};
	
	public class TileLoadError extends Exception {
		private static final long serialVersionUID = 1L;
		public TileLoadError(Throwable e) {
			super(e);
		}
		public TileLoadError(String string) {
			super(string);
		}
	};
	
	PyramidTextureData loadToRam(PyramidTileIndex tileIndex)
		throws TileLoadError, MissingTileException;
}
