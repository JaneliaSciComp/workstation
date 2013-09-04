package org.janelia.it.FlyWorkstation.gui.slice_viewer;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;

import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;

import org.apache.commons.io.IOUtils;

public class Mp4OctreeLoadAdapter extends AbstractTextureLoadAdapter 
{
	private URL topFolder;

	private static boolean urlExists(URL url) {
		try {
			url.openStream();
		} catch (IOException e) {
			return false;
		}
		return true;
	}
	
	public Mp4OctreeLoadAdapter() {
		tileFormat.setIndexStyle(TileIndex.IndexStyle.OCTREE);		
	}

	@Override
	TextureData2dGL loadToRam(TileIndex tileIndex) throws TileLoadError,
			MissingTileException 
	{
		URL folder;
		try {
			// second part of URL must not begin with "/", or it will be treated as absolute
			String subFolder = BlockTiffOctreeLoadAdapter.getOctreeFilePath(tileIndex, tileFormat).toString();
			subFolder = subFolder.replaceAll("\\\\", "/"); // replace backslash with slash (Windows File->URL)
			subFolder = subFolder.replaceAll("^[/]+", ""); // remove leading slash, if present
			if ( (subFolder.length() > 0) && (! subFolder.endsWith("/")) )
				subFolder = subFolder+"/";
			// System.out.println(subFolder);
			folder = new URL(topFolder, subFolder);
		} catch (MalformedURLException e) {
			throw new TileLoadError(e);
		}
		// Compute local z slice
		int zoomScale = (int)Math.pow(2, tileIndex.getZoom());
		int tileDepth = tileFormat.getTileSize()[2];
		int relativeZ = (tileIndex.getZ() / zoomScale) % tileDepth;
		
		return loadSlice(folder, relativeZ);
	}

	public URL getTopFolder() {
		return topFolder;
	}

	public void setTopFolder(URL topFolder) throws IOException {
		this.topFolder = topFolder;
		sniffMetadata(topFolder);
	}
	
	protected TextureData2dGL loadSlice(URL folder, int relativeZ) 
	throws TileLoadError
	{
		int sc = tileFormat.getChannelCount();
		RenderedImage channels[] = new RenderedImage[sc];
		
		// TODO load from mp4
		// TODO combine multiple channels
		for (int c = 0; c < sc; ++c) {
			// TODO cache mp4 bytes
			// Read entire mp4 into memory, because it should be pretty small
			try {
				URL url = new URL(folder, "default."+c+".mp4");
				
				/*
				InputStream is0 = url.openStream();
				byte[] mp4Bytes = IOUtils.toByteArray(is0);
				ByteBuffer mp4Buffer = ByteBuffer.wrap(mp4Bytes);
				// mp4Buffer.order(ByteOrder.LITTLE_ENDIAN); // doesn't help
				mp4Buffer.rewind();
				ByteBufferSeekableByteChannel channel = 
						new ByteBufferSeekableByteChannel(mp4Buffer);
				// channel.position(0); // doesn't help
				 * 
				 */
				
				File f;
				try {
				  f = new File(url.toURI());
				} catch(URISyntaxException e) {
				  f = new File(url.getPath());
				}
				/*
				FileChannelWrapper channel = NIOUtils.readableFileChannel(f);
				
				FrameGrab frameGrab = new FrameGrab(channel);
				frameGrab.seek(relativeZ);
				channels[c] = frameGrab.getFrame();
			} catch (JCodecException e) {
				throw new TileLoadError(e);
				*/
			} catch (IOException e) {
				throw new TileLoadError(e);
			}
		}
		
		// Combine channels into one image
		RenderedImage composite = channels[0];
		if (sc > 1) {
			ParameterBlockJAI pb = new ParameterBlockJAI("bandmerge");
			for (int c = 0; c < sc; ++c)
				pb.addSource(channels[c]);
			composite = JAI.create("bandmerge", pb);
			// localLoadTimer.mark("merged channels");
		}
		
		TextureData2dGL tex = new TextureData2dGL();
		tex.loadRenderedImage(composite);
		return tex;	
	}
	
	protected void sniffMetadata(URL topFolderParam) 
	throws IOException
	{
		tileFormat.setDefaultParameters();
		// Count channels by counting number of grayscale mp4 files
		int channelCount = 0;
		while (true) {
			String fname = "default."+channelCount+".mp4";
			URL testFile = new URL(topFolderParam, fname);
			if (! urlExists(testFile))
				break;
			channelCount += 1;
		}
		tileFormat.setChannelCount(channelCount);
		System.out.println("Number of channels = "+channelCount);
		// TODO other metadata
	}

}
