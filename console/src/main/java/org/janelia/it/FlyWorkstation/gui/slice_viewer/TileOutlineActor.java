package org.janelia.it.FlyWorkstation.gui.slice_viewer;

import javax.media.opengl.GL2;

import org.janelia.it.FlyWorkstation.gui.camera.Camera3d;
import org.janelia.it.FlyWorkstation.gui.opengl.GLActor;
import org.janelia.it.FlyWorkstation.gui.slice_viewer.shader.OutlineShader;
import org.janelia.it.FlyWorkstation.gui.viewer3d.BoundingBox3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.shader.AbstractShader.ShaderCreationException;

public class TileOutlineActor 
implements GLActor
{
	private ViewTileManager viewTileManager;
	private OutlineShader outlineShader = new OutlineShader();

	public TileOutlineActor(ViewTileManager viewTileManager) {
		this.viewTileManager = viewTileManager;
	}
	
	@Override
	public void display(GL2 gl) {
		display(gl, viewTileManager.createLatestTiles());
	}
	
	private void display(GL2 gl, TileSet tiles) {
		if (tiles == null)
			return;
		if (tiles.size() < 1)
			return;
		Camera3d camera = viewTileManager.getTileConsumer().getCamera();
		outlineShader.load(gl);
		gl.glLineWidth(2.0f);
		for (Tile2d tile : tiles) {
			tile.displayBoundingBox(gl, camera);
			// System.out.println("paint tile outline");
		}
		outlineShader.unload(gl);
	}

	@Override
	public BoundingBox3d getBoundingBox3d() {
		return viewTileManager.getVolumeImage().getBoundingBox3d();
	}

	@Override
	public void init(GL2 gl) {
		try {
			outlineShader.init(gl);
		} catch (ShaderCreationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void dispose(GL2 gl) {
		// TODO shader?
	}

}
