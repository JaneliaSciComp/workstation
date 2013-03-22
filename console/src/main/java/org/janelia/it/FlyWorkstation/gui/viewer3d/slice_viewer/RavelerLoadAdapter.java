package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

public class RavelerLoadAdapter 
extends PyramidTextureLoadAdapter 
{

	private URL urlStalk;
	
	public RavelerLoadAdapter(URL urlStalk) {
		this.urlStalk = urlStalk;
		parseMetadata(urlStalk);
	}
	
	protected ByteArrayInputStream downloadBytes(URL url) 
	throws IOException 
	{
		// First load bytes, THEN parse image (for more surgical timing measurements)
		// http://stackoverflow.com/questions/2295221/java-net-url-read-stream-to-byte
		// TODO - speed things up by combining download and decompress
		ByteArrayOutputStream byteStream0 = new ByteArrayOutputStream();
		byte[] chunk = new byte[32768];
		int bytesRead;
		InputStream stream = new BufferedInputStream(url.openStream());
		while ((bytesRead = stream.read(chunk)) > 0) {
			byteStream0.write(chunk, 0, bytesRead);
		}
		byte[] byteArray = byteStream0.toByteArray();
		ByteArrayInputStream byteStream = new ByteArrayInputStream(byteArray);
		return byteStream;
	}

	private BufferedImage decodeImage(ByteArrayInputStream byteStream)
	throws IOException 
	{
		BufferedImage image = ImageIO.read(byteStream);
		return image;
	}

	@Override
	public PyramidTextureData loadToRam(PyramidTileIndex index) 
	throws TileLoadError, MissingTileException
	{
		int z = index.getZ();
		// Raveler uses a separate directory for each group of 1000 slices
		String thousands_dir = "";
		if (z >= 1000) {
			thousands_dir = Integer.toString(z/1000) + "000/";
		}
		int tileSize = getTileFormat().getTileSize()[0];
		String path = String.format(
				"tiles/%d/%d/%d/%d/g/%s%03d.png",
				tileSize,
				index.getZoom(),
				index.getY(),
				index.getX(),
				thousands_dir, z);
		URL url;
		try {
			url = new URL(urlStalk, path);
		} catch (MalformedURLException e) {
			throw new TileLoadError(e);
		}
		// log.info("Loading texture from " + url);
		// TODO - download and parse simultaneously to save time
		// AFTER performance optimization is complete
		BufferedImage image;
		try {
			ByteArrayInputStream byteStream = downloadBytes(url);
			image = decodeImage(byteStream);
		} catch (IOException e) {
			throw new TileLoadError(e);
		}
		return convertToGlFormat(image);
	}

	protected boolean parseMetadata(URL folderUrl) {
		// Parse metadata BEFORE overwriting current data
		try {
			URL metadataUrl = new URL(folderUrl, "tiles/metadata.txt");
			BufferedReader in = new BufferedReader(new InputStreamReader(metadataUrl.openStream()));
			String line;
			// finds lines like "key=value"
			Pattern pattern = Pattern.compile("^(.*)=(.*)\\n?$");
			Map<String, String> metadata = new Hashtable<String, String>();
			metadata.clear();
			while ((line = in.readLine()) != null) {
				Matcher m = pattern.matcher(line);
				if (! m.matches())
					continue;
				String key = m.group(1);
				String value = m.group(2);
				metadata.put(key, value);
			}
			// Parse particular metadata values
			PyramidTileFormat tf = getTileFormat();
			tf.setDefaultParameters();
			tf.getTileSize()[0] = 1024;
			tf.getTileSize()[1] = 1024;
			tf.getTileSize()[2] = 1;
			// TODO - parse voxel size first
			if (metadata.containsKey("zmin")) {
				int zmin = Integer.parseInt(metadata.get("zmin"));
				tf.getOrigin()[2] = zmin;
			}
			if (metadata.containsKey("zmax")) {
				int zmax = Integer.parseInt(metadata.get("zmax"));
				tf.getVolumeSize()[2] = zmax - tf.getOrigin()[2];
			}
			if (metadata.containsKey("width")) {
				int w = Integer.parseInt(metadata.get("width"));
				tf.getOrigin()[0] = 0;
				tf.getVolumeSize()[0] = w;
			}
			if (metadata.containsKey("height")) {
				int h = Integer.parseInt(metadata.get("height"));
				tf.getOrigin()[1] = 0;
				tf.getVolumeSize()[1] = h;
			}
	        if (metadata.containsKey("imax")) {
	        		int i = Integer.parseInt(metadata.get("imax"));
	        		tf.setIntensityMax(i);
	            if (i > 255)
	            		tf.setBitDepth(16);
	        }
	        if (metadata.containsKey("bitdepth")) {
	        		int bitDepth = Integer.parseInt(metadata.get("bitdepth"));
	        		tf.setBitDepth(bitDepth);
	        }
	        if (metadata.containsKey("channel-count")) {
	        		int c = Integer.parseInt(metadata.get("channel-count"));
	        		tf.setChannelCount(c);
	        		// System.out.println("channel count = "+c);
	        }
	        // Compute zoom min/max from dimensions...
	        double tileX = tf.getVolumeSize()[0]/tf.getTileSize()[0];
	        double tileY = tf.getVolumeSize()[1]/tf.getTileSize()[1];
	        double tileMax = Math.max(tileX, tileY);
	        int zoomMax = 0;
	        if (tileMax != 0) {
	        		zoomMax = (int)Math.ceil(Math.log(tileMax)/Math.log(2.0));
	        }
	        if (zoomMax < 0) {
	            zoomMax = 0;
	        }
	        tf.setZoomLevelCount(zoomMax + 1);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
}
