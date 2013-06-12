package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import org.janelia.it.FlyWorkstation.gui.viewer3d.BoundingBox3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Vec3;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.VolumeImage3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.AbstractTextureLoadAdapter.MissingTileException;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.AbstractTextureLoadAdapter.TileLoadError;

public class SharedVolumeImage 
implements VolumeImage3d
{
	private AbstractTextureLoadAdapter loadAdapter;
	private BoundingBox3d boundingBox3d = new BoundingBox3d();

	@Override
	public BoundingBox3d getBoundingBox3d() {
		return boundingBox3d;
	}

	@Override
	public int getMaximumIntensity() {
		if (getLoadAdapter() == null)
			return 0;
		return getLoadAdapter().getTileFormat().getIntensityMax();
	}

	@Override
	public double getXResolution() {
		if (getLoadAdapter() == null)
			return 0;
		return getLoadAdapter().getTileFormat().getVoxelMicrometers()[0];
	}

	@Override
	public double getYResolution() {
		if (getLoadAdapter() == null)
			return 0;
		return getLoadAdapter().getTileFormat().getVoxelMicrometers()[1];
	}

	@Override
	public double getZResolution() {
		if (getLoadAdapter() == null)
			return 0;
		return getLoadAdapter().getTileFormat().getVoxelMicrometers()[2];
	}

	@Override
	public int getNumberOfChannels() {
		if (getLoadAdapter() == null)
			return 0;
		return getLoadAdapter().getTileFormat().getChannelCount();
	}

	@Override
	public boolean loadURL(URL folderUrl) {
		// Sanity check before overwriting current view
		if (folderUrl == null)
			return false;
		
		// Sniff which back end we need
		AbstractTextureLoadAdapter testLoadAdapter = null;
		URL testUrl;

		// Is this a PAM octree folder?
		if (testLoadAdapter == null) {
			try {
				testUrl = new URL(folderUrl, "slice_00000.pam");
				InputStream is = testUrl.openStream();
				is.close();
				PamOctreeLoadAdapter pola = new PamOctreeLoadAdapter();
				pola.setTopFolder(folderUrl);
				testLoadAdapter = pola;
			} 
			catch (IOException e2) {} // not a PAM folder
			catch (MissingTileException e) {} 
			catch (TileLoadError e) {}
		}
		
		// Is this a MP4 octree folder?
		if (testLoadAdapter == null) {
			try {
				// diagnostic mp4 file
				testUrl = new URL(folderUrl, "default.0.mp4");
				testUrl.openStream();
				Mp4OctreeLoadAdapter btola = new Mp4OctreeLoadAdapter();
				btola.setTopFolder(folderUrl);
				testLoadAdapter = btola;
			} catch (MalformedURLException e1) {} 
			catch (IOException e) {} 
		}

		// Is this a Block tiff octree folder?
		if (testLoadAdapter == null) {
			try {
				// Look for diagnostic block tiff file
				testUrl = new URL(folderUrl, "default.0.tif");
				testUrl.openStream();
				File fileFolder = new File(folderUrl.toURI());
				BlockTiffOctreeLoadAdapter btola = new BlockTiffOctreeLoadAdapter();
				btola.setTopFolder(fileFolder);
				testLoadAdapter = btola;
			} catch (MalformedURLException e1) {} 
			catch (IOException e) {} 
			catch (URISyntaxException e) {}
		}

		// Is this a Raveler format?
		if (testLoadAdapter == null) {
			try {
				testLoadAdapter = new RavelerLoadAdapter(folderUrl);
			} catch (IOException e) {}
		}

		// Did we identify a folder format?
		if (testLoadAdapter == null)
			return false; // NO
		
		loadAdapter = testLoadAdapter;
		
		// Update bounding box
		// Compute bounding box
		TileFormat tf = getLoadAdapter().getTileFormat();
		double sv[] = tf.getVoxelMicrometers();
		int s0[] = tf.getOrigin();
		int s1[] = tf.getVolumeSize();
		Vec3 b0 = new Vec3(sv[0]*s0[0], sv[1]*s0[1], sv[2]*s0[2]);
		Vec3 b1 = new Vec3(sv[0]*(s0[0]+s1[0]), sv[1]*(s0[1]+s1[1]), sv[2]*(s0[2]+s1[2]));
		boundingBox3d.setMin(b0);
		boundingBox3d.setMax(b1);
		
		return true;
	}

	@Override
	public double getResolution(int ix) {
		if (ix == 0) return getXResolution();
		else if (ix == 1) return getYResolution();
		else return getZResolution();
	}

	public AbstractTextureLoadAdapter getLoadAdapter() {
		return loadAdapter;
	}

}
