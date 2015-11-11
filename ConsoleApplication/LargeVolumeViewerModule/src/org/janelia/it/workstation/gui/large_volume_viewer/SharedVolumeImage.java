package org.janelia.it.workstation.gui.large_volume_viewer;

import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.viewer3d.BoundingBox3d;
import org.janelia.it.workstation.gui.viewer3d.interfaces.VolumeImage3d;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.VolumeLoadListener;
import org.janelia.it.workstation.gui.large_volume_viewer.exception.DataSourceInitializeException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

public class SharedVolumeImage 
implements VolumeImage3d
{
	private AbstractTextureLoadAdapter loadAdapter;
	private BoundingBox3d boundingBox3d = new BoundingBox3d();
    private Collection<VolumeLoadListener> volumeLoadListeners = new ArrayList<>();
    private String remoteBasePath;

    public void addVolumeLoadListener( VolumeLoadListener l ) {
        volumeLoadListeners.add(l);
    }
    
    public void removeVolumeLoadListener( VolumeLoadListener l ) {
        volumeLoadListeners.remove(l);
    }
    
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
            final Double boundingMin = getBoundingBox3d().getMin().get(i);
	        double range = getBoundingBox3d().getMax().get(i) - boundingMin;
	        int voxelCount = (int)Math.round(range/getResolution(i));
	        int midVoxel = voxelCount/2;
            double center = 0.0;
            center = (midVoxel+0.5)*getResolution(i) + boundingMin;
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
    
    public int[] getOrigin() {
		if (getLoadAdapter() == null) {
			return new int[]{0,0,0};
        }
        else {
            return getLoadAdapter().getTileFormat().getOrigin();
        }
        
    }

	@Override
	public int getNumberOfChannels() {
		if (getLoadAdapter() == null)
			return 0;
		return getLoadAdapter().getTileFormat().getChannelCount();
	}

    public void setRemoteBasePath(String basePath) {
        this.remoteBasePath = basePath;
    }

    public String getRemoteBasePath() {
        return remoteBasePath;
    }


	private AbstractTextureLoadAdapter createLoadAdapter(URL folderUrl) {
		// Sniff which back end we need
		AbstractTextureLoadAdapter testLoadAdapter = null;
		URL testUrl;

		try {
			testUrl = new URL(folderUrl, "default.0.tif");
			testUrl.openStream();
			File fileFolder = new File(folderUrl.toURI());
			testLoadAdapter=new TileStackOctreeLoadAdapter(remoteBasePath, fileFolder);
		} catch (IOException | URISyntaxException | DataSourceInitializeException ex) {
			ex.printStackTrace();
			ModelMgr.getModelMgr().handleException(ex);
		}

		return testLoadAdapter;
	}

	@Override
	public boolean loadURL(URL folderUrl) {
		// Sanity check before overwriting current view
		if (folderUrl == null)
			return false;
		
		loadAdapter = createLoadAdapter(folderUrl);

		if (loadAdapter==null)
			return false;
		
		// Update bounding box
		// Compute bounding box
		TileFormat tf = getLoadAdapter().getTileFormat();
		BoundingBox3d newBox = tf.calcBoundingBox();

		//log.info("Bounding box min Vec3="+newBox.getMin().toString());
		//log.info("Bounding box max Vec3="+newBox.getMax().toString());

		boundingBox3d.setMin(newBox.getMin());
		boundingBox3d.setMax(newBox.getMax());
		fireVolumeLoaded(folderUrl);
		
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

    private void fireVolumeLoaded(URL volume) {
        for ( VolumeLoadListener l: volumeLoadListeners ) {
            l.volumeLoaded(volume);
        }
    }
}
