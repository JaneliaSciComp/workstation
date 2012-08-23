package org.janelia.it.FlyWorkstation.gui.viewer3d;

import com.xuggle.mediatool.IMediaReader;
import com.xuggle.mediatool.MediaListenerAdapter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.mediatool.event.IOpenCoderEvent;
import com.xuggle.mediatool.event.IVideoPictureEvent;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;
import loci.formats.FormatException;
import loci.formats.IFormatReader;
import loci.formats.gui.BufferedImageReader;
import loci.formats.in.TiffReader;
import loci.formats.in.ZeissLSMReader;
import org.apache.commons.io.FilenameUtils;
import java.util.zip.DataFormatException;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class VolumeLoader 
{
	private int[] argbIntArray;
	// private IntBuffer rgbaBuffer;
	private int sx, sy, sz;
	private VolumeBrick.TextureColorSpace colorSpace = 
		VolumeBrick.TextureColorSpace.COLOR_SPACE_LINEAR;
	
	public boolean loadLociReader(BufferedImageReader in) 
	throws IOException, FormatException
	{
		sx = in.getSizeX();
		sy = in.getSizeY();
		sz = in.getSizeZ();
		argbIntArray = new int[sx*sy*sz];
		// rgbaBuffer = Buffers.newDirectIntBuffer(intArray);
		int scanLineStride = sx;
		for (int z = 0; z < sz; ++z) {
			BufferedImage zSlice = in.openImage(z);
			int zOffset = z * sx * sy;
			// int[] pixels = ((DataBufferInt)zSlice.getData().getDataBuffer()).getData();
			zSlice.getRGB(0, 0, 
				sx, sy,
				argbIntArray,
				zOffset,
				scanLineStride);
		}
		in.close();
		return true;
	}
	
	public boolean loadMpegVideo(String fileName) 
	{
		IMediaReader mediaReader = ToolFactory.makeReader(fileName);
		// use premultiplied alpha for this opengl mip technique
		mediaReader.setBufferedImageTypeToGenerate(BufferedImage.TYPE_3BYTE_BGR);
		mediaReader.addListener(new VolumeFrameListener());
		while (mediaReader.readPacket() == null);
		setAlphaToSaturateColors();
		// Assume all mpeg videos are color corrected for now
		colorSpace = VolumeBrick.TextureColorSpace.COLOR_SPACE_SRGB;
		return true;
	}
	
	public void loadV3dRaw(InputStream inputStream) 
	throws IOException, DataFormatException
	{
		V3dRawImageStream sliceStream = new V3dRawImageStream(inputStream);
		for (V3dRawImageStream.Slice slice : sliceStream) {
			; // TODO
		}
	}
	
	public boolean loadVolume(String fileName) 
	{
		try {
			String extension = FilenameUtils.getExtension(fileName).toUpperCase();

			IFormatReader reader = null;
			if (extension.startsWith("TIF")) {
				reader = new TiffReader();
			} else if (extension.startsWith("LSM")) {
				reader = new ZeissLSMReader();
			}
			if (reader != null) {
				BufferedImageReader in = new BufferedImageReader(reader);
				in.setId(fileName);
				return loadLociReader(in);
			}
			if (extension.startsWith("V3DRAW")) {
				InputStream v3dRawStream = new BufferedInputStream(
								new FileInputStream(fileName));
				loadV3dRaw(v3dRawStream);
				return true;
			}
			if (extension.startsWith("MP4")) {
				return loadMpegVideo(fileName);
			}
		}
		catch (Exception exc) {
			exc.printStackTrace();
		}
		return false;
	}

	public void populateBrick(VolumeBrick brick) {
		brick.setVolumeData(sx, sy, sz, argbIntArray);
		brick.setTextureColorSpace(colorSpace);
		brick.setVolumeMicrometers(sx, sy, sz);
		brick.setVoxelMicrometers(1.0, 1.0, 1.0);
	}
	
	private class VolumeFrameListener 
	extends MediaListenerAdapter
	{
		// mpeg loading state variables
		private int mVideoStreamIndex = -1;
		private int frameIndex = 0;

	    @Override
	    public void onOpenCoder(IOpenCoderEvent event) 
	    {
	    		IContainer container = event.getSource().getContainer();
	    		// Duration might be useful for computing number of frames
	    		long duration = container.getDuration(); // microseconds
	    		int numStreams = container.getNumStreams();
	    		for (int i = 0; i < numStreams; ++i) {
	    			IStream stream = container.getStream(i);
	    			IStreamCoder coder = stream.getStreamCoder();
	    			ICodec.Type type = coder.getCodecType();
	    			if (type != ICodec.Type.CODEC_TYPE_VIDEO)
	    				continue;
	    			double frameRate = coder.getFrameRate().getDouble();
		    		frameIndex = 0;
		    		sx = sy = sz = 0;
	    			sx = coder.getWidth();
	    			sy = coder.getHeight();
	    			sz = (int)(frameRate * duration / 1e6 + 0.5);
	    			argbIntArray = new int[sx*sy*sz];
	    			return;
	    		}
	    }
	    
	    @Override
	    public void onVideoPicture(IVideoPictureEvent event) {
			if (event.getStreamIndex() != mVideoStreamIndex) {
				// if the selected video stream id is not yet set, go ahead an
				// select this lucky video stream
				if (mVideoStreamIndex == -1)
					mVideoStreamIndex = event.getStreamIndex();
				// no need to show frames from this video stream
				else
					return;
			}
			storeFramePixels(frameIndex, event.getImage());
			++frameIndex;
		}
	}
	
	/**
	 * Set alpha component of each voxel assuming that R,G,B
	 * values represent a saturated color with premultiplied alpha.
	 * Similar to Vaa3D.  In other words, alpha = max(R,G,B)
	 */
	public void setAlphaToSaturateColors() {
		int numVoxels = argbIntArray.length;
		for (int v = 0; v < numVoxels; ++v) {
			int argb = argbIntArray[v];
			int red   = (argb & 0x00ff0000) >>> 16;
			int green = (argb & 0x0000ff00) >>> 8;
			int blue  = (argb & 0x000000ff);
			int alpha = Math.max(red, Math.max(green, blue));
			argb = (argb & 0x00ffffff) | (alpha << 24);
			argbIntArray[v] = argb;
		}
	}
	

	private void storeFramePixels(int frameIndex, BufferedImage image) 
	{
		// System.out.println("Reading frame " + frameIndex);
		int offset = frameIndex * sx * sy;
		image.getRGB(0, 0, sx, sy,
				argbIntArray, 
				offset, sx);
	}
	
}
