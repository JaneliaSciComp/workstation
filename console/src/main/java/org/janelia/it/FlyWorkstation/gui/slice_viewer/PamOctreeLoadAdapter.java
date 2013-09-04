package org.janelia.it.FlyWorkstation.gui.slice_viewer;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;

public class PamOctreeLoadAdapter 
extends AbstractTextureLoadAdapter
{
	private URL topFolder;
	private static NumberFormat sliceIndexFormat = new DecimalFormat("00000");
	
	// for initial testing
	public static void main(String[] args) {
		PamOctreeLoadAdapter pola = new PamOctreeLoadAdapter();
		try {
			pola.loadToRam(new URL("file:/Users/brunsc/test/slice_00000.pam"));
		} catch (MissingTileException e) {
			e.printStackTrace();
		} catch (TileLoadError e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}
	
	public PamOctreeLoadAdapter()
	{
		tileFormat.setIndexStyle(TileIndex.IndexStyle.OCTREE);
	}
	
	@Override
	public TextureData2dGL loadToRam(TileIndex tileIndex) 
	throws TileLoadError, MissingTileException 
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
		
		URL sliceUrl;
		try {
			sliceUrl = new URL(folder, "slice_"+sliceIndexFormat.format(relativeZ)+".pam");
			// System.out.println(topFolder+" : "+folder+" : "+sliceUrl);
		} catch (MalformedURLException e) {
			throw new TileLoadError(e);
		}
		return loadToRam(sliceUrl);
	}

	protected TextureData2dGL loadToRam(URL pamUrl) 
	throws MissingTileException, TileLoadError 
	{
		// Read file into memory first
		byte fileBuffer[];
		try {
			InputStream pamStream0 = new BufferedInputStream(pamUrl.openStream());
			// InputStream pamStream1 = new LZ4BlockInputStream(pamStream0);
			fileBuffer = IOUtils.toByteArray(pamStream0);
			// compressedStream.close();
			pamStream0.close();
		} catch (IOException e) {
			throw new MissingTileException();
		}
		BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(fileBuffer)));
		// Parse header section
		String line;
		try {
			line = reader.readLine();
		} catch (IOException e) {
			throw new TileLoadError(e);
		}
		if (! line.matches("^P7$"))
			throw new TileLoadError("Not a PAM file");
		int w, h, sc, bpp;
		w = h = sc = bpp = 0;
		int usedWidth = 0;
		while (true) {
			try {
				line = reader.readLine();
			} catch (IOException e) {
				throw new TileLoadError(e);
			}
			if (line.matches("ENDHDR"))
				break;
			String tokens[] = line.split(" ");
			if (tokens.length < 2)
				continue;
			if (tokens[0].equals("WIDTH"))
				w = Integer.parseInt(tokens[1]);
			else if (tokens[0].equals("HEIGHT"))
				h = Integer.parseInt(tokens[1]);
			else if (tokens[0].equals("DEPTH"))
				sc = Integer.parseInt(tokens[1]);
			else if (tokens[0].equals("MAXVAL")) {
				int maxVal = Integer.parseInt(tokens[1]);
				if (maxVal > 255)
					bpp = 16;
				else 
					bpp = 8;
			}
			// Look for custom attributes after a comment symbol
			else if (tokens[1].equals("USEDWIDTH")) {
				usedWidth = Integer.parseInt(tokens[2]);
			}
		}
		if (usedWidth == 0)
			usedWidth = w;
		int byteCount = w*h*sc*bpp/8;
		// Compute offset to image data (header size), rather than track it tediously above...
		int headerOffset = fileBuffer.length - byteCount;
		// System.out.println(headerOffset+" : "+measuredHeaderSize);
		// Populate texture
		TextureData2dGL result = new TextureData2dGL();
		result.setBitDepth(bpp);
		result.setWidth(w);
		result.setUsedWidth(usedWidth);
		result.setHeight(h);
		result.setChannelCount(sc);
		// Seems necessary to have backing array actually begin with first pixel.
		byte pixelArray[] = Arrays.copyOfRange(fileBuffer, headerOffset, byteCount+headerOffset);
		ByteBuffer pixelBuffer = ByteBuffer.wrap(pixelArray);
		pixelBuffer.rewind();
		pixelBuffer.order(ByteOrder.BIG_ENDIAN);
		result.setPixels(pixelBuffer);
		result.updateTexImageParams();
		result.setSwapBytes(true); // pam is big endian
		return result;
	}
	
	public URL getTopFolder() {
		return topFolder;
	}

	public void setTopFolder(URL topFolder) 
	throws MissingTileException, TileLoadError
	{
		this.topFolder = topFolder;
		sniffMetadata(topFolder);
	}
	
	private static boolean urlExists(URL url) {
		try {
			url.openStream();
		} catch (IOException e) {
			return false;
		}
		return true;
	}
	
	protected void sniffMetadata(URL topFolderParam) 
	throws MissingTileException, TileLoadError
	{
		final String firstFile = "slice_00000.pam";
		
		// Set some default parameters, to be replaced my measured parameters
		tileFormat.setDefaultParameters();

		// Deduce octree depth from directory structure depth
		int octreeDepth = 0;
		URL deepFolder = topFolderParam;
		URL deepFile;
		try {
			deepFile = new URL(topFolderParam, firstFile);
		} catch (MalformedURLException e1) {
			throw new TileLoadError(e1);
		}
		while (urlExists(deepFile)) {
			octreeDepth += 1;
			URL parentFolder = deepFolder;
			// Check all possible children: some might be empty
			for (int branch = 1; branch <= 8; ++branch) {
				try {
					deepFolder = new URL(parentFolder, ""+branch+"/");
					deepFile = new URL(deepFolder, firstFile);
					// System.out.println(parentFolder+" : "+deepFolder+" : "+deepFile);
				} catch (MalformedURLException e) {
					throw new TileLoadError(e);
				}
				if (urlExists(deepFile))
					break; // found a deeper branch
			}
		}
		int zoomFactor = (int)Math.pow(2, octreeDepth - 1);
		tileFormat.setZoomLevelCount(octreeDepth);
		
		// Count color channels by loading one image slice
		// NOTE - assumes slices start at slice zero
		URL sliceFile;
		try {
			sliceFile = new URL(topFolderParam, firstFile);
		} catch (MalformedURLException e1) {
			throw new TileLoadError(e1);
		}
		TextureData2dGL tex;
		tex = loadToRam(sliceFile);

		tileFormat.setChannelCount(tex.getChannelCount());
		tileFormat.setBitDepth(tex.getBitDepth());
		tileFormat.setIntensityMax((int)Math.pow(2, tex.getBitDepth()));
		tileFormat.setSrgb(false);

		// Count Z slices by counting files
		int z = 0;
		URL testFile;
		try {
			testFile = new URL(topFolderParam, 
					"slice_"+sliceIndexFormat.format(z)+".pam");
		} catch (MalformedURLException e) {
			throw new TileLoadError(e);
		}
		while (urlExists(testFile)) {
			z += 1;
			try {
				testFile = new URL(topFolderParam, 
						"slice_"+sliceIndexFormat.format(z)+".pam");
			} catch (MalformedURLException e) {
				throw new TileLoadError(e);
			}
		}
		int tileSize[] = {tex.getUsedWidth(), tex.getHeight(), z};
		tileFormat.setTileSize(tileSize);
		int volumeSize[] = {
				tileSize[0] * zoomFactor,
				tileSize[1] * zoomFactor,
				tileSize[2] * zoomFactor};
		tileFormat.setVolumeSize(volumeSize);
	}
	
	public static byte[] packChannels(byte unpacked[], int numChannels, int bytesPerIntensity) {
		int totalBytes = unpacked.length;
		int totalGroups = numChannels * bytesPerIntensity;
		int groupBytes = totalBytes / totalGroups;
		// initialize input buffers
		ByteBuffer in[] = new ByteBuffer[totalGroups];
		for (int g = 0; g < totalGroups; ++g) {
			in[g] = ByteBuffer.wrap(unpacked, g*groupBytes, groupBytes);
			// in[g].rewind();
		}
		byte result[] = new byte[totalBytes];
		ByteBuffer out = ByteBuffer.wrap(result);
		out.rewind();
		while (in[0].hasRemaining())
		{
			for (ByteBuffer i : in) {
				out.put(i.get());
			}
		}
		return result;
	}
	
	/**
	 * unpack native image byte order 
	 * 		1a 1b 2a 2b 3a 3b 1a ...
	 * to more compressible
	 * 		1a 1a ... 1b 1b ... 2a 2a ... 2b 2b ... 3a 3a ... 3b 3b
	 * @param packed
	 * @param numChannels
	 * @param bytesPerIntensity
	 * @return
	 */
	public static byte[] unpackChannels(byte packed[], int numChannels, int bytesPerIntensity) {
		int totalBytes = packed.length;
		int totalGroups = numChannels * bytesPerIntensity;
		int groupBytes = totalBytes / totalGroups;
		// initialize output buffers
		ByteBuffer out[] = new ByteBuffer[totalGroups];
		byte result[] = new byte[totalBytes];
		for (int g = 0; g < totalGroups; ++g) {
			out[g] = ByteBuffer.wrap(result, g*groupBytes, groupBytes);
			// out[g].rewind();
		}
		ByteBuffer in = ByteBuffer.wrap(packed);
		in.rewind();
		while (in.hasRemaining())
		{
			for (ByteBuffer o : out) {
				o.put(in.get());
			}
		}
		
		// TODO temporary sanity check
		byte sanity[] = packChannels(result, numChannels, bytesPerIntensity);
		for (int i = 0; i < totalBytes; ++i) {
			byte a = packed[i];
			byte b = sanity[i];
			if (a != b) {
				System.out.println("error");
			}
		}
		
		return result;
	}
}
