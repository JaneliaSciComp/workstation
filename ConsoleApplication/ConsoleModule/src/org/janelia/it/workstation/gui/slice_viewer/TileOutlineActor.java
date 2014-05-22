package org.janelia.it.workstation.gui.slice_viewer;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;

import org.janelia.it.workstation.gui.viewer3d.shader.AbstractShader;

public class TileOutlineActor 
implements org.janelia.it.workstation.gui.opengl.GLActor
{
	private ViewTileManager viewTileManager;
	private org.janelia.it.workstation.gui.slice_viewer.shader.OutlineShader outlineShader = new org.janelia.it.workstation.gui.slice_viewer.shader.OutlineShader();

	public TileOutlineActor(ViewTileManager viewTileManager) {
		this.viewTileManager = viewTileManager;
	}
	
	@Override
	public void display(GLAutoDrawable glDrawable) {
		display(glDrawable, viewTileManager.createLatestTiles());
	}
	
	private void display(GLAutoDrawable glDrawable, TileSet tiles) {
		if (tiles == null)
			return;
		if (tiles.size() < 1)
			return;
		org.janelia.it.workstation.gui.camera.Camera3d camera = viewTileManager.getTileConsumer().getCamera();
        GL2 gl = glDrawable.getGL().getGL2();
		outlineShader.load(gl);
		gl.glLineWidth(2.0f);
		for (Tile2d tile : tiles) {
			tile.displayBoundingBox(gl, camera);
			// System.out.println("paint tile outline");
		}
		outlineShader.unload(gl);
	}

	@Override
	public org.janelia.it.workstation.gui.viewer3d.BoundingBox3d getBoundingBox3d() {
		return viewTileManager.getVolumeImage().getBoundingBox3d();
	}

	@Override
	public void init(GLAutoDrawable glDrawable) {
		try {
	        GL2 gl = glDrawable.getGL().getGL2();
			outlineShader.init(gl);
		} catch (AbstractShader.ShaderCreationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void dispose(GLAutoDrawable glDrawable) {
		// TODO shader?
	}

}
