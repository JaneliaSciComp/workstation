package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.media.opengl.GL2;

import org.janelia.it.FlyWorkstation.gui.viewer3d.BoundingBox3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.GLActor;
import org.janelia.it.FlyWorkstation.gui.viewer3d.camera.Camera3d;

import com.jogamp.opengl.util.texture.Texture;

public class RavelerTileServer 
implements GLActor, VolumeImage3d
{
	private URL urlStalk; // url of top level folder
	private BoundingBox3d boundingBox3d = new BoundingBox3d();
	private int numberOfChannels = 3;
	private int maximumIntensity = 255;
	private int bitDepth = 8;
	// Emergency tiles list stores a recently displayed view, so that
	// SOMETHING gets displayed while the current view is being loaded.
	private List<Tile2d> emergencyTiles;
	// Latest tiles list stores the current desired tile set, even if
	// not all of the tiles are ready.
	private List<Tile2d> latestTiles;
	private Map<TileIndex, Texture> textureCache = new Hashtable<TileIndex, Texture>();
	private Map<TileIndex, Tile2d> tileCache = new Hashtable<TileIndex, Tile2d>();

	@Override
	public void display(GL2 gl) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void dispose(GL2 gl) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public BoundingBox3d getBoundingBox3d() {
		return boundingBox3d;
	}

	@Override
	public int getMaximumIntensity() {
		return maximumIntensity;
	}

	@Override
	public double getMaxResolution() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getNumberOfChannels() {
		return numberOfChannels;
	}

	// Produce a list of renderable tiles to complete this view
	protected List<Tile2d> getTiles(Camera3d camera, Viewport viewport) {
		List<Tile2d> result = new Vector<Tile2d>();
		// TODO
		return result;
	}
	
	@Override
	public void init(GL2 gl) {
		// TODO Auto-generated method stub
		
	}

	public boolean openFolder(URL folderUrl) {
		// Sanity check before overwriting current view
		if (folderUrl == null)
			return false;
		// Parse metadata BEFORE overwriting current data
		try {
			URL metadataUrl = new URL(folderUrl, "tiles/metadata.txt");
			BufferedReader in = new BufferedReader(new InputStreamReader(metadataUrl.openStream()));
			String line;
			// finds lines like "key=value"
			Pattern pattern = Pattern.compile("'^(.*)=(.*)\\n?$"); 
			while ((line = in.readLine()) != null) {
				Matcher m = pattern.matcher(line);
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		// Now we can start replacing the previous state
		this.urlStalk = folderUrl;
		setDefaultParameters();
		// TODO load metadata file
		return true;
	}
	
	protected void setDefaultParameters() {
		maximumIntensity= 255;
		bitDepth = 8;
		numberOfChannels = 3;
	}
	
}
