package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

public class TileTexture {
	public enum Stage {
	    UNINITIALIZED,
	    RAM_LOADING,
	    RAM_LOADED,
	    GL_LOADED
	}
	
	private Stage stage = Stage.UNINITIALIZED;

	public Stage getStage() {
		return stage;
	}

	public void setStage(Stage stage) {
		this.stage = stage;
	}
}
