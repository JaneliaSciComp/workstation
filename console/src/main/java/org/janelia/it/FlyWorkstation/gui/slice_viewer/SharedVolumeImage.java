package org.janelia.it.FlyWorkstation.gui.slice_viewer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import org.janelia.it.FlyWorkstation.geom.Vec3;
import org.janelia.it.FlyWorkstation.gui.slice_viewer.AbstractTextureLoadAdapter.MissingTileException;
import org.janelia.it.FlyWorkstation.gui.slice_viewer.AbstractTextureLoadAdapter.TileLoadError;
import org.janelia.it.FlyWorkstation.gui.viewer3d.BoundingBox3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.VolumeImage3d;
import org.janelia.it.FlyWorkstation.signal.Signal1;

public class SharedVolumeImage 
implements VolumeImage3d
{
	private AbstractTextureLoadAdapter loadAdapter;
	private BoundingBox3d boundingBox3d = new BoundingBox3d();

	public Signal1<URL> volumeInitializedSignal = new Signal1<URL>();

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
	public Vec3 getVoxelCenter() {
	    Vec3 result = new Vec3();
	    for (int i = 0; i < 3; ++i) {
	        double range = getBoundingBox3d().getMax().get(i) - getBoundingBox3d().getMin().get(i);
	        int voxelCount = (int)Math.round(range/getResolution(i));
	        int midVoxel = voxelCount/2;
	        double center = (midVoxel+0.5)*getResolution(i);
	        result.set(i, center);
	    }
	    return result;
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
		BoundingBox3d newBox = tf.calcBoundingBox();
		boundingBox3d.setMin(newBox.getMin());
		boundingBox3d.setMax(newBox.getMax());
		
		volumeInitializedSignal.emit(folderUrl);
		
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
