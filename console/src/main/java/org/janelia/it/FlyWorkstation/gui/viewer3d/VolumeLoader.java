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
import org.janelia.it.FlyWorkstation.gui.viewer3d.VolumeDataAcceptor.TextureColorSpace;
import org.janelia.it.FlyWorkstation.gui.viewer3d.resolver.FileResolver;
import org.janelia.it.FlyWorkstation.gui.viewer3d.stream.V3dRawImageStream;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.MaskTextureDataBean;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureDataBean;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureDataI;

import java.nio.ByteOrder;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.DataFormatException;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class VolumeLoader 
{
    public enum FileType{
        TIF, LSM, V3DMASK, V3DSIGNAL, MP4, UNKNOWN
    };

    private static final String CONSOLIDATED_SIGNAL_FILE = "ConsolidatedSignal2";
    private static final String REFERENCE_FILE = "Reference2";
    private static final String TIF_EXT = "TIF";
    private static final String LSM_EXT = "LSM";
    private static final String V3D_EXT = "V3D";
    private static final String MP4_EXT = "MP4";
    private int[] argbIntArray;
    private byte[] maskByteArray;
	private int sx, sy, sz;
    private int channelCount = 1; // Default for non-data-bearing file formats.
	private VolumeBrick.TextureColorSpace colorSpace =
		VolumeBrick.TextureColorSpace.COLOR_SPACE_LINEAR;

    private String unCachedFileName;
    private FileResolver resolver;
    private String header = null;
    private int pixelBytes = 1;
    private boolean isMask = false;
    private ByteOrder pixelByteOrder = ByteOrder.LITTLE_ENDIAN;

    public VolumeLoader( FileResolver resolver ) {
        this.resolver = resolver;
    }

    public boolean loadVolume(String unCachedFileName)
    {
        try {
            this.unCachedFileName = unCachedFileName;
            String localFileName = resolver.getResolvedFilename( unCachedFileName );

            String extension = FilenameUtils.getExtension(localFileName).toUpperCase();
            System.out.println("FILENAME: " + localFileName);
            String baseName = FilenameUtils.getBaseName(localFileName);

            resolveColorSpace(baseName, extension);
            switch ( getFileType( localFileName, baseName, extension ) ) {
                case TIF:
                    loadLociReader(localFileName, new TiffReader());
                    break;
                case LSM:
                    loadLociReader(localFileName, new ZeissLSMReader());
                    break;
                case V3DSIGNAL:
                    loadV3dRaw(new BufferedInputStream(
                            new FileInputStream(localFileName))
                    );
                    break;
                case V3DMASK:
                    loadV3dMask(new BufferedInputStream(
                            new FileInputStream(localFileName))
                    );
                    break;
                case MP4:
                    loadMpegVideo(localFileName);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown filename/extension combination " + baseName + "/" + extension);
            }

            // Because we use premultiplied transparency...
            if ( ! isMask )
                setAlphaToSaturateColors(colorSpace);

            return true;
        }
        catch (Exception exc) {
            exc.printStackTrace();
        }
        return false;
    }

    private FileType getFileType( String filename, String baseName, String extension ) {
        System.out.println("FILENAME: " + filename);

        if (extension.startsWith(TIF_EXT)) {
            return FileType.TIF;
        }
        else if (extension.startsWith(LSM_EXT)) {
            return FileType.LSM;
        }
        else if (extension.startsWith(MP4_EXT)) {
            return FileType.MP4;
        }
        else if (extension.startsWith(V3D_EXT) && baseName.startsWith("ConsolidatedLabel")) {
            return FileType.V3DMASK;
        }
        else if (extension.startsWith(V3D_EXT)) {
            return FileType.V3DSIGNAL;
        }
        else {
            return FileType.UNKNOWN;
        }
    }

    private void loadLociReader(String localFileName, IFormatReader reader) throws FormatException, IOException {
        BufferedImageReader in = new BufferedImageReader(reader);
        in.setId(localFileName);
        loadLociReader(in);
    }

    private void resolveColorSpace(String baseName, String extension) {
        // Default to linear color space
        // But look for some exceptions we know about
        colorSpace = TextureColorSpace.COLOR_SPACE_LINEAR;
        if (baseName.startsWith(CONSOLIDATED_SIGNAL_FILE))
            colorSpace = TextureColorSpace.COLOR_SPACE_SRGB;
        if (baseName.startsWith(REFERENCE_FILE))
            colorSpace = TextureColorSpace.COLOR_SPACE_SRGB;
        // assume all mpegs are in sRGB color space
        if (extension.startsWith(MP4_EXT))
            colorSpace = TextureColorSpace.COLOR_SPACE_SRGB;

    }

    public void populateVolumeAcceptor(VolumeDataAcceptor dataAcceptor) {

        TextureDataI textureData = null;

        if ( isMask ) {
            textureData = new MaskTextureDataBean( maskByteArray, sx, sy, sz );
        }
        else {
            textureData = new TextureDataBean( argbIntArray, sx, sy, sz );
        }

        textureData.setColorSpace(colorSpace);
        textureData.setVolumeMicrometers(new Double[]{(double) sx, (double) sy, (double) sz});
        textureData.setVoxelMicrometers(new Double[]{1.0, 1.0, 1.0});
        if ( header != null ) {
            textureData.setHeader(header);
        }
        textureData.setByteOrder(pixelByteOrder);
        textureData.setPixelByteCount(pixelBytes);
        textureData.setFilename( unCachedFileName );
        textureData.setChannelCount(channelCount);
        dataAcceptor.setTextureData( textureData );
    }

    /**
     * Set alpha component of each voxel assuming that R,G,B
     * values represent a saturated color with premultiplied alpha.
     * Similar to Vaa3D.  In other words, alpha = max(R,G,B)
     */
    private void setAlphaToSaturateColors(TextureColorSpace space) {
        if ( space == null )
            return;

        // Use modified alpha value for sRGB textures
        int[] alphaMap = new int[256];
        double exponent = 1.0;
        if (space == TextureColorSpace.COLOR_SPACE_SRGB)
            exponent  = 2.2;
        for (int i = 0; i < 256; ++i) {
            double i0 = i / 255.0;
            double i1 = Math.pow(i0, exponent);
            alphaMap[i] = (int)(i1 * 255.0 + 0.5);
        }
        int numVoxels = argbIntArray.length;
        for (int v = 0; v < numVoxels; ++v) {
            int argb = argbIntArray[v];
            int red   = (argb & 0x00ff0000) >>> 16;
            int green = (argb & 0x0000ff00) >>> 8;
            int blue  = (argb & 0x000000ff);
            int alpha = Math.max(red, Math.max(green, blue));
            alpha = alphaMap[alpha];
            argb = (argb & 0x00ffffff) | (alpha << 24);
            argbIntArray[v] = argb;
        }
    }

    private boolean loadLociReader(BufferedImageReader in)
	throws IOException, FormatException
	{
		sx = in.getSizeX();
		sy = in.getSizeY();
		sz = in.getSizeZ();
		argbIntArray = new int[sx*sy*sz];
		int scanLineStride = sx;
		for (int z = 0; z < sz; z++) {
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
		setAlphaToSaturateColors(colorSpace);
		return true;
	}
	
	private boolean loadMpegVideo(String fileName)
	{
		IMediaReader mediaReader = ToolFactory.makeReader(fileName);
		// use premultiplied alpha for this opengl mip technique
		mediaReader.setBufferedImageTypeToGenerate(BufferedImage.TYPE_3BYTE_BGR);
		mediaReader.addListener(new VolumeFrameListener());
		while (mediaReader.readPacket() == null);
		return true;
	}
	
	private void loadV3dRaw(InputStream inputStream)
	throws IOException, DataFormatException
	{
		V3dRawImageStream sliceStream = new V3dRawImageStream(inputStream);
		sx = sliceStream.getDimension(0);
		sy = sliceStream.getDimension(1);
		sz = sliceStream.getDimension(2);
        pixelBytes = sliceStream.getPixelBytes();
		int sc = sliceStream.getDimension(3);
        channelCount = sc;
        pixelByteOrder = sliceStream.getEndian();

		double scale = 1.0;
		if (sliceStream.getPixelBytes() > 1)
			scale = 255.0 / 4095.0; // assume it's 12 bits

		argbIntArray = new int[sx*sy*sz];
		zeroColors();
		for (int c = 0; c < sc; ++c) {
			// create a mask to manipulate one color byte of a 32-bit ARGB int
			int bitShift = 8 * (c + 2);
			while (bitShift >= 32) bitShift -= 32; // channel 4 gets shifted zero (no shift)
			bitShift = 32 - bitShift;  // opposite shift inside loop
			int mask = (0x000000ff << bitShift);
			int notMask = ~mask;
			for (int z = 0; z < sz; ++z) {
				int zOffset = z * sx * sy;
				sliceStream.loadNextSlice();
				V3dRawImageStream.Slice slice = sliceStream.getCurrentSlice();
				for (int y = 0; y < sy; ++y) {
					int yOffset = zOffset + y * sx;
					for (int x = 0; x < sx; ++x) {
						int argb = argbIntArray[yOffset + x] & notMask; // zero color component
						double value = scale * slice.getValue(x, y);
						int ival = (int)(value + 0.5);
						if (ival < 0) ival = 0;
						if (ival > 255) ival = 255;
						ival = ival << bitShift;
						argb = argb | ival; // insert updated color component
						argbIntArray[yOffset + x] = argb;
					}
				}
			}
		}

        header = sliceStream.getHeaderKey();
	}

    private void loadV3dMask(InputStream inputStream)
            throws IOException, DataFormatException
    {
        isMask = true;

        V3dRawImageStream sliceStream = new V3dRawImageStream(inputStream);
        sx = sliceStream.getDimension(0);
        sy = sliceStream.getDimension(1);
        sz = sliceStream.getDimension(2);
        pixelBytes = sliceStream.getPixelBytes();
        int sc = sliceStream.getDimension(3);
        channelCount = sc;
        pixelByteOrder = sliceStream.getEndian();

        // Java implicitly sets newly-allocated byte arrays to all zeros.
        maskByteArray = new byte[(sx*sy*sz) * pixelBytes];

        if ( sc > 1 ) {
            throw new RuntimeException( "Unexpected multi-channel mask file." );
        }

        if ( sc == 0 ) {
            throw new RuntimeException( "Unexpected zero channel count mask file." );
        }

        Set<Integer> values = new TreeSet<Integer>();
        for (int z = 0; z < sz; z ++ ) {
            int zOffset = z * sx * sy;
            sliceStream.loadNextSlice();
            V3dRawImageStream.Slice slice = sliceStream.getCurrentSlice();
            for (int y = 0; y < sy; y ++ ) {
                int yOffset = zOffset + (sy-y) * sx;
                for (int x = 0; x < sx; x ++ ) {
                    Integer value = slice.getValue(x, y);
                    if ( value > 0 ) {
                        values.add( value );
                        for ( int pi = 0; pi < pixelBytes; pi ++ ) {
                            byte piByte = (byte)(value >>> (pi * 8) & 0x000000ff);
//                            maskByteArray[(yOffset * 2) + (x * 2) + (pixelBytes - pi - 1)] = piByte;
                            maskByteArray[(yOffset * pixelBytes) + (x * pixelBytes) + (pi)] = piByte;
                        }
                    }
                }
            }
        }

        for ( Integer value: values ) {
            System.out.print( value + "," );
        }
        System.out.println();
        header = sliceStream.getHeaderKey();
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
                    channelCount = 3;
                    pixelBytes = 4;
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

	private void storeFramePixels(int frameIndex, BufferedImage image) 
	{
		// System.out.println("Reading frame " + frameIndex);
		int offset = frameIndex * sx * sy;
        image.getRGB(0, 0, sx, sy,
                argbIntArray,
                offset, sx);
	}
	
	private void zeroColors() {
		int numVoxels = argbIntArray.length;
		for (int v = 0; v < numVoxels; ++v)
			argbIntArray[v] = 0;
	}

}
