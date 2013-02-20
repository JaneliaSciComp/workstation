package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import javax.media.opengl.GL2;

import org.janelia.it.FlyWorkstation.gui.viewer3d.BoundingBox3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.GLActor;

public class Tile2d 
implements GLActor
{
	public enum Stage {
	    NO_TEXTURE_LOADED,
	    COARSE_TEXTURE_LOADED,
	    BEST_TEXTURE_LOADED
	}


	private Stage stage = Stage.NO_TEXTURE_LOADED;
	private TileTexture bestTexture;
	private TileIndex index;

	
	public Tile2d(TileIndex key) {
		this.index = key;
	}

	public TileIndex getIndex() {
		return index;
	}

	public Stage getStage() {
		return stage;
	}

	public void setStage(Stage stage) {
		this.stage = stage;
	}

	public void setBestTexture(TileTexture bestTexture) {
		this.bestTexture = bestTexture;
	}

	@Override
	public void display(GL2 gl) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public BoundingBox3d getBoundingBox3d() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void init(GL2 gl) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void dispose(GL2 gl) {
		// TODO Auto-generated method stub
		
	}

	public TileTexture getBestTexture() {
		return bestTexture;
	}

}
